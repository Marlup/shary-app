package com.shary.app.ui.screens.summary

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.ResponseDraft
import com.shary.app.core.domain.models.UserDomain
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryFieldReviewStateTest {

    @Test
    fun responseRouteIgnoresStaleContactsAndUsesOnlyAuthenticatedRequester() {
        val state = resolveSummaryFieldReviewState(
            cachedFields = listOf(field("secret"), field("unrelated")),
            generalRecipients = listOf(UserDomain(email = "bob@example.com")),
            workflow = workflow(matchedKeys = listOf("secret")),
            responseRequestId = "request-alice"
        )

        assertEquals(listOf("secret"), state.fields.map { it.key })
        assertEquals(listOf("alice@example.com"), state.recipients.map { it.email })
        assertFalse(state.responseStateMissing)
    }

    @Test
    fun normalShareRouteKeepsGeneralRecipients() {
        val bob = UserDomain(email = "bob@example.com")
        val state = resolveSummaryFieldReviewState(
            cachedFields = listOf(field("secret")),
            generalRecipients = listOf(bob),
            workflow = workflow(matchedKeys = listOf("secret")),
            responseRequestId = null
        )

        assertEquals(listOf(bob), state.recipients)
        assertFalse(state.responseStateMissing)
    }

    @Test
    fun mismatchedOrIncompleteResponseNeverFallsBackToContacts() {
        val bob = UserDomain(email = "bob@example.com")
        val mismatched = resolveSummaryFieldReviewState(
            cachedFields = listOf(field("secret")),
            generalRecipients = listOf(bob),
            workflow = workflow(matchedKeys = listOf("secret")),
            responseRequestId = "request-other"
        )
        val missingField = resolveSummaryFieldReviewState(
            cachedFields = listOf(field("secret")),
            generalRecipients = listOf(bob),
            workflow = workflow(matchedKeys = listOf("missing")),
            responseRequestId = "request-alice"
        )

        assertTrue(mismatched.responseStateMissing)
        assertTrue(mismatched.recipients.isEmpty())
        assertTrue(missingField.responseStateMissing)
        assertTrue(missingField.recipients.isEmpty())
    }

    private fun workflow(matchedKeys: List<String>): RequestWorkflowState {
        val request = RequestDomain(
            fields = listOf(field("requested")),
            user = "alice@example.com",
            recipients = listOf("owner@example.com"),
            dateAdded = Instant.EPOCH,
            owned = false,
            responded = false,
            id = "request-alice",
            requesterId = "sender-hash-alice"
        )
        return RequestWorkflowState(
            activeReceivedRequest = request,
            responseDraft = ResponseDraft(
                requestId = request.id,
                requesterId = request.requesterId,
                requesterAddress = request.user,
                matchedFieldKeys = matchedKeys
            )
        )
    }

    private fun field(key: String) = FieldDomain(
        key = key,
        value = "value",
        dateAdded = Instant.EPOCH
    )
}
