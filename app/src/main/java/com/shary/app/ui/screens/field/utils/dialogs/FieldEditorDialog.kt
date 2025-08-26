package com.shary.app.ui.screens.field.utils.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.PredefinedKey
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.ui.screens.field.utils.InputWithSuggestions
import com.shary.app.ui.screens.utils.components.TagPicker
import java.time.Instant

@Composable
fun FieldEditorDialog(
    initialField: FieldDomain,
    allTags: List<UiFieldTag>,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (FieldDomain) -> Unit
) {
    val context = LocalContext.current

    // Keep a working draftField of the field being edited/created
    var draftField by remember(initialField) { mutableStateOf(initialField) }

    // Simple validation: key and value are required
    fun isValid(): Boolean = draftField.key.isNotBlank() && draftField.value.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(16.dp))

                // --- Key with suggestions (same component you already use) ---
                InputWithSuggestions(
                    key = draftField.key,
                    onKeyChange = { draftField = draftField.copy(key = it) },
                    predefinedKeys = PredefinedKey.entries.map { it.key }
                )

                // --- Value (required) ---
                OutlinedTextField(
                    value = draftField.value,
                    onValueChange = { draftField = draftField.copy(value = it) },
                    label = { Text("Value") },
                    isError = draftField.value.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Alias (optional) ---
                OutlinedTextField(
                    value = draftField.keyAlias.orEmpty(),
                    onValueChange = { draftField = draftField.copy(keyAlias = it.ifBlank { null }) },
                    label = { Text("Key alias (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // --- Tag picker (expects String; convert to/from UiFieldTag) ---
                TagPicker(
                    selectedTag = draftField.tag,
                    onSelected = { selectedStringTag ->
                        draftField = draftField.copy(tag = selectedStringTag)
                    },
                    allowNone = true,
                    allTags = allTags
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            if (!isValid()) return@TextButton
                            // Ensure timestamp is set at confirm time
                            val normalized = draftField.copy(
                                key = draftField.key.trim(),
                                value = draftField.value.trim(),
                                keyAlias = draftField.keyAlias?.trim(),
                                dateAdded = if (draftField.dateAdded == Instant.EPOCH) Instant.now() else draftField.dateAdded
                            )
                            onConfirm(normalized)
                            Toast.makeText(context, "Field added", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        enabled = isValid()
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}
