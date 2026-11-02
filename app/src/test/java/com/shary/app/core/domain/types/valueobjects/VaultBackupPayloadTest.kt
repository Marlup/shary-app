package com.shary.app.core.domain.types.valueobjects

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultBackupPayloadTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun jsonRoundTripPreservesPortableData() {
        val payload = VaultBackupPayload(
            exportedAtEpochMillis = 1_760_000_000_000L,
            fields = listOf(
                FieldBackup(
                    key = "email",
                    value = "person@example.com",
                    keyAlias = "Personal email",
                    tag = "Personal|#FF00FF00",
                    dateAddedEpochMillis = 1_760_000_000_000L,
                    isMcpShareable = true
                )
            ),
            contacts = listOf(
                ContactBackup(
                    username = "Alex",
                    email = "alex@example.com",
                    tag = "Friends|#FF0000FF",
                    dateAddedEpochMillis = 1_760_000_000_000L
                )
            ),
            requests = listOf(
                RequestBackup(
                    fields = listOf(
                        FieldBackup(
                            key = "phone",
                            value = "+34600000000",
                            dateAddedEpochMillis = 1_760_000_000_000L
                        )
                    ),
                    user = "owner@example.com",
                    recipients = listOf("alex@example.com"),
                    dateAddedEpochMillis = 1_760_000_000_000L,
                    owned = true,
                    responded = false,
                    id = "request-1",
                    deliveryChannel = "CLOUD",
                    deliveryOutcomes = listOf(
                        DeliveryOutcomeBackup(
                            recipient = "alex@example.com",
                            status = "SUCCESS"
                        )
                    )
                )
            ),
            fieldTags = listOf("Personal|#FF00FF00"),
            contactTags = listOf("Friends|#FF0000FF"),
            settings = SettingsBackup(defaultCloudExpiryDays = 14)
        )

        val encoded = json.encodeToString(VaultBackupPayload.serializer(), payload)
        val decoded = json.decodeFromString(VaultBackupPayload.serializer(), encoded)

        assertEquals(payload, decoded)
    }
}
