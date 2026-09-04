package com.shary.app.infrastructure.connector.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class RemoteMcpSecureStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveCredential(credential: RemoteGrantCredential) {
        val payload = JSONObject()
            .put("grant_id", credential.grantId)
            .put("agent_profile_id", credential.agentProfileId)
            .put("client_id", credential.clientId)
            .put("session_id", credential.sessionId)
            .put("access_token", credential.accessToken)
            .put("refresh_secret", credential.refreshSecret)
            .put("expires_at", credential.expiresAt)
            .put("grant_expires_at", credential.grantExpiresAt)
        preferences.edit().putString(credentialKey(credential.grantId), encrypt(payload.toString())).apply()
    }

    @Synchronized
    fun credential(grantId: String): RemoteGrantCredential? {
        val encoded = preferences.getString(credentialKey(grantId), null) ?: return null
        return runCatching {
            val payload = JSONObject(decrypt(encoded))
            RemoteGrantCredential(
                grantId = payload.getString("grant_id"),
                agentProfileId = payload.getString("agent_profile_id"),
                clientId = payload.getString("client_id"),
                sessionId = payload.getString("session_id"),
                accessToken = payload.getString("access_token"),
                refreshSecret = payload.getString("refresh_secret"),
                expiresAt = payload.getLong("expires_at"),
                grantExpiresAt = payload.getLong("grant_expires_at")
            )
        }.getOrNull()
    }

    @Synchronized
    fun credentials(): List<RemoteGrantCredential> =
        preferences.all.keys
            .filter { it.startsWith(CREDENTIAL_PREFIX) }
            .mapNotNull { credential(it.removePrefix(CREDENTIAL_PREFIX)) }

    @Synchronized
    fun removeCredential(grantId: String) {
        preferences.edit().remove(credentialKey(grantId)).apply()
    }

    @Synchronized
    fun savePending(operation: PendingRemoteOperation) {
        val payload = JSONObject()
            .put("operation_id", operation.operationId)
            .put("grant_id", operation.grantId)
            .put("tool_name", operation.toolName)
            .put("command_json", operation.commandJson)
            .put("expires_at", operation.expiresAt)
        preferences.edit().putString(pendingKey(operation.operationId), encrypt(payload.toString())).apply()
    }

    @Synchronized
    fun pendingOperations(): List<PendingRemoteOperation> {
        val now = System.currentTimeMillis() / 1000
        return preferences.all.mapNotNull { (key, value) ->
            if (!key.startsWith(PENDING_PREFIX) || value !is String) return@mapNotNull null
            runCatching {
                val payload = JSONObject(decrypt(value))
                PendingRemoteOperation(
                    operationId = payload.getString("operation_id"),
                    grantId = payload.getString("grant_id"),
                    toolName = payload.getString("tool_name"),
                    commandJson = payload.getString("command_json"),
                    expiresAt = payload.getLong("expires_at")
                )
            }.getOrNull()
        }.filter { it.expiresAt > now }.sortedBy { it.expiresAt }
    }

    @Synchronized
    fun removePending(operationId: String) {
        preferences.edit().remove(pendingKey(operationId)).apply()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_LENGTH_BYTES) { "Invalid encrypted MCP credential." }
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val encrypted = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun credentialKey(grantId: String) = "$CREDENTIAL_PREFIX$grantId"
    private fun pendingKey(operationId: String) = "$PENDING_PREFIX$operationId"

    private companion object {
        const val PREFERENCES_NAME = "remote_mcp_secure"
        const val CREDENTIAL_PREFIX = "credential_"
        const val PENDING_PREFIX = "pending_"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "shary_remote_mcp_aes_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
    }
}
