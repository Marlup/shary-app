package com.shary.app.services.cloud

import android.util.Log
import com.shary.app.Field
import com.shary.app.core.Session
import com.shary.app.core.enums.StatusDataSentDb
import com.shary.app.services.security.CryptographyManager
import com.shary.app.services.security.CryptographyManager.getPubKeyFromString
import com.shary.app.services.security.securityUtils.SecurityUtils.base64Encode
import com.shary.app.services.security.securityUtils.SecurityUtils.getCurrentUtcTimestamp
import com.shary.app.services.security.securityUtils.SecurityUtils.getTimestampAfterExpiry
import com.shary.app.services.security.securityUtils.SecurityUtils.hashMessageExtended
import com.shary.app.services.security.securityUtils.SecurityUtils.hashMessageToString
import com.shary.app.services.cloud.Constants.ENDPOINT_DELETE_USER
import com.shary.app.services.cloud.Constants.ENDPOINT_GET_PUB_KEY
import com.shary.app.services.cloud.Constants.ENDPOINT_PING
import com.shary.app.services.cloud.Constants.ENDPOINT_SEND_DATA
import com.shary.app.services.cloud.Constants.ENDPOINT_STORE_USER
import com.shary.app.services.cloud.Constants.TIME_ALIVE_DOCUMENT
import com.shary.app.services.cloud.Utils.authHeader
import com.shary.app.services.cloud.Utils.buildPostRequest
import com.shary.app.services.cloud.Utils.evaluateStatusCode
import com.shary.app.services.security.RsaCrypto.encrypt
import com.shary.app.services.security.RsaCrypto.sign
import com.shary.app.utils.UtilsFunctions.makeJsonStringFromFields
import com.shary.app.utils.UtilsFunctions.makeJsonStringFromRequestKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import okhttp3.*
import java.io.IOException

class CloudService(
    private val session: Session,
    private val cryptographyManager: CryptographyManager
): ICloudService {

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
        try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                session.isOnline = response.isSuccessful
            }
        } catch (e: IOException) {
            Log.e("CloudService", "Ping failed: ${e.message}")
            session.isOnline = false
        }
        session.isOnline
    }

    override suspend fun isUserRegistered(user: String):
            Boolean = withContext(Dispatchers.IO)
    {
        val pubKey = getPubKey(hashMessageToString(user))
        return@withContext pubKey != ""
    }

    override suspend fun uploadUser(user: String):
            Pair<Boolean, String> = withContext(Dispatchers.IO) {
        return@withContext runUploadUser(user)
    }

    private suspend fun runUploadUser(user: String):
            Pair<Boolean, String> = withContext(Dispatchers.IO)
    {
        if (!session.isOnline && !sendPing()) return@withContext Pair(false, "")

        val userHash = hashMessageToString(user)
        val pubKey = cryptographyManager.getPubKeyToString()
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
            Log.d("runUploadUser Endpoint: ", ENDPOINT_STORE_USER)

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

                    Pair(true, verificationToken)
                } else {
                    Log.w("CloudService", "${response.code}. Upload user failed: ${jsonBody["message"]}")
                    Pair(false, "")
                }
            }
        } catch (e: IOException) {
            Log.e("CloudService", "Upload user exception: ${e.message}")
            Pair(false, "")
        }
    }
    override suspend fun deleteUser(user: String):
            Boolean = withContext(Dispatchers.IO) {
        return@withContext runDeleteUser(user)
    }

    private suspend fun runDeleteUser(user: String): Boolean {
        if (!session.isOnline && !sendPing()) return false

        val userHash = hashMessageToString(user)
        val (signature, _) = makeCredentials(listOf(userHash))
        val payload = mapOf("user" to userHash, "signature" to signature)
        val request = buildPostRequest(
            ENDPOINT_DELETE_USER,
            payload,
            authHeader(session.authToken)
        )

        return try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                response.code == 200
            }
        } catch (e: IOException) {
            Log.e("CloudService", "Delete user exception: ${e.message}")
            false
        }
    }

    override suspend fun uploadData (
        fields: List<Field>,
        user: String,
        consumers: List<String>,
        onRequest: Boolean
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO) {
        return@withContext runUploadData(fields, user, consumers, onRequest)
    }

    private suspend fun runUploadData(
        fields: List<Field>,
        user: String,
        consumers: List<String>,
        onRequest: Boolean
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO){
        if (!session.isOnline && !sendPing()) return@withContext emptyMap()
        if (fields.isEmpty() || consumers.isEmpty()) return@withContext emptyMap()

        val data = if (onRequest)
            makeJsonStringFromRequestKeys(fields, user)
        else
            makeJsonStringFromFields(fields)

        val userHash = hashMessageToString(user)
        val results = mutableMapOf<String, StatusDataSentDb>()

        consumers.forEach { consumer ->
            val consumerHash: String = hashMessageToString(consumer)
            val consumerPubKey: String = getPubKey(consumerHash)

            if (consumerPubKey == "") {
                results[consumer] = StatusDataSentDb.ERROR
                return@forEach
            }

            val encryptedData = encrypt(data.toByteArray(), getPubKeyFromString(consumerPubKey))
            val payloadData = base64Encode(encryptedData)
            val (signature, verification) = makeCredentials(listOf(userHash, consumerHash, hashMessageToString(data)))

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
                authHeader(session.authToken)
            )

            try {
                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    results[consumer] = evaluateStatusCode(response.code)
                }
            } catch (e: IOException) {
                Log.e("CloudService", "Upload data exception: ${e.message}")
                results[consumer] = StatusDataSentDb.ERROR
            }
        }
        return@withContext results
    }

    override suspend fun getPubKey(userHash: String):
            String = withContext(Dispatchers.IO) {
        return@withContext runGetPubKey(userHash)
    }

    private suspend fun runGetPubKey(userHash: String):
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
            Log.e("CloudService", "Get pubKey exception: ${e.message}")
            return@withContext ""
        }
    }

    private fun makeCredentials(fields: List<String>):
            Pair<String, String> {
        val (rawHash, verification) = hashMessageExtended(fields.joinToString("."))
        val signature = sign(rawHash)
        return Pair(base64Encode(signature), verification)
    }
}
