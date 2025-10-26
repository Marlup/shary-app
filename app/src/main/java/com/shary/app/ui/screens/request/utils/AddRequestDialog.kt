package com.shary.app.ui.screens.request.utils

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.PredefinedKey
import com.shary.app.ui.screens.field.components.InputWithSuggestions

@Composable
fun AddRequestDialog(
    onDismiss: () -> Unit,
    onAddRequest: (String, String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var aliasKey by remember { mutableStateOf("") }
    val predefinedKeys = PredefinedKey.entries.map { it.key }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add New Request", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                InputWithSuggestions(
                    key = key,
                    onKeyChange = { key = it },
                    predefinedKeys = predefinedKeys
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

