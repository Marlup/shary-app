package com.shary.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shary.app.ui.theme.SharyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharyModalScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cleanCloseDismissesImmediately() {
        var dismissCount = 0

        setScaffold(
            onDismissRequest = { dismissCount++ }
        )

        composeTestRule.onNodeWithContentDescription("Close").performClick()

        assertEquals(1, dismissCount)
    }

    @Test
    fun dirtyCloseRequiresExplicitDiscard() {
        var dismissCount = 0

        setScaffold(
            onDismissRequest = { dismissCount++ },
            isDirty = true
        )

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Discard changes?").assertExists()
        assertEquals(0, dismissCount)

        composeTestRule.onNodeWithText("Keep editing").performClick()
        assertEquals(0, dismissCount)

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Discard").performClick()
        assertEquals(1, dismissCount)
    }

    @Test
    fun destructiveActionRequiresConfirmation() {
        var deleteCount = 0

        setScaffold(
            destructiveAction = SharyModalDestructiveAction(
                label = "Delete",
                confirmationTitle = "Delete Contact?",
                confirmationMessage = "This cannot be undone.",
                onConfirm = { deleteCount++ }
            )
        )

        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete Contact?").assertExists()
        assertEquals(0, deleteCount)

        composeTestRule.onNodeWithText("Confirm Delete").performClick()
        assertEquals(1, deleteCount)
    }

    @Test
    fun savingStateBlocksDismissAndPrimaryAction() {
        var dismissCount = 0

        setScaffold(
            onDismissRequest = { dismissCount++ },
            operationState = SharyModalOperationState.Saving,
            primaryAction = SharyModalAction("Save", onClick = {})
        )

        composeTestRule.onNodeWithText("Saving…").assertExists()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Close").assertIsNotEnabled()
        assertEquals(0, dismissCount)
    }

    private fun setScaffold(
        onDismissRequest: () -> Unit = {},
        isDirty: Boolean = false,
        operationState: SharyModalOperationState = SharyModalOperationState.Idle,
        primaryAction: SharyModalAction? = null,
        destructiveAction: SharyModalDestructiveAction? = null
    ) {
        composeTestRule.setContent {
            SharyTheme {
                SharyModalScaffold(
                    title = "Edit Contact",
                    onDismissRequest = onDismissRequest,
                    presentationOverride = SharyModalPresentation.FullScreen,
                    isDirty = isDirty,
                    operationState = operationState,
                    primaryAction = primaryAction,
                    destructiveAction = destructiveAction
                ) {
                    Text("Modal body")
                }
            }
        }
    }
}
