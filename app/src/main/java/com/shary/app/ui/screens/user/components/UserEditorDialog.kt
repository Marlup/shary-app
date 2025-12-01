package com.shary.app.ui.screens.user.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.core.domain.models.UserDomain
import java.time.Instant

@Composable
fun UserEditorDialog(
    initial: UserDomain,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (UserDomain) -> Unit
) {
    var draft by remember(initial) { mutableStateOf(initial) }

    // very light validation (you can plug your own)
    fun isValid(): Boolean =
        draft.username.isNotBlank() && draft.email.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = draft.username,
                    onValueChange = { draft = draft.copy(username = it) },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = draft.email,
                    onValueChange = { draft = draft.copy(email = it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("CANCEL") }
                    TextButton(
                        onClick = {
                            if (!isValid()) return@TextButton
                            val normalized = draft.copy(
                                username = draft.username.trim(),
                                email = draft.email.trim(),
                                // dateAdded is set only if missing (EPOCH sentinel)
                                dateAdded = if (draft.dateAdded == Instant.EPOCH) Instant.now() else draft.dateAdded
                            )
                            onConfirm(normalized)
                            onDismiss()
                        },
                        enabled = isValid()
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}
