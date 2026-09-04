package com.shary.app.application.connector

import com.shary.app.core.constants.ConnectorPolicyConfig
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import com.shary.app.core.domain.types.valueobjects.ConnectorOwnerBinding
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairConnectorManagementUseCaseTest {
    @Test
    fun `pairing stops at configured active connector limit`() = runBlocking {
        val repository = ConnectorManagementRepositoryFake()
        val pair = PairConnectorUseCase(repository)

        repeat(ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS) { index ->
            pair(readOnlyRequest("Connector $index"))
        }

        var error: ConnectorLimitReachedException? = null
        try {
            pair(readOnlyRequest("One too many"))
        } catch (caught: ConnectorLimitReachedException) {
            error = caught
        }

        assertNotNull(error)
        assertTrue(error?.message.orEmpty().contains(ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS.toString()))
        assertEquals(ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS, repository.clientsState.value.size)
    }

    @Test
    fun `revoking a connector revokes its sessions and frees one limit slot`() = runBlocking {
        val repository = ConnectorManagementRepositoryFake()
        val pair = PairConnectorUseCase(repository)
        val manage = ManageConnectorsUseCase(repository)
        val paired = List(ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS) { index ->
            pair(readOnlyRequest("Connector $index"))
        }

        assertTrue(manage.revoke(paired.first().clientId))
        val revokedClient = repository.getClient(paired.first().clientId)
        val revokedSession = repository.getSession(paired.first().sessionId)
        assertEquals(ConnectorClientStatus.REVOKED, revokedClient?.status)
        assertNotNull(revokedSession?.revokedAt)

        pair(readOnlyRequest("Replacement"))
        assertEquals(
            ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS,
            repository.clientsState.value.count { it.status != ConnectorClientStatus.REVOKED }
        )
    }

    @Test
    fun `revoke all preserves records but leaves no active connector`() = runBlocking {
        val repository = ConnectorManagementRepositoryFake()
        val pair = PairConnectorUseCase(repository)
        val manage = ManageConnectorsUseCase(repository)
        repeat(3) { index -> pair(readOnlyRequest("Connector $index")) }

        assertEquals(3, manage.revokeAll())
        assertEquals(3, repository.clientsState.value.size)
        assertTrue(repository.clientsState.value.all { it.status == ConnectorClientStatus.REVOKED })
        assertTrue(repository.sessionsState.value.all { it.revokedAt != null })
    }

    private fun readOnlyRequest(name: String) = PairConnectorRequest(
        displayName = name,
        allowedCapabilities = setOf(
            ConnectorCapability.FIELDS_READ,
            ConnectorCapability.USERS_READ,
            ConnectorCapability.ACTIONS_PREVIEW
        ),
        ownerBinding = ConnectorOwnerBinding(
            ownerUid = "owner-test",
            vaultId = "vault-test",
            agentProfileId = "agent-test",
            grantId = "grant-test"
        )
    )
}

private class ConnectorManagementRepositoryFake : ConnectorRepository {
    val clientsState = MutableStateFlow<List<ConnectorClient>>(emptyList())
    val sessionsState = MutableStateFlow<List<ConnectorSession>>(emptyList())
    private val policiesState = MutableStateFlow<List<ConnectorPolicy>>(emptyList())
    private val auditState = MutableStateFlow<List<ConnectorAuditEvent>>(emptyList())
    private val pendingState = MutableStateFlow<List<ConnectorPendingAction>>(emptyList())

    override val clients: Flow<List<ConnectorClient>> = clientsState
    override val sessions: Flow<List<ConnectorSession>> = sessionsState
    override val policies: Flow<List<ConnectorPolicy>> = policiesState
    override val auditEvents: Flow<List<ConnectorAuditEvent>> = auditState
    override val pendingActions: Flow<List<ConnectorPendingAction>> = pendingState

    override suspend fun upsertClient(client: ConnectorClient) {
        clientsState.value = clientsState.value.upsert(client) { it.clientId == client.clientId }
    }

    override suspend fun upsertSession(session: ConnectorSession) {
        sessionsState.value = sessionsState.value.upsert(session) { it.sessionId == session.sessionId }
    }

    override suspend fun upsertPolicy(policy: ConnectorPolicy) {
        policiesState.value = policiesState.value.upsert(policy) { it.clientId == policy.clientId }
    }

    override suspend fun appendAuditEvent(event: ConnectorAuditEvent) {
        auditState.value += event
    }

    override suspend fun upsertPendingAction(pendingAction: ConnectorPendingAction) {
        pendingState.value = pendingState.value.upsert(pendingAction) {
            it.pendingId == pendingAction.pendingId
        }
    }

    override suspend fun getClient(clientId: String): ConnectorClient? =
        clientsState.value.firstOrNull { it.clientId == clientId }

    override suspend fun getSession(sessionId: String): ConnectorSession? =
        sessionsState.value.firstOrNull { it.sessionId == sessionId }

    override suspend fun getLatestSessionForClient(clientId: String): ConnectorSession? =
        sessionsState.value.filter { it.clientId == clientId }.maxByOrNull { it.issuedAt }

    override suspend fun getSessionByAccessTokenHash(accessTokenHash: String): ConnectorSession? =
        sessionsState.value.firstOrNull { it.accessTokenHash == accessTokenHash }

    override suspend fun getPolicy(clientId: String): ConnectorPolicy? =
        policiesState.value.firstOrNull { it.clientId == clientId }

    private fun <T> List<T>.upsert(item: T, matches: (T) -> Boolean): List<T> {
        val updated = toMutableList()
        val index = updated.indexOfFirst(matches)
        if (index >= 0) updated[index] = item else updated += item
        return updated
    }
}
