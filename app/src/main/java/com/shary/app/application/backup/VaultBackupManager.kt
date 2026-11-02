package com.shary.app.application.backup

import com.shary.app.core.domain.interfaces.repositories.ContactTagRepository
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.repositories.SettingsRepository
import com.shary.app.core.domain.interfaces.repositories.TagCreateResult
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.models.stableRequestId
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.deserialize
import com.shary.app.core.domain.types.enums.serialize
import com.shary.app.core.domain.types.valueobjects.AppSettings
import com.shary.app.core.domain.types.valueobjects.ContactBackup
import com.shary.app.core.domain.types.valueobjects.DeliveryOutcomeBackup
import com.shary.app.core.domain.types.valueobjects.FieldBackup
import com.shary.app.core.domain.types.valueobjects.RequestBackup
import com.shary.app.core.domain.types.valueobjects.SettingsBackup
import com.shary.app.core.domain.types.valueobjects.VaultBackupApplyResult
import com.shary.app.core.domain.types.valueobjects.VaultBackupPayload
import com.shary.app.core.domain.types.valueobjects.VaultBackupPreview
import com.shary.app.infrastructure.security.backup.VaultBackupCipher
import com.shary.app.infrastructure.security.backup.VaultBackupFormatException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Builds and applies the portable, encrypted vault backup.
 *
 * Import is additive and non-destructive: existing fields (by key), contacts
 * (by email), requests (by stable ID), and tags (by name) are kept. Credentials,
 * MCP grants/tokens, connector state, and workflow drafts are not exported.
 */
