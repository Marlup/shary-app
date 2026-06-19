package com.shary.app.ui.screens.request.utils

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.core.domain.types.valueobjects.CloudInboxItem
import com.shary.app.ui.components.SharyPrimaryButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RequestInboxReviewDialog(
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
                    text = "Pending cloud requests",
                    style = MaterialTheme.typography.titleLarge
                )

                if (isLoading) {
                    CircularProgressIndicator()
                    Text("Loading pending encrypted request items...")
                } else {
                    val current = pendingItems.firstOrNull()
                    if (current == null) {
                        Text("No pending request items right now.")
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

                        if (current.previewFieldNames.isNotEmpty()) {
                            Text(
                                text = "Requested keys (preview): ${current.previewFieldNames.joinToString(", ")}",
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
