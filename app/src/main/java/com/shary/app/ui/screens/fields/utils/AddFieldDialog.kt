package com.shary.app.ui.screens.fields.utils

import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shary.app.core.enums.PredefinedKey

@Composable
fun AddFieldDialog(
    onDismiss: () -> Unit,
    onAddField: (String, String, String) -> Unit
) {
    val context = LocalContext.current

    var key by remember { mutableStateOf("") }
    val predefinedKeys = PredefinedKey.entries.map { it.key }
    var keyAlias by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    fun isValidInputs(): Boolean {
        return key.isNotBlank() && value.isNotBlank()
    }

    LaunchedEffect(Unit) {
        key = ""
        keyAlias = ""
        value = ""
    }

    Dialog(onDismissRequest = onDismiss ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Add new field",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                InputWithSuggestions(
                    key = key,
                    onKeyChange = { key = it },
                    predefinedKeys = predefinedKeys
                )

                OutlinedTextField(
                    value = keyAlias,
                    onValueChange = { keyAlias = it },
                    label = { Text("Key alias (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    isError = value.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    /*
                    TextButton(onClick = { onAddField(key, keyAlias, value) }) {
                        Text("Add")
                    }
                    */
                    TextButton(
                        onClick = {
                            if (isValidInputs()) {
                                onAddField(key, keyAlias, value)
                                Toast.makeText(context, "Field added", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        enabled = isValidInputs()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}