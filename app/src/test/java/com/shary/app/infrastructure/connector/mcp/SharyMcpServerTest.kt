package com.shary.app.infrastructure.connector.mcp

import com.shary.app.application.connector.CommitConnectorActionsUseCase
import com.shary.app.application.connector.PairConnectorRequest
import com.shary.app.application.connector.PairConnectorUseCase
import com.shary.app.application.connector.PreviewConnectorActionsUseCase
import com.shary.app.application.connector.policy.ConnectorPolicyEvaluator
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import com.shary.app.infrastructure.connector.transport.ConnectorAuthService
import com.shary.app.infrastructure.connector.transport.LoopbackConnectorRouter
import com.shary.app.infrastructure.connector.transport.LoopbackServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharyMcpServerTest {

    @Test
    fun tools_list_exposes_fundamental_shary_tools() = runBlocking {
        val server = testMcpServer()

        val response = server.handleRawJson(
            rawBody = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
        )

        assertEquals(200, response.statusCode)
        assertTrue(response.body.orEmpty().contains("shary.fields.list"))
        assertTrue(response.body.orEmpty().contains("shary.users.list"))
        assertTrue(response.body.orEmpty().contains("shary.actions.preview"))
        assertTrue(response.body.orEmpty().contains("shary.actions.commit"))
    }

    @Test
    fun fields_list_uses_connector_bearer_auth_and_returns_fields() = runBlocking {
        val fixture = testFixture()
        fixture.fieldRepository.saveField(FieldDomain.create(key = "api-key", value = "secret"))
        val token = fixture.pair(
            setOf(
                ConnectorCapability.FIELDS_READ,
                ConnectorCapability.USERS_READ
            )
        )

        val response = fixture.server.handleRawJson(
            rawBody = """
                {
                  "jsonrpc":"2.0",
                  "id":"read-fields",
                  "method":"tools/call",
                  "params":{
                    "name":"shary.fields.list",
                    "arguments":{}
                  }
                }
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertEquals(200, response.statusCode)
        assertTrue(response.body.orEmpty().contains("api-key"))
        assertTrue(response.body.orEmpty().contains("\"isError\":false"))
    }

    @Test
    fun read_only_connector_gets_tool_error_for_write_tool() = runBlocking {
        val fixture = testFixture()
        val token = fixture.pair(setOf(ConnectorCapability.FIELDS_READ))

        val response = fixture.server.handleRawJson(
            rawBody = """
                {
                  "jsonrpc":"2.0",
                  "id":"write-field",
                  "method":"tools/call",
                  "params":{
                    "name":"shary.fields.create",
                    "arguments":{
                      "key":"token",
                      "value":"secret",
                      "idempotency_key":"idem-1",
                      "confirm":true
                    }
                  }
                }
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertEquals(200, response.statusCode)
        assertTrue(response.body.orEmpty().contains("\"isError\":true"))
        assertTrue(response.body.orEmpty().contains("unauthorized_scope"))
        assertEquals(1, fixture.connectorRepository.auditEvents.first().size)
    }
}

private fun testMcpServer(): SharyMcpServer = testFixture().server

private fun testFixture(): McpFixture {
    val connectorRepository = McpConnectorRepositoryFake()
    val fieldRepository = McpFieldRepositoryFake()
    val userRepository = McpUserRepositoryFake()
    val router = LoopbackConnectorRouter(
        authService = ConnectorAuthService(connectorRepository),
        policyEvaluator = ConnectorPolicyEvaluator(),
        connectorRepository = connectorRepository,
        fieldRepository = fieldRepository,
        userRepository = userRepository,
        previewConnectorActionsUseCase = PreviewConnectorActionsUseCase(connectorRepository),
        commitConnectorActionsUseCase = CommitConnectorActionsUseCase(
            connectorRepository = connectorRepository,
            fieldRepository = fieldRepository,
            userRepository = userRepository
        )
    )
    return McpFixture(
        connectorRepository = connectorRepository,
        fieldRepository = fieldRepository,
        server = SharyMcpServer(LoopbackServer(router))
    )
}

private data class McpFixture(
    val connectorRepository: McpConnectorRepositoryFake,
    val fieldRepository: McpFieldRepositoryFake,
    val server: SharyMcpServer
) {
    suspend fun pair(capabilities: Set<ConnectorCapability>): String =
        PairConnectorUseCase(connectorRepository)(
            PairConnectorRequest(
                displayName = "MCP Test",
                allowedCapabilities = capabilities
            )
        ).accessToken
}

private class McpConnectorRepositoryFake : ConnectorRepository {
    private val clientsState = MutableStateFlow<List<ConnectorClient>>(emptyList())
    private val sessionsState = MutableStateFlow<List<ConnectorSession>>(emptyList())
    private val policiesState = MutableStateFlow<List<ConnectorPolicy>>(emptyList())
    private val auditEventsState = MutableStateFlow<List<ConnectorAuditEvent>>(emptyList())
    private val pendingActionsState = MutableStateFlow<List<ConnectorPendingAction>>(emptyList())

    override val clients: Flow<List<ConnectorClient>> = clientsState
    override val sessions: Flow<List<ConnectorSession>> = sessionsState
    override val policies: Flow<List<ConnectorPolicy>> = policiesState
    override val auditEvents: Flow<List<ConnectorAuditEvent>> = auditEventsState
    override val pendingActions: Flow<List<ConnectorPendingAction>> = pendingActionsState

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
        auditEventsState.value = auditEventsState.value + event
    }

    override suspend fun upsertPendingAction(pendingAction: ConnectorPendingAction) {
        pendingActionsState.value = pendingActionsState.value.upsert(pendingAction) {
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

    private fun <T> List<T>.upsert(item: T, sameItem: (T) -> Boolean): List<T> {
        val updated = toMutableList()
        val index = updated.indexOfFirst(sameItem)
        if (index >= 0) updated[index] = item else updated += item
        return updated
    }
}

private class McpFieldRepositoryFake : FieldRepository {
    private val fieldsState = MutableStateFlow<List<FieldDomain>>(emptyList())

    override suspend fun getAllFields(): Flow<List<FieldDomain>> = fieldsState
    override suspend fun getAllFieldsProgressive(firstBatchSize: Int, chunkSize: Int): Flow<List<FieldDomain>> =
        fieldsState

    override suspend fun getFieldsByTag(tag: Tag): List<FieldDomain> =
        fieldsState.value.filter { it.tag.name == tag.name }

    override suspend fun saveField(field: FieldDomain) {
        fieldsState.value = fieldsState.value.filterNot { it.key.equals(field.key, ignoreCase = true) } + field
    }

    override suspend fun saveFieldIfNotExists(field: FieldDomain): Boolean {
        if (fieldsState.value.any { it.key.equals(field.key, ignoreCase = true) }) return false
        fieldsState.value = fieldsState.value + field
        return true
    }

    override suspend fun deleteField(key: String) {
        fieldsState.value = fieldsState.value.filterNot { it.key.equals(key, ignoreCase = true) }
    }

    override suspend fun deleteFields(keys: List<String>) {
        val normalized = keys.map { it.lowercase() }.toSet()
        fieldsState.value = fieldsState.value.filterNot { it.key.lowercase() in normalized }
    }

    override suspend fun updateValue(key: String, newValue: String) {
        fieldsState.value = fieldsState.value.map {
            if (it.key.equals(key, ignoreCase = true)) it.copy(value = newValue) else it
        }
    }

    override suspend fun updateAlias(key: String, newAlias: String) {
        fieldsState.value = fieldsState.value.map {
            if (it.key.equals(key, ignoreCase = true)) it.copy(keyAlias = newAlias) else it
        }
    }

    override suspend fun updateTag(key: String, newTag: Tag) {
        fieldsState.value = fieldsState.value.map {
            if (it.key.equals(key, ignoreCase = true)) it.copy(tag = newTag) else it
        }
    }
}

private class McpUserRepositoryFake : UserRepository {
    private val usersState = MutableStateFlow<List<UserDomain>>(emptyList())
    override val users: Flow<List<UserDomain>> = usersState

    override suspend fun getAllUsers(): List<UserDomain> = usersState.value

    override suspend fun saveUserIfNotExists(user: UserDomain): Boolean {
        if (usersState.value.any { it.email.equals(user.email, ignoreCase = true) }) return false
        usersState.value = usersState.value + user
        return true
    }

    override suspend fun upsertUser(user: UserDomain): Boolean {
        val existed = usersState.value.any { it.email.equals(user.email, ignoreCase = true) }
        usersState.value = usersState.value.filterNot { it.email.equals(user.email, ignoreCase = true) } + user
        return !existed
    }

    override suspend fun deleteUser(email: String): Boolean {
        val before = usersState.value.size
        usersState.value = usersState.value.filterNot { it.email.equals(email, ignoreCase = true) }
        return before != usersState.value.size
    }

    override suspend fun deleteUsers(emails: Collection<String>): Int {
        val normalized = emails.map { it.lowercase() }.toSet()
        val before = usersState.value.size
        usersState.value = usersState.value.filterNot { it.email.lowercase() in normalized }
        return before - usersState.value.size
    }
}
