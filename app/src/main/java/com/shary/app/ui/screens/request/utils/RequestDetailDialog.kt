package com.shary.app.ui.screens.request.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet900

@Composable
fun RequestDetailDialog(
    senderName: String,
    timestamp: String,
    keys: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Text(
                text = senderName,
                style = MaterialTheme.typography.titleLarge,
                color = Violet900
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Requested at", style = MaterialTheme.typography.labelSmall, color = Violet500)
                Text(timestamp, style = MaterialTheme.typography.bodyMedium, color = Violet900)
                Text("Requested keys", style = MaterialTheme.typography.labelSmall, color = Violet500)
                Text(
                    text = if (keys.isEmpty()) "-" else keys.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Violet900
                )
            }
        }
    )
}
