package com.shary.app.application.connector

import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

class ManageConnectorsUseCase @Inject constructor(
    private val connectorRepository: ConnectorRepository
) {
    suspend fun revoke(clientId: String): Boolean = connectorManagementMutex.withLock {
        val client = connectorRepository.getClient(clientId) ?: return@withLock false
        if (client.status == ConnectorClientStatus.REVOKED) return@withLock false

        val now = System.currentTimeMillis() / 1000
        connectorRepository.upsertClient(
            client.copy(
                status = ConnectorClientStatus.REVOKED,
                lastActivityAt = now
            )
        )
        connectorRepository.sessions.first()
            .filter { it.clientId == clientId && it.revokedAt == null }
            .forEach { session ->
                connectorRepository.upsertSession(session.copy(revokedAt = now))
            }
        true
    }

    suspend fun revokeAll(): Int = connectorManagementMutex.withLock {
        val now = System.currentTimeMillis() / 1000
        val activeClients = connectorRepository.clients.first()
            .filter { it.status != ConnectorClientStatus.REVOKED }
        if (activeClients.isEmpty()) return@withLock 0

        val activeClientIds = activeClients.mapTo(mutableSetOf()) { it.clientId }
        activeClients.forEach { client ->
            connectorRepository.upsertClient(
                client.copy(
                    status = ConnectorClientStatus.REVOKED,
                    lastActivityAt = now
                )
            )
        }
        connectorRepository.sessions.first()
            .filter { it.clientId in activeClientIds && it.revokedAt == null }
            .forEach { session ->
                connectorRepository.upsertSession(session.copy(revokedAt = now))
            }
        activeClients.size
    }
}
