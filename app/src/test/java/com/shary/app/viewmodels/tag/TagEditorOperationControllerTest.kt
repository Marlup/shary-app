package com.shary.app.viewmodels.tag

import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.types.enums.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagEditorOperationControllerTest {

    @Test
    fun savingClaimBlocksAnotherOperation() {
        val controller = TagEditorOperationController()

        assertTrue(controller.claim(TagEditorOperation.ADD, "Work"))
        assertFalse(controller.claim(TagEditorOperation.UPDATE, "Personal"))
    }

    @Test
    fun matchingSuccessCarriesThePersistedTag() {
        val controller = TagEditorOperationController()
        val tag = Tag.Entry("Work", Color.Blue)
        controller.claim(TagEditorOperation.ADD, "Work")

        controller.succeed(TagEditorOperation.ADD, "work", tag)

        assertEquals(
            TagEditorOperationState.Saved(TagEditorOperation.ADD, "work", tag),
            controller.state.value
        )
    }

    @Test
    fun mismatchedTerminalResultCannotReplaceTheActiveOperation() {
        val controller = TagEditorOperationController()
        controller.claim(TagEditorOperation.UPDATE, "Work")

        controller.fail(TagEditorOperation.UPDATE, "Personal", "wrong result")

        assertEquals(
            TagEditorOperationState.Saving(TagEditorOperation.UPDATE, "work"),
            controller.state.value
        )
    }

    @Test
    fun resetCannotCancelSavingButClearsFailure() {
        val controller = TagEditorOperationController()
        controller.claim(TagEditorOperation.ADD, "Work")

        controller.reset()
        assertTrue(controller.state.value is TagEditorOperationState.Saving)

        controller.fail(TagEditorOperation.ADD, "Work", "Disk write failed")
        controller.reset()
        assertEquals(TagEditorOperationState.Idle, controller.state.value)
    }
}
