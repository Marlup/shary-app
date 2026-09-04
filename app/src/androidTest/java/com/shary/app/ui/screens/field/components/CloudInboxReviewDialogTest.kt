package com.shary.app.ui.screens.field.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shary.app.application.cloud.CloudImportDiff
import com.shary.app.core.domain.types.valueobjects.CloudInboxItem
import com.shary.app.core.domain.types.valueobjects.DataPayload
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CloudInboxReviewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsImportDiffAndInvokesDecisionCallbacks() {
        var accepted = 0
        var rejected = 0
        val item = inboxItem(
            previewKeys = listOf("email", "passport"),
            diff = CloudImportDiff(
                addedKeys = listOf("passport"),
                duplicateKeys = listOf("email"),
                changedKeys = listOf("email")
            )
        )

        composeTestRule.setContent {
            CloudInboxReviewDialog(
                isLoading = false,
                pendingItems = listOf(item),
                resolveSenderLabel = { "Known sender" },
                onAccept = { accepted++ },
                onReject = { rejected++ },
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Pending cloud data").assertExists()
        composeTestRule.onNodeWithText("From: Known sender").assertExists()
        composeTestRule.onNodeWithText("Schema: v1").assertExists()
        composeTestRule.onNodeWithText("Review needed before import").assertExists()
        composeTestRule.onNodeWithText("added: passport", substring = true).assertExists()
        composeTestRule.onNodeWithText("changed: email", substring = true).assertExists()
        composeTestRule.onNodeWithText("Field names (preview): email, passport").assertExists()

        composeTestRule.onNodeWithText("Reject").performClick()
        composeTestRule.onNodeWithText("Accept").performClick()

        assertEquals(1, rejected)
        assertEquals(1, accepted)
    }

    private fun inboxItem(
        previewKeys: List<String>,
        diff: CloudImportDiff
    ): CloudInboxItem = CloudInboxItem(
        payload = DataPayload(
            user = "sender-hash",
            recipient = "recipient-hash",
            creationAt = 1_700_000_000,
            expiresAt = 1_700_086_400,
            data = "encrypted",
            verification = "verification",
            signature = "signature"
        ),
        previewFieldNames = previewKeys,
        importDiff = diff
    )
}
