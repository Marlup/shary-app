package com.shary.app.application.cloud

import com.shary.app.core.domain.types.enums.StatusDataSentDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudDeliveryControllerTest {

    @Test
    fun `acknowledged results normalize recipients and preserve display order`() {
        val controller = CloudDeliveryController()
        val attempt = controller.begin(
            kind = CloudDeliveryKind.DATA,
            recipients = listOf(
                " Alice@Example.com ",
                "bob@example.com",
                "ALICE@example.com",
                "carol@example.com"
            )
        )

        assertNotNull(attempt)
        assertEquals(
            listOf("Alice@Example.com", "bob@example.com", "carol@example.com"),
            attempt?.expectedRecipients
        )

        val receipt = controller.complete(
            attempt = requireNotNull(attempt),
            result = mapOf(
                "alice@example.com" to StatusDataSentDb.SUCCESS,
                "BOB@EXAMPLE.COM" to StatusDataSentDb.STORED,
                "carol@example.com" to StatusDataSentDb.EXISTS,
                "unexpected@example.com" to StatusDataSentDb.ERROR
            )
        )

        assertEquals(CloudDeliverySummary.SENT, receipt?.summary)
        assertEquals(
            listOf("Alice@Example.com", "bob@example.com", "carol@example.com"),
            receipt?.outcomes?.map(CloudRecipientOutcome::address)
        )
        assertTrue(receipt?.outcomes?.all(CloudRecipientOutcome::acknowledged) == true)
        assertTrue(controller.state.value is CloudDeliveryState.Receipt)
    }

    @Test
    fun `missing attempted result becomes error and produces partial receipt`() {
        val controller = CloudDeliveryController()
        val attempt = requireNotNull(
            controller.begin(
                CloudDeliveryKind.DATA,
                listOf("alice@example.com", "bob@example.com")
            )
        )

        val receipt = requireNotNull(
            controller.complete(
                attempt,
                mapOf("alice@example.com" to StatusDataSentDb.SUCCESS)
            )
        )

        assertEquals(CloudDeliverySummary.PARTIAL, receipt.summary)
        assertEquals(StatusDataSentDb.SUCCESS, receipt.outcomes[0].status)
        assertEquals(StatusDataSentDb.ERROR, receipt.outcomes[1].status)
        assertEquals(listOf("bob@example.com"), receipt.failedRecipients)
    }

    @Test
    fun `nonempty all error map and transport failure produce retryable failed receipts`() {
        val allErrorController = CloudDeliveryController()
        val allErrorAttempt = requireNotNull(
            allErrorController.begin(
                CloudDeliveryKind.DATA,
                listOf("alice@example.com", "bob@example.com")
            )
        )
        val allErrorReceipt = requireNotNull(
            allErrorController.complete(
                allErrorAttempt,
                mapOf(
                    "alice@example.com" to StatusDataSentDb.ERROR,
                    "bob@example.com" to StatusDataSentDb.FORBIDDEN
                )
            )
        )

        assertEquals(CloudDeliverySummary.FAILED, allErrorReceipt.summary)
        assertEquals(
            listOf("alice@example.com", "bob@example.com"),
            allErrorReceipt.failedRecipients
        )

        val failedTransportController = CloudDeliveryController()
        val failedTransportAttempt = requireNotNull(
            failedTransportController.begin(
                CloudDeliveryKind.REQUEST,
                listOf("alice@example.com"),
                transactionId = REQUEST_ID
            )
        )
        val failedTransportReceipt = requireNotNull(
            failedTransportController.fail(failedTransportAttempt)
        )

        assertEquals(CloudDeliverySummary.FAILED, failedTransportReceipt.summary)
        assertEquals(StatusDataSentDb.ERROR, failedTransportReceipt.outcomes.single().status)
        assertNotNull(
            failedTransportController.retryFailed(
                CloudDeliveryKind.REQUEST,
                transactionId = REQUEST_ID
            )
        )
    }

    @Test
    fun `retry claims only failures and accumulates prior successes`() {
        val controller = CloudDeliveryController()
        val firstAttempt = requireNotNull(
            controller.begin(
                CloudDeliveryKind.REQUEST,
                listOf("alice@example.com", "bob@example.com", "carol@example.com"),
                transactionId = REQUEST_ID
            )
        )
        val partial = requireNotNull(
            controller.complete(
                firstAttempt,
                mapOf(
                    "alice@example.com" to StatusDataSentDb.SUCCESS,
                    "bob@example.com" to StatusDataSentDb.ERROR,
                    "carol@example.com" to StatusDataSentDb.NOT_FOUND
                )
            )
        )

        assertEquals(CloudDeliverySummary.PARTIAL, partial.summary)
        val retry = requireNotNull(
            controller.retryFailed(
                CloudDeliveryKind.REQUEST,
                transactionId = REQUEST_ID
            )
        )
        assertEquals(listOf("bob@example.com", "carol@example.com"), retry.attemptedRecipients)
        assertNull(
            controller.retryFailed(
                CloudDeliveryKind.REQUEST,
                transactionId = REQUEST_ID
            )
        )

        val sent = requireNotNull(
            controller.complete(
                retry,
                mapOf(
                    "bob@example.com" to StatusDataSentDb.STORED,
                    "carol@example.com" to StatusDataSentDb.EXISTS
                )
            )
        )

        assertEquals(CloudDeliverySummary.SENT, sent.summary)
        assertEquals(StatusDataSentDb.SUCCESS, sent.outcomes[0].status)
        assertEquals(StatusDataSentDb.STORED, sent.outcomes[1].status)
        assertEquals(StatusDataSentDb.EXISTS, sent.outcomes[2].status)
        assertTrue(sent.failedRecipients.isEmpty())
    }

    @Test
    fun `empty retry result preserves earlier success and remains retryable`() {
        val controller = CloudDeliveryController()
        val firstAttempt = requireNotNull(
            controller.begin(
                CloudDeliveryKind.DATA,
                listOf("alice@example.com", "bob@example.com")
            )
        )
        controller.complete(
            firstAttempt,
            mapOf(
                "alice@example.com" to StatusDataSentDb.SUCCESS,
                "bob@example.com" to StatusDataSentDb.ERROR
            )
        )
        val retry = requireNotNull(controller.retryFailed(CloudDeliveryKind.DATA))

        val receipt = requireNotNull(controller.complete(retry, emptyMap()))

        assertEquals(CloudDeliverySummary.PARTIAL, receipt.summary)
        assertEquals(StatusDataSentDb.SUCCESS, receipt.outcomes[0].status)
        assertEquals(StatusDataSentDb.ERROR, receipt.outcomes[1].status)
        assertEquals(listOf("bob@example.com"), receipt.failedRecipients)
        assertNotNull(controller.retryFailed(CloudDeliveryKind.DATA))
    }

    @Test
    fun `initial and terminal states reject ordinary duplicate sends`() {
        val controller = CloudDeliveryController()
        val firstAttempt = requireNotNull(
            controller.begin(CloudDeliveryKind.DATA, listOf("alice@example.com"))
        )

        assertTrue(controller.state.value is CloudDeliveryState.Sending)
        assertNull(controller.begin(CloudDeliveryKind.DATA, listOf("alice@example.com")))
        assertFalse(controller.reset())

        controller.complete(
            firstAttempt,
            mapOf("alice@example.com" to StatusDataSentDb.SUCCESS)
        )

        assertNull(controller.begin(CloudDeliveryKind.DATA, listOf("alice@example.com")))
        assertNull(controller.retryFailed(CloudDeliveryKind.DATA))
        assertTrue(controller.reset())
        assertEquals(CloudDeliveryState.Review, controller.state.value)
        assertNotNull(controller.begin(CloudDeliveryKind.DATA, listOf("alice@example.com")))
    }

    @Test
    fun `request retry requires the original transaction identity`() {
        val controller = CloudDeliveryController()
        assertNull(
            controller.begin(
                CloudDeliveryKind.REQUEST,
                listOf("alice@example.com"),
                transactionId = " "
            )
        )
        val attempt = requireNotNull(
            controller.begin(
                CloudDeliveryKind.REQUEST,
                listOf("alice@example.com"),
                transactionId = REQUEST_ID
            )
        )
        controller.fail(attempt)

        assertNull(
            controller.retryFailed(
                CloudDeliveryKind.REQUEST,
                transactionId = "087f4f24-e6f1-45f0-8ed3-31ef5178d428"
            )
        )
        assertNotNull(
            controller.retryFailed(
                CloudDeliveryKind.REQUEST,
                transactionId = REQUEST_ID
            )
        )
    }

    @Test
    fun `restored partial request receipt retries only persisted failures`() {
        val controller = CloudDeliveryController()
        val restored = controller.restore(
            CloudDeliveryReceipt(
                kind = CloudDeliveryKind.REQUEST,
                transactionId = REQUEST_ID,
                summary = CloudDeliverySummary.SENT,
                outcomes = listOf(
                    CloudRecipientOutcome("Alice@Example.com", StatusDataSentDb.SUCCESS),
                    CloudRecipientOutcome("bob@example.com", StatusDataSentDb.ERROR)
                )
            )
        )

        assertTrue(restored)
        val receipt = (controller.state.value as CloudDeliveryState.Receipt).receipt
        assertEquals(CloudDeliverySummary.PARTIAL, receipt.summary)
        assertEquals(
            listOf("bob@example.com"),
            controller.retryFailed(CloudDeliveryKind.REQUEST, REQUEST_ID)?.attemptedRecipients
        )
    }

    @Test
    fun `restore rejects invalid or duplicate receipt replacement`() {
        val controller = CloudDeliveryController()

        assertFalse(
            controller.restore(
                CloudDeliveryReceipt(
                    kind = CloudDeliveryKind.REQUEST,
                    transactionId = " ",
                    summary = CloudDeliverySummary.FAILED,
                    outcomes = listOf(CloudRecipientOutcome("alice@example.com", StatusDataSentDb.ERROR))
                )
            )
        )
        val valid = CloudDeliveryReceipt(
            kind = CloudDeliveryKind.REQUEST,
            transactionId = REQUEST_ID,
            summary = CloudDeliverySummary.FAILED,
            outcomes = listOf(CloudRecipientOutcome("alice@example.com", StatusDataSentDb.ERROR))
        )
        assertTrue(controller.restore(valid))
        assertFalse(controller.restore(valid))
    }

    @Test
    fun `restored response receipt keeps request identity for retry`() {
        val controller = CloudDeliveryController()
        assertTrue(
            controller.restore(
                CloudDeliveryReceipt(
                    kind = CloudDeliveryKind.DATA,
                    transactionId = REQUEST_ID,
                    summary = CloudDeliverySummary.FAILED,
                    outcomes = listOf(
                        CloudRecipientOutcome("requester@example.com", StatusDataSentDb.ERROR)
                    )
                )
            )
        )

        assertNull(controller.retryFailed(CloudDeliveryKind.DATA, transactionId = null))
        assertEquals(
            listOf("requester@example.com"),
            controller.retryFailed(CloudDeliveryKind.DATA, REQUEST_ID)?.attemptedRecipients
        )
    }

    private companion object {
        const val REQUEST_ID = "0f870d06-ff21-4eb3-8e21-465773882293"
    }
}
