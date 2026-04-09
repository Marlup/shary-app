package com.shary.app.ui.screens.field.components

import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.FieldDomain
import java.time.Instant
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.shary.app.core.domain.types.enums.PredefinedKey
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import com.shary.app.ui.screens.utils.LongPressHint


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
        onConfirm = onAddField,
        confirmLabel = "Add"
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
        onConfirm = onUpdateField,
        confirmLabel = "Save"
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
        onConfirm = onAddCopiedField,
        confirmLabel = "Save"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorAddFieldDialog(
    onDismiss: () -> Unit,
    onConfirm: (FieldDomain) -> Unit,
    confirmLabel: String
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.7f).dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Seed empty domain model (dateAdded set on confirm inside FieldEditorDialog)
    var editingField by remember { mutableStateOf<FieldDomain>(FieldDomain.initialize()) }
    var showErrors by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    // Simple validation: key and value are required
    fun isValid(): Boolean = editingField.key.isNotBlank() && editingField.value.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Add New Field",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Required",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // --- Key with suggestions (same component you already use) ---
            InputWithSuggestions(
                key = editingField.key,
                onKeyChange = { editingField = editingField.copy(key = it) },
                predefinedKeys = PredefinedKey.entries.map { it.key },
                label = "Key *",
                showError = showErrors
            )

            // --- Value (required) ---
            OutlinedTextField(
                value = editingField.value,
                onValueChange = { editingField = editingField.copy(value = it) },
                label = { Text("Value *") },
                isError = showErrors && editingField.value.isBlank(),
                supportingText = {
                    if (showErrors && editingField.value.isBlank()) {
                        Text("Required")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Advanced options",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (showAdvanced) {
                Spacer(Modifier.height(8.dp))

                // --- Alias (optional) ---
                OutlinedTextField(
                    value = editingField.keyAlias,
                    onValueChange = { editingField = editingField.copy(keyAlias = it) },
                    label = { Text("Alias") },
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
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                LongPressHint("Close without saving") {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
                LongPressHint("Save this field") {
                    FilledTonalButton(
                        onClick = {
                            showErrors = true
                            if (!isValid()) return@FilledTonalButton
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
                        }
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorUpdateFieldDialog(
    initialField: FieldDomain,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (FieldDomain) -> Unit,
    confirmLabel: String
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.7f).dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Keep a working draftField of the field being edited/created
    var editingField by remember(initialField) { mutableStateOf(initialField) }
    var showErrors by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    // Simple validation: key and value are required
    fun isValid(): Boolean = editingField.key.isNotBlank() && editingField.value.isNotBlank()


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Required",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // --- Key with suggestions (same component you already use) ---
            InputWithSuggestions(
                key = editingField.key,
                onKeyChange = { editingField = editingField.copy(key = it) },
                predefinedKeys = PredefinedKey.entries.map { it.key },
                label = "Key *",
                showError = showErrors
            )

            // --- Value (required) ---
            OutlinedTextField(
                value = editingField.value,
                onValueChange = { editingField = editingField.copy(value = it) },
                label = { Text("Value *") },
                isError = showErrors && editingField.value.isBlank(),
                supportingText = {
                    if (showErrors && editingField.value.isBlank()) {
                        Text("Required")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Advanced options",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (showAdvanced) {
                Spacer(Modifier.height(8.dp))

                // --- Alias (optional) ---
                OutlinedTextField(
                    value = editingField.keyAlias,
                    onValueChange = { editingField = editingField.copy(keyAlias = it) },
                    label = { Text("Alias") },
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
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                LongPressHint("Close without saving") {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
                LongPressHint("Save this field") {
                    FilledTonalButton(
                        onClick = {
                            showErrors = true
                            if (!isValid()) return@FilledTonalButton
                            // Ensure timestamp is set at confirm time
                            val normalizedField = editingField.copy(
                                key = editingField.key.trim(),
                                value = editingField.value.trim(),
                                keyAlias = editingField.keyAlias.trim(),
                                tag = editingField.tag,
                                dateAdded = if (editingField.dateAdded == Instant.EPOCH) Instant.now() else editingField.dateAdded
                            )
                            onConfirm(normalizedField)
                            Toast.makeText(context, "Field saved", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}
