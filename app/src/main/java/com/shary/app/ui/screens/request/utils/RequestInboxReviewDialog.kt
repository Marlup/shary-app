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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.application.request.RequestInboxDecision
import com.shary.app.application.request.RequestInboxDecisionState
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
    decisionState: RequestInboxDecisionState,
    resolveSenderLabel: (String) -> String,
    onAccept: (CloudInboxItem) -> Unit,
    onReject: (CloudInboxItem) -> Unit,
    onReceiptConsumed: (hasMoreItems: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val stateVerification = when (decisionState) {
        RequestInboxDecisionState.Idle -> null
        is RequestInboxDecisionState.Processing -> decisionState.verification
        is RequestInboxDecisionState.Failed -> decisionState.verification
        is RequestInboxDecisionState.Succeeded -> decisionState.receipt.verification
    }
    val current = stateVerification
        ?.let { verification ->
            pendingItems.firstOrNull { it.payload.verification.trim() == verification }
        }
        ?: pendingItems.firstOrNull()
    val reviewKey = stateVerification ?: current?.payload?.verification.orEmpty()
    var confirmingReject by rememberSaveable(reviewKey) { mutableStateOf(false) }
    var decisionSubmitted by rememberSaveable(reviewKey) { mutableStateOf(false) }
    var receiptSubmitted by rememberSaveable(reviewKey) { mutableStateOf(false) }

    LaunchedEffect(decisionState) {
        if (
            decisionState is RequestInboxDecisionState.Idle ||
            decisionState is RequestInboxDecisionState.Failed
        ) {
            decisionSubmitted = false
        }
        if (decisionState !is RequestInboxDecisionState.Idle) {
            confirmingReject = false
        }
        if (decisionState !is RequestInboxDecisionState.Succeeded) {
            receiptSubmitted = false
        }
    }

    fun submitOnce(item: CloudInboxItem, decision: RequestInboxDecision) {
        if (decisionSubmitted) return
        decisionSubmitted = true
        confirmingReject = false
        when (decision) {
            RequestInboxDecision.ACCEPT -> onAccept(item)
            RequestInboxDecision.REJECT -> onReject(item)
        }
    }

    Dialog(
        onDismissRequest = {
            if (
                decisionState !is RequestInboxDecisionState.Processing &&
                decisionState !is RequestInboxDecisionState.Succeeded &&
                !decisionSubmitted
            ) {
                onCancel()
            }
        }
    ) {
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

                when (val state = decisionState) {
                    RequestInboxDecisionState.Idle -> {
                        if (isLoading) {
                            CircularProgressIndicator()
                            Text("Loading pending encrypted request items...")
                        } else if (current == null) {
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

                            if (confirmingReject) {
                                Text(
                                    text = "Reject this request? No local request will be imported.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { confirmingReject = false },
                                        enabled = !decisionSubmitted,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Keep request")
                                    }
                                    SharyPrimaryButton(
                                        text = "Confirm reject",
                                        onClick = {
                                            submitOnce(current, RequestInboxDecision.REJECT)
                                        },
                                        enabled = !decisionSubmitted,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onCancel,
                                        enabled = !decisionSubmitted,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                    TextButton(
                                        onClick = { confirmingReject = true },
                                        enabled = !decisionSubmitted,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject")
                                    }
                                    SharyPrimaryButton(
                                        text = "Accept",
                                        onClick = {
                                            submitOnce(current, RequestInboxDecision.ACCEPT)
                                        },
                                        enabled = !decisionSubmitted,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    is RequestInboxDecisionState.Processing -> {
                        CircularProgressIndicator()
                        Text("Processing ${state.decision.label()}...")
                        Text(
                            text = "This request is locked until the operation finishes.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    is RequestInboxDecisionState.Failed -> {
                        Text("${state.decision.label()} was not confirmed")
                        if (state.decision == RequestInboxDecision.ACCEPT) {
                            if (state.localRequestAvailable) {
                                Text(
                                    "Local result: a received request is available with " +
                                        "${state.requestedKeyCount} requested keys."
                                )
                            } else {
                                Text("Local result: no received request was imported in this attempt.")
                            }
                        } else {
                            Text("Local result: no received request was imported.")
                        }
                        Text(
                            "Cloud result: ${state.decision.label()} was not acknowledged. " +
                                "Only retry the same decision."
                        )
                        Text(
                            text = state.error.message ?: "The cloud decision failed.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close")
                            }
                            SharyPrimaryButton(
                                text = "Retry ${state.decision.label()}",
                                onClick = {
                                    current?.let { submitOnce(it, state.decision) }
                                },
                                enabled = current != null && !decisionSubmitted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    is RequestInboxDecisionState.Succeeded -> {
                        val receipt = state.receipt
                        Text("Decision complete")
                        if (receipt.decision == RequestInboxDecision.ACCEPT) {
                            val localResult = if (receipt.localRequestAvailable) {
                                "a received request is available with " +
                                    "${receipt.requestedKeyCount} requested keys."
                            } else {
                                "no received request was imported."
                            }
                            Text("Local result: $localResult")
                        } else {
                            Text("Local result: no received request was imported.")
                        }
                        Text(
                            if (receipt.backendAcknowledged) {
                                "Cloud result: ${receipt.decision.label()} was acknowledged."
                            } else {
                                "Cloud result: ${receipt.decision.label()} was not acknowledged."
                            }
                        )
                        Text("Remaining pending requests: ${pendingItems.size}")
                        SharyPrimaryButton(
                            text = if (pendingItems.isEmpty()) "Done" else "Continue",
                            onClick = {
                                if (!receiptSubmitted) {
                                    receiptSubmitted = true
                                    onReceiptConsumed(pendingItems.isNotEmpty())
                                }
                            },
                            enabled = !receiptSubmitted,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun RequestInboxDecision.label(): String = when (this) {
    RequestInboxDecision.ACCEPT -> "Accept"
    RequestInboxDecision.REJECT -> "Reject"
}

private fun formatEpochSeconds(epochSeconds: Long): String = runCatching {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(epochSeconds))
}.getOrElse { "-" }
