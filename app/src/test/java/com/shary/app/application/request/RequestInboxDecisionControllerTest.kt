package com.shary.app.application.request

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestInboxDecisionControllerTest {

    @Test
    fun doubleAcceptClaimsTheOperationOnce() {
        val controller = RequestInboxDecisionController()

        assertTrue(controller.tryStart("verification-1", RequestInboxDecision.ACCEPT))
        assertFalse(controller.tryStart("verification-1", RequestInboxDecision.ACCEPT))
        assertEquals(
            RequestInboxDecisionState.Processing(
                verification = "verification-1",
                decision = RequestInboxDecision.ACCEPT
            ),
            controller.state.value
        )
    }

    @Test
    fun acceptThenRejectClaimsOnlyAccept() {
        val controller = RequestInboxDecisionController()

        assertTrue(controller.tryStart("verification-1", RequestInboxDecision.ACCEPT))
        assertFalse(controller.tryStart("verification-1", RequestInboxDecision.REJECT))
        assertEquals(RequestInboxDecision.ACCEPT, controller.state.value.decisionOrNull())
    }

    @Test
    fun failedAcceptCanRetryTheSameDecision() {
        val controller = RequestInboxDecisionController()
        val failure = IllegalStateException("response unavailable")
        assertTrue(controller.tryStart("verification-1", RequestInboxDecision.ACCEPT))

        assertTrue(
            controller.fail(
                verification = "verification-1",
                decision = RequestInboxDecision.ACCEPT,
                error = failure,
                localRequestAvailable = true,
                requestedKeyCount = 2
            )
        )
        val failed = controller.state.value as RequestInboxDecisionState.Failed
        assertSame(failure, failed.error)
        assertTrue(failed.localRequestAvailable)
        assertEquals(2, failed.requestedKeyCount)

        assertTrue(controller.tryStart("verification-1", RequestInboxDecision.ACCEPT))
    }

    @Test
    fun successRemainsUntilReceiptIsConsumed() {
        val controller = RequestInboxDecisionController()
        val receipt = RequestInboxDecisionReceipt(
            verification = "verification-1",
            decision = RequestInboxDecision.ACCEPT,
            localRequestAvailable = true,
            requestedKeyCount = 3,
            backendAcknowledged = true
        )
        assertTrue(controller.tryStart("verification-1", RequestInboxDecision.ACCEPT))

        assertTrue(controller.succeed(receipt))
        assertEquals(RequestInboxDecisionState.Succeeded(receipt), controller.state.value)
        assertFalse(controller.tryStart("verification-2", RequestInboxDecision.ACCEPT))

        assertTrue(controller.consumeReceipt())
        assertEquals(RequestInboxDecisionState.Idle, controller.state.value)
        assertTrue(controller.tryStart("verification-2", RequestInboxDecision.REJECT))
    }

    @Test
    fun failedDecisionRejectsOppositeAndDifferentItemRetries() {
        val controller = RequestInboxDecisionController()
        assertTrue(controller.tryStart("verification-1", RequestInboxDecision.REJECT))
        assertTrue(
            controller.fail(
                verification = "verification-1",
                decision = RequestInboxDecision.REJECT,
                error = IllegalStateException("response unavailable")
            )
        )

        assertFalse(controller.tryStart("verification-1", RequestInboxDecision.ACCEPT))
        assertFalse(controller.tryStart("verification-2", RequestInboxDecision.REJECT))
        assertEquals(RequestInboxDecision.REJECT, controller.state.value.decisionOrNull())
    }
}

private fun RequestInboxDecisionState.decisionOrNull(): RequestInboxDecision? = when (this) {
    RequestInboxDecisionState.Idle -> null
    is RequestInboxDecisionState.Processing -> decision
    is RequestInboxDecisionState.Failed -> decision
    is RequestInboxDecisionState.Succeeded -> receipt.decision
}
