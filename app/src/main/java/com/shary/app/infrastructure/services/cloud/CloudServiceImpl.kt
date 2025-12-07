package com.shary.app.infrastructure.services.cloud

import android.util.Log
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Decode
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Encode
import com.shary.app.infrastructure.security.helper.SecurityUtils.getCurrentUtcTimestamp
import com.shary.app.infrastructure.security.helper.SecurityUtils.getTimestampAfterExpiry
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessage
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessageB64
import com.shary.app.infrastructure.services.cloud.Utils.authBearerHeader
import com.shary.app.infrastructure.services.cloud.Utils.buildPostRequest
import com.shary.app.infrastructure.services.cloud.Utils.evaluateStatusCode
import com.google.firebase.auth.FirebaseAuth
import com.shary.app.core.domain.interfaces.states.CloudState
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_DELETE_USER
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_GET_PUB_KEY
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_PING
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_SEND_DATA
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_SEND_USER
import com.shary.app.infrastructure.services.cloud.Constants.TIME_ALIVE_FIREBASE_DOCUMENT
import com.shary.app.utils.Functions.buildJsonStringFromFields
import com.shary.app.utils.Functions.makeJsonStringFromRequestKeys
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * CloudServiceImpl
 *
 * Concrete implementation of CloudService.
 * - Handles communication with Firebase-backed endpoints (ping, upload, delete, etc.).
 * - Injects FirebaseAuth to establish and maintain anonymous sessions.
 * - Always attaches a valid ID token (Authorization: Bearer <token>) when required.
 * - Encrypts payloads end-to-end (before leaving the device) using CryptographyManager.
 */
