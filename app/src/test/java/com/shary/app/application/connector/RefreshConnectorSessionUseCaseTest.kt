package com.shary.app.application.connector

import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshConnectorSessionUseCaseTest {

    @Test
    fun refresh_rotates_access_token_and_refresh_secret() = runBlocking {
        val repository = RefreshRepoFake()
        val useCase = RefreshConnectorSessionUseCase(repository)

        val originalRefresh = "rt_original"
        val originalSession = ConnectorSession(
            sessionId = "ses_1",
            clientId = "cl_1",
            accessTokenHash = sha256Hex("at_original"),
            refreshSecretHash = sha256Hex(originalRefresh),
            issuedAt = 1L,
            expiresAt = (System.currentTimeMillis() / 1000) + 600
        )
        repository.upsertSession(originalSession)

        val result = useCase(
            RefreshConnectorSessionRequest(
                sessionId = "ses_1",
                refreshSecret = originalRefresh,
                sessionTtlSeconds = 1200
            )
        ).getOrThrow()

        val updated = repository.getSession("ses_1")!!
        assertTrue(result.accessToken.isNotBlank())
        assertTrue(result.refreshSecret.isNotBlank())
        assertNotEquals(originalSession.accessTokenHash, updated.accessTokenHash)
        assertNotEquals(originalSession.refreshSecretHash, updated.refreshSecretHash)
        assertTrue(updated.refreshSecretHash == sha256Hex(result.refreshSecret))
    }
}

private class RefreshRepoFake : ConnectorRepository {
    override val clients: Flow<List<ConnectorClient>> = MutableStateFlow(emptyList())
    override val sessions = MutableStateFlow<List<ConnectorSession>>(emptyList())
    override val policies: Flow<List<ConnectorPolicy>> = MutableStateFlow(emptyList())
    override val auditEvents: Flow<List<ConnectorAuditEvent>> = MutableStateFlow(emptyList())
    override val pendingActions: Flow<List<ConnectorPendingAction>> = MutableStateFlow(emptyList())

    override suspend fun upsertClient(client: ConnectorClient) {}

    override suspend fun upsertSession(session: ConnectorSession) {
        val updated = sessions.value.toMutableList()
        val index = updated.indexOfFirst { it.sessionId == session.sessionId }
        if (index >= 0) updated[index] = session else updated += session
        sessions.value = updated
    }

    override suspend fun upsertPolicy(policy: ConnectorPolicy) {}

    override suspend fun appendAuditEvent(event: ConnectorAuditEvent) {}

    override suspend fun upsertPendingAction(pendingAction: ConnectorPendingAction) {}

    override suspend fun getClient(clientId: String): ConnectorClient? = null

    override suspend fun getSession(sessionId: String): ConnectorSession? =
        sessions.value.firstOrNull { it.sessionId == sessionId }

    override suspend fun getLatestSessionForClient(clientId: String): ConnectorSession? =
        sessions.value.filter { it.clientId == clientId }.maxByOrNull { it.issuedAt }

    override suspend fun getSessionByAccessTokenHash(accessTokenHash: String): ConnectorSession? =
        sessions.value.firstOrNull { it.accessTokenHash == accessTokenHash }

    override suspend fun getPolicy(clientId: String): ConnectorPolicy? = null
}
