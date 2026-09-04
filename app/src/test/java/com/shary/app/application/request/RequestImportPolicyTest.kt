package com.shary.app.application.request

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RequestImportPolicyTest {

    @Test
    fun matchingAuthenticatedSenderIsRetained() {
        val requesterId = requireAuthenticatedRequester(
            requesterAddress = " Alice@Example.com ",
            authenticatedRequesterId = "hash:alice@example.com",
            addressHasher = { "hash:$it" }
        )

        assertEquals("hash:alice@example.com", requesterId)
    }

    @Test
    fun embeddedThirdPartyAddressIsRejected() {
        assertThrows(SecurityException::class.java) {
            requireAuthenticatedRequester(
                requesterAddress = "bob@example.com",
                authenticatedRequesterId = "hash:alice@example.com",
                addressHasher = { "hash:$it" }
            )
        }
    }

    @Test
    fun requestedKeysAreTrimmedDeduplicatedAndBlankKeysAreDropped() {
        assertEquals(
            listOf("Secret", "Address"),
            normalizeRequestedFieldKeys(
                listOf(" Secret ", "", "secret", " Address ")
            )
        )
    }
}
