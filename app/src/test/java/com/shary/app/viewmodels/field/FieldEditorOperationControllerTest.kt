package com.shary.app.viewmodels.field

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldEditorOperationControllerTest {

    @Test
    fun claimIsSynchronousAndRejectsDuplicateSubmission() {
        val controller = FieldEditorOperationController()

        assertTrue(controller.claim(FieldEditorOperation.ADD, " account "))
        assertEquals(
            FieldEditorOperationState.Saving(FieldEditorOperation.ADD, "account"),
            controller.state.value
        )
        assertFalse(controller.claim(FieldEditorOperation.ADD, "account"))
    }

    @Test
    fun successRequiresTheActiveOperationAndKey() {
        val controller = FieldEditorOperationController()
        controller.claim(FieldEditorOperation.UPDATE, "account")

        controller.succeed(FieldEditorOperation.UPDATE, "other")
        assertEquals(
            FieldEditorOperationState.Saving(FieldEditorOperation.UPDATE, "account"),
            controller.state.value
        )

        controller.succeed(FieldEditorOperation.UPDATE, "account")
        assertEquals(
            FieldEditorOperationState.Saved(FieldEditorOperation.UPDATE, "account"),
            controller.state.value
        )
    }

    @Test
    fun failurePreservesAnActionableMessageUntilReset() {
        val controller = FieldEditorOperationController()
        controller.claim(FieldEditorOperation.RECOVER, "account")

        controller.fail(FieldEditorOperation.RECOVER, "account", "Disk write failed")

        assertEquals(
            FieldEditorOperationState.Failed(
                operation = FieldEditorOperation.RECOVER,
                key = "account",
                message = "Disk write failed"
            ),
            controller.state.value
        )
        controller.reset()
        assertEquals(FieldEditorOperationState.Idle, controller.state.value)
    }

    @Test
    fun resetCannotCancelAnInFlightOperation() {
        val controller = FieldEditorOperationController()
        controller.claim(FieldEditorOperation.ADD, "account")

        controller.reset()

        assertEquals(
            FieldEditorOperationState.Saving(FieldEditorOperation.ADD, "account"),
            controller.state.value
        )
    }
}
