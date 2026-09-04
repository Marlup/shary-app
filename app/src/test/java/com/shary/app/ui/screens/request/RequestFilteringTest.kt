package com.shary.app.ui.screens.request

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestFilteringTest {

    private val requests = listOf(
        request(
            id = "request-alice",
            sender = "alice@example.com",
            recipients = listOf("bob@example.com"),
            keys = listOf("wifi-password", "address")
        ),
        request(
            id = "request-carol",
            sender = "carol@example.com",
            recipients = listOf("dave@example.com"),
            keys = listOf("phone-number")
        )
    )

    @Test
    fun blankQueryReturnsOriginalRequests() {
        assertEquals(requests, filterRequestsByQuery(requests, "  "))
    }

    @Test
    fun queryMatchesSenderRecipientAndFieldKeyIgnoringCase() {
        assertEquals(listOf(requests[0]), filterRequestsByQuery(requests, "ALICE"))
        assertEquals(listOf(requests[0]), filterRequestsByQuery(requests, "BoB@"))
        assertEquals(listOf(requests[1]), filterRequestsByQuery(requests, "PHONE"))
    }

    @Test
    fun unmatchedQueryReturnsEmptyList() {
        assertEquals(emptyList<RequestDomain>(), filterRequestsByQuery(requests, "missing"))
    }

    @Test
    fun detailLookupUsesStableIdAfterTheListReorders() {
        val selectedId = requests[1].id
        val reordered = requests.reversed()

        assertEquals(requests[1], findRequestById(reordered, selectedId))
        assertEquals(null, findRequestById(reordered, "missing-request"))
    }

    private fun request(
        id: String,
        sender: String,
        recipients: List<String>,
        keys: List<String>
    ) = RequestDomain(
        fields = keys.map { key -> FieldDomain.create(key = key, value = "value") },
        user = sender,
        recipients = recipients,
        dateAdded = Instant.EPOCH,
        owned = false,
        responded = false,
        id = id
    )
}