@Singleton
class VaultBackupManager @Inject constructor(
    private val fieldRepository: FieldRepository,
    private val userRepository: UserRepository,
    private val requestRepository: RequestRepository,
    private val tagRepository: TagRepository,
    private val contactTagRepository: ContactTagRepository,
    private val settingsRepository: SettingsRepository,
    private val cipher: VaultBackupCipher
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(passphrase: CharArray): ByteArray = withContext(Dispatchers.Default) {
        val payload = VaultBackupPayload(
            exportedAtEpochMillis = System.currentTimeMillis(),
            fields = fieldRepository.getAllFields().first().map { it.toBackup() },
            contacts = userRepository.getAllUsers().map { it.toBackup() },
            requests = buildList {
                requestRepository.getReceivedRequests().first().forEach { add(it.toBackup()) }
                requestRepository.getSentRequests().first().forEach { add(it.toBackup()) }
            },
            fieldTags = tagRepository.allTags.first().map { it.serialize() }.distinct(),
            contactTags = contactTagRepository.allTags.first().map { it.serialize() }.distinct(),
            settings = settingsRepository.settings.first().toBackup()
        )
        val plaintext = json.encodeToString(VaultBackupPayload.serializer(), payload)
            .toByteArray(Charsets.UTF_8)
        cipher.encrypt(plaintext, passphrase)
    }

    suspend fun inspect(bytes: ByteArray, passphrase: CharArray): VaultBackupPreview =
        withContext(Dispatchers.Default) {
            val plaintext = cipher.decrypt(bytes, passphrase)
            val payload = try {
                json.decodeFromString(
                    VaultBackupPayload.serializer(),
                    plaintext.toString(Charsets.UTF_8)
                )
            } catch (error: Exception) {
                throw VaultBackupFormatException(
                    "Vault backup JSON is invalid: ${error.message ?: "parse error"}."
                )
            }
            if (payload.format != VaultBackupPayload.FORMAT) {
                throw VaultBackupFormatException("Not a Shary vault backup.")
            }
            if (payload.version != VaultBackupPayload.VERSION) {
                throw VaultBackupFormatException(
                    "Unsupported vault backup version: ${payload.version}."
                )
            }
            VaultBackupPreview(
                payload = payload,
                fieldCount = payload.fields.size,
                contactCount = payload.contacts.size,
                requestCount = payload.requests.size,
                tagCount = (payload.fieldTags + payload.contactTags).distinct().size
            )
        }

    suspend fun apply(preview: VaultBackupPreview): VaultBackupApplyResult =
        withContext(Dispatchers.Default) {
            val warnings = mutableListOf<String>()
            val tags = importTags(preview.payload, warnings)
            val fields = importFields(preview.payload.fields, warnings)
            val contacts = importContacts(preview.payload.contacts, warnings)
            val requests = importRequests(preview.payload.requests, warnings)
            val settingsApplied = applySettings(preview.payload.settings)

            VaultBackupApplyResult(
                fieldsCreated = fields.first,
                fieldsSkipped = fields.second,
                contactsCreated = contacts.first,
                contactsSkipped = contacts.second,
                requestsCreated = requests.first,
                requestsSkipped = requests.second,
                tagsCreated = tags.first,
                tagsSkipped = tags.second,
                settingsApplied = settingsApplied,
                warnings = warnings
            )
        }

    private suspend fun importTags(
        payload: VaultBackupPayload,
        warnings: MutableList<String>
    ): Pair<Int, Int> {
        var created = 0
        var skipped = 0

        payload.fieldTags.forEach { raw ->
            val tag = tagFromBackup(raw) ?: return@forEach
            if (tag.name.equals("Unknown", ignoreCase = true)) return@forEach
            runCatching { tagRepository.addTag(tag.name, tag.toColor()) }
                .onSuccess { result ->
                    when (result) {
                        is TagCreateResult.Created -> created++
                        is TagCreateResult.AlreadyExists -> skipped++
                        is TagCreateResult.Invalid -> warnings += "Field tag '${tag.name}' was invalid."
                    }
                }
                .onFailure { warnings += "Field tag '${tag.name}' could not be imported." }
        }

        payload.contactTags.forEach { raw ->
            val tag = tagFromBackup(raw) ?: return@forEach
            if (tag.name.equals("Unknown", ignoreCase = true)) return@forEach
            runCatching { contactTagRepository.addTag(tag.name, tag.toColor()) }
                .onSuccess { result ->
                    when (result) {
                        is TagCreateResult.Created -> created++
                        is TagCreateResult.AlreadyExists -> skipped++
                        is TagCreateResult.Invalid -> warnings += "Contact tag '${tag.name}' was invalid."
                    }
                }
                .onFailure { warnings += "Contact tag '${tag.name}' could not be imported." }
        }

        return created to skipped
    }

    private suspend fun importFields(
        fields: List<FieldBackup>,
        warnings: MutableList<String>
    ): Pair<Int, Int> {
        var created = 0
        var skipped = 0
        fields.forEach { backup ->
            val field = backup.toDomain()
            if (field.key.isBlank()) {
                warnings += "A field with an empty key was skipped."
                return@forEach
            }
            runCatching { fieldRepository.saveFieldIfNotExists(field) }
                .onSuccess { added -> if (added) created++ else skipped++ }
                .onFailure { warnings += "Field '${field.key}' could not be imported." }
        }
        return created to skipped
    }

    private suspend fun importContacts(
        contacts: List<ContactBackup>,
        warnings: MutableList<String>
    ): Pair<Int, Int> {
        var created = 0
        var skipped = 0
        contacts.forEach { backup ->
            val contact = backup.toDomain()
            if (contact.email.isBlank()) {
                warnings += "A contact with an empty email was skipped."
                return@forEach
            }
            runCatching { userRepository.saveUserIfNotExists(contact) }
                .onSuccess { added -> if (added) created++ else skipped++ }
                .onFailure { warnings += "Contact '${contact.email}' could not be imported." }
        }
        return created to skipped
    }

    private suspend fun importRequests(
        requests: List<RequestBackup>,
        warnings: MutableList<String>
    ): Pair<Int, Int> {
        var created = 0
        var skipped = 0
        requests.forEach { backup ->
            val request = backup.toDomain().let { candidate ->
                candidate.copy(id = candidate.stableRequestId())
            }
            val alreadyExists = if (request.owned) {
                requestRepository.findSentRequest(request.id) != null
            } else {
                requestRepository.findReceivedRequest(request.id) != null
            }
            if (alreadyExists) {
                skipped++
                return@forEach
            }
            runCatching {
                if (request.owned) {
                    requestRepository.saveSentRequest(request)
                } else {
                    requestRepository.saveReceivedRequest(request)
                }
            }.onSuccess {
                created++
            }.onFailure {
                warnings += "Request '${request.id}' could not be imported."
            }
        }
        return created to skipped
    }

    private suspend fun applySettings(settings: SettingsBackup): Int {
        settingsRepository.setAutoOpenCloudInboxOnStart(settings.autoOpenCloudInboxOnStart)
        settingsRepository.setDefaultCloudExpiryDays(settings.defaultCloudExpiryDays)
        settingsRepository.setShowFriendlySenderIdentity(settings.showFriendlySenderIdentity)
        settingsRepository.setRequireBiometricForCloudInbox(settings.requireBiometricForCloudInbox)
        settingsRepository.setCompactListMode(settings.compactListMode)
        settingsRepository.setWifiOnlyCloudSync(settings.wifiOnlyCloudSync)
        settingsRepository.setCopyLongPressMillis(settings.copyLongPressMillis)
        settingsRepository.setClipboardAutoClearSeconds(settings.clipboardAutoClearSeconds)
        return 8
    }

    private fun tagFromBackup(raw: String): Tag? {
        if (raw.isBlank()) return null
        val tag = runCatching { Tag.deserialize(raw) }.getOrNull() ?: return null
        return if (tag.name.equals("Unknown", ignoreCase = true)) Tag.Unknown else tag
    }

    private fun FieldDomain.toBackup(): FieldBackup = FieldBackup(
        key = key,
        value = value,
        keyAlias = keyAlias,
        tag = tag.serialize(),
        dateAddedEpochMillis = dateAdded.toEpochMilli(),
        isMcpShareable = isMcpShareable
    )

    private fun FieldBackup.toDomain(): FieldDomain = FieldDomain(
        key = key,
        value = value,
        keyAlias = keyAlias,
        tag = tagFromBackup(tag) ?: Tag.Unknown,
        dateAdded = Instant.ofEpochMilli(dateAddedEpochMillis),
        isMcpShareable = isMcpShareable
    )

    private fun UserDomain.toBackup(): ContactBackup = ContactBackup(
        username = username,
        email = email,
        tag = tag.serialize(),
        dateAddedEpochMillis = dateAdded.toEpochMilli()
    )

    private fun ContactBackup.toDomain(): UserDomain = UserDomain(
        username = username,
        email = email,
        dateAdded = Instant.ofEpochMilli(dateAddedEpochMillis),
        tag = tagFromBackup(tag) ?: Tag.Unknown
    )

    private fun RequestDomain.toBackup(): RequestBackup = RequestBackup(
        fields = fields.map { it.toBackup() },
        user = user,
        recipients = recipients,
        dateAddedEpochMillis = dateAdded.toEpochMilli(),
        owned = owned,
        responded = responded,
        id = id,
        requesterId = requesterId,
        deliveryChannel = deliveryChannel?.name,
        deliveryOutcomes = deliveryOutcomes.map {
            DeliveryOutcomeBackup(recipient = it.recipient, status = it.status.name)
        },
        deliveryUpdatedAtEpochMillis = deliveryUpdatedAt?.toEpochMilli()
    )

    private fun RequestBackup.toDomain(): RequestDomain = RequestDomain(
        fields = fields.map { it.toDomain() },
        user = user,
        recipients = recipients,
        dateAdded = Instant.ofEpochMilli(dateAddedEpochMillis),
        owned = owned,
        responded = responded,
        id = id,
        requesterId = requesterId,
        deliveryChannel = deliveryChannel?.let {
            runCatching { RequestDeliveryChannel.valueOf(it) }.getOrNull()
        },
        deliveryOutcomes = deliveryOutcomes.mapNotNull {
            val status = runCatching { StatusDataSentDb.valueOf(it.status) }.getOrNull()
            status?.let { value -> RequestDeliveryOutcome(it.recipient, value) }
        },
        deliveryUpdatedAt = deliveryUpdatedAtEpochMillis?.let(Instant::ofEpochMilli)
    )

    private fun AppSettings.toBackup(): SettingsBackup =
        SettingsBackup(
            autoOpenCloudInboxOnStart = autoOpenCloudInboxOnStart,
            defaultCloudExpiryDays = defaultCloudExpiryDays,
            showFriendlySenderIdentity = showFriendlySenderIdentity,
            requireBiometricForCloudInbox = requireBiometricForCloudInbox,
            compactListMode = compactListMode,
            wifiOnlyCloudSync = wifiOnlyCloudSync,
            copyLongPressMillis = copyLongPressMillis,
            clipboardAutoClearSeconds = clipboardAutoClearSeconds
        )
}
