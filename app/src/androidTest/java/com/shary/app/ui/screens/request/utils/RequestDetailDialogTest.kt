package com.shary.app.ui.screens.request.utils

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.ui.components.SharyModalAction
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RequestDetailDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun receivedDetailShowsCompleteIdentityFieldsAndMatchProgress() {
        var actionCount = 0
        val request = request(
            fields = listOf(
                FieldDomain.create(key = "legal-name", value = "").copy(keyAlias = "Full name"),
                FieldDomain.create(key = "phone-number", value = "")
            ),
            user = "requester@example.com",
            recipients = listOf("owner@example.com")
        )

        composeTestRule.setContent {
            RequestDetailDialog(
                request = request,
                mode = RequestListMode.RECEIVED,
                onDismiss = {},
                matchedFieldCount = 1,
                primaryAction = SharyModalAction(
                    label = "Continue matching",
                    onClick = { actionCount++ }
                )
            )
        }

        composeTestRule.onNodeWithText("Received • Pending").assertExists()
        composeTestRule.onNodeWithText("Requester").assertExists()
        composeTestRule.onAllNodesWithText("requester@example.com").assertCountEquals(2)
        composeTestRule.onNodeWithText("Recipients (1)").assertExists()
        composeTestRule.onNodeWithText("owner@example.com").assertExists()
        composeTestRule.onNodeWithText("Requested fields (2)").assertExists()
        composeTestRule.onNodeWithText("legal-name").assertExists()
        composeTestRule.onNodeWithText("Alias: Full name").assertExists()
        composeTestRule.onNodeWithText("phone-number").assertExists()
        composeTestRule.onNodeWithText("1 of 2 matched").assertExists()
        composeTestRule.onNodeWithText("Relative time").assertExists()
        composeTestRule.onNodeWithText("Absolute time").assertExists()

        composeTestRule.onNodeWithText("Continue matching").performClick()
        assertEquals(1, actionCount)
    }

    @Test
    fun sentDetailShowsTruthfulChannelAndPerRecipientResults() {
        val request = request(
            fields = listOf(FieldDomain.create(key = "address", value = "")),
            user = "owner@example.com",
            recipients = listOf("alice@example.com", "bob@example.com")
        ).copy(
            deliveryChannel = RequestDeliveryChannel.CLOUD,
            deliveryOutcomes = listOf(
                RequestDeliveryOutcome("alice@example.com", StatusDataSentDb.STORED),
                RequestDeliveryOutcome("bob@example.com", StatusDataSentDb.ERROR)
            ),
            deliveryUpdatedAt = Instant.parse("2026-07-23T11:30:00Z")
        )

        composeTestRule.setContent {
            RequestDetailDialog(
                request = request,
                mode = RequestListMode.SENT,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Sent • Partial").assertExists()
        composeTestRule.onNodeWithText("Cloud relay").assertExists()
        composeTestRule.onAllNodesWithText("alice@example.com").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("bob@example.com").assertCountEquals(2)
        composeTestRule.onNodeWithText("Sent").assertExists()
        composeTestRule.onNodeWithText("Failed · retry available").assertExists()
    }

    @Test
    fun legacySentDetailDoesNotInventDeliveryEvidence() {
        val request = request(
            fields = listOf(FieldDomain.create(key = "address", value = "")),
            user = "owner@example.com",
            recipients = emptyList()
        )

        composeTestRule.setContent {
            RequestDetailDialog(
                request = request,
                mode = RequestListMode.SENT,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Sent • Outcome not recorded").assertExists()
        composeTestRule.onAllNodesWithText("Not recorded").assertCountEquals(3)
        composeTestRule
            .onNodeWithText("No per-recipient delivery result recorded")
            .assertExists()
        composeTestRule.onNodeWithText("Delivered").assertDoesNotExist()
    }

    private fun request(
        fields: List<FieldDomain>,
        user: String,
        recipients: List<String>
    ) = RequestDomain(
        fields = fields,
        user = user,
        recipients = recipients,
        dateAdded = Instant.parse("2026-07-23T10:15:30Z"),
        owned = false,
        responded = false,
        id = "request-42",
        requesterId = "requester-hash"
    )
}
