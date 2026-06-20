package com.shary.app.application.connector

import com.shary.app.core.constants.ConnectorPolicyConfig
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.enums.ConnectorPolicyMode
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal val connectorManagementMutex = Mutex()

class ConnectorLimitReachedException(maxActiveConnectors: Int) :
    IllegalStateException("Active connector limit reached ($maxActiveConnectors)")

data class PairConnectorRequest(
    val displayName: String,
    val allowedCapabilities: Set<ConnectorCapability>,
    val sessionTtlSeconds: Long = DEFAULT_SESSION_TTL_SECONDS
) {
    companion object {
        const val DEFAULT_SESSION_TTL_SECONDS: Long = 20 * 60
    }
}

data class PairConnectorResult(
    val clientId: String,
    val sessionId: String,
    val accessToken: String,
    val refreshSecret: String,
    val expiresAtEpochSeconds: Long
)

@Singleton
class PairConnectorUseCase @Inject constructor(
    private val connectorRepository: ConnectorRepository
) {
    suspend operator fun invoke(request: PairConnectorRequest): PairConnectorResult =
        connectorManagementMutex.withLock {
            require(request.displayName.isNotBlank()) { "displayName cannot be blank" }
            require(request.allowedCapabilities.isNotEmpty()) { "at least one capability is required" }
            require(request.sessionTtlSeconds in 60..(24 * 60 * 60)) {
                "sessionTtlSeconds out of range"
            }

            val activeConnectorCount = connectorRepository.clients.first()
                .count { it.status != ConnectorClientStatus.REVOKED }
            if (activeConnectorCount >= ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS) {
                throw ConnectorLimitReachedException(ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS)
            }

            val now = System.currentTimeMillis() / 1000
            val clientId = "cl_${UUID.randomUUID()}"
            val sessionId = "ses_${UUID.randomUUID()}"
            val accessToken = "at_${UUID.randomUUID()}_${System.nanoTime()}"
            val refreshSecret = "rt_${UUID.randomUUID()}_${System.nanoTime()}"
            val expiresAt = now + request.sessionTtlSeconds

            val writeCapabilities = setOf(
                ConnectorCapability.FIELDS_WRITE,
                ConnectorCapability.USERS_WRITE,
                ConnectorCapability.ACTIONS_COMMIT
            )
            val status = if (request.allowedCapabilities.any { it in writeCapabilities }) {
                ConnectorClientStatus.PAIRED_FULL
            } else {
                ConnectorClientStatus.PAIRED_LIMITED
            }

            connectorRepository.upsertClient(
                ConnectorClient(
                    clientId = clientId,
                    displayName = request.displayName.trim(),
                    createdAt = now,
                    status = status,
                    lastActivityAt = now
                )
            )

            connectorRepository.upsertPolicy(
                ConnectorPolicy(
                    clientId = clientId,
                    mode = ConnectorPolicyMode.STRICT,
                    askEveryTime = true,
                    allowedCapabilities = request.allowedCapabilities,
                    updatedAt = now
                )
            )

            connectorRepository.upsertSession(
                ConnectorSession(
                    sessionId = sessionId,
                    clientId = clientId,
                    accessTokenHash = sha256Hex(accessToken),
                    refreshSecretHash = sha256Hex(refreshSecret),
                    issuedAt = now,
                    expiresAt = expiresAt
                )
            )

            PairConnectorResult(
                clientId = clientId,
                sessionId = sessionId,
                accessToken = accessToken,
                refreshSecret = refreshSecret,
                expiresAtEpochSeconds = expiresAt
            )
        }
}
