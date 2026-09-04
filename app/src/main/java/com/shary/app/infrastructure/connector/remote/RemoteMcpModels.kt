package com.shary.app.infrastructure.connector.remote

import org.json.JSONObject

enum class RemoteApprovalKind {
    OAUTH,
    INVITATION
}

data class RemoteApprovalDetails(
    val kind: RemoteApprovalKind,
    val approvalId: String,
    val clientName: String,
    val scopes: List<String>,
    val expiresAt: Long
)

data class RemoteGrantBinding(
    val agentProfileId: String,
    val grantId: String,
    val vaultId: String,
    val scopes: List<String>,
    val expiresAt: Long
)

data class RemoteClaimedOperation(
    val operationId: String,
    val vaultId: String,
    val grantId: String,
    val connectorClientId: String,
    val status: String,
    val requiresConfirmation: Boolean,
    val expiresAt: Long,
    val command: JSONObject
)

data class RemoteGrantCredential(
    val grantId: String,
    val agentProfileId: String,
    val clientId: String,
    val sessionId: String,
    val accessToken: String,
    val refreshSecret: String,
    val expiresAt: Long,
    val grantExpiresAt: Long
)

data class PendingRemoteOperation(
    val operationId: String,
    val grantId: String,
    val toolName: String,
    val commandJson: String,
    val expiresAt: Long
) {
    fun confirmationSummary(): String {
        val arguments = runCatching {
            JSONObject(commandJson).optJSONObject("arguments") ?: JSONObject()
        }.getOrDefault(JSONObject())
        val target = when {
            arguments.optString("key").isNotBlank() -> "Field: ${arguments.optString("key")}"
            arguments.optString("email").isNotBlank() -> "User: ${arguments.optString("email")}"
            arguments.optString("pending_id").isNotBlank() -> "Batch: ${arguments.optString("pending_id")}"
            else -> "Review the requested Shary change."
        }
        return "$toolName\n$target"
    }
}

sealed interface RemoteApprovalState {
    data object Idle : RemoteApprovalState
    data object Loading : RemoteApprovalState
    data class Ready(val details: RemoteApprovalDetails) : RemoteApprovalState
    data class Processing(val details: RemoteApprovalDetails) : RemoteApprovalState
    data class Completed(val message: String) : RemoteApprovalState
    data class Failed(val message: String, val details: RemoteApprovalDetails? = null) : RemoteApprovalState
}
