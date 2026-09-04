package com.shary.app.core.domain.types.valueobjects

import com.shary.app.application.connector.ConnectorMutationAction
import com.shary.app.core.domain.types.enums.ConnectorAuditDecision
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.enums.ConnectorPolicyMode

data class ConnectorOwnerBinding(
    val ownerUid: String,
    val vaultId: String,
    val agentProfileId: String,
    val grantId: String
)

data class ConnectorClient(
    val clientId: String,
    val displayName: String,
    val createdAt: Long,
    val status: ConnectorClientStatus,
    val lastActivityAt: Long? = null,
    val ownerUid: String = "",
    val vaultId: String = "",
    val agentProfileId: String = "",
    val grantId: String = ""
)

data class ConnectorSession(
    val sessionId: String,
    val clientId: String,
    val accessTokenHash: String,
    val refreshSecretHash: String,
    val issuedAt: Long,
    val expiresAt: Long,
    val revokedAt: Long? = null,
    val ownerUid: String = "",
    val vaultId: String = "",
    val agentProfileId: String = "",
    val grantId: String = ""
)

data class ConnectorPolicy(
    val clientId: String,
    val mode: ConnectorPolicyMode = ConnectorPolicyMode.STRICT,
    val askEveryTime: Boolean = true,
    val allowedCapabilities: Set<ConnectorCapability> = emptySet(),
    val updatedAt: Long,
    val ownerUid: String = "",
    val vaultId: String = "",
    val agentProfileId: String = "",
    val grantId: String = ""
)

data class ConnectorAuditEvent(
    val eventId: String,
    val timestamp: Long,
    val clientId: String,
    val requestId: String,
    val operation: String,
    val targetType: String,
    val targetIds: List<String>,
    val decision: ConnectorAuditDecision,
    val reasonCode: String,
    val metadata: Map<String, String> = emptyMap(),
    val ownerUid: String = "",
    val vaultId: String = "",
    val agentProfileId: String = "",
    val grantId: String = ""
)

data class ConnectorPendingAction(
    val pendingId: String,
    val clientId: String,
    val requestId: String,
    val proposedActions: List<ConnectorMutationAction>,
    val previewSummary: Map<String, String>,
    val status: String,
    val updatedAt: Long,
    val ownerUid: String = "",
    val vaultId: String = "",
    val agentProfileId: String = "",
    val grantId: String = ""
)
