package com.shary.app.ui.screens.requests.utils

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddRequestDialog(
    onDismiss: () -> Unit,
    onAddRequest: (String, String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var aliasKey by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add New Request", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Requested Key") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = aliasKey,
                    onValueChange = { aliasKey = it },
                    label = { Text("Key Alias") },
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
                    TextButton(onClick = { onAddRequest(key, aliasKey) }) {
                        Text("ADD")
                    }
                }
            }
        }
    }
}

