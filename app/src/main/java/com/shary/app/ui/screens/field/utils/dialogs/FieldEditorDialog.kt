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
    initial: FieldDomain,
    allTags: List<UiFieldTag>,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (FieldDomain) -> Unit
) {
    val context = LocalContext.current

    // Keep a working draft of the field being edited/created
    var draft by remember(initial) { mutableStateOf(initial) }

    // Simple validation: key and value are required
    fun isValid(): Boolean = draft.key.isNotBlank() && draft.value.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(16.dp))

                // --- Key with suggestions (same component you already use) ---
                InputWithSuggestions(
                    key = draft.key,
                    onKeyChange = { draft = draft.copy(key = it) },
                    predefinedKeys = PredefinedKey.entries.map { it.key }
                )

                // --- Alias (optional) ---
                OutlinedTextField(
                    value = draft.keyAlias.orEmpty(),
                    onValueChange = { draft = draft.copy(keyAlias = it.ifBlank { null }) },
                    label = { Text("Key alias (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Value (required) ---
                OutlinedTextField(
                    value = draft.value,
                    onValueChange = { draft = draft.copy(value = it) },
                    label = { Text("Value") },
                    isError = draft.value.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // --- Tag picker (expects String; convert to/from UiFieldTag) ---
                TagPicker(
                    selected = UiFieldTag.toString(draft.tag),
                    onSelected = { selectedName ->
                        draft = draft.copy(tag = UiFieldTag.fromString(selectedName))
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
                            val normalized = draft.copy(
                                key = draft.key.trim(),
                                value = draft.value.trim(),
                                keyAlias = draft.keyAlias?.trim(),
                                dateAdded = if (draft.dateAdded == Instant.EPOCH) Instant.now() else draft.dateAdded
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
