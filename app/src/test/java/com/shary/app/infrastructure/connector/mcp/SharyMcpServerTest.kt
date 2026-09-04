package com.shary.app.infrastructure.connector.mcp

import com.shary.app.application.connector.CommitConnectorActionsUseCase
import com.shary.app.application.connector.PairConnectorRequest
import com.shary.app.application.connector.PairConnectorUseCase
import com.shary.app.application.connector.PreviewConnectorActionsUseCase
import com.shary.app.application.connector.policy.ConnectorPolicyEvaluator
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.interfaces.repositories.ContactUpdateResult
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import com.shary.app.core.domain.types.valueobjects.ConnectorOwnerBinding
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.FieldValueTypes
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
        assertTrue(response.body.orEmpty().contains("shary.fields.types"))
        assertTrue(response.body.orEmpty().contains("value_type"))
        assertTrue(response.body.orEmpty().contains("email"))
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
        assertTrue(!response.body.orEmpty().contains("secret"))
        assertTrue(response.body.orEmpty().contains("\"isError\":false"))
    }

    @Test
    fun fields_get_requires_value_scope_and_returns_one_value() = runBlocking {
        val fixture = testFixture()
        fixture.fieldRepository.saveField(FieldDomain.create(key = "api-key", value = "secret"))
        val token = fixture.pair(
            setOf(
                ConnectorCapability.FIELDS_READ,
                ConnectorCapability.FIELDS_VALUE_READ
            )
        )

        val response = fixture.server.handleRawJson(
            rawBody = """
                {
                  "jsonrpc":"2.0",
                  "id":"read-field-value",
                  "method":"tools/call",
                  "params":{
                    "name":"shary.fields.get",
                    "arguments":{"key":"api-key"}
                  }
                }
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertTrue(response.body.orEmpty().contains("secret"))
        assertTrue(response.body.orEmpty().contains("\"isError\":false"))
    }

    @Test
    fun fields_types_returns_canonical_type_list() = runBlocking {
        val fixture = testFixture()
        val token = fixture.pair(setOf(ConnectorCapability.FIELDS_WRITE))

        val response = fixture.server.handleRawJson(
            rawBody = """
                {
                  "jsonrpc":"2.0",
                  "id":"field-types",
                  "method":"tools/call",
                  "params":{
                    "name":"shary.fields.types",
                    "arguments":{}
                  }
                }
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertTrue(response.body.orEmpty().contains("\"name\":\"plain\""))
        assertTrue(response.body.orEmpty().contains("\"name\":\"email\""))
        assertTrue(response.body.orEmpty().contains("\"isError\":false"))
    }

    @Test
    fun fields_create_writes_email_type_alias_and_tag() = runBlocking {
        val fixture = testFixture()
        val token = fixture.pair(setOf(ConnectorCapability.FIELDS_WRITE))

        val response = fixture.server.handleRawJson(
            rawBody = """
                {
                  "jsonrpc":"2.0",
                  "id":"typed-field",
                  "method":"tools/call",
                  "params":{
                    "name":"shary.fields.create",
                    "arguments":{
                      "key":"contact-email",
                      "value":"person@example.com",
                      "value_type":"email",
                      "key_alias":"Primary contact",
                      "tag":"Work",
                      "idempotency_key":"typed-field-1",
                      "confirm":true
                    }
                  }
                }
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        val stored = fixture.fieldRepository.getAllFields().first().single()
        val parsed = FieldValueContract.parse(stored.value)
        assertTrue(response.body.orEmpty().contains("\"isError\":false"))
        assertEquals("Primary contact", stored.keyAlias)
        assertEquals("Work", stored.tag.name)
        assertEquals("email", parsed.envelope?.spec?.kind)
        assertEquals("person@example.com", parsed.plainData)
    }

    @Test
    fun fields_create_rejects_invalid_email_value() = runBlocking {
        val fixture = testFixture()
        val token = fixture.pair(setOf(ConnectorCapability.FIELDS_WRITE))

        val response = fixture.server.handleRawJson(
            rawBody = """
                {
                  "jsonrpc":"2.0",
                  "id":"invalid-email",
                  "method":"tools/call",
                  "params":{
                    "name":"shary.fields.create",
                    "arguments":{
                      "key":"contact-email",
                      "value":"invalid",
                      "value_type":"email",
                      "idempotency_key":"typed-field-2",
                      "confirm":true
                    }
                  }
                }
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertTrue(response.body.orEmpty().contains("\"isError\":true"))
        assertTrue(response.body.orEmpty().contains("valid email"))
        assertTrue(fixture.fieldRepository.getAllFields().first().isEmpty())
    }

    @Test
    fun sensitive_field_metadata_is_masked_and_value_read_requires_local_share_toggle() = runBlocking {
        val fixture = testFixture()
        val sensitive = FieldValueTypes.encodeForStorage("secret-value", "sensitive").getOrThrow()
        fixture.fieldRepository.saveField(
            FieldDomain.create(
                key = "private-token",
                value = sensitive,
                keyAlias = "production credential"
            )
        )
        val token = fixture.pair(
            setOf(
                ConnectorCapability.FIELDS_READ,
                ConnectorCapability.FIELDS_VALUE_READ
            )
        )

        val listResponse = fixture.server.handleRawJson(
            rawBody = """
                {"jsonrpc":"2.0","id":"sensitive-list","method":"tools/call",
                 "params":{"name":"shary.fields.list","arguments":{}}}
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val blockedRead = fixture.server.handleRawJson(
            rawBody = """
                {"jsonrpc":"2.0","id":"sensitive-get","method":"tools/call",
                 "params":{"name":"shary.fields.get","arguments":{"key":"private-token"}}}
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertTrue(listResponse.body.orEmpty().contains("*****"))
        assertTrue(!listResponse.body.orEmpty().contains("production credential"))
        assertTrue(blockedRead.body.orEmpty().contains("sensitive_field_not_mcp_shareable"))
        assertTrue(blockedRead.body.orEmpty().contains("\"isError\":true"))

        fixture.fieldRepository.updateMcpShareable("private-token", true)
        val allowedRead = fixture.server.handleRawJson(
            rawBody = """
                {"jsonrpc":"2.0","id":"sensitive-get-allowed","method":"tools/call",
                 "params":{"name":"shary.fields.get","arguments":{"key":"private-token"}}}
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertTrue(allowedRead.body.orEmpty().contains("secret-value"))
        assertTrue(allowedRead.body.orEmpty().contains("\"isError\":false"))
    }

    @Test
    fun mcp_cannot_create_sensitive_field() = runBlocking {
        val fixture = testFixture()
        val token = fixture.pair(setOf(ConnectorCapability.FIELDS_WRITE))

        val response = fixture.server.handleRawJson(
            rawBody = """
                {
                  "jsonrpc":"2.0",
                  "id":"sensitive-create",
                  "method":"tools/call",
                  "params":{
                    "name":"shary.fields.create",
                    "arguments":{
                      "key":"private-token",
                      "value":"secret",
                      "value_type":"sensitive",
                      "idempotency_key":"sensitive-create-1",
                      "confirm":true
                    }
                  }
                }
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertTrue(response.body.orEmpty().contains("sensitive_field_creation_denied"))
        assertTrue(response.body.orEmpty().contains("\"isError\":true"))
        assertTrue(fixture.fieldRepository.getAllFields().first().isEmpty())
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

    @Test
    fun locked_vault_denies_tool_calls_without_reaching_connector_core() = runBlocking {
        val fixture = testFixture(vaultAvailable = false)

        val response = fixture.server.handleRawJson(
            rawBody = """
                {
                  "jsonrpc":"2.0",
                  "id":"locked-read",
                  "method":"tools/call",
                  "params":{
                    "name":"shary.fields.list",
                    "arguments":{}
                  }
                }
            """.trimIndent(),
            headers = mapOf("Authorization" to "Bearer unavailable")
        )

        assertEquals(200, response.statusCode)
        assertTrue(response.body.orEmpty().contains("\"isError\":true"))
        assertTrue(response.body.orEmpty().contains("policy_denied"))
        assertTrue(response.body.orEmpty().contains("locked or signed out"))
        assertEquals(0, fixture.connectorRepository.auditEvents.first().size)
    }

    @Test
    fun rate_limit_is_scoped_by_bearer_credential() = runBlocking {
        var now = 1_000L
        val limiter = McpConnectorRateLimiter(
            maxRequests = 1,
            windowSeconds = 60,
            maxTrackedCredentials = MCP_RATE_LIMIT_TRACKED_CREDENTIALS,
            nowEpochSeconds = { now }
        )
        val fixture = testFixture(rateLimiter = limiter)
        val firstToken = fixture.pair(setOf(ConnectorCapability.FIELDS_READ))
        val secondToken = fixture.pair(setOf(ConnectorCapability.FIELDS_READ))
        val request = """
            {
              "jsonrpc":"2.0",
              "id":"rate-read",
              "method":"tools/call",
              "params":{
                "name":"shary.fields.list",
                "arguments":{}
              }
            }
        """.trimIndent()

        val first = fixture.server.handleRawJson(
            rawBody = request,
            headers = mapOf("Authorization" to "Bearer $firstToken")
        )
        val limited = fixture.server.handleRawJson(
            rawBody = request,
            headers = mapOf("Authorization" to "Bearer $firstToken")
        )
        val independent = fixture.server.handleRawJson(
            rawBody = request,
            headers = mapOf("Authorization" to "Bearer $secondToken")
        )
        now += 60
        val afterWindow = fixture.server.handleRawJson(
            rawBody = request,
            headers = mapOf("Authorization" to "Bearer $firstToken")
        )

        assertTrue(first.body.orEmpty().contains("\"isError\":false"))
        assertTrue(limited.body.orEmpty().contains("rate_limited"))
        assertTrue(limited.body.orEmpty().contains("\"retry_after_seconds\":60"))
        assertTrue(independent.body.orEmpty().contains("\"isError\":false"))
        assertTrue(afterWindow.body.orEmpty().contains("\"isError\":false"))
    }

    @Test
    fun rate_limiter_bounds_unknown_credential_tracking() {
        var now = 2_000L
        val limiter = McpConnectorRateLimiter(
            maxRequests = 1,
            windowSeconds = 60,
            maxTrackedCredentials = 1,
            nowEpochSeconds = { now }
        )

        assertTrue(limiter.acquire("known").allowed)
        assertTrue(limiter.acquire("unknown-a").allowed)
        assertTrue(!limiter.acquire("unknown-b").allowed)
        now += 60
        assertTrue(limiter.acquire("unknown-b").allowed)
    }
}

private fun testMcpServer(): SharyMcpServer = testFixture().server

private fun testFixture(
    vaultAvailable: Boolean = true,
    rateLimiter: McpConnectorRateLimiter = McpConnectorRateLimiter(
        maxRequests = MCP_RATE_LIMIT_REQUESTS,
        windowSeconds = MCP_RATE_LIMIT_WINDOW_SECONDS,
        maxTrackedCredentials = MCP_RATE_LIMIT_TRACKED_CREDENTIALS,
        nowEpochSeconds = { System.currentTimeMillis() / 1000 }
    )
): McpFixture {
    val connectorRepository = McpConnectorRepositoryFake()
    val fieldRepository = McpFieldRepositoryFake()
    val userRepository = McpUserRepositoryFake()
    val router = LoopbackConnectorRouter(
        authService = ConnectorAuthService(connectorRepository),
        policyEvaluator = ConnectorPolicyEvaluator(),
        connectorRepository = connectorRepository,
        fieldRepository = fieldRepository,
        userRepository = userRepository,
        previewConnectorActionsUseCase = PreviewConnectorActionsUseCase(
            connectorRepository,
            fieldRepository
        ),
        commitConnectorActionsUseCase = CommitConnectorActionsUseCase(
            connectorRepository = connectorRepository,
            fieldRepository = fieldRepository,
            userRepository = userRepository
        )
    )
    return McpFixture(
        connectorRepository = connectorRepository,
        fieldRepository = fieldRepository,
        server = SharyMcpServer(
            SharyMcpDispatcher(
                loopbackServer = LoopbackServer(router),
                accessController = McpAccessController(
                    vaultAvailable = { vaultAvailable },
                    rateLimiter = rateLimiter
                )
            )
        )
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
                allowedCapabilities = capabilities,
                ownerBinding = ConnectorOwnerBinding(
                    ownerUid = "owner-test",
                    vaultId = "vault-test",
                    agentProfileId = "agent-test",
                    grantId = "grant-test"
                )
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
    override suspend fun getFieldLoadSnapshots(
        firstBatchSize: Int,
        chunkSize: Int
    ): Flow<com.shary.app.core.domain.interfaces.repositories.FieldLoadSnapshot> =
        MutableStateFlow(
            com.shary.app.core.domain.interfaces.repositories.FieldLoadSnapshot(
                fields = fieldsState.value,
                attemptedCount = fieldsState.value.size,
                totalCount = fieldsState.value.size,
                failureCount = 0,
                complete = true
            )
        )

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

    override suspend fun updateMcpShareable(key: String, shareable: Boolean) {
        fieldsState.value = fieldsState.value.map {
            if (it.key.equals(key, ignoreCase = true)) it.copy(isMcpShareable = shareable) else it
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

    override suspend fun updateUser(
        originalEmail: String,
        updatedUser: UserDomain
    ): ContactUpdateResult {
        val current = usersState.value
        val index = current.indexOfFirst { it.email.equals(originalEmail, ignoreCase = true) }
        if (index < 0) return ContactUpdateResult.NotFound
        if (current.withIndex().any { (otherIndex, user) ->
                otherIndex != index && user.email.equals(updatedUser.email, ignoreCase = true)
            }
        ) {
            return ContactUpdateResult.EmailAlreadyExists
        }
        if (current[index] == updatedUser) return ContactUpdateResult.Unchanged
        usersState.value = current.toMutableList().also { it[index] = updatedUser }
        return ContactUpdateResult.Updated
    }

    override suspend fun deleteUser(email: String): Boolean {
        val before = usersState.value.size
        usersState.value = usersState.value.filterNot { it.email.equals(email, ignoreCase = true) }
        return before != usersState.value.size
    }

    override suspend fun updateTag(email: String, newTag: Tag) {
        usersState.value = usersState.value.map {
            if (it.email.equals(email, ignoreCase = true)) it.copy(tag = newTag) else it
        }
    }

    override suspend fun deleteUsers(emails: Collection<String>): Int {
        val normalized = emails.map { it.lowercase() }.toSet()
        val before = usersState.value.size
        usersState.value = usersState.value.filterNot { it.email.lowercase() in normalized }
        return before - usersState.value.size
    }
}
