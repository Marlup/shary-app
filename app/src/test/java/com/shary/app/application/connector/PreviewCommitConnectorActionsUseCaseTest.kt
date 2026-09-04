package com.shary.app.application.connector

import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.interfaces.repositories.ContactUpdateResult
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.FieldLoadSnapshot
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.enums.ConnectorPolicyMode
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewCommitConnectorActionsUseCaseTest {

    @Test
    fun preview_then_commit_applies_actions_and_emits_audit() = runBlocking {
        val connectorRepository = InMemoryConnectorRepositoryFake()
        val fieldRepository = InMemoryFieldRepositoryFake()
        val userRepository = InMemoryUserRepositoryFake()

        val previewUseCase = PreviewConnectorActionsUseCase(connectorRepository, fieldRepository)
        val commitUseCase = CommitConnectorActionsUseCase(
            connectorRepository = connectorRepository,
            fieldRepository = fieldRepository,
            userRepository = userRepository
        )

        val preview = previewUseCase(
            PreviewConnectorActionsRequest(
                clientId = "client-1",
                requestId = "req-1",
                actions = listOf(
                    ConnectorMutationAction(
                        type = ConnectorMutationType.CREATE_FIELD,
                        key = "contact-email",
                        value = "owner@example.com",
                        valueType = "email",
                        keyAlias = "Owner email",
                        tag = "Work"
                    ),
                    ConnectorMutationAction(
                        type = ConnectorMutationType.CREATE_USER,
                        username = "Alice",
                        email = "alice@test.dev"
                    )
                )
            )
        ).getOrThrow()

        val commit = commitUseCase(
            CommitConnectorActionsRequest(
                clientId = "client-1",
                pendingId = preview.pendingId,
                requestId = "req-2",
                confirmationGranted = true
            )
        ).getOrThrow()

        assertTrue(commit.committed)
        assertEquals(2, commit.appliedCount)
        assertEquals(0, commit.failedCount)
        assertEquals(1, fieldRepository.fields.size)
        val storedField = fieldRepository.fields.single()
        assertEquals("Owner email", storedField.keyAlias)
        assertEquals("Work", storedField.tag.name)
        assertEquals("email", FieldValueContract.parse(storedField.value).envelope?.spec?.kind)
        assertEquals(1, userRepository.usersList.size)
        assertEquals(1, connectorRepository.auditEvents.first().size)
    }

    @Test
    fun commit_fails_when_pending_action_belongs_to_other_client() = runBlocking {
        val connectorRepository = InMemoryConnectorRepositoryFake()
        val fieldRepository = InMemoryFieldRepositoryFake()
        val userRepository = InMemoryUserRepositoryFake()

        val previewUseCase = PreviewConnectorActionsUseCase(connectorRepository, fieldRepository)
        val commitUseCase = CommitConnectorActionsUseCase(
            connectorRepository = connectorRepository,
            fieldRepository = fieldRepository,
            userRepository = userRepository
        )

        val preview = previewUseCase(
            PreviewConnectorActionsRequest(
                clientId = "client-owner",
                requestId = "req-owner",
                actions = listOf(
                    ConnectorMutationAction(
                        type = ConnectorMutationType.CREATE_FIELD,
                        key = "api-key",
                        value = "secret"
                    )
                )
            )
        ).getOrThrow()

        val commit = commitUseCase(
            CommitConnectorActionsRequest(
                clientId = "client-attacker",
                pendingId = preview.pendingId,
                requestId = "req-attacker",
                confirmationGranted = true
            )
        )

        assertTrue(commit.isFailure)
        assertTrue(commit.exceptionOrNull()?.message?.contains("not owned by client") == true)
    }
}

private class InMemoryConnectorRepositoryFake : ConnectorRepository {
    override val clients: Flow<List<ConnectorClient>> = MutableStateFlow(emptyList())
    override val sessions: Flow<List<ConnectorSession>> = MutableStateFlow(emptyList())
    override val policies: Flow<List<ConnectorPolicy>> = MutableStateFlow(emptyList())
    override val auditEvents = MutableStateFlow<List<ConnectorAuditEvent>>(emptyList())
    override val pendingActions = MutableStateFlow<List<ConnectorPendingAction>>(emptyList())

    override suspend fun upsertClient(client: ConnectorClient) {}
    override suspend fun upsertSession(session: ConnectorSession) {}
    override suspend fun upsertPolicy(policy: ConnectorPolicy) {}
    override suspend fun appendAuditEvent(event: ConnectorAuditEvent) {
        auditEvents.value = auditEvents.value + event
    }

    override suspend fun upsertPendingAction(pendingAction: ConnectorPendingAction) {
        val current = pendingActions.value.toMutableList()
        val idx = current.indexOfFirst { it.pendingId == pendingAction.pendingId }
        if (idx >= 0) current[idx] = pendingAction else current += pendingAction
        pendingActions.value = current
    }

    override suspend fun getClient(clientId: String): ConnectorClient? = ConnectorClient(
        clientId = clientId,
        displayName = "test",
        createdAt = 1L,
        status = ConnectorClientStatus.PAIRED_FULL,
        ownerUid = "owner-test",
        vaultId = "vault-test",
        agentProfileId = "agent-test",
        grantId = "grant-test"
    )

