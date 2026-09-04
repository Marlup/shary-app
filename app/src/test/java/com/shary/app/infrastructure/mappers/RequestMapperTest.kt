package com.shary.app.infrastructure.mappers

import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.core.domain.types.valueobjects.Purpose
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestMapperTest {

    @Test
    fun protoRoundTripPreservesExplicitRequestIdentity() {
        val original = request(id = "request-123")

        val restored = original.toProto(IdentityCodec).toDomain(IdentityCodec)

        assertEquals("request-123", restored.id)
        assertEquals("sender-hash-alice", restored.requesterId)
        assertEquals(original.user, restored.user)
        assertEquals(original.recipients, restored.recipients)
        assertEquals(original.dateAdded, restored.dateAdded)
        assertEquals(original.fields.map { it.key }, restored.fields.map { it.key })
    }

    @Test
    fun legacyBlankProtoIdentityIsStableAcrossReads() {
        val legacy = request(id = "").toProto(IdentityCodec).toBuilder().clearId().build()

        val first = legacy.toDomain(IdentityCodec)
        val second = legacy.toDomain(IdentityCodec)

        assertTrue(first.id.startsWith("legacy-"))
        assertEquals(first.id, second.id)
    }

    @Test
    fun protoRoundTripPreservesDeliveryChannelOutcomesAndTimestamp() {
        val original = request(id = "request-123").copy(
            deliveryChannel = RequestDeliveryChannel.CLOUD,
            deliveryOutcomes = listOf(
                RequestDeliveryOutcome("alice@example.com", StatusDataSentDb.SUCCESS),
                RequestDeliveryOutcome("bob@example.com", StatusDataSentDb.ERROR)
            ),
            deliveryUpdatedAt = Instant.parse("2026-07-20T09:30:00Z")
        )

        val restored = original.toProto(IdentityCodec).toDomain(IdentityCodec)

        assertEquals(original.deliveryChannel, restored.deliveryChannel)
        assertEquals(original.deliveryOutcomes, restored.deliveryOutcomes)
        assertEquals(original.deliveryUpdatedAt, restored.deliveryUpdatedAt)
    }

    private fun request(id: String) = RequestDomain(
        fields = listOf(
            FieldDomain(
                key = "Secret",
                value = "value",
                dateAdded = Instant.parse("2026-07-18T10:00:00Z")
            )
        ),
        user = "alice@example.com",
        recipients = listOf("owner@example.com"),
        dateAdded = Instant.parse("2026-07-18T10:15:30Z"),
        owned = false,
        responded = false,
        id = id,
        requesterId = "sender-hash-alice"
    )

    private data object IdentityCodec : FieldCodec {
        override fun encode(message: String, purpose: Purpose): String = message
        override fun decode(message: String, purpose: Purpose): String = message
    }
}