class CloudServiceImpl @Inject constructor(
    private val cryptographyManager: CryptographyManager,
    private val firebaseAuth: FirebaseAuth
) : CloudService {

    private val authAdapter = FirebaseAnonymousAuthAdapter(firebaseAuth)
    override var cloudState = MutableStateFlow(CloudState())

    // -------------------- AUTH --------------------

    /**
     * Ensures that the client has a valid ID token before making requests.
     * If the Session has no token (or expired), refreshes it via FirebaseAuth.
     */
    private suspend fun ensureAuth(): Unit = withContext(Dispatchers.IO) {
        if (cloudState.value.token.isNullOrEmpty()) {
            val session = authAdapter.ensureSession()
            cloudState.value = cloudState.value.copy(
                username = session.uid,
                token = session.idToken
            )
            setIsOnlineInCLoud(true)
        } else setIsOnlineInCLoud(true)
    }

    override suspend fun ensureAnonymousSession(): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val session = authAdapter.ensureSession()
            cloudState.value = cloudState.value.copy(
                username = session.uid,
                token = session.idToken
            )
            session.uid
        }
    }

    override suspend fun refreshIdToken(): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val token = authAdapter.refreshToken()
            cloudState.value = cloudState.value.copy(token = token)
            token
        }
    }

    override suspend fun signOutCloud(): Result<Unit> = runCatching {
        authAdapter.signOut()
        cloudState.value = cloudState.value.copy(
            username = "",
            token = "",
            isOnline = false
        )
    }

    // -------------------- EXISTING FUNCTIONALITY --------------------

    /**
     * Sends a "ping" to the cloud service to check reachability.
     * Updates Session.isOnline based on response.
     */
    override suspend fun sendPing(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(FIREBASE_ENDPOINT_PING).get().build()
        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                cloudState.value.isOnline = response.isSuccessful
                response.code
                Log.e("CloudServiceImpl", "Ping sent. " +
                        "online status: ${cloudState.value.isOnline}; " +
                        "status code: ${response.code};" +
                        "url: $FIREBASE_ENDPOINT_PING"
                )
                response.isSuccessful
            }
        } catch (e: IOException) {
            Log.e("CloudServiceImpl", "Ping failed: ${e.message}")
            cloudState.value.isOnline = false
            false
        }
    }

    /**
     * Checks if a user is registered by verifying if their public key exists.
     */
    override suspend fun isUserRegisteredInCloud(username: String): Boolean = withContext(Dispatchers.IO) {
        ensureAuth()
        val safeUser = safeGetUser(username)
        val pubKey = getPubKey(hashMessageB64(safeUser))
        return@withContext pubKey.isNotEmpty()
    }

    /**
     * Uploads a new user identity (with keys) to the backend.
     */
    override suspend fun uploadUser(username: String): String = withContext(Dispatchers.IO) {
        ensureAuth()
        val safeUser = safeGetUser(username)
        return@withContext doUploadUser(safeUser)
    }

    /**
     * Performs actual user upload: hashes username, attaches pubKey, signature, and verification.
     */
    private suspend fun doUploadUser(username: String): String = withContext(Dispatchers.IO) {
        if (!isCloudReachable()) return@withContext ""
        ensureAuth()

        val safeUsername = safeGetUser(username)
        val usernameHash = hashMessageB64(safeUsername)
        val pubKey = base64Encode(cryptographyManager.getKexPublic())
        val (signature, verification) = makeCredentials(listOf(usernameHash, pubKey))

        val payload = mutableMapOf(
            "user" to usernameHash,
            "creation_at" to getCurrentUtcTimestamp().toString(),
            "expires_at" to getTimestampAfterExpiry(TIME_ALIVE_FIREBASE_DOCUMENT).toString(),
            "pubkey" to pubKey,
            "verification" to verification,
            "signature" to signature
        )

        // If we already cached a token in CloudState, attach it
        cloudState.value.fcmToken?.let { payload["fcmToken"] = it }

        Log.d("CloudServiceImpl - doUploadUser", "Username $usernameHash; " +
                    "AuthToken() - ${cloudState.value.token}")

        val request = buildPostRequest(
            FIREBASE_ENDPOINT_SEND_USER,
            payload,
            authBearerHeader(cloudState.value.token)
        )

        Log.i("CloudServiceImpl", "doUploadUser() - request: $request; " +
                                                               "body: ${request.body.toString()}")

        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                Log.d("CloudService - doUploadUser", "Upload user response message : ${response.body?.string()}")
                val bodyString = response.body.string().orEmpty()
                val jsonBody = Json.parseToJsonElement(bodyString).jsonObject
                if (response.code == 200) {
                    jsonBody["token"]?.jsonPrimitive?.content.orEmpty()
                } else {
                    Log.w("CloudServiceImpl",
                        "${response.code}. Upload user failed: ${jsonBody["message"]}")
                    ""
                }
            }
        } catch (e: Exception) {
            Log.e("CloudServiceImpl", "doUploadUser exception: ${e.message}")
            ""
        }
    }

    /**
     * Deletes a user by sending signature + hash.
     */
    override suspend fun deleteUser(username: String): Boolean = withContext(Dispatchers.IO) {
        ensureAuth()
        val safeUsername = safeGetUser(username)
        return@withContext doDeleteUser(safeUsername)
    }

    private suspend fun doDeleteUser(username: String): Boolean = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) return@withContext false
        val safeUsername = safeGetUser(username)
        val usernameHash = hashMessageB64(safeUsername)
        val (signature, _) = makeCredentials(listOf(usernameHash))
        val payload = mapOf("user" to usernameHash, "signature" to signature)
        val request = buildPostRequest(FIREBASE_ENDPOINT_DELETE_USER, payload, authBearerHeader(cloudState.value.token))
        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response -> response.code == 200 }
        } catch (e: IOException) {
            Log.e("CloudServiceImpl", "Delete user exception: ${e.message}")
            false
        }
    }

    /**
     * Uploads encrypted data (fields or request) to one or more consumers.
     */
    override suspend fun uploadData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        consumers: List<UserDomain>,
        isRequest: Boolean
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) return@withContext emptyMap()
        if (isEmptyArguments(fields, consumers)) return@withContext emptyMap()
        if (isRequest) doUploadRequest(fields, owner, consumers)
        else doUploadData(fields, owner, consumers)
    }

    override suspend fun updateUserFcmToken(token: String): Boolean = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) return@withContext false

        val safeUsername = safeGetUser(cloudState.value.username)
        val usernameHash = hashMessageB64(safeUsername)
        val (signature, verification) = makeCredentials(listOf(usernameHash, token))

        val payload = mapOf(
            "user" to usernameHash,
            "fcmToken" to token,
            "signature" to signature,
            "verification" to verification,
            "updated_at" to getCurrentUtcTimestamp().toString()
        )

        val request = buildPostRequest(
            FIREBASE_ENDPOINT_SEND_USER, // reuse same endpoint!
            payload,
            authBearerHeader(cloudState.value.token)
        )

        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("CloudServiceImpl", "FCM token updated successfully")
                    cloudState.value.fcmToken = token
                    true
                } else {
                    Log.w("CloudServiceImpl", "Failed to update FCM token: ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("CloudServiceImpl", "updateUserFcmToken exception: ${e.message}")
            false
        }
    }

    private suspend fun doUploadData(fields: List<FieldDomain>, owner: UserDomain, consumers: List<UserDomain>) =
        withContext(Dispatchers.IO) {
            val dataJson = buildJsonStringFromFields(fields)
            val safeOwnerUsername = safeGetUser(owner.username)
            val usernames = consumers.map { it.username }

            buildLoad(dataJson, safeOwnerUsername, usernames)
        }

    private suspend fun doUploadRequest(fields: List<FieldDomain>, owner: UserDomain, consumers: List<UserDomain>) =
        withContext(Dispatchers.IO) {
            val safeOwnerUsername = safeGetUser(owner.username)
            val dataRequestJson = makeJsonStringFromRequestKeys(fields, safeOwnerUsername)
            val usernames = consumers.map { it.username }

            buildLoad(dataRequestJson, safeOwnerUsername, usernames)
        }

    /**
     * Core logic for encrypting data for each consumerUsername and sending via backend.
     */
    private suspend fun buildLoad(data: String, username: String, consumers: List<String>) =
        withContext(Dispatchers.IO) {
            ensureAuth()
            val safeUsername = safeGetUser(username)
            val userHash = hashMessageB64(safeUsername)
            val results = mutableMapOf<String, StatusDataSentDb>()

            val client = OkHttpClient()
            consumers.forEach { consumerUsername ->
                val consumerHash = hashMessageB64(consumerUsername)
                val consumerPubKeyB64 = getPubKey(consumerHash)
                if (consumerPubKeyB64.isEmpty()) {
                    results[consumerUsername] = StatusDataSentDb.ERROR
                    return@forEach
                }
                val consumerPubKey = base64Decode(consumerPubKeyB64)
                val aad = "shary:$userHash:$consumerHash".toByteArray(Charsets.UTF_8)

                val encryptedData = cryptographyManager.encryptWithPeerPublic(
                    data.toByteArray(),
                    consumerPubKey,
                    aad
                )
                val payloadData = base64Encode(encryptedData)
                val (signature, verification) =
                    makeCredentials(listOf(userHash, consumerHash, hashMessageB64(data)))

                val payload = mapOf(
                    "user" to userHash,
                    "consumer" to consumerHash,
                    "creation_at" to getCurrentUtcTimestamp().toString(),
                    "expires_at" to getTimestampAfterExpiry(TIME_ALIVE_FIREBASE_DOCUMENT).toString(),
                    "data" to payloadData,
                    "verification" to verification,
                    "signature" to signature
                )
                val request = buildPostRequest(FIREBASE_ENDPOINT_SEND_DATA, payload, authBearerHeader(cloudState.value.token))
                try {
                    client.newCall(request).execute().use { response ->
                        results[consumerUsername] = evaluateStatusCode(response.code)
                    }
                } catch (e: IOException) {
                    Log.e("CloudServiceImpl", "Upload data exception: ${e.message}")
                    results[consumerUsername] = StatusDataSentDb.ERROR
                }
            }
            results
        }

    /**
     * Retrieves a public key for a given user hash.
     */
    override suspend fun getPubKey(usernameHash: String): String = withContext(Dispatchers.IO) {
        Log.i("CloudServiceImpl", "Launching getPubKey() for usernameHash: $usernameHash")
        ensureAuth()
        val request = Request.Builder()
            .url("$FIREBASE_ENDPOINT_GET_PUB_KEY?user=$usernameHash")
            .addHeader("Authorization", "Bearer ${cloudState.value.token}")
            .build()
        Log.i("CloudServiceImpl", "getPubKey() - request: $request; " +
                "headers: ${request.headers} " +
                "current cloudState: ${cloudState.value.token} "
        )
        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string().orEmpty()
                    Json.parseToJsonElement(body).jsonObject["pubkey"]?.jsonPrimitive?.content.orEmpty()
                } else ""
            }
        } catch (e: IOException) {
            Log.e("CloudServiceImpl", "Get pubKey exception: ${e.message}; url: ${request.url}")
            ""
        }
    }

    /**
     * Creates signature + verification pair for a canonical list of fields.
     */
    private fun makeCredentials(fields: List<String>): Pair<String, String> {
        val canonical = fields.joinToString(".")
        val rawHash = hashMessage(canonical)
        val verificationB64 = base64Encode(rawHash)
        val signatureB64 = base64Encode(cryptographyManager.signDetached(rawHash))
        return signatureB64 to verificationB64
    }

    /** Check if backend is reachable (ping + cached state). */
    private suspend fun isCloudReachable(): Boolean =
        cloudState.value.isOnline || sendPing()

    private fun isEmptyArguments(fields: List<FieldDomain>, consumers: List<UserDomain>) =
        fields.isEmpty() || consumers.isEmpty()

    /** Returns provided username or fallback to Session.ownerUsername. */
    private fun safeGetUser(username: String?): String =
        if (username.isNullOrEmpty()) cloudState.value.username else username

    suspend fun getAuthToken(): String? {
        val username = FirebaseAuth.getInstance().currentUser ?: return null
        return username.getIdToken(true).await().token
    }

    private fun setIsOnlineInCLoud(v: Boolean) { cloudState.value.isOnline = v }
}
