package com.shary.app.viewmodels.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactEditorOperationControllerTest {

    @Test
    fun claimNormalizesIdentityAndRejectsRapidDuplicateSubmission() {
        val controller = ContactEditorOperationController()

        assertTrue(controller.claim(ContactEditorOperation.UPDATE, " Alice@Example.com "))
        assertEquals(
            ContactEditorOperationState.Saving(
                ContactEditorOperation.UPDATE,
                "alice@example.com"
            ),
            controller.state.value
        )
        assertFalse(controller.claim(ContactEditorOperation.UPDATE, "alice@example.com"))
    }

    @Test
    fun terminalResultMustMatchTheActiveOriginalIdentity() {
        val controller = ContactEditorOperationController()
        controller.claim(ContactEditorOperation.UPDATE, "alice@example.com")

        controller.succeed(ContactEditorOperation.UPDATE, "bob@example.com")
        assertTrue(controller.state.value is ContactEditorOperationState.Saving)

        controller.succeed(ContactEditorOperation.UPDATE, "alice@example.com")
        assertEquals(
            ContactEditorOperationState.Saved(
                ContactEditorOperation.UPDATE,
                "alice@example.com"
            ),
            controller.state.value
        )
    }

    @Test
    fun failureRemainsVisibleUntilTheEditorResets() {
        val controller = ContactEditorOperationController()
        controller.claim(ContactEditorOperation.ADD, "alice@example.com")

        controller.fail(
            ContactEditorOperation.ADD,
            "alice@example.com",
            "A contact with this email already exists."
        )

        assertEquals(
            ContactEditorOperationState.Failed(
                ContactEditorOperation.ADD,
                "alice@example.com",
                "A contact with this email already exists."
            ),
            controller.state.value
        )
        controller.reset()
        assertEquals(ContactEditorOperationState.Idle, controller.state.value)
    }

    @Test
    fun resetDoesNotCancelAnActiveWrite() {
        val controller = ContactEditorOperationController()
        controller.claim(ContactEditorOperation.ADD, "alice@example.com")

        controller.reset()

        assertTrue(controller.state.value is ContactEditorOperationState.Saving)
    }
}
