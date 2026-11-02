package com.shary.app.core.domain.types.valueobjects

import kotlinx.serialization.Serializable

/**
 * Versioned, encrypted vault backup payload.
 *
 * The file on disk is a binary container produced by VaultBackupCipher. This
 * payload is the plaintext JSON inside that container. It deliberately excludes
 * credentials, MCP tokens/grants, connector state, and workflow drafts.
 */
@Serializable
data class VaultBackupPayload(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAtEpochMillis: Long,
    val fields: List<FieldBackup> = emptyList(),
    val contacts: List<ContactBackup> = emptyList(),
    val requests: List<RequestBackup> = emptyList(),
    val fieldTags: List<String> = emptyList(),
    val contactTags: List<String> = emptyList(),
    val settings: SettingsBackup = SettingsBackup()
) {
    companion object {
        const val FORMAT = "shary-vault"
        const val VERSION = 1
    }
}

@Serializable
data class FieldBackup(
    val key: String,
    val value: String,
    val keyAlias: String = "",
    val tag: String = "",
    val dateAddedEpochMillis: Long,
    val isMcpShareable: Boolean = false
)

@Serializable
data class ContactBackup(
    val username: String = "",
    val email: String,
    val tag: String = "",
    val dateAddedEpochMillis: Long
)

@Serializable
data class RequestBackup(
    val fields: List<FieldBackup> = emptyList(),
    val user: String = "",
    val recipients: List<String> = emptyList(),
    val dateAddedEpochMillis: Long,
    val owned: Boolean,
    val responded: Boolean,
    val id: String = "",
    val requesterId: String = "",
    val deliveryChannel: String? = null,
    val deliveryOutcomes: List<DeliveryOutcomeBackup> = emptyList(),
    val deliveryUpdatedAtEpochMillis: Long? = null
)

@Serializable
data class DeliveryOutcomeBackup(
    val recipient: String,
    val status: String
)

@Serializable
data class SettingsBackup(
    val autoOpenCloudInboxOnStart: Boolean = false,
    val defaultCloudExpiryDays: Int = 7,
    val showFriendlySenderIdentity: Boolean = true,
    val requireBiometricForCloudInbox: Boolean = false,
    val compactListMode: Boolean = false,
    val wifiOnlyCloudSync: Boolean = false,
    val copyLongPressMillis: Int = 600,
    val clipboardAutoClearSeconds: Int = 0
)

data class VaultBackupPreview(
    val payload: VaultBackupPayload,
    val fieldCount: Int,
    val contactCount: Int,
    val requestCount: Int,
    val tagCount: Int
)

data class VaultBackupApplyResult(
    val fieldsCreated: Int = 0,
    val fieldsSkipped: Int = 0,
    val contactsCreated: Int = 0,
    val contactsSkipped: Int = 0,
    val requestsCreated: Int = 0,
    val requestsSkipped: Int = 0,
    val tagsCreated: Int = 0,
    val tagsSkipped: Int = 0,
    val settingsApplied: Int = 0,
    val warnings: List<String> = emptyList()
)
