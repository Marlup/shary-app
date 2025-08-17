package com.shary.app.infrastructure.services.cloud

import android.util.Log
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.session.Session
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Decode
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Encode
import com.shary.app.infrastructure.security.helper.SecurityUtils.getCurrentUtcTimestamp
import com.shary.app.infrastructure.security.helper.SecurityUtils.getTimestampAfterExpiry
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessage
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessageB64
import com.shary.app.infrastructure.services.cloud.Constants.ENDPOINT_DELETE_USER
import com.shary.app.infrastructure.services.cloud.Constants.ENDPOINT_GET_PUB_KEY
import com.shary.app.infrastructure.services.cloud.Constants.ENDPOINT_PING
import com.shary.app.infrastructure.services.cloud.Constants.ENDPOINT_SEND_DATA
import com.shary.app.infrastructure.services.cloud.Constants.ENDPOINT_STORE_USER
import com.shary.app.infrastructure.services.cloud.Constants.TIME_ALIVE_DOCUMENT
import com.shary.app.infrastructure.services.cloud.Utils.authHeader
import com.shary.app.infrastructure.services.cloud.Utils.buildPostRequest
import com.shary.app.infrastructure.services.cloud.Utils.evaluateStatusCode
import com.shary.app.utils.Functions.buildJsonStringFromFields
import com.shary.app.utils.Functions.makeJsonStringFromRequestKeys
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import okhttp3.*
import java.io.IOException

