package com.shary.app.ui.screens.field.components

import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import java.time.Instant
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.core.domain.types.enums.PredefinedKey
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


/**
 * Thin wrapper over FieldEditorDialog to create a brand-new field.
 * Keeps UI consistent and reuse the same editor as "copy" flow.
 */
@Composable
fun AddFieldDialog(
    onDismiss: () -> Unit,
    onAddField: (FieldDomain) -> Unit,
) {
    EditorAddFieldDialog(
        onDismiss = onDismiss,
        onConfirm = onAddField
    )
}

@Composable
fun UpdateFieldDialog(
    targetField: FieldDomain,
    onDismiss: () -> Unit,
    onUpdateField: (FieldDomain) -> Unit
) {
    EditorUpdateFieldDialog(
        initialField = targetField,
        title = "Update ${targetField.key}",
        onDismiss = onDismiss,
        onConfirm = onUpdateField
    )
}

@Composable
fun AddCopiedFieldDialog(
    targetField: FieldDomain,
    onDismiss: () -> Unit,
    onAddCopiedField: (FieldDomain) -> Unit
) {
    val initialField = targetField.copy(
        key = "${targetField.key} - Copy",
        dateAdded = Instant.EPOCH
    )

    EditorUpdateFieldDialog(
        initialField = initialField,
        title = "Update Field ${initialField.key}",
        onDismiss = onDismiss,
        onConfirm = onAddCopiedField
    )
}

@Composable
fun EditorAddFieldDialog(
    onDismiss: () -> Unit,
    onConfirm: (FieldDomain) -> Unit
) {
    val context = LocalContext.current

    // Seed empty domain model (dateAdded set on confirm inside FieldEditorDialog)
    var editingField by remember { mutableStateOf<FieldDomain>(FieldDomain.initialize()) }
    // Simple validation: key and value are required
    fun isValid(): Boolean = editingField.key.isNotBlank() && editingField.value.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Text(
                    "Add New Field",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.surface
                    )

                Spacer(Modifier.height(16.dp))

                // --- Key with suggestions (same component you already use) ---
                InputWithSuggestions(
                    key = editingField.key,
                    onKeyChange = { editingField = editingField.copy(key = it) },
                    predefinedKeys = PredefinedKey.entries.map { it.key }
                )

                // --- Value (required) ---
                OutlinedTextField(
                    value = editingField.value,
                    onValueChange = { editingField = editingField.copy(value = it) },
                    label = { Text("Enter value") },
                    isError = editingField.value.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Alias (optional) ---
                OutlinedTextField(
                    value = editingField.keyAlias,
                    onValueChange = { editingField = editingField.copy(keyAlias = it) },
                    label = { Text("Enter Key alias (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // --- Tag picker (expects String; convert to/from UiFieldTag) ---
                TagPicker(
                    selectedTag = editingField.tag,
                    onTagSelected = { selectedTag ->
                        editingField = editingField.copy(tag = selectedTag)
                                    },
                )

                Spacer(Modifier.height(16.dp))

                // Cancel / Add buttons

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSecondary)
                    }
                    TextButton(
                        onClick = {
                            if (!isValid()) return@TextButton
                            // Ensure timestamp is set at confirm time
                            val normalizedField = editingField.copy(
                                key = editingField.key.trim(),
                                value = editingField.value.trim(),
                                keyAlias = editingField.keyAlias.trim(),
                                tag = editingField.tag,
                                dateAdded = if (editingField.dateAdded == Instant.EPOCH) Instant.now() else editingField.dateAdded
                            )
                            onConfirm(normalizedField)
                            Toast.makeText(context, "Field added", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        enabled = isValid()
                    ) { Text("Add", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
fun EditorUpdateFieldDialog(
    initialField: FieldDomain,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (FieldDomain) -> Unit
) {
    val context = LocalContext.current

    // Keep a working draftField of the field being edited/created
    var editingField by remember(initialField) { mutableStateOf(initialField) }
    // Simple validation: key and value are required
    fun isValid(): Boolean = editingField.key.isNotBlank() && editingField.value.isNotBlank()


    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.surface
                    )

                Spacer(Modifier.height(16.dp))

                // --- Key with suggestions (same component you already use) ---
                InputWithSuggestions(
                    key = editingField.key,
                    onKeyChange = { editingField = editingField.copy(key = it) },
                    predefinedKeys = PredefinedKey.entries.map { it.key }
                )

                // --- Value (required) ---
                OutlinedTextField(
                    value = editingField.value,
                    onValueChange = { editingField = editingField.copy(value = it) },
                    label = { Text("Enter value") },
                    isError = editingField.value.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Alias (optional) ---
                OutlinedTextField(
                    value = editingField.keyAlias,
                    onValueChange = { editingField = editingField.copy(keyAlias = it) },
                    label = { Text("Enter Key alias (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // --- Tag picker (expects String; convert to/from UiFieldTag) ---
                TagPicker(
                    selectedTag = editingField.tag,
                    onTagSelected = { selectedTag ->
                        editingField = editingField.copy(tag = selectedTag)
                                    },
                )

                Spacer(Modifier.height(16.dp))

                // Cancel / Add buttons

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSecondary)
                    }
                    TextButton(
                        onClick = {
                            if (!isValid()) return@TextButton
                            // Ensure timestamp is set at confirm time
                            val normalizedField = editingField.copy(
                                key = editingField.key.trim(),
                                value = editingField.value.trim(),
                                keyAlias = editingField.keyAlias.trim(),
                                tag = editingField.tag,
                                dateAdded = if (editingField.dateAdded == Instant.EPOCH) Instant.now() else editingField.dateAdded
                            )
                            onConfirm(normalizedField)
                            Toast.makeText(context, "Field added", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        enabled = isValid()
                    ) { Text("Add", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}