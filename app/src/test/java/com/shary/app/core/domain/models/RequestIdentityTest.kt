package com.shary.app.core.domain.models

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestIdentityTest {

    @Test
    fun legacyIdentityIsDeterministicAndIgnoresResponseState() {
        val request = request(id = "")

        val initialId = request.stableRequestId()
        val respondedId = request.copy(responded = true).stableRequestId()

        assertTrue(initialId.startsWith("legacy-"))
        assertEquals(initialId, respondedId)
    }

    @Test
    fun explicitIdentityIsPreservedAndDifferentLegacyRequestsDoNotCollide() {
        assertEquals("request-123", request(id = " request-123 ").stableRequestId())
        assertNotEquals(
            request(id = "").stableRequestId(),
            request(id = "", requester = "bob@example.com").stableRequestId()
        )
    }

    @Test
    fun importedIdentityIsRouteSafeAndCanonicalizesAddressesAndKeys() {
        val first = stableImportedRequestId(
            requesterAddress = " Alice@Example.com ",
            recipientAddress = "owner@example.com",
            fieldKeys = listOf("Address", " secret "),
            externalReference = "cloud/reference+value"
        )
        val second = stableImportedRequestId(
            requesterAddress = "alice@example.com",
            recipientAddress = "OWNER@EXAMPLE.COM",
            fieldKeys = listOf("SECRET", "address"),
            externalReference = "cloud/reference+value"
        )

        assertTrue(first.matches(Regex("import-[a-f0-9]{64}")))
        assertEquals(first, second)
    }

    @Test
    fun legacyIdentitySeparatesRecipientAndFieldCategories() {
        val recipientsThenField = stableLegacyRequestId(
            user = "alice@example.com",
            requesterId = "sender-hash",
            dateAddedEpochMillis = 1L,
            owned = false,
            recipients = listOf("owner@example.com", "x"),
            fieldIdentityParts = listOf("key")
        )
        val recipientThenFields = stableLegacyRequestId(
            user = "alice@example.com",
            requesterId = "sender-hash",
            dateAddedEpochMillis = 1L,
            owned = false,
            recipients = listOf("owner@example.com"),
            fieldIdentityParts = listOf("x", "key")
        )

        assertNotEquals(recipientsThenField, recipientThenFields)
    }

    @Test
    fun cloudRequestIdentityAcceptsOnlyCanonicalUuid() {
        assertEquals(
            "123e4567-e89b-12d3-a456-426614174000",
            canonicalCloudRequestId(" 123e4567-e89b-12d3-a456-426614174000 ")
        )
        assertEquals(null, canonicalCloudRequestId("request-with-route/separator"))
    }

    private fun request(
        id: String,
        requester: String = "alice@example.com"
    ) = RequestDomain(
        fields = listOf(FieldDomain.create(key = "Secret", value = "")),
        user = requester,
        recipients = listOf("owner@example.com"),
        dateAdded = Instant.parse("2026-07-18T10:15:30Z"),
        owned = false,
        responded = false,
        id = id
    )
}
