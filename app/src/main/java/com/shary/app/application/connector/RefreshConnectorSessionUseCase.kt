package com.shary.app.application.connector

import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import java.util.UUID
import javax.inject.Inject

data class RefreshConnectorSessionRequest(
    val sessionId: String,
    val refreshSecret: String,
    val sessionTtlSeconds: Long = PairConnectorRequest.DEFAULT_SESSION_TTL_SECONDS
)

data class RefreshConnectorSessionResult(
    val sessionId: String,
    val accessToken: String,
    val refreshSecret: String,
    val expiresAtEpochSeconds: Long
)

class RefreshConnectorSessionUseCase @Inject constructor(
    private val connectorRepository: ConnectorRepository
) {
    suspend operator fun invoke(request: RefreshConnectorSessionRequest): Result<RefreshConnectorSessionResult> =
        runCatching {
            require(request.sessionId.isNotBlank()) { "sessionId cannot be blank" }
            require(request.refreshSecret.isNotBlank()) { "refreshSecret cannot be blank" }
            require(request.sessionTtlSeconds in 60..(24 * 60 * 60)) { "sessionTtlSeconds out of range" }

            val session = connectorRepository.getSession(request.sessionId)
                ?: error("Session not found")
            val now = System.currentTimeMillis() / 1000
            require(session.revokedAt == null) { "Session revoked" }
            require(sha256Hex(request.refreshSecret) == session.refreshSecretHash) { "Invalid refresh secret" }

            val newAccessToken = "at_${UUID.randomUUID()}_${System.nanoTime()}"
            val newRefreshSecret = "rt_${UUID.randomUUID()}_${System.nanoTime()}"
            val updated = session.copy(
                accessTokenHash = sha256Hex(newAccessToken),
                refreshSecretHash = sha256Hex(newRefreshSecret),
                issuedAt = now,
                expiresAt = now + request.sessionTtlSeconds
            )
            connectorRepository.upsertSession(updated)

            RefreshConnectorSessionResult(
                sessionId = updated.sessionId,
                accessToken = newAccessToken,
                refreshSecret = newRefreshSecret,
                expiresAtEpochSeconds = updated.expiresAt
            )
        }
}
