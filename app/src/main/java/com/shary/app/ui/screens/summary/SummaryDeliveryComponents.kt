package com.shary.app.ui.screens.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.application.cloud.CloudDeliveryReceipt
import com.shary.app.application.cloud.CloudDeliverySummary
import com.shary.app.application.email.EmailShareState
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.ui.components.RecipientBlob
import com.shary.app.ui.components.ReviewKeyValueRow
import com.shary.app.ui.components.ReviewSectionCard
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet900

@Composable
internal fun SelectedRecipientsSection(recipients: List<UserDomain>) {
    ReviewSectionCard(sectionLabel = "Selected recipients (${recipients.size})") {
        if (recipients.isEmpty()) {
            Text("No recipient selected", color = Violet500)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipients.forEach { recipient ->
                    RecipientBlob(
                        name = recipient.username.ifBlank { recipient.email },
                        email = recipient.email
                    )
                }
            }
        }
    }
}

@Composable
internal fun CloudDeliveryReceiptSection(receipt: CloudDeliveryReceipt) {
    ReviewSectionCard(sectionLabel = "Cloud delivery receipt") {
        Text(
            text = when (receipt.summary) {
                CloudDeliverySummary.SENT -> "Sent"
                CloudDeliverySummary.PARTIAL -> "Partially sent"
                CloudDeliverySummary.FAILED -> "Failed"
            },
            style = MaterialTheme.typography.titleLarge,
            color = Violet900
        )
        Spacer(Modifier.height(6.dp))
        receipt.outcomes.forEachIndexed { index, outcome ->
            ReviewKeyValueRow(
                key = outcome.address,
                value = outcome.status.deliveryLabel(),
                last = index == receipt.outcomes.lastIndex
            )
        }
    }
}

@Composable
internal fun EmailShareReceiptSection(state: EmailShareState) {
    val message = when (state) {
        is EmailShareState.ShareSheetOpened ->
            "Share sheet opened. Sending and delivery are not confirmed. " +
                "The temporary local attachment is scheduled for deletion after 24 hours; " +
                "expired files are removed when Shary next starts."
        is EmailShareState.PreparationFailed -> state.message
        is EmailShareState.LaunchFailed -> state.message
        else -> return
    }
    ReviewSectionCard(sectionLabel = "Email handoff") {
        Text(
            text = when (state) {
                is EmailShareState.ShareSheetOpened -> "Share sheet opened"
                else -> "Handoff failed"
            },
            style = MaterialTheme.typography.titleLarge,
            color = Violet900
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Violet500
        )
    }
}

private fun StatusDataSentDb.deliveryLabel(): String = when (this) {
    StatusDataSentDb.SUCCESS,
    StatusDataSentDb.STORED,
    StatusDataSentDb.EXISTS -> "Sent"
    StatusDataSentDb.FORBIDDEN -> "Failed · not authorized"
    StatusDataSentDb.NOT_FOUND -> "Failed · recipient unavailable"
    StatusDataSentDb.MISSING_FIELD -> "Failed · invalid request"
    StatusDataSentDb.ERROR -> "Failed · retry available"
}
