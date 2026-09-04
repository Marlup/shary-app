package com.shary.app.ui.screens.user.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.ui.theme.SharyTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class UserEditorDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun validSaveSubmitsWithoutOptimisticallyDismissing() {
        var submitted: UserDomain? = null
        var dismissCount = 0

        setEditor(
            onDismiss = { dismissCount++ },
            onConfirm = { submitted = it }
        )

        composeTestRule.onNodeWithText("Save").performClick()

        assertNotNull(submitted)
        assertEquals(0, dismissCount)
    }

    @Test
    fun failureRemainsVisibleWithTheContactDraft() {
        setEditor(errorMessage = "Disk write failed")

        composeTestRule.onNodeWithText("Disk write failed").assertExists()
        composeTestRule.onNodeWithText("alice@example.com").assertExists()
    }

    @Test
    fun savingStateBlocksAnotherSubmitAndDismiss() {
        setEditor(isSaving = true)

        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Close").assertIsNotEnabled()
    }

    @Test
    fun editedDraftRequiresExplicitDiscard() {
        var dismissCount = 0
        setEditor(onDismiss = { dismissCount++ })

        composeTestRule.onNodeWithText("Alice").performTextReplacement("Alice Updated")
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        composeTestRule.onNodeWithText("Discard changes?").assertExists()
        assertEquals(0, dismissCount)

        composeTestRule.onNodeWithText("Discard").performClick()
        assertEquals(1, dismissCount)
    }

    @Test
    fun emailImeDoneSubmitsAValidDraft() {
        var submitted: UserDomain? = null
        setEditor(onConfirm = { submitted = it })

        composeTestRule.onNodeWithText("alice@example.com").performImeAction()

        assertNotNull(submitted)
    }

    private fun setEditor(
        onDismiss: () -> Unit = {},
        onConfirm: (UserDomain) -> Unit = {},
        isSaving: Boolean = false,
        errorMessage: String? = null
    ) {
        composeTestRule.setContent {
            SharyTheme {
                UserEditorDialog(
                    initial = contact(),
                    title = "Edit Contact",
                    confirmLabel = "Save",
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                    isSaving = isSaving,
                    errorMessage = errorMessage
                )
            }
        }
    }

    private fun contact() = UserDomain(
        username = "Alice",
        email = "alice@example.com",
        dateAdded = Instant.parse("2026-07-19T10:15:30Z")
    )
}
