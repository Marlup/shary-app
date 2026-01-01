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
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.valueobjects.DataPayload
import com.shary.app.core.domain.types.valueobjects.DeleteUserPayload
import com.shary.app.core.domain.types.valueobjects.UserPayload
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_DELETE_USER
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_FETCH_PAYLOAD
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_FETCH_REQUEST
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_GET_PUB_KEY
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_PING
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_UPLOAD_PAYLOAD
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_UPLOAD_REQUEST
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_UPLOAD_USER
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_MAIN_ENTRYPOINT
import com.shary.app.utils.Functions.buildJsonStringFromFields
import com.shary.app.utils.Functions.makeJsonStringFromRequestKeys
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder


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
                //username = session.uid,
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
        val url: String = FIREBASE_MAIN_ENTRYPOINT + FIREBASE_ENDPOINT_PING

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            OkHttpClient().newCall(request).execute().use { response ->
                cloudState.value.isOnline = response.isSuccessful
                response.isSuccessful
            }
        } catch (e: IOException) {
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

        val payload = UserPayload(
            user = usernameHash,
            creationAt = getCurrentUtcTimestamp(),
            expiresAt = getTimestampAfterExpiry(),
            pubkey = pubKey,
            verification = verification,
            signature = signature,
            fcmToken = cloudState.value.fcmToken
        )

        Log.d("CloudServiceImpl - doUploadUser", "Username $usernameHash; " +
                    "AuthToken() - ${cloudState.value.token}")

        val url = FIREBASE_MAIN_ENTRYPOINT + FIREBASE_ENDPOINT_UPLOAD_USER
        val request = buildPostRequest(
            url,
            Json.encodeToString(UserPayload.serializer(), payload),
            authBearerHeader(cloudState.value.token)
        )

        Log.i("Authbearer", "authBearerHeader(cloudState.value.token): ${authBearerHeader(cloudState.value.token).toString()}")

        Log.i("CloudServiceImpl", "doUploadUser() - request: $request; " +
                                                               "body: ${request.body.toString()}")

        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                val bodyString = response.body.string().orEmpty()
                Log.d("CloudService - doUploadUser", "Upload user response message : $bodyString")
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
        val payload = DeleteUserPayload(usernameHash, signature)

        val url = Constants.FIREBASE_MAIN_ENTRYPOINT + FIREBASE_ENDPOINT_DELETE_USER
        val request = buildPostRequest(
            url,
            Json.encodeToString(DeleteUserPayload.serializer(), payload),
            authBearerHeader(cloudState.value.token)
        )
        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response -> response.code == 200 }
        } catch (e: IOException) {
            Log.e("CloudServiceImpl", "Delete user exception: ${e.message}")
            false
        }
    }

    /**
     * Uploads encrypted data fields to one or more recipients.
     */
    override suspend fun uploadData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) return@withContext emptyMap()
        if (isEmptyArguments(fields, recipients)) return@withContext emptyMap()

        doUploadData(fields, owner, recipients)
    }

    private suspend fun doUploadData(fields: List<FieldDomain>, owner: UserDomain, recipients: List<UserDomain>) =
        withContext(Dispatchers.IO) {
            val dataJson = buildJsonStringFromFields(fields)
            val safeOwnerUsername = safeGetUser(owner.username)
            val recipients = recipients.map { it.username }

            buildLoad(dataJson, safeOwnerUsername, recipients, isRequest=false)
        }

    /**
     * Uploads encrypted request to one or more recipients.
     */
    override suspend fun uploadRequest(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) return@withContext emptyMap()
        if (isEmptyArguments(fields, recipients)) return@withContext emptyMap()

        doUploadRequest(fields, owner, recipients)
    }

    private suspend fun doUploadRequest(fields: List<FieldDomain>, owner: UserDomain, recipients: List<UserDomain>) =
        withContext(Dispatchers.IO) {
            val safeOwnerUsername = safeGetUser(owner.username)
            val dataRequestJson = makeJsonStringFromRequestKeys(fields, safeOwnerUsername)
            val recipients = recipients.map { it.username }

            buildLoad(dataRequestJson, safeOwnerUsername, recipients, isRequest=true)
        }

    /**
     * Core logic for encrypting data or request-data for each recipientUsername and sending via backend.
     */
    private suspend fun buildLoad(data: String, username: String, recipients: List<String>, isRequest: Boolean) =
        withContext(Dispatchers.IO) {
            ensureAuth()
            val safeUsername = safeGetUser(username)
            val userHash = hashMessageB64(safeUsername)
            val results = mutableMapOf<String, StatusDataSentDb>()

            val client = OkHttpClient()
            recipients.forEach { recipientUsername ->
                val recipientHash = hashMessageB64(recipientUsername)
                val recipientPubKeyB64 = getPubKey(recipientHash)
                if (recipientPubKeyB64.isEmpty()) {
                    results[recipientUsername] = StatusDataSentDb.ERROR
                    return@forEach
                }
                val recipientPubKey = base64Decode(recipientPubKeyB64)
                val aad = "shary:$userHash:$recipientHash".toByteArray(Charsets.UTF_8)

                val encryptedData = cryptographyManager.encryptWithPeerPublic(
                    data.toByteArray(),
                    recipientPubKey,
                    aad
                )
                val payloadData = base64Encode(encryptedData)
                val (signature, verification) =
                    makeCredentials(listOf(userHash, recipientHash, hashMessageB64(data)))

                val payload = DataPayload(
                    user = userHash,
                    recipient = recipientHash,
                    creationAt = getCurrentUtcTimestamp(),
                    expiresAt = getTimestampAfterExpiry(),
                    data = payloadData,
                    verification = verification,
                    signature = signature
                )

                val url = FIREBASE_MAIN_ENTRYPOINT + when {
                    isRequest -> FIREBASE_ENDPOINT_UPLOAD_REQUEST
                    else -> FIREBASE_ENDPOINT_UPLOAD_PAYLOAD
                }

                val request = buildPostRequest(
                    url,
                    Json.encodeToString(DataPayload.serializer(), payload),
                    authBearerHeader(cloudState.value.token)
                )
                try {
                    client.newCall(request).execute().use { response ->
                        results[recipientUsername] = evaluateStatusCode(response.code)
                    }
                } catch (e: IOException) {
                    Log.e("CloudServiceImpl", "Upload data exception: ${e.message}")
                    results[recipientUsername] = StatusDataSentDb.ERROR
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

        val encodedUser = URLEncoder.encode(usernameHash, Charsets.UTF_8.name())
        val url = "$FIREBASE_MAIN_ENTRYPOINT$FIREBASE_ENDPOINT_GET_PUB_KEY?user=$encodedUser"

        val request = Request.Builder()
            .url(url)
            .headers(authBearerHeader(cloudState.value.token).toHeaders())
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

    private fun isEmptyArguments(fields: List<FieldDomain>, recipients: List<UserDomain>) =
        fields.isEmpty() || recipients.isEmpty()

    /** Returns provided username or fallback to Session.ownerUsername. */
    private fun safeGetUser(username: String?): String =
        if (username.isNullOrEmpty()) cloudState.value.username else username

    suspend fun getAuthToken(): String? {
        val username = FirebaseAuth.getInstance().currentUser ?: return null
        return username.getIdToken(true).await().token
    }

    /**
     * Fetches encrypted data from Firebase for the current user.
     * Decrypts the data and returns it as a JSON string.
     * Note: Firebase returns the MOST RECENT payload if multiple exist.
     */
    override suspend fun fetchPayloadData(username: String): Result<String> {
        return fetchdData(username, isRequest = false)
    }
    override suspend fun fetchRequestData(username: String): Result<String> {
        return fetchdData(username, isRequest = true)
    }

    suspend fun fetchdData(username: String, isRequest: Boolean): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            // Ensure we have a fresh auth token
            ensureAuth()

            // Try to refresh the token to ensure it's valid
            try {
                refreshIdToken()
            } catch (e: Exception) {
                Log.w("CloudServiceImpl", "Could not refresh token: ${e.message}")
            }

            if (!isCloudReachable()) {
                throw IOException("Cloud service is not reachable")
            }

            val safeUsername = safeGetUser(username)
            val recipientHash = hashMessageB64(safeUsername)
            val encodedRecipient = URLEncoder.encode(recipientHash, Charsets.UTF_8.name())

            // Note: The parameter name is "user" but it represents the recipient
            val url = FIREBASE_MAIN_ENTRYPOINT + when {
                isRequest -> FIREBASE_ENDPOINT_FETCH_REQUEST
                else -> FIREBASE_ENDPOINT_FETCH_PAYLOAD
            } + "?user=$encodedRecipient"

            val request = Request.Builder()
                .url(url)
                .get()
                .headers(authBearerHeader(cloudState.value.token).toHeaders())
                .build()

            Log.i("CloudServiceImpl", "fetchData() - URL: $url")
            Log.i("CloudServiceImpl", "fetchData() - recipient hash: $recipientHash")
            Log.i("CloudServiceImpl", "fetchData() - headers: ${request.headers}")
            Log.i("CloudServiceImpl", "fetchData() - token: ${cloudState.value.token?.take(20)}...")

            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    Log.e("CloudServiceImpl", "Failed to fetch data: ${response.code} - $errorBody")
                    throw IOException("Failed to fetch data: ${response.code} - $errorBody")
                }

                val bodyString = response.body.string().orEmpty()
                Log.d("CloudServiceImpl - fetchData", "Fetch response: $bodyString")

                val jsonBody = Json.parseToJsonElement(bodyString).jsonObject

                // The response contains a "payload" array
                val payloadArray = jsonBody["payload"]?.jsonArray
                    ?: throw IllegalStateException("No payload array in response")

                if (payloadArray.isEmpty()) {
                    throw IllegalStateException("No payloads available")
                }

                // Get the most recent payload (last in the array)
                val latestPayload = payloadArray.last().jsonObject

                val encryptedDataB64 = latestPayload["data"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("No data field in payload")
                val senderHash = latestPayload["user"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("No user field in payload")

                Log.d("CloudServiceImpl", "Processing payload from sender: $senderHash")

                // Get sender's public key
                val senderPubKeyB64 = getPubKey(senderHash)
                if (senderPubKeyB64.isEmpty()) {
                    throw IllegalStateException("Cannot retrieve sender's public key")
                }

                val senderPubKey = base64Decode(senderPubKeyB64)
                val encryptedData = base64Decode(encryptedDataB64)
                val aad = "shary:$senderHash:$recipientHash".toByteArray(Charsets.UTF_8)

                // Decrypt the data
                val decryptedBytes = cryptographyManager.decryptFromPeerPublic(
                    encryptedData,
                    senderPubKey,
                    aad
                )

                val decryptedString = decryptedBytes.toString(Charsets.UTF_8)
                Log.d("CloudServiceImpl", "Successfully decrypted data")
                decryptedString
            }
        }
    }

    private fun setIsOnlineInCLoud(v: Boolean) { cloudState.value.isOnline = v }
}
