package com.shary.app.ui.screens.request.utils

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.shary.app.application.request.RequestDraftFieldAddOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddRequestDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun duplicateOutcomeKeepsDraftAndShowsTheError() {
        var dismissCount = 0
        val duplicateMessage = "Requested key 'zebra-key-44' is already in this draft"

        composeTestRule.setContent {
            AddRequestDialog(
                onDismiss = { dismissCount++ },
                onAddRequest = { _, _ -> RequestDraftFieldAddOutcome.DUPLICATE }
            )
        }

        composeTestRule.onNodeWithText("Key *").performTextInput("zebra-key-44")
        composeTestRule.onNodeWithText("Add to draft").performClick()

        composeTestRule.onNodeWithText(duplicateMessage).assertExists()
        composeTestRule.onNodeWithText("zebra-key-44").assertExists()
        assertEquals(0, dismissCount)
    }

    @Test
    fun validOutcomeDismissesOnlyAfterTheDraftCallbackSucceeds() {
        var submittedKey: String? = null
        var dismissCount = 0

        composeTestRule.setContent {
            AddRequestDialog(
                onDismiss = { dismissCount++ },
                onAddRequest = { key, _ ->
                    submittedKey = key
                    RequestDraftFieldAddOutcome.ADDED
                }
            )
        }

        composeTestRule.onNodeWithText("Key *").performTextInput("zebra-key-44")
        composeTestRule.onNodeWithText("Add to draft").performClick()

        assertEquals("zebra-key-44", submittedKey)
        assertEquals(1, dismissCount)
    }

    @Test
    fun blankInputRemainsOpenWithRequiredMessage() {
        var submitCount = 0
        var dismissCount = 0

        composeTestRule.setContent {
            AddRequestDialog(
                onDismiss = { dismissCount++ },
                onAddRequest = { _, _ ->
                    submitCount++
                    RequestDraftFieldAddOutcome.ADDED
                }
            )
        }

        composeTestRule.onNodeWithText("Add to draft").performClick()

        composeTestRule.onNodeWithText("Requested key is required").assertExists()
        assertEquals(0, submitCount)
        assertEquals(0, dismissCount)
    }

    @Test
    fun editedDraftRequiresExplicitDiscard() {
        var dismissCount = 0

        composeTestRule.setContent {
            AddRequestDialog(
                onDismiss = { dismissCount++ },
                onAddRequest = { _, _ -> RequestDraftFieldAddOutcome.ADDED }
            )
        }

        composeTestRule.onNodeWithText("Key *").performTextInput("zebra-key-44")
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        composeTestRule.onNodeWithText("Discard changes?").assertExists()
        composeTestRule.onNodeWithText("Keep editing").performClick()
        composeTestRule.onNodeWithText("zebra-key-44").assertExists()
        assertEquals(0, dismissCount)

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Discard").performClick()
        assertEquals(1, dismissCount)
    }

    @Test
    fun aliasImeDoneSubmitsTrimmedDraft() {
        var submitted: Pair<String, String>? = null
        var dismissCount = 0

        composeTestRule.setContent {
            AddRequestDialog(
                onDismiss = { dismissCount++ },
                onAddRequest = { key, alias ->
                    submitted = key to alias
                    RequestDraftFieldAddOutcome.ADDED
                }
            )
        }

        composeTestRule.onNodeWithText("Advanced options").performClick()
        composeTestRule.onNodeWithText("Key *").performTextInput("  zebra-key-44  ")
        composeTestRule.onNodeWithText("Alias").performTextInput("  Zebra key  ")
        composeTestRule.onNodeWithText("Alias").performImeAction()

        assertEquals("zebra-key-44" to "Zebra key", submitted)
        assertEquals(1, dismissCount)
    }
}
