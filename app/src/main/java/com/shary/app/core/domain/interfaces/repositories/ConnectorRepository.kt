package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import kotlinx.coroutines.flow.Flow

interface ConnectorRepository {
    val clients: Flow<List<ConnectorClient>>
    val sessions: Flow<List<ConnectorSession>>
    val policies: Flow<List<ConnectorPolicy>>
    val auditEvents: Flow<List<ConnectorAuditEvent>>
    val pendingActions: Flow<List<ConnectorPendingAction>>

    suspend fun upsertClient(client: ConnectorClient)
    suspend fun upsertSession(session: ConnectorSession)
    suspend fun upsertPolicy(policy: ConnectorPolicy)
    suspend fun appendAuditEvent(event: ConnectorAuditEvent)
    suspend fun upsertPendingAction(pendingAction: ConnectorPendingAction)

    suspend fun getClient(clientId: String): ConnectorClient?
    suspend fun getSession(sessionId: String): ConnectorSession?
    suspend fun getLatestSessionForClient(clientId: String): ConnectorSession?
    suspend fun getSessionByAccessTokenHash(accessTokenHash: String): ConnectorSession?
    suspend fun getPolicy(clientId: String): ConnectorPolicy?
}
