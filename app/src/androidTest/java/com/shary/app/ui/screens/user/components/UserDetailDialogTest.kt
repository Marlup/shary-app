package com.shary.app.ui.screens.user.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.ui.components.SharyModalAction
import com.shary.app.ui.theme.SharyTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UserDetailDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ordinaryDetailExposesTruthfulMetadataAndEveryManagementAction() {
        var editCount = 0
        var copyCount = 0
        var similarCount = 0
        var deleteCount = 0
        var flowCount = 0

        composeTestRule.setContent {
            SharyTheme {
                UserDetailDialog(
                    user = contact(),
                    onDismiss = {},
                    onEdit = { editCount++ },
                    onCopyEmail = { copyCount++ },
                    onCreateSimilar = { similarCount++ },
                    onDelete = { deleteCount++ },
                    flowAction = SharyModalAction(
                        label = "Select Fields",
                        onClick = { flowCount++ }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Stored locally in this vault").assertExists()
        composeTestRule.onNodeWithText("alice@example.com").assertExists()
        composeTestRule.onNodeWithText("Added").assertExists()
        composeTestRule.onNodeWithText("Verified").assertDoesNotExist()

        composeTestRule.onNodeWithText("Copy email").performClick()
        composeTestRule.onNodeWithText("Create similar").performClick()
        composeTestRule.onNodeWithText("Select Fields").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()

        composeTestRule.onNodeWithText("Delete Alice?").assertExists()
        assertEquals(0, deleteCount)
        composeTestRule.onNodeWithText("Confirm Delete").performClick()

        assertEquals(1, editCount)
        assertEquals(1, copyCount)
        assertEquals(1, similarCount)
        assertEquals(1, deleteCount)
        assertEquals(1, flowCount)
    }

    private fun contact() = UserDomain(
        username = "Alice",
        email = "alice@example.com",
        dateAdded = Instant.parse("2026-07-19T10:15:30Z")
    )
}
