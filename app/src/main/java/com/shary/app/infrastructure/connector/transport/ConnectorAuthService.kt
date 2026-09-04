package com.shary.app.infrastructure.connector.transport

import com.shary.app.application.connector.sha256Hex
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.valueobjects.ConnectorErrorCodes
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import com.shary.app.application.connector.ConnectorOwnerResolver
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectorAuthContext(
    val session: ConnectorSession,
    val policy: ConnectorPolicy
)

sealed interface ConnectorAuthResult {
    data class Authorized(val context: ConnectorAuthContext) : ConnectorAuthResult
    data class Denied(
        val code: String,
        val clientId: String? = null
    ) : ConnectorAuthResult
}

@Singleton
class ConnectorAuthService private constructor(
    private val connectorRepository: ConnectorRepository,
    private val ownerResolver: ConnectorOwnerResolver?,
    @Suppress("UNUSED_PARAMETER") constructorMarker: Unit
) {
    @Inject
    constructor(
        connectorRepository: ConnectorRepository,
        ownerResolver: ConnectorOwnerResolver
    ) : this(connectorRepository, ownerResolver, Unit)

    internal constructor(connectorRepository: ConnectorRepository) : this(connectorRepository, null, Unit)

    suspend fun authenticateBearer(headers: Map<String, String>): ConnectorAuthResult {
        val authHeader = headers.entries.firstOrNull { it.key.equals("Authorization", ignoreCase = true) }?.value
            ?: return ConnectorAuthResult.Denied(ConnectorErrorCodes.SESSION_EXPIRED)
        val token = authHeader.removePrefix("Bearer").trim()
        if (token.isBlank()) {
            return ConnectorAuthResult.Denied(ConnectorErrorCodes.SESSION_EXPIRED)
        }

        val session = connectorRepository.getSessionByAccessTokenHash(sha256Hex(token))
            ?: return ConnectorAuthResult.Denied(ConnectorErrorCodes.SESSION_EXPIRED)
        val now = System.currentTimeMillis() / 1000
        if (session.revokedAt != null || session.expiresAt < now) {
            return ConnectorAuthResult.Denied(
                code = ConnectorErrorCodes.SESSION_EXPIRED,
                clientId = session.clientId
            )
        }
        val activeOwner = ownerResolver?.currentOrNull()
        if (
            session.ownerUid.isBlank() ||
            session.vaultId.isBlank() ||
            (
                activeOwner != null &&
                    (session.ownerUid != activeOwner.ownerUid || session.vaultId != activeOwner.vaultId)
            )
        ) {
            return ConnectorAuthResult.Denied(
                code = ConnectorErrorCodes.POLICY_DENIED,
                clientId = session.clientId
            )
        }

        val client = connectorRepository.getClient(session.clientId)
            ?: return ConnectorAuthResult.Denied(
                code = ConnectorErrorCodes.SESSION_EXPIRED,
                clientId = session.clientId
            )
        if (client.status == ConnectorClientStatus.LOCKED || client.status == ConnectorClientStatus.REVOKED) {
            return ConnectorAuthResult.Denied(
                code = ConnectorErrorCodes.POLICY_DENIED,
                clientId = session.clientId
            )
        }
        if (
            client.ownerUid != session.ownerUid ||
            client.vaultId != session.vaultId ||
            client.agentProfileId != session.agentProfileId ||
            client.grantId != session.grantId
        ) {
            return ConnectorAuthResult.Denied(
                code = ConnectorErrorCodes.POLICY_DENIED,
                clientId = session.clientId
            )
        }

        val policy = connectorRepository.getPolicy(session.clientId)
            ?: return ConnectorAuthResult.Denied(
                code = ConnectorErrorCodes.UNAUTHORIZED_SCOPE,
                clientId = session.clientId
            )
        if (
            policy.ownerUid != session.ownerUid ||
            policy.vaultId != session.vaultId ||
            policy.agentProfileId != session.agentProfileId ||
            policy.grantId != session.grantId
        ) {
            return ConnectorAuthResult.Denied(
                code = ConnectorErrorCodes.POLICY_DENIED,
                clientId = session.clientId
            )
        }
        return ConnectorAuthResult.Authorized(
            context = ConnectorAuthContext(session = session, policy = policy)
        )
    }
}
