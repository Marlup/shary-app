package com.shary.app.application.request

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.OutgoingRequestDraft
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.RequestedFieldDraft
import com.shary.app.core.domain.types.enums.RequestListMode
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestWorkflowReducerTest {

    @Test
    fun selectingReceivedRequestDoesNotChangeOutgoingDraft() {
        val outgoing = OutgoingRequestDraft(listOf(RequestedFieldDraft("outgoing-key")))
        val initial = RequestWorkflowState(outgoingRequestDraft = outgoing)
        val request = receivedRequest(id = "request-alice", requester = "alice@example.com")

        val updated = reduceRequestWorkflow(
            initial,
            RequestWorkflowAction.SelectReceivedRequest(request)
        )

        assertEquals(outgoing, updated.outgoingRequestDraft)
        assertEquals(request, updated.activeReceivedRequest)
        assertEquals("request-alice", updated.responseDraft?.requestId)
        assertEquals("alice@example.com", updated.responseDraft?.requesterAddress)
        assertEquals("sender-hash-alice", updated.responseDraft?.requesterId)
    }

    @Test
    fun modeSwitchesNeverCopyReceivedFieldsIntoOutgoingDraft() {
        val outgoing = OutgoingRequestDraft(listOf(RequestedFieldDraft("outgoing-key")))
        val selected = reduceRequestWorkflow(
            RequestWorkflowState(outgoingRequestDraft = outgoing),
            RequestWorkflowAction.SelectReceivedRequest(
                receivedRequest(id = "request-alice", requester = "alice@example.com")
            )
        )

        val sent = reduceRequestWorkflow(
            selected,
            RequestWorkflowAction.ChangeMode(RequestListMode.SENT)
        )
        val receivedAgain = reduceRequestWorkflow(
            sent,
            RequestWorkflowAction.ChangeMode(RequestListMode.RECEIVED)
        )

        assertEquals(outgoing, sent.outgoingRequestDraft)
        assertEquals(outgoing, receivedAgain.outgoingRequestDraft)
        assertNull(sent.activeReceivedRequest)
        assertNull(sent.responseDraft)
        assertNull(receivedAgain.activeReceivedRequest)
        assertNull(receivedAgain.responseDraft)
    }

    @Test
    fun responseMatchesRequireTheCurrentRequestIdentity() {
        val selected = reduceRequestWorkflow(
            RequestWorkflowState(),
            RequestWorkflowAction.SelectReceivedRequest(
                receivedRequest(id = "request-alice", requester = "alice@example.com")
            )
        )

        val rejected = reduceRequestWorkflow(
            selected,
            RequestWorkflowAction.SetResponseMatches(
                requestId = "request-bob",
                matchedFieldKeys = listOf("secret")
            )
        )
        val accepted = reduceRequestWorkflow(
            selected,
            RequestWorkflowAction.SetResponseMatches(
                requestId = "request-alice",
                matchedFieldKeys = listOf(" Secret ", "secret", "Address")
            )
        )

        assertEquals(selected, rejected)
        assertEquals(emptyList<String>(), rejected.responseDraft?.matchedFieldKeys)
        assertEquals(listOf("secret", "address"), accepted.responseDraft?.matchedFieldKeys)
        assertEquals("alice@example.com", accepted.responseDraft?.requesterAddress)
    }

    @Test
    fun clearingResponsePreservesOutgoingDraftAndSessionResetClearsEverything() {
        val outgoing = OutgoingRequestDraft(listOf(RequestedFieldDraft("outgoing-key")))
        val selected = reduceRequestWorkflow(
            RequestWorkflowState(outgoingRequestDraft = outgoing),
            RequestWorkflowAction.SelectReceivedRequest(
                receivedRequest(id = "request-alice", requester = "alice@example.com")
            )
        )

        val responseCleared = reduceRequestWorkflow(
            selected,
            RequestWorkflowAction.ClearReceivedWorkflow
        )
        val sessionCleared = reduceRequestWorkflow(
            selected,
            RequestWorkflowAction.ClearSession
        )

        assertNull(responseCleared.activeReceivedRequest)
        assertNull(responseCleared.responseDraft)
        assertEquals(outgoing, responseCleared.outgoingRequestDraft)
        assertEquals(RequestWorkflowState(), sessionCleared)
    }

    @Test
    fun invalidReceivedSelectionClearsAnyStaleResponseState() {
        val selected = reduceRequestWorkflow(
            RequestWorkflowState(),
            RequestWorkflowAction.SelectReceivedRequest(
                receivedRequest(id = "request-alice", requester = "alice@example.com")
            )
        )

        val rejected = reduceRequestWorkflow(
            selected,
            RequestWorkflowAction.SelectReceivedRequest(
                receivedRequest(id = "", requester = "mallory@example.com")
            )
        )

        assertNull(rejected.activeReceivedRequest)
        assertNull(rejected.responseDraft)
    }

    @Test
    fun completedReceivedRequestCannotStartAnotherResponse() {
        val completedRequest = receivedRequest(
            id = "request-alice",
            requester = "alice@example.com"
        ).copy(responded = true)

        val state = reduceRequestWorkflow(
            RequestWorkflowState(),
            RequestWorkflowAction.SelectReceivedRequest(completedRequest)
        )

        assertNull(state.activeReceivedRequest)
        assertNull(state.responseDraft)
    }

    @Test
    fun outgoingDraftRetainsOnlyRequestedKeyMetadata() {
        val updated = reduceRequestWorkflow(
            RequestWorkflowState(mode = RequestListMode.SENT),
            RequestWorkflowAction.SetOutgoingFields(
                listOf(
                    FieldDomain.create(
                        key = "secret",
                        value = "must-not-enter-request-draft",
                        keyAlias = "Account secret"
                    )
                ),
                candidateRequestId = "0f870d06-ff21-4eb3-8e21-465773882293"
            )
        )

        assertEquals(
            listOf(RequestedFieldDraft("secret", "Account secret")),
            updated.outgoingRequestDraft.requestedFields
        )
        assertFalse(updated.toString().contains("must-not-enter-request-draft"))
    }

    @Test
    fun outgoingRetryKeepsIdUntilDraftContentChangesOrClears() {
        val firstId = "0f870d06-ff21-4eb3-8e21-465773882293"
        val replacementId = "087f4f24-e6f1-45f0-8ed3-31ef5178d428"
        val firstFields = listOf(field("secret"))
        val created = reduceRequestWorkflow(
            RequestWorkflowState(mode = RequestListMode.SENT),
            RequestWorkflowAction.SetOutgoingFields(firstFields, firstId)
        )

        val retried = reduceRequestWorkflow(
            created,
            RequestWorkflowAction.SetOutgoingFields(firstFields, replacementId)
        )
        val replaced = reduceRequestWorkflow(
            retried,
            RequestWorkflowAction.SetOutgoingFields(
                firstFields + field("address"),
                replacementId
            )
        )
        val cleared = reduceRequestWorkflow(
            replaced,
            RequestWorkflowAction.ClearOutgoingDraft
        )

        assertEquals(firstId, created.outgoingRequestDraft.requestId)
        assertEquals(firstId, retried.outgoingRequestDraft.requestId)
        assertEquals(replacementId, replaced.outgoingRequestDraft.requestId)
        assertEquals(OutgoingRequestDraft(), cleared.outgoingRequestDraft)
    }

    @Test
    fun outgoingRecipientsAreNormalizedAndRotateIdentityOnlyWhenTheyChange() {
        val fieldsId = "0f870d06-ff21-4eb3-8e21-465773882293"
        val recipientsId = "087f4f24-e6f1-45f0-8ed3-31ef5178d428"
        val replacementId = "a0999dc1-aec2-41ec-bf54-4dc3415c1d33"
        val withFields = reduceRequestWorkflow(
            RequestWorkflowState(mode = RequestListMode.SENT),
            RequestWorkflowAction.SetOutgoingFields(listOf(field("secret")), fieldsId)
        )
        val withRecipients = reduceRequestWorkflow(
            withFields,
            RequestWorkflowAction.SetOutgoingRecipients(
                recipientAddresses = listOf(" Alice@Example.com ", "alice@example.com"),
                candidateRequestId = recipientsId
            )
        )
        val unchanged = reduceRequestWorkflow(
            withRecipients,
            RequestWorkflowAction.SetOutgoingRecipients(
                recipientAddresses = listOf("alice@example.com"),
                candidateRequestId = replacementId
            )
        )
        val changed = reduceRequestWorkflow(
            unchanged,
            RequestWorkflowAction.SetOutgoingRecipients(
                recipientAddresses = listOf("bob@example.com"),
                candidateRequestId = replacementId
            )
        )

        assertEquals(listOf("alice@example.com"), withRecipients.outgoingRequestDraft.recipientAddresses)
        assertEquals(recipientsId, withRecipients.outgoingRequestDraft.requestId)
        assertEquals(recipientsId, unchanged.outgoingRequestDraft.requestId)
        assertEquals(replacementId, changed.outgoingRequestDraft.requestId)
    }

    @Test
    fun completingResponseKeepsReviewIdentityAndMarksItComplete() {
        val selected = reduceRequestWorkflow(
            RequestWorkflowState(),
            RequestWorkflowAction.SelectReceivedRequest(
                receivedRequest(id = "request-alice", requester = "alice@example.com")
            )
        )

        val completed = reduceRequestWorkflow(
            selected,
            RequestWorkflowAction.CompleteResponse("request-alice")
        )

        assertEquals("request-alice", completed.activeReceivedRequest?.id)
        assertTrue(completed.activeReceivedRequest?.responded == true)
        assertTrue(completed.responseDraft?.completed == true)
    }

    private fun receivedRequest(id: String, requester: String) = RequestDomain(
        fields = listOf(field("requested-key")),
        user = requester,
        recipients = listOf("owner@example.com"),
        dateAdded = Instant.EPOCH,
        owned = false,
        responded = false,
        id = id,
        requesterId = "sender-hash-alice"
    )

    private fun field(key: String) = FieldDomain.create(key = key, value = "value")
}
