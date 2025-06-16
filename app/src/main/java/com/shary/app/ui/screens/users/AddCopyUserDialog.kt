package com.shary.app.ui.screens.users

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.Field
import com.shary.app.User

@Composable
fun AddCopyUserDialog(
    targetUser: User,
    onDismiss: () -> Unit,
    onAddUser: (String, String) -> Unit
) {
    var username by remember { mutableStateOf(targetUser.username + " - Copy") }
    var email by remember { mutableStateOf(targetUser.email) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add New User", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                    TextButton(onClick = { onAddUser(username, email) }) {
                        Text("ADD")
                    }
                }
            }
        }
    }
}

