package com.shary.app.application.cloud

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RequestDeliveryHistoryTest {

    @Test
    fun `partial cloud receipt round trips through request history`() {
        val receipt = CloudDeliveryReceipt(
            kind = CloudDeliveryKind.REQUEST,
            transactionId = REQUEST_ID,
            summary = CloudDeliverySummary.PARTIAL,
            outcomes = listOf(
                CloudRecipientOutcome("alice@example.com", StatusDataSentDb.SUCCESS),
                CloudRecipientOutcome("bob@example.com", StatusDataSentDb.NOT_FOUND)
            )
        )

        val stored = receipt.toPersistedRequest(
            fields = listOf(FieldDomain.create("secret", "")),
            ownerEmail = "owner@example.com",
            updatedAt = UPDATED_AT
        )
        val restored = stored?.toCloudDeliveryReceipt()

        assertNotNull(stored)
        assertEquals(RequestDeliveryChannel.CLOUD, stored?.deliveryChannel)
        assertEquals(UPDATED_AT, stored?.deliveryUpdatedAt)
        assertEquals(receipt, restored)
    }

    @Test
    fun `data receipt and request without outcomes are not persisted or restored`() {
        val dataReceipt = CloudDeliveryReceipt(
            kind = CloudDeliveryKind.DATA,
            transactionId = null,
            summary = CloudDeliverySummary.SENT,
            outcomes = listOf(CloudRecipientOutcome("alice@example.com", StatusDataSentDb.SUCCESS))
        )

        assertNull(
            dataReceipt.toPersistedRequest(
                fields = emptyList(),
                ownerEmail = "owner@example.com",
                updatedAt = UPDATED_AT
            )
        )
    }

    @Test
    fun `received request delivery metadata restores a response receipt`() {
        val request = RequestDomain.initialize().copy(
            id = REQUEST_ID,
            deliveryChannel = RequestDeliveryChannel.CLOUD,
            deliveryOutcomes = listOf(
                RequestDeliveryOutcome("requester@example.com", StatusDataSentDb.ERROR)
            ),
            deliveryUpdatedAt = UPDATED_AT
        )

        val receipt = request.toCloudDeliveryReceipt(CloudDeliveryKind.DATA)

        assertEquals(CloudDeliveryKind.DATA, receipt?.kind)
        assertEquals(REQUEST_ID, receipt?.transactionId)
        assertEquals(CloudDeliverySummary.FAILED, receipt?.summary)
        assertEquals(listOf("requester@example.com"), receipt?.failedRecipients)
    }

    private companion object {
        const val REQUEST_ID = "0f870d06-ff21-4eb3-8e21-465773882293"
        val UPDATED_AT: Instant = Instant.parse("2026-07-20T09:30:00Z")
    }
}
