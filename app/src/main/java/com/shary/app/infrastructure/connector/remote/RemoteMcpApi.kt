package com.shary.app.infrastructure.connector.remote

import com.google.firebase.auth.FirebaseAuth
import com.shary.app.BuildConfig
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class RemoteMcpApi @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = BuildConfig.SHARY_MCP_BASE_URL.trimEnd('/')

    suspend fun registerDevice(
        deviceId: String,
        vaultId: String,
        displayName: String,
        appVersion: String,
        fcmToken: String
    ): JSONObject = ownerPost(
        "/device/register",
        JSONObject()
            .put("device_id", deviceId)
            .put("vault_id", vaultId)
            .put("display_name", displayName)
            .put("app_version", appVersion)
            .put("fcm_token", fcmToken)
    )

    suspend fun heartbeat(deviceId: String): JSONObject = ownerPost(
        "/device/heartbeat",
        JSONObject().put("device_id", deviceId)
    )

    suspend fun approvalDetails(kind: RemoteApprovalKind, approvalId: String): RemoteApprovalDetails {
        val path = when (kind) {
            RemoteApprovalKind.OAUTH -> "/device/authorizations/get"
            RemoteApprovalKind.INVITATION -> "/device/invitations/get"
        }
        val key = if (kind == RemoteApprovalKind.OAUTH) "authorization_id" else "invitation_id"
        val response = ownerPost(path, JSONObject().put(key, approvalId))
        return RemoteApprovalDetails(
            kind = kind,
            approvalId = approvalId,
            clientName = response.optString(
                if (kind == RemoteApprovalKind.OAUTH) "client_name" else "requested_alias",
                "MCP agent"
            ),
            scopes = response.optJSONArray("scopes").toStringList(),
            expiresAt = response.optLong("expires_at")
        )
    }

    suspend fun approve(
        details: RemoteApprovalDetails,
        vaultId: String,
        deviceId: String,
        ownerAlias: String,
        approvedScopes: Set<String>
    ): RemoteGrantBinding {
        val oauth = details.kind == RemoteApprovalKind.OAUTH
        val response = ownerPost(
            if (oauth) "/device/authorizations/approve" else "/device/invitations/approve",
            JSONObject()
                .put(if (oauth) "authorization_id" else "invitation_id", details.approvalId)
                .put("vault_id", vaultId)
                .put("device_id", deviceId)
                .put("owner_alias", ownerAlias)
                .put("scopes", JSONArray(approvedScopes.toList().sorted()))
        )
        return RemoteGrantBinding(
            agentProfileId = response.getString("agent_profile_id"),
            grantId = response.getString("grant_id"),
            vaultId = response.getString("vault_id"),
            scopes = response.optJSONArray("scopes").toStringList(),
            expiresAt = response.getLong("expires_at")
        )
    }

    suspend fun bindGrant(grantId: String, connectorClientId: String): JSONObject = ownerPost(
        "/device/grants/bind",
        JSONObject()
            .put("grant_id", grantId)
            .put("connector_client_id", connectorClientId)
    )

    suspend fun revokeGrant(grantId: String): JSONObject = ownerPost(
        "/device/grants/revoke",
        JSONObject().put("grant_id", grantId)
    )

    suspend fun renewGrant(grantId: String): Long = ownerPost(
        "/device/grants/renew",
        JSONObject().put("grant_id", grantId)
    ).getLong("expires_at")

    suspend fun claimOperations(deviceId: String): List<RemoteClaimedOperation> {
        val response = ownerPost(
            "/device/operations/claim",
            JSONObject().put("device_id", deviceId).put("limit", 20)
        )
        val operations = response.optJSONArray("operations") ?: JSONArray()
        return buildList {
            for (index in 0 until operations.length()) {
                val item = operations.optJSONObject(index) ?: continue
                add(
                    RemoteClaimedOperation(
                        operationId = item.getString("operation_id"),
                        vaultId = item.getString("vault_id"),
                        grantId = item.getString("grant_id"),
                        connectorClientId = item.optString("connector_client_id"),
                        status = item.getString("status"),
                        requiresConfirmation = item.optBoolean("requires_confirmation"),
                        expiresAt = item.getLong("expires_at"),
                        command = item.getJSONObject("command")
                    )
                )
            }
        }
    }

    suspend fun approveOperation(deviceId: String, operationId: String): JSONObject = ownerPost(
        "/device/operations/approve",
        JSONObject().put("device_id", deviceId).put("operation_id", operationId)
    )

    suspend fun rejectOperation(deviceId: String, operationId: String): JSONObject = ownerPost(
        "/device/operations/reject",
        JSONObject().put("device_id", deviceId).put("operation_id", operationId)
    )

    suspend fun completeOperation(
        deviceId: String,
        operationId: String,
        result: JSONObject? = null,
        error: JSONObject? = null
    ): JSONObject = ownerPost(
        "/device/operations/complete",
        JSONObject()
            .put("device_id", deviceId)
            .put("operation_id", operationId)
            .apply {
                if (result != null) put("result", result)
                if (error != null) put("error", error)
            }
    )

    private suspend fun ownerPost(path: String, payload: JSONObject): JSONObject {
        val token = ownerToken()
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl$path")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                val parsed = runCatching { JSONObject(body) }.getOrElse { JSONObject() }
                if (!response.isSuccessful) {
                    val code = parsed.optString("error", "http_${response.code}")
                    val message = parsed.optString("message", parsed.optString("error_description", code))
                    throw IOException("$code: $message")
                }
                parsed
            }
        }
    }

    private suspend fun ownerToken(): String {
        val owner = firebaseAuth.currentUser ?: error("Sign in to Shary before authorizing an MCP agent.")
        check(owner.isEmailVerified && !owner.email.isNullOrBlank()) {
            "Verify the Shary account email before authorizing an MCP agent."
        }
        return owner.getIdToken(true).await().token
            ?: error("Firebase did not return an owner identity token.")
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}
