package com.shary.app.application.cloud

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.RequestDomain
import java.time.Instant

internal fun CloudDeliveryReceipt.toPersistedRequest(
    fields: List<FieldDomain>,
    ownerEmail: String,
    updatedAt: Instant
): RequestDomain? {
    val requestId = transactionId?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (kind != CloudDeliveryKind.REQUEST || outcomes.isEmpty()) return null

    return RequestDomain.create(
        fields = fields,
        user = ownerEmail,
        recipients = outcomes.map(CloudRecipientOutcome::address),
        id = requestId
    ).copy(
        deliveryChannel = RequestDeliveryChannel.CLOUD,
        deliveryOutcomes = outcomes.map { outcome ->
            RequestDeliveryOutcome(
                recipient = outcome.address,
                status = outcome.status
            )
        },
        deliveryUpdatedAt = updatedAt
    )
}

internal fun RequestDomain.toCloudDeliveryReceipt(
    kind: CloudDeliveryKind = CloudDeliveryKind.REQUEST
): CloudDeliveryReceipt? {
    if (deliveryChannel != RequestDeliveryChannel.CLOUD || deliveryOutcomes.isEmpty()) return null
    val requestId = id.trim().takeIf(String::isNotEmpty) ?: return null
    val outcomes = deliveryOutcomes.map { outcome ->
        CloudRecipientOutcome(
            address = outcome.recipient,
            status = outcome.status
        )
    }
    val acknowledged = outcomes.count { it.acknowledged }
    val summary = when {
        acknowledged == outcomes.size -> CloudDeliverySummary.SENT
        acknowledged == 0 -> CloudDeliverySummary.FAILED
        else -> CloudDeliverySummary.PARTIAL
    }
    return CloudDeliveryReceipt(
        kind = kind,
        transactionId = requestId,
        summary = summary,
        outcomes = outcomes
    )
}

internal fun CloudDeliveryReceipt.toRequestDeliveryOutcomes(): List<RequestDeliveryOutcome> =
    outcomes.map { outcome ->
        RequestDeliveryOutcome(
            recipient = outcome.address,
            status = outcome.status
        )
    }
