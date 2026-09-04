package com.shary.app.core.domain.models

import java.time.Instant
import java.util.UUID
import com.shary.app.core.domain.models.RequestDomain.Companion.initialize
import com.shary.app.core.domain.types.enums.StatusDataSentDb

enum class RequestDeliveryChannel {
    CLOUD,
    EMAIL
}

enum class RequestDeliverySummary {
    SENT,
    PARTIAL,
    FAILED
}

data class RequestDeliveryOutcome(
    val recipient: String,
    val status: StatusDataSentDb
)


data class RequestDomain(
    val fields: List<FieldDomain>,
    val user: String,
    val recipients: List<String>,
    val dateAdded: Instant,
    val owned: Boolean,
    val responded: Boolean,
    val id: String = "",
    val requesterId: String = "",
    val deliveryChannel: RequestDeliveryChannel? = null,
    val deliveryOutcomes: List<RequestDeliveryOutcome> = emptyList(),
    val deliveryUpdatedAt: Instant? = null
) {

    companion object {
        fun RequestDomain?.orEmpty(): RequestDomain =
            this ?: initialize()

        fun create(
            fields: List<FieldDomain>,
            user: String,
            recipients: List<String> = emptyList(),
            id: String = UUID.randomUUID().toString()
        ): RequestDomain {
            return RequestDomain(
                fields = fields,
                user = user,
                recipients = recipients,
                dateAdded = Instant.now(),
                owned = true,
                responded = false,
                id = id,
                requesterId = "",
                deliveryChannel = null,
                deliveryOutcomes = emptyList(),
                deliveryUpdatedAt = null
            )
        }

        fun initialize(): RequestDomain = RequestDomain(
            fields = emptyList(),
            user = String(),
            recipients = emptyList(),
            dateAdded = Instant.now(),
            owned = false,
            responded = false,
            id = "",
            requesterId = "",
            deliveryChannel = null,
            deliveryOutcomes = emptyList(),
            deliveryUpdatedAt = null
        )
    }
}

fun RequestDomain?.orEmpty(): RequestDomain =
    this ?: initialize()

fun RequestDomain.reset(): RequestDomain = this.copy(
    fields = emptyList(),
    user = String(),
    recipients = emptyList(),
    dateAdded = Instant.EPOCH,
    owned = false,
    responded = false,
    id = "",
    requesterId = "",
    deliveryChannel = null,
    deliveryOutcomes = emptyList(),
    deliveryUpdatedAt = null
)

fun RequestDomain.deliverySummaryOrNull(): RequestDeliverySummary? {
    if (deliveryOutcomes.isEmpty()) return null
    val acknowledgedCount = deliveryOutcomes.count { outcome ->
        outcome.status == StatusDataSentDb.SUCCESS ||
            outcome.status == StatusDataSentDb.STORED ||
            outcome.status == StatusDataSentDb.EXISTS
    }
    return when {
        acknowledgedCount == deliveryOutcomes.size -> RequestDeliverySummary.SENT
        acknowledgedCount == 0 -> RequestDeliverySummary.FAILED
        else -> RequestDeliverySummary.PARTIAL
    }
}
