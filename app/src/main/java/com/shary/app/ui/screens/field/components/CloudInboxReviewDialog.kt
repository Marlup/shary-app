package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.shary.app.core.domain.types.valueobjects.CloudInboxItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.unit.dp
import com.shary.app.ui.components.SharyPrimaryButton

@Composable
fun CloudInboxReviewDialog(
    isLoading: Boolean,
    pendingItems: List<CloudInboxItem>,
    resolveSenderLabel: (String) -> String,
    onAccept: (CloudInboxItem) -> Unit,
    onReject: (CloudInboxItem) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pending cloud data",
                    style = MaterialTheme.typography.titleLarge
                )

                if (isLoading) {
                    CircularProgressIndicator()
                    Text("Loading pending encrypted items...")
                } else {
                    val current = pendingItems.firstOrNull()
                    if (current == null) {
                        Text("No pending items right now.")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = onCancel) {
                                Text("Close")
                            }
                        }
                    } else {
                        val sender = resolveSenderLabel(current.senderHash)
                        Text("From: $sender")
                        Text("Sent: ${formatEpochSeconds(current.createdAt)}")
                        Text("Expires: ${formatEpochSeconds(current.expiresAt)}")
                        Text("Schema: v${current.payload.schemaVersion}")
                        Text("Pending queue: ${pendingItems.size} (one-by-one review)")
                        current.importDiff?.let { diff ->
                            Text(
                                text = if (diff.requiresDetailedReview) {
                                    "Review needed before import"
                                } else {
                                    "Clean import preview"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = formatImportDiff(diff),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (current.previewFieldNames.isNotEmpty()) {
                            Text(
                                text = "Field names (preview): ${current.previewFieldNames.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Preview mode: metadata only",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = { onReject(current) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject")
                            }
                            SharyPrimaryButton(
                                text = "Accept",
                                onClick = { onAccept(current) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatEpochSeconds(epochSeconds: Long): String = runCatching {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(epochSeconds))
}.getOrElse { "-" }

private fun formatImportDiff(diff: com.shary.app.application.cloud.CloudImportDiff): String {
    val parts = buildList {
        if (diff.addedKeys.isNotEmpty()) add("added: ${diff.addedKeys.joinToString(", ")}")
        if (diff.changedKeys.isNotEmpty()) add("changed: ${diff.changedKeys.joinToString(", ")}")
        if (diff.unchangedKeys.isNotEmpty()) add("unchanged: ${diff.unchangedKeys.joinToString(", ")}")
        if (diff.duplicateKeys.isNotEmpty()) add("already local: ${diff.duplicateKeys.joinToString(", ")}")
        if (diff.ignoredKeys.isNotEmpty()) add("ignored: ${diff.ignoredKeys.size}")
    }
    return parts.ifEmpty { listOf("metadata only") }.joinToString(" · ")
}
