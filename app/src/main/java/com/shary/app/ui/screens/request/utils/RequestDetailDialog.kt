package com.shary.app.ui.screens.request.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliverySummary
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.deliverySummaryOrNull
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.ui.components.SharyModalAction
import com.shary.app.ui.components.SharyModalComplexity
import com.shary.app.ui.components.SharyModalScaffold
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun RequestDetailDialog(
    request: RequestDomain,
    mode: RequestListMode,
    onDismiss: () -> Unit,
    matchedFieldCount: Int? = null,
    primaryAction: SharyModalAction? = null
) {
    val relativeTimestamp = remember(request.dateAdded) {
        formatRequestRelativeTimestamp(request.dateAdded)
    }
    val absoluteTimestamp = remember(request.dateAdded) {
        formatRequestAbsoluteTimestamp(request.dateAdded)
    }
    val deliveryUpdatedLabel = remember(request.deliveryUpdatedAt) {
        request.deliveryUpdatedAt?.let { timestamp -> formatRequestAbsoluteTimestamp(timestamp) }
    }
    val title = when (mode) {
        RequestListMode.RECEIVED -> request.user.ifBlank { "Unknown requester" }
        RequestListMode.SENT -> "Sent request"
    }
    val modeLabel = when (mode) {
        RequestListMode.RECEIVED -> "Received"
        RequestListMode.SENT -> "Sent"
    }

    SharyModalScaffold(
        title = title,
        subtitle = "$modeLabel • ${request.requestStatusLabel(mode)}",
        onDismissRequest = onDismiss,
        complexity = SharyModalComplexity.Compact,
        primaryAction = primaryAction
    ) {
        RequestDetailSection(title = "Timing") {
            RequestDetailValue(label = "Relative time", value = relativeTimestamp)
            RequestDetailValue(label = "Absolute time", value = absoluteTimestamp)
        }

        RequestSectionDivider()

        RequestDetailSection(title = "People") {
            RequestDetailValue(
                label = "Requester",
                value = request.user.ifBlank { "Not recorded" }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Recipients (${request.recipients.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (request.recipients.isEmpty()) {
                Text(
                    text = "Not recorded",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    request.recipients.forEach { recipient ->
                        Text(
                            text = recipient,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        RequestSectionDivider()

        RequestDetailSection(title = "Requested fields (${request.fields.size})") {
            if (request.fields.isEmpty()) {
                Text(
                    text = "No requested fields recorded",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    request.fields.forEach { field ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = field.key.ifBlank { "Unnamed field" },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (field.keyAlias.isNotBlank()) {
                                    Text(
                                        text = "Alias: ${field.keyAlias}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mode == RequestListMode.RECEIVED) {
            RequestSectionDivider()

            RequestDetailSection(title = "Response progress") {
                val progress = when {
                    request.responded -> "Response completed"
                    request.fields.isEmpty() -> "No requested fields"
                    matchedFieldCount != null -> {
                        val matched = matchedFieldCount.coerceIn(0, request.fields.size)
                        "$matched of ${request.fields.size} matched"
                    }
                    else -> "Not started"
                }
                Text(
                    text = progress,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (
            mode == RequestListMode.SENT ||
            request.deliveryChannel != null ||
            request.deliveryOutcomes.isNotEmpty()
        ) {
            RequestSectionDivider()

            RequestDetailSection(title = "Delivery") {
                RequestDetailValue(
                    label = "Channel",
                    value = request.deliveryChannel.requestChannelLabel()
                )

                Spacer(Modifier.height(12.dp))

                RequestDetailValue(
                    label = "Last delivery update",
                    value = deliveryUpdatedLabel ?: "Not recorded"
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Per-recipient result",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (request.deliveryOutcomes.isEmpty()) {
                    Text(
                        text = "No per-recipient delivery result recorded",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        request.deliveryOutcomes.forEach { outcome ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = outcome.recipient,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = outcome.status.requestDeliveryLabel(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestDetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        content()
    }
}

@Composable
private fun RequestDetailValue(
    label: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun RequestSectionDivider() {
    Spacer(Modifier.height(18.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(18.dp))
}

internal fun formatRequestRelativeTimestamp(
    timestamp: Instant,
    now: Instant = Instant.now()
): String {
    if (timestamp == Instant.EPOCH) return "Unknown"
    val elapsedSeconds = Duration.between(timestamp, now).seconds.coerceAtLeast(0)
    val minutes = elapsedSeconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        elapsedSeconds < 60 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
        hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        else -> "$days day${if (days == 1L) "" else "s"} ago"
    }
}

internal fun formatRequestAbsoluteTimestamp(
    timestamp: Instant,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    if (timestamp == Instant.EPOCH) return "Unknown"
    return DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(zoneId)
        .format(timestamp)
}

private fun RequestDomain.requestStatusLabel(mode: RequestListMode): String = when (mode) {
    RequestListMode.RECEIVED -> if (responded) "Responded" else "Pending"
    RequestListMode.SENT -> when (deliverySummaryOrNull()) {
        RequestDeliverySummary.SENT -> "Sent"
        RequestDeliverySummary.PARTIAL -> "Partial"
        RequestDeliverySummary.FAILED -> "Failed"
        null -> "Outcome not recorded"
    }
}

private fun RequestDeliveryChannel?.requestChannelLabel(): String = when (this) {
    RequestDeliveryChannel.CLOUD -> "Cloud relay"
    RequestDeliveryChannel.EMAIL -> "Email handoff"
    null -> "Not recorded"
}

private fun StatusDataSentDb.requestDeliveryLabel(): String = when (this) {
    StatusDataSentDb.SUCCESS,
    StatusDataSentDb.STORED,
    StatusDataSentDb.EXISTS -> "Sent"
    StatusDataSentDb.FORBIDDEN -> "Failed · not authorized"
    StatusDataSentDb.NOT_FOUND -> "Failed · recipient unavailable"
    StatusDataSentDb.MISSING_FIELD -> "Failed · invalid request"
    StatusDataSentDb.ERROR -> "Failed · retry available"
}
