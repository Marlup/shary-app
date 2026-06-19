package com.shary.app.ui.screens.user.components

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
fun UserDetailDialog(
    name: String,
    email: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = Violet900
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Email", style = MaterialTheme.typography.labelSmall, color = Violet500)
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Violet900
                )
            }
        }
    )
}