class CloudServiceImpl @Inject constructor(
    private val session: Session,
    private val cryptographyManager: CryptographyManager
): CloudService {

    //private val client = OkHttpClient()

    /*llamada de red en el hilo principal (Main/UI Thread), lo cual está prohibido en Android
    desde Android 3.0 (Honeycomb) porque puede bloquear la interfaz de usuario.
    */
    override suspend fun sendPing(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(ENDPOINT_PING)
            .get()
            .build()

        //return@withContext try {
        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                session.setOwnerIsOnline(response.isSuccessful)
                response.isSuccessful
            }
        } catch (e: IOException) {
            Log.e("CloudServiceImpl", "Ping failed: ${e.message}")
            session.setOwnerIsOnline(false)
            false
        }
    }

    override suspend fun isUserRegistered(user: String):
            Boolean = withContext(Dispatchers.IO)
    {
        val pubKey = getPubKey(hashMessageB64(user))
        return@withContext pubKey != ""
    }

    override suspend fun uploadUser(user: String):
            String = withContext(Dispatchers.IO) {
        return@withContext doUploadUser(user)
    }

    private suspend fun doUploadUser(user: String): String = withContext(Dispatchers.IO) {
        if (!isCloudReachable()) return@withContext ""

        val userHash = hashMessageB64(user)
        val pubKey = base64Encode(cryptographyManager.getKexPublic())
        val (signature, verification) = makeCredentials(listOf(userHash, pubKey))

        val payload = mapOf(
            "user" to userHash,
            "creation_at" to getCurrentUtcTimestamp().toString(),
            "expires_at" to getTimestampAfterExpiry(TIME_ALIVE_DOCUMENT).toString(),
            "pubkey" to pubKey,
            "verification" to verification,
            "signature" to signature
        )

        val request = buildPostRequest(ENDPOINT_STORE_USER, payload)

        return@withContext try {
            Log.d("doUploadUser Endpoint: ", ENDPOINT_STORE_USER)

            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                val jsonBody = Json.parseToJsonElement(
                    response.body?.string() ?: "")
                    .jsonObject
                if (response.code == 200) {
                    val verificationToken = jsonBody["token"]
                        ?.jsonPrimitive
                        ?.content
                        .toString()

                    verificationToken
                } else {
                    Log.w("CloudServiceImpl", "${response.code}. Upload user failed: ${jsonBody["message"]}")
                    ""
                }
            }
        } catch (e: IOException) {
            Log.e("CloudServiceImpl", "Upload user exception: ${e.message}")
            ""
        }
    }
    override suspend fun deleteUser(user: String):
            Boolean = withContext(Dispatchers.IO) {
        return@withContext doDeleteUser(user)
    }

    private suspend fun doDeleteUser(user: String): Boolean {
        if (!isCloudReachable()) return false

        val userHash = hashMessageB64(user)
        val (signature, _) = makeCredentials(listOf(userHash))
        val payload = mapOf("user" to userHash, "signature" to signature)
        val request = buildPostRequest(
            ENDPOINT_DELETE_USER,
            payload,
            authHeader(session.getOwnerAuthToken())
        )

        return try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                response.code == 200
            }
        } catch (e: IOException) {
            Log.e("CloudServiceImpl", "Delete user exception: ${e.message}")
            false
        }
    }

    override suspend fun uploadData (
        fields: List<FieldDomain>,
        user: String,
        consumers: List<String>,
        isRequest: Boolean
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO) {
        if (!isCloudReachable()) return@withContext emptyMap()
        if (isEmptyArguments(fields, consumers)) return@withContext emptyMap()

        if (isRequest)
            return@withContext doUploadRequest(fields, user, consumers)
        else 
            return@withContext doUploadData(fields, user, consumers)
    }
    
    private suspend fun doUploadData(
        fields: List<FieldDomain>,
        user: String,
        consumers: List<String>,
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO){
        val dataJson = buildJsonStringFromFields(fields)
        return@withContext buildLoad(dataJson, user, consumers)
    }
    
    private suspend fun doUploadRequest(
        fields: List<FieldDomain>,
        user: String,
        consumers: List<String>,
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO){
        val dataRequestJson = makeJsonStringFromRequestKeys(fields, user)
        return@withContext buildLoad(dataRequestJson, user, consumers)
    }
    
    private suspend fun buildLoad(
        data: String,
        user: String,
        consumers: List<String>,
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO){
        // Owner identifiers/hashes
        val userHash = hashMessageB64(user)

        val username = session.getOwnerUsername()
        val safePassword = session.getOwnerSafePassword() // base64 or the format you use
        val appId = cryptographyManager.getAppId()

        val results = mutableMapOf<String, StatusDataSentDb>()


        consumers.forEach { consumer ->


            // 1) Resolve consumer pubkey (X25519 PUBLIC) and validate
            val consumerHash: String = hashMessageB64(consumer)
            val consumerPubKeyB64: String = getPubKey(consumerHash)
            val consumerPubKey: ByteArray = base64Decode(consumerPubKeyB64)

            if (consumerPubKeyB64.isEmpty()) {
                results[consumer] = StatusDataSentDb.ERROR
                return@forEach
            }



            // 2) Build AAD (bind ciphertext to identities) and a fresh nonce for sender’s ephemeral
            val aad = "shary:$userHash:$consumerHash".toByteArray(Charsets.UTF_8)
            //val nonce = java.security.SecureRandom().generateSeed(32)

            val encryptedData = cryptographyManager.encryptWithPeerPublic(
                data.toByteArray(),
                consumerPubKey,
                aad
            )

            val payloadData = base64Encode(encryptedData)
            val (signature, verification) = makeCredentials(listOf(userHash, consumerHash, hashMessageB64(data)))

            val payload = mapOf(
                "user" to userHash,
                "consumer" to consumerHash,
                "creation_at" to getCurrentUtcTimestamp().toString(),
                "expires_at" to getTimestampAfterExpiry(TIME_ALIVE_DOCUMENT).toString(),
                "data" to payloadData,
                "verification" to verification,
                "signature" to signature
            )

            val request = buildPostRequest(
                ENDPOINT_SEND_DATA,
                payload,
                authHeader(session.getOwnerAuthToken())
            )

            try {
                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    results[consumer] = evaluateStatusCode(response.code)
                }
            } catch (e: IOException) {
                Log.e("CloudServiceImpl", "Upload data exception: ${e.message}")
                results[consumer] = StatusDataSentDb.ERROR
            }
        }
        return@withContext results
    }

    override suspend fun getPubKey(userHash: String):
            String = withContext(Dispatchers.IO) {
        return@withContext doGetPubKey(userHash)
    }

    private suspend fun doGetPubKey(userHash: String):
            String = withContext(Dispatchers.IO) {
        Log.d("GetPubKey Endpoint: ", "$ENDPOINT_GET_PUB_KEY?user=$userHash")
        val request = Request.Builder()
            .url("$ENDPOINT_GET_PUB_KEY?user=$userHash")
            .addHeader("Authorization", "Bearer $session.authToken")
            .build()

        return@withContext try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val pubKey = Json.parseToJsonElement(bodyString)
                        .jsonObject["pubkey"]
                        ?.jsonPrimitive
                        ?.content
                        .toString()
                    pubKey
                } else {
                    ""
                }
            }
        } catch (e: IOException) {
            Log.e("CloudServiceImpl", "Get pubKey exception: ${e.message}")
            return@withContext ""
        }
    }

    private fun makeCredentials(fields: List<String>): Pair<String, String> {
        val canonical = fields.joinToString(".")
        val rawHash = hashMessage(canonical)

        val verificationB64 = base64Encode(rawHash)           // server can recompute this
        val signatureB64 = base64Encode(cryptographyManager.signDetached(rawHash))        // proof (client private key)

        return signatureB64 to verificationB64
    }

    private suspend fun isCloudReachable(): Boolean =
        session.getOwnerIsOnline() || sendPing()

    private fun isEmptyArguments(
        fields: List<FieldDomain>,
        consumers: List<String>
    ) = fields.isEmpty() || consumers.isEmpty()
}