    override suspend fun getSession(sessionId: String): ConnectorSession? = null
    override suspend fun getLatestSessionForClient(clientId: String): ConnectorSession? = null
    override suspend fun getSessionByAccessTokenHash(accessTokenHash: String): ConnectorSession? = null
    override suspend fun getPolicy(clientId: String): ConnectorPolicy? = ConnectorPolicy(
        clientId = clientId,
        mode = ConnectorPolicyMode.AUTOMATION,
        askEveryTime = false,
        allowedCapabilities = setOf(
            ConnectorCapability.FIELDS_READ,
            ConnectorCapability.FIELDS_WRITE,
            ConnectorCapability.USERS_READ,
            ConnectorCapability.USERS_WRITE,
            ConnectorCapability.ACTIONS_PREVIEW,
            ConnectorCapability.ACTIONS_COMMIT
        ),
        updatedAt = 1L,
        ownerUid = "owner-test",
        vaultId = "vault-test",
        agentProfileId = "agent-test",
        grantId = "grant-test"
    )
}

private class InMemoryFieldRepositoryFake : FieldRepository {
    val fields = mutableListOf<FieldDomain>()

    override suspend fun getAllFields() = MutableStateFlow(fields.toList())
    override suspend fun getAllFieldsProgressive(firstBatchSize: Int, chunkSize: Int) =
        MutableStateFlow(fields.toList())
    override suspend fun getFieldLoadSnapshots(firstBatchSize: Int, chunkSize: Int) =
        MutableStateFlow(
            FieldLoadSnapshot(
                fields = fields.toList(),
                attemptedCount = fields.size,
                totalCount = fields.size,
                failureCount = 0,
                complete = true
            )
        )
    override suspend fun getFieldsByTag(tag: Tag): List<FieldDomain> =
        fields.filter { it.tag.name == tag.name }

    override suspend fun saveField(field: FieldDomain) {
        fields.removeAll { it.key.equals(field.key, ignoreCase = true) }
        fields += field
    }

    override suspend fun saveFieldIfNotExists(field: FieldDomain): Boolean {
        if (fields.any { it.key.equals(field.key, ignoreCase = true) }) return false
        fields += field
        return true
    }

    override suspend fun deleteField(key: String) {
        fields.removeAll { it.key.equals(key, ignoreCase = true) }
    }

    override suspend fun deleteFields(keys: List<String>) {
        val normalized = keys.map { it.lowercase() }.toSet()
        fields.removeAll { it.key.lowercase() in normalized }
    }

    override suspend fun updateValue(key: String, newValue: String) {
        fields.replaceAll { if (it.key.equals(key, ignoreCase = true)) it.copy(value = newValue) else it }
    }

    override suspend fun updateAlias(key: String, newAlias: String) {
        fields.replaceAll { if (it.key.equals(key, ignoreCase = true)) it.copy(keyAlias = newAlias) else it }
    }

    override suspend fun updateTag(key: String, newTag: Tag) {
        fields.replaceAll { if (it.key.equals(key, ignoreCase = true)) it.copy(tag = newTag) else it }
    }

    override suspend fun updateMcpShareable(key: String, shareable: Boolean) {
        fields.replaceAll {
            if (it.key.equals(key, ignoreCase = true)) it.copy(isMcpShareable = shareable) else it
        }
    }
}

private class InMemoryUserRepositoryFake : UserRepository {
    val usersList = mutableListOf<UserDomain>()
    override val users: Flow<List<UserDomain>> = MutableStateFlow(emptyList())
    override suspend fun getAllUsers(): List<UserDomain> = usersList.toList()

    override suspend fun saveUserIfNotExists(user: UserDomain): Boolean {
        if (usersList.any { it.email.equals(user.email, ignoreCase = true) }) return false
        usersList += user
        return true
    }

    override suspend fun upsertUser(user: UserDomain): Boolean {
        val idx = usersList.indexOfFirst { it.email.equals(user.email, ignoreCase = true) }
        return if (idx >= 0) {
            usersList[idx] = user
            false
        } else {
            usersList += user
            true
        }
    }

    override suspend fun updateUser(
        originalEmail: String,
        updatedUser: UserDomain
    ): ContactUpdateResult {
        val index = usersList.indexOfFirst { it.email.equals(originalEmail, ignoreCase = true) }
        if (index < 0) return ContactUpdateResult.NotFound
        if (usersList.withIndex().any { (otherIndex, user) ->
                otherIndex != index && user.email.equals(updatedUser.email, ignoreCase = true)
            }
        ) {
            return ContactUpdateResult.EmailAlreadyExists
        }
        if (usersList[index] == updatedUser) return ContactUpdateResult.Unchanged
        usersList[index] = updatedUser
        return ContactUpdateResult.Updated
    }

    override suspend fun deleteUser(email: String): Boolean =
        usersList.removeIf { it.email.equals(email, ignoreCase = true) }

    override suspend fun updateTag(email: String, newTag: Tag) {
        val idx = usersList.indexOfFirst { it.email.equals(email, ignoreCase = true) }
        if (idx >= 0) usersList[idx] = usersList[idx].copy(tag = newTag)
    }

    override suspend fun deleteUsers(emails: Collection<String>): Int {
        val before = usersList.size
        val normalized = emails.map { it.lowercase() }.toSet()
        usersList.removeIf { it.email.lowercase() in normalized }
        return before - usersList.size
    }
}
