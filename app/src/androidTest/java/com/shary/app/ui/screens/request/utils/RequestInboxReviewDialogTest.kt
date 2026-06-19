package com.shary.app.ui.screens.request.utils

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shary.app.core.domain.types.valueobjects.CloudInboxItem
import com.shary.app.core.domain.types.valueobjects.DataPayload
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RequestInboxReviewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsRequestPreviewAndInvokesDecisionCallbacks() {
        var accepted = 0
        var rejected = 0
        val item = CloudInboxItem(
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

        composeTestRule.setContent {
            RequestInboxReviewDialog(
                isLoading = false,
                pendingItems = listOf(item),
                resolveSenderLabel = { "Known requester" },
                onAccept = { accepted++ },
                onReject = { rejected++ },
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Pending cloud requests").assertExists()
        composeTestRule.onNodeWithText("From: Known requester").assertExists()
        composeTestRule.onNodeWithText("Schema: v1").assertExists()
        composeTestRule.onNodeWithText("Requested keys (preview): passport, email").assertExists()

        composeTestRule.onNodeWithText("Reject").performClick()
        composeTestRule.onNodeWithText("Accept").performClick()

        assertEquals(1, rejected)
        assertEquals(1, accepted)
    }
}
