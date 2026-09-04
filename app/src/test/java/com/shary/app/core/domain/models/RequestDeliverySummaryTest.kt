package com.shary.app.core.domain.models

import com.shary.app.core.domain.types.enums.StatusDataSentDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RequestDeliverySummaryTest {

    @Test
    fun `summary distinguishes sent partial failed and legacy history`() {
        val request = RequestDomain.initialize()

        assertNull(request.deliverySummaryOrNull())
        assertEquals(
            RequestDeliverySummary.SENT,
            request.withStatuses(StatusDataSentDb.SUCCESS, StatusDataSentDb.EXISTS)
                .deliverySummaryOrNull()
        )
        assertEquals(
            RequestDeliverySummary.PARTIAL,
            request.withStatuses(StatusDataSentDb.STORED, StatusDataSentDb.ERROR)
                .deliverySummaryOrNull()
        )
        assertEquals(
            RequestDeliverySummary.FAILED,
            request.withStatuses(StatusDataSentDb.NOT_FOUND, StatusDataSentDb.FORBIDDEN)
                .deliverySummaryOrNull()
        )
    }

    private fun RequestDomain.withStatuses(
        vararg statuses: StatusDataSentDb
    ): RequestDomain = copy(
        deliveryOutcomes = statuses.mapIndexed { index, status ->
            RequestDeliveryOutcome("recipient-$index@example.com", status)
        }
    )
}
