package com.shary.app.application.backup

import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.interfaces.repositories.ContactTagRepository
import com.shary.app.core.domain.interfaces.repositories.FieldLoadSnapshot
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.repositories.SettingsRepository
import com.shary.app.core.domain.interfaces.repositories.TagCreateResult
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.interfaces.repositories.TagUpdateResult
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.AppSettings
import com.shary.app.core.domain.types.valueobjects.ContactBackup
import com.shary.app.core.domain.types.valueobjects.FieldBackup
import com.shary.app.core.domain.types.valueobjects.RequestBackup
import com.shary.app.core.domain.types.valueobjects.SettingsBackup
import com.shary.app.core.domain.types.valueobjects.VaultBackupPayload
import com.shary.app.core.domain.types.valueobjects.VaultBackupPreview
import com.shary.app.infrastructure.security.backup.VaultBackupCipher
import com.shary.app.infrastructure.security.derivation.kdf.Pbkdf2Kdf
import com.shary.app.infrastructure.security.messageCipher.AesGcmCipher
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultBackupManagerTest {

    @Test
    fun importIsAdditiveAndSkipsDuplicates() = runBlocking {
        val fields = FakeFieldRepository(
            mutableListOf(FieldDomain("existing", "old", dateAdded = Instant.ofEpochMilli(1)))
        )
        val users = FakeUserRepository(
            mutableListOf(UserDomain(username = "Existing", email = "existing@example.com"))
        )
        val requests = FakeRequestRepository(
            sent = mutableListOf(
                RequestDomain(
                    fields = emptyList(),
                    user = "me@example.com",
                    recipients = listOf("a@example.com"),
                    dateAdded = Instant.ofEpochMilli(1),
                    owned = true,
                    responded = false,
                    id = "req-existing"
                )
            )
        )
        val fieldTags = FakeTagRepository(mutableListOf("Friends" to Color.Red))
        val contactTags = FakeContactTagRepository(mutableListOf("Family" to Color.Blue))
        val settings = FakeSettingsRepository()

        val manager = VaultBackupManager(
            fieldRepository = fields,
            userRepository = users,
            requestRepository = requests,
            tagRepository = fieldTags,
            contactTagRepository = contactTags,
            settingsRepository = settings,
            cipher = VaultBackupCipher(Pbkdf2Kdf(), AesGcmCipher())
        )

        val payload = VaultBackupPayload(
            exportedAtEpochMillis = 1,
            fields = listOf(
                FieldBackup(
                    key = "existing",
                    value = "changed",
                    dateAddedEpochMillis = 1
                ),
                FieldBackup(
                    key = "new",
                    value = "value",
                    dateAddedEpochMillis = 1
                )
            ),
            contacts = listOf(
                ContactBackup(email = "existing@example.com", dateAddedEpochMillis = 1),
                ContactBackup(email = "new@example.com", dateAddedEpochMillis = 1)
            ),
            requests = listOf(
                RequestBackup(
                    dateAddedEpochMillis = 1,
                    owned = true,
                    responded = false,
                    id = "req-existing"
                ),
                RequestBackup(
                    dateAddedEpochMillis = 1,
                    owned = true,
                    responded = false,
                    id = "req-new-sent"
                ),
                RequestBackup(
                    dateAddedEpochMillis = 1,
                    owned = false,
                    responded = false,
                    id = "req-new-received"
                )
            ),
            fieldTags = listOf("Friends|#FFFF0000", "Work|#FF00FF00"),
            contactTags = listOf("Family|#FF0000FF"),
            settings = SettingsBackup(defaultCloudExpiryDays = 14)
        )

        val result = manager.apply(
            VaultBackupPreview(
                payload = payload,
                fieldCount = payload.fields.size,
                contactCount = payload.contacts.size,
                requestCount = payload.requests.size,
                tagCount = 3
            )
        )

        assertEquals(1, result.fieldsCreated)
        assertEquals(1, result.fieldsSkipped)
        assertEquals(1, result.contactsCreated)
        assertEquals(1, result.contactsSkipped)
        assertEquals(2, result.requestsCreated)
        assertEquals(1, result.requestsSkipped)
        assertEquals(1, result.tagsCreated)
        assertEquals(2, result.tagsSkipped)
        assertEquals(8, result.settingsApplied)
        assertEquals(14, settings.state.value.defaultCloudExpiryDays)
        assertTrue(fields.fields.any { it.key == "new" })
        assertTrue(users.contacts.any { it.email == "new@example.com" })
        assertTrue(requests.sent.any { it.id == "req-new-sent" })
        assertTrue(requests.received.any { it.id == "req-new-received" })
    }

    private class FakeFieldRepository(
        val fields: MutableList<FieldDomain>
    ) : FieldRepository {
        override suspend fun getAllFields(): Flow<List<FieldDomain>> = flowOf(fields.toList())

        override suspend fun getAllFieldsProgressive(
            firstBatchSize: Int,
            chunkSize: Int
        ): Flow<List<FieldDomain>> = flowOf(fields.toList())

        override suspend fun getFieldLoadSnapshots(
            firstBatchSize: Int,
            chunkSize: Int
        ): Flow<FieldLoadSnapshot> = flowOf(
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
            fields += field
        }

        override suspend fun saveFieldIfNotExists(field: FieldDomain): Boolean {
            if (fields.any { it.key.equals(field.key, ignoreCase = true) }) return false
            fields += field
            return true
        }

        override suspend fun deleteField(key: String) {
            fields.removeAll { it.key == key }
        }

        override suspend fun deleteFields(keys: List<String>) {
            fields.removeAll { it.key in keys }
        }

        override suspend fun updateValue(key: String, newValue: String) = Unit
        override suspend fun updateAlias(key: String, newAlias: String) = Unit
        override suspend fun updateTag(key: String, newTag: Tag) = Unit
        override suspend fun updateMcpShareable(key: String, shareable: Boolean) = Unit
    }

    private class FakeUserRepository(
        val contacts: MutableList<UserDomain>
    ) : UserRepository {
        override val users: Flow<List<UserDomain>> = flowOf(contacts.toList())

        override suspend fun getAllUsers(): List<UserDomain> = contacts.toList()

        override suspend fun saveUserIfNotExists(user: UserDomain): Boolean {
            if (contacts.any { it.email.equals(user.email, ignoreCase = true) }) return false
            contacts += user
            return true
        }

        override suspend fun upsertUser(user: UserDomain): Boolean {
            val index = contacts.indexOfFirst { it.email.equals(user.email, ignoreCase = true) }
            if (index >= 0) {
                contacts[index] = user
                return false
            }
            contacts += user
            return true
        }

        override suspend fun updateUser(
            originalEmail: String,
            updatedUser: UserDomain
        ) = com.shary.app.core.domain.interfaces.repositories.ContactUpdateResult.Updated

        override suspend fun updateTag(email: String, newTag: Tag) = Unit
        override suspend fun deleteUser(email: String): Boolean =
            contacts.removeAll { it.email == email }

        override suspend fun deleteUsers(emails: Collection<String>): Int {
            val before = contacts.size
            contacts.removeAll { it.email in emails }
            return before - contacts.size
        }
    }

    private class FakeRequestRepository(
        val sent: MutableList<RequestDomain>,
        val received: MutableList<RequestDomain> = mutableListOf()
    ) : RequestRepository {
        override fun getReceivedRequests(): Flow<List<RequestDomain>> = flowOf(received.toList())
        override fun getSentRequests(): Flow<List<RequestDomain>> = flowOf(sent.toList())

        override suspend fun findReceivedRequest(requestId: String): RequestDomain? =
            received.firstOrNull { it.id == requestId }

        override suspend fun findSentRequest(requestId: String): RequestDomain? =
            sent.firstOrNull { it.id == requestId }

        override suspend fun saveReceivedRequest(request: RequestDomain) {
            received += request
        }

        override suspend fun saveSentRequest(request: RequestDomain) {
            sent.removeAll { it.id == request.id }
            sent += request
        }

        override suspend fun findWorkflowDraft(ownerAddress: String) = null
        override suspend fun saveWorkflowDraft(
            ownerAddress: String,
            workflow: com.shary.app.core.domain.models.RequestWorkflowState
        ) = Unit

        override suspend fun clearWorkflowDraft(ownerAddress: String) = Unit

        override suspend fun updateReceivedRequestDelivery(
            requestId: String,
            channel: RequestDeliveryChannel,
            outcomes: List<RequestDeliveryOutcome>,
            updatedAt: Instant
        ): Boolean = false

        override suspend fun markReceivedRequestResponded(
            request: RequestDomain,
            responded: Boolean
        ): Boolean = false
    }

    private class FakeTagRepository(
        val tags: MutableList<Pair<String, Color>>
    ) : TagRepository {
        override val allTags: Flow<List<Tag>> = flowOf(
            tags.map { Tag.fromString(it.first, it.second) }
        )

        override suspend fun addTag(name: String, color: Color): TagCreateResult {
            if (tags.any { it.first.equals(name, ignoreCase = true) }) {
                return TagCreateResult.AlreadyExists(Tag.fromString(name, color))
            }
            tags += name to color
            return TagCreateResult.Created(Tag.fromString(name, color))
        }

        override suspend fun updateTag(
            originalName: String,
            name: String,
            color: Color
        ): TagUpdateResult = TagUpdateResult.NotFound

        override suspend fun removeTag(name: String) = Unit
    }

    private class FakeContactTagRepository(
        val tags: MutableList<Pair<String, Color>>
    ) : ContactTagRepository {
        override val allTags: Flow<List<Tag>> = flowOf(
            tags.map { Tag.fromString(it.first, it.second) }
        )

        override suspend fun addTag(name: String, color: Color): TagCreateResult {
            if (tags.any { it.first.equals(name, ignoreCase = true) }) {
                return TagCreateResult.AlreadyExists(Tag.fromString(name, color))
            }
            tags += name to color
            return TagCreateResult.Created(Tag.fromString(name, color))
        }

        override suspend fun updateTag(
            originalName: String,
            name: String,
            color: Color
        ): TagUpdateResult = TagUpdateResult.NotFound

        override suspend fun removeTag(name: String) = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        val state = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = state

        override suspend fun setRememberEmailAtLogin(enabled: Boolean) = Unit
        override suspend fun saveRememberedEmail(email: String) = Unit
        override suspend fun clearRememberedEmail() = Unit
        override suspend fun setMcpEnabled(enabled: Boolean) = Unit
        override suspend fun setAutoOpenCloudInboxOnStart(enabled: Boolean) = Unit
        override suspend fun setDefaultCloudExpiryDays(days: Int) {
            state.value = state.value.copy(defaultCloudExpiryDays = days)
        }

        override suspend fun setShowFriendlySenderIdentity(enabled: Boolean) = Unit
        override suspend fun setRequireBiometricForCloudInbox(enabled: Boolean) = Unit
        override suspend fun setCompactListMode(enabled: Boolean) = Unit
        override suspend fun setWifiOnlyCloudSync(enabled: Boolean) = Unit
        override suspend fun setCopyLongPressMillis(milliseconds: Int) = Unit
        override suspend fun setClipboardAutoClearSeconds(seconds: Int) = Unit
    }
}
