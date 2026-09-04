package com.shary.app.ui.screens.request.utils

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shary.app.application.request.RequestInboxDecision
import com.shary.app.application.request.RequestInboxDecisionReceipt
import com.shary.app.application.request.RequestInboxDecisionState
import com.shary.app.core.domain.types.valueobjects.CloudInboxItem
import com.shary.app.core.domain.types.valueobjects.DataPayload
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RequestInboxReviewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun requestPreviewAcceptUsesASameFrameSubmissionLatch() {
        var accepted = 0
        var rejected = 0
        val item = sampleItem()

        composeTestRule.setContent {
            RequestInboxReviewDialog(
                isLoading = false,
                pendingItems = listOf(item),
                decisionState = RequestInboxDecisionState.Idle,
                resolveSenderLabel = { "Known requester" },
                onAccept = { accepted++ },
                onReject = { rejected++ },
                onReceiptConsumed = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Pending cloud requests").assertExists()
        composeTestRule.onNodeWithText("From: Known requester").assertExists()
        composeTestRule.onNodeWithText("Schema: v1").assertExists()
        composeTestRule.onNodeWithText("Requested keys (preview): passport, email").assertExists()

        composeTestRule.onNodeWithText("Accept").performClick()
        composeTestRule.onNodeWithText("Accept").assertIsNotEnabled()

        assertEquals(1, accepted)
        assertEquals(0, rejected)
    }

    @Test
    fun rejectRequiresConfirmationAndDoesNotExposeAccept() {
        var accepted = 0
        var rejected = 0
        val item = sampleItem()

        composeTestRule.setContent {
            RequestInboxReviewDialog(
                isLoading = false,
                pendingItems = listOf(item),
                decisionState = RequestInboxDecisionState.Idle,
                resolveSenderLabel = { "Known requester" },
                onAccept = { accepted++ },
                onReject = { rejected++ },
                onReceiptConsumed = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Reject").performClick()
        composeTestRule.onNodeWithText(
            "Reject this request? No local request will be imported."
        ).assertExists()
        composeTestRule.onNodeWithText("Accept").assertDoesNotExist()
        composeTestRule.onNodeWithText("Confirm reject").performClick()
        composeTestRule.onNodeWithText("Confirm reject").assertDoesNotExist()
        composeTestRule.onNodeWithText("Accept").assertIsNotEnabled()

        assertEquals(0, accepted)
        assertEquals(1, rejected)
    }

    @Test
    fun processingLocksTheDialogDecisionActions() {
        val item = sampleItem()

        composeTestRule.setContent {
            RequestInboxReviewDialog(
                isLoading = true,
                pendingItems = listOf(item),
                decisionState = RequestInboxDecisionState.Processing(
                    verification = item.payload.verification,
                    decision = RequestInboxDecision.ACCEPT
                ),
                resolveSenderLabel = { "Known requester" },
                onAccept = {},
                onReject = {},
                onReceiptConsumed = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Processing Accept...").assertExists()
        composeTestRule.onNodeWithText("Accept").assertDoesNotExist()
        composeTestRule.onNodeWithText("Reject").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun failedAcceptExposesOnlyTheSameDecisionRetry() {
        var accepted = 0
        val item = sampleItem()

        composeTestRule.setContent {
            RequestInboxReviewDialog(
                isLoading = false,
                pendingItems = listOf(item),
                decisionState = RequestInboxDecisionState.Failed(
                    verification = item.payload.verification,
                    decision = RequestInboxDecision.ACCEPT,
                    error = IllegalStateException("response unavailable"),
                    localRequestAvailable = true,
                    requestedKeyCount = 2
                ),
                resolveSenderLabel = { "Known requester" },
                onAccept = { accepted++ },
                onReject = {},
                onReceiptConsumed = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Retry Accept").performClick()
        composeTestRule.onNodeWithText("Retry Accept").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Retry Reject").assertDoesNotExist()

        assertEquals(1, accepted)
    }

    @Test
    fun successShowsTruthfulTerminalReceiptUntilDone() {
        var hasMoreItems: Boolean? = null
        val item = sampleItem()

        composeTestRule.setContent {
            RequestInboxReviewDialog(
                isLoading = false,
                pendingItems = emptyList(),
                decisionState = RequestInboxDecisionState.Succeeded(
                    RequestInboxDecisionReceipt(
                        verification = item.payload.verification,
                        decision = RequestInboxDecision.ACCEPT,
                        localRequestAvailable = true,
                        requestedKeyCount = 2,
                        backendAcknowledged = true
                    )
                ),
                resolveSenderLabel = { "Known requester" },
                onAccept = {},
                onReject = {},
                onReceiptConsumed = { hasMoreItems = it },
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Decision complete").assertExists()
        composeTestRule.onNodeWithText(
            "Local result: a received request is available with 2 requested keys."
        ).assertExists()
        composeTestRule.onNodeWithText("Cloud result: Accept was acknowledged.").assertExists()
        composeTestRule.onNodeWithText("Accept").assertDoesNotExist()
        composeTestRule.onNodeWithText("Reject").assertDoesNotExist()
        composeTestRule.onNodeWithText("Done").performClick()

        assertEquals(false, hasMoreItems)
    }
}

private fun sampleItem(): CloudInboxItem = CloudInboxItem(
    payload = DataPayload(
        user = "sender-hash",
        recipient = "recipient-hash",
        creationAt = 1_700_000_000,
        expiresAt = 1_700_086_400,
        data = "encrypted",
        verification = "verification",
        signature = "signature"
    ),
    previewFieldNames = listOf("passport", "email")
)
