package com.shary.app.ui.screens.field.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.FieldDomain
import java.time.Instant
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.FieldValueMeta
import com.shary.app.core.domain.types.valueobjects.FieldValueSpec
import com.shary.app.ui.screens.utils.LongPressHint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private enum class ValueTypeOption(val wireKind: String, val label: String) {
    List("list", "List"),
    Json("json", "JSON"),
    Questionnaire("questionnaire", "Questionnaire"),
    Yaml("yaml", "YAML"),
    File("file", "File");

    companion object {
        fun fromWire(kind: String?): ValueTypeOption =
            entries.firstOrNull { it.wireKind.equals(kind.orEmpty(), ignoreCase = true) } ?: Json
    }
}

private data class ValueSpecDraft(
    val typedEnabled: Boolean = false,
    val type: ValueTypeOption = ValueTypeOption.Json,
    val separator: String = ",",
    val isMultiSelection: Boolean = false,
    val answerCount: String = "",
    val answerOptions: List<String> = emptyList(),
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long? = null
)

private data class ParsedValueDraft(
    val plainValue: String,
    val specDraft: ValueSpecDraft
)

private val valueSpecJson = Json { ignoreUnknownKeys = true }

private fun parseValueDraft(rawValue: String): ParsedValueDraft {
    val parsed = FieldValueContract.parse(rawValue)
    if (parsed.envelope?.spec == null) {
        return ParsedValueDraft(
            plainValue = rawValue,
            specDraft = ValueSpecDraft(typedEnabled = false)
        )
    }

    val spec = parsed.envelope.spec
    val meta = parsed.envelope.meta
    return ParsedValueDraft(
        plainValue = parsed.plainData,
        specDraft = ValueSpecDraft(
            typedEnabled = true,
            type = ValueTypeOption.fromWire(spec.kind),
            separator = spec.options["separator"].orEmpty().ifBlank { "," },
            isMultiSelection = spec.options["isMultiSelection"]?.toBooleanStrictOrNull() ?: false,
            answerCount = spec.options["answerCount"].orEmpty(),
            answerOptions = spec.options["answers"]
                ?.let { encodedAnswers ->
                    runCatching { valueSpecJson.decodeFromString<List<String>>(encodedAnswers) }.getOrNull()
                }
                .orEmpty(),
            fileName = meta?.fileName.orEmpty(),
            mimeType = meta?.mimeType.orEmpty(),
            sizeBytes = meta?.sizeBytes
        )
    )
}

private fun encodeValueFromDraft(
    plainValue: String,
    draft: ValueSpecDraft
): String {
    if (!draft.typedEnabled) return plainValue

    val options = mutableMapOf<String, String>()
    when (draft.type) {
        ValueTypeOption.List -> {
            options["separator"] = draft.separator.ifBlank { "," }
        }
        ValueTypeOption.Questionnaire -> {
            options["isMultiSelection"] = draft.isMultiSelection.toString()
            val normalizedAnswers = draft.answerOptions
                .map(String::trim)
                .filter { it.isNotEmpty() }
            if (normalizedAnswers.isNotEmpty()) {
                options["answers"] = valueSpecJson.encodeToString(normalizedAnswers)
                options["answerCount"] = normalizedAnswers.size.toString()
            } else if (draft.answerCount.isNotBlank()) {
                options["answerCount"] = draft.answerCount
            }
        }
        else -> Unit
    }

    val meta = when (draft.type) {
        ValueTypeOption.File -> FieldValueMeta(
            fileName = draft.fileName.trim().ifBlank { plainValue.trim() },
            mimeType = draft.mimeType.trim().ifBlank { null },
            sizeBytes = draft.sizeBytes
        )
        else -> null
    }

    return FieldValueContract.encode(
        data = plainValue,
        spec = FieldValueSpec(
            kind = draft.type.wireKind,
            options = options
        ),
        meta = meta
    )
}

private data class PickedFileMeta(
    val uriString: String,
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long?
)

private fun splitListItems(rawValue: String, separator: String): List<String> {
    val effectiveSeparator = separator.ifEmpty { "," }
    return rawValue
        .split(effectiveSeparator)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun joinListItems(
    items: List<String>,
    pendingItem: String,
    separator: String
): String {
    val allItems = buildList {
        addAll(items.map(String::trim).filter { it.isNotEmpty() })
        pendingItem.trim().takeIf { it.isNotEmpty() }?.let(::add)
    }
    return allItems.joinToString(separator.ifEmpty { "," })
}

private fun queryPickedFileMeta(context: Context, uri: Uri): PickedFileMeta {
    var fileName = ""
    var sizeBytes: Long? = null

    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) {
                fileName = cursor.getString(nameIndex).orEmpty()
            }
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                sizeBytes = cursor.getLong(sizeIndex)
            }
        }
    }

    return PickedFileMeta(
        uriString = uri.toString(),
        fileName = fileName,
        mimeType = context.contentResolver.getType(uri),
        sizeBytes = sizeBytes
    )
}

@Composable
private fun QuestionnaireAnswersEditor(
    answers: List<String>,
    onAddAnswer: (String) -> Unit,
    onRemoveAnswer: (Int) -> Unit
) {
    var pendingAnswer by remember(answers) { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = pendingAnswer,
                onValueChange = { pendingAnswer = it },
                label = { Text("Answer option") },
                supportingText = { Text("Write one answer and tap + to add it") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = {
                    pendingAnswer.trim().takeIf { it.isNotEmpty() }?.let {
                        onAddAnswer(it)
                        pendingAnswer = ""
                    }
                },
                enabled = pendingAnswer.isNotBlank(),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("+")
            }
        }

        Text(
            text = "Configured answers: ${answers.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (answers.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                answers.forEachIndexed { index, answer ->
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = answer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveAnswer(index) }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove answer"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldValueInput(
    value: String,
    onValueChange: (String) -> Unit,
    draft: ValueSpecDraft,
    showError: Boolean,
    listItems: List<String>,
    onAddListItem: () -> Unit,
    onRemoveListItem: (Int) -> Unit,
    onBrowseFile: () -> Unit
) {
    when {
        draft.typedEnabled && draft.type == ValueTypeOption.List -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text("Value *") },
                        supportingText = {
                            when {
                                showError && listItems.isEmpty() && value.isBlank() -> Text("Add at least one element")
                                else -> Text("Write one element and tap + to include it in the list")
                            }
                        },
                        isError = showError && listItems.isEmpty() && value.isBlank(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onAddListItem,
                        enabled = value.isNotBlank(),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("+")
                    }
                }

                if (listItems.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listItems.forEachIndexed { index, item ->
                            Surface(
                                tonalElevation = 1.dp,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onRemoveListItem(index) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove element"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        draft.typedEnabled && draft.type == ValueTypeOption.File -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text("File reference *") },
                        supportingText = {
                            when {
                                showError && value.isBlank() -> Text("Required")
                                draft.fileName.isNotBlank() -> Text("Selected file: ${draft.fileName}")
                                else -> Text("Allowed: txt,csv,xlsx,pdf,yml,yaml,json,docx,md,ppt,pptx")
                            }
                        },
                        isError = showError && value.isBlank(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onBrowseFile,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Browse")
                    }
                }
            }
        }

        else -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Value *") },
                isError = showError && value.isBlank(),
                supportingText = {
                    if (showError && value.isBlank()) {
                        Text("Required")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypedValueEditor(
    draft: ValueSpecDraft,
    onChange: (ValueSpecDraft) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Typed value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = draft.typedEnabled,
            onCheckedChange = { enabled ->
                onChange(draft.copy(typedEnabled = enabled))
            }
        )
    }

    if (!draft.typedEnabled) return

    Spacer(Modifier.height(8.dp))

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = draft.type.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Value type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ValueTypeOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onChange(draft.copy(type = option))
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    when (draft.type) {
        ValueTypeOption.List -> {
            OutlinedTextField(
                value = draft.separator,
                onValueChange = { onChange(draft.copy(separator = it)) },
                label = { Text("Separator") },
                supportingText = { Text("Default: comma") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        ValueTypeOption.Questionnaire -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Multi-selection")
                Switch(
                    checked = draft.isMultiSelection,
                    onCheckedChange = { onChange(draft.copy(isMultiSelection = it)) }
                )
            }
            Spacer(Modifier.height(8.dp))
            QuestionnaireAnswersEditor(
                answers = draft.answerOptions,
                onAddAnswer = { answer ->
                    onChange(draft.copy(answerOptions = draft.answerOptions + answer))
                },
                onRemoveAnswer = { index ->
                    onChange(
                        draft.copy(
                            answerOptions = draft.answerOptions.filterIndexed { currentIndex, _ ->
                                currentIndex != index
                            }
                        )
                    )
                }
            )
            if (draft.answerOptions.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.answerCount,
                    onValueChange = { onChange(draft.copy(answerCount = it.filter(Char::isDigit))) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Answer count (optional)") },
                    supportingText = { Text("Used only when you do not define explicit answers") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        ValueTypeOption.File -> {
            OutlinedTextField(
                value = draft.fileName,
                onValueChange = { onChange(draft.copy(fileName = it)) },
                label = { Text("Selected file name") },
                readOnly = true,
                supportingText = {
                    val mimePart = draft.mimeType.takeIf { it.isNotBlank() }?.let { "MIME: $it" }
                    val sizePart = draft.sizeBytes?.let { "Size: $it bytes" }
                    Text(listOfNotNull(mimePart, sizePart).joinToString(" | ").ifBlank {
                        "Use Browse in the value field to select a file"
                    })
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        ValueTypeOption.Json, ValueTypeOption.Yaml -> {
            Text(
                "No extra options for this type.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


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
    onUpdateField: (FieldDomain) -> Unit,
    canRecoverPreviousValue: Boolean = false,
    onRecoverPreviousValue: (() -> Unit)? = null
) {
    EditorUpdateFieldDialog(
        initialField = targetField,
        title = "Update ${targetField.key}",
        onDismiss = onDismiss,
        onConfirm = onUpdateField,
        confirmLabel = "Save",
        canRecoverPreviousValue = canRecoverPreviousValue,
        onRecoverPreviousValue = onRecoverPreviousValue
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
    var valueSpecDraft by remember { mutableStateOf(ValueSpecDraft()) }
    var listItems by remember { mutableStateOf(emptyList<String>()) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val fileMeta = queryPickedFileMeta(context, uri)
        editingField = editingField.copy(value = fileMeta.uriString)
        valueSpecDraft = valueSpecDraft.copy(
            fileName = fileMeta.fileName,
            mimeType = fileMeta.mimeType.orEmpty(),
            sizeBytes = fileMeta.sizeBytes
        )
    }
    fun currentValueToPersist(): String =
        if (valueSpecDraft.typedEnabled && valueSpecDraft.type == ValueTypeOption.List) {
            joinListItems(listItems, editingField.value, valueSpecDraft.separator)
        } else {
            editingField.value.trim()
        }
    fun isValid(): Boolean = editingField.key.isNotBlank() && currentValueToPersist().isNotBlank()
    fun onSpecDraftChange(updatedDraft: ValueSpecDraft) {
        val previousDraft = valueSpecDraft
        val wasList = previousDraft.typedEnabled && previousDraft.type == ValueTypeOption.List
        val isList = updatedDraft.typedEnabled && updatedDraft.type == ValueTypeOption.List

        if (!wasList && isList) {
            listItems = splitListItems(editingField.value, updatedDraft.separator)
            editingField = editingField.copy(value = "")
        } else if (wasList && !isList) {
            editingField = editingField.copy(
                value = joinListItems(listItems, editingField.value, previousDraft.separator)
            )
            listItems = emptyList()
        }
        valueSpecDraft = updatedDraft
    }

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
            FieldValueInput(
                value = editingField.value,
                onValueChange = { editingField = editingField.copy(value = it) },
                draft = valueSpecDraft,
                showError = showErrors,
                listItems = listItems,
                onAddListItem = {
                    editingField.value.trim().takeIf { it.isNotEmpty() }?.let { newItem ->
                        listItems = listItems + newItem
                        editingField = editingField.copy(value = "")
                    }
                },
                onRemoveListItem = { index ->
                    listItems = listItems.filterIndexed { currentIndex, _ -> currentIndex != index }
                },
                onBrowseFile = { filePickerLauncher.launch(arrayOf("*/*")) }
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

                Spacer(Modifier.height(8.dp))

                TypedValueEditor(
                    draft = valueSpecDraft,
                    onChange = ::onSpecDraftChange
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
                                value = encodeValueFromDraft(
                                    plainValue = currentValueToPersist(),
                                    draft = valueSpecDraft
                                ),
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
    confirmLabel: String,
    canRecoverPreviousValue: Boolean = false,
    onRecoverPreviousValue: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.7f).dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val parsedDraft = remember(initialField) { parseValueDraft(initialField.value) }

    // Keep a working draftField of the field being edited/created
    var editingField by remember(initialField) {
        mutableStateOf(
            initialField.copy(
                value = if (
                    parsedDraft.specDraft.typedEnabled &&
                    parsedDraft.specDraft.type == ValueTypeOption.List
                ) "" else parsedDraft.plainValue
            )
        )
    }
    var showErrors by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var valueSpecDraft by remember(initialField) { mutableStateOf(parsedDraft.specDraft) }
    var listItems by remember(initialField) {
        mutableStateOf(
            if (parsedDraft.specDraft.typedEnabled && parsedDraft.specDraft.type == ValueTypeOption.List) {
                splitListItems(parsedDraft.plainValue, parsedDraft.specDraft.separator)
            } else {
                emptyList()
            }
        )
    }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val fileMeta = queryPickedFileMeta(context, uri)
        editingField = editingField.copy(value = fileMeta.uriString)
        valueSpecDraft = valueSpecDraft.copy(
            fileName = fileMeta.fileName,
            mimeType = fileMeta.mimeType.orEmpty(),
            sizeBytes = fileMeta.sizeBytes
        )
    }
    fun currentValueToPersist(): String =
        if (valueSpecDraft.typedEnabled && valueSpecDraft.type == ValueTypeOption.List) {
            joinListItems(listItems, editingField.value, valueSpecDraft.separator)
        } else {
            editingField.value.trim()
        }
    fun isValid(): Boolean = editingField.key.isNotBlank() && currentValueToPersist().isNotBlank()
    fun onSpecDraftChange(updatedDraft: ValueSpecDraft) {
        val previousDraft = valueSpecDraft
        val wasList = previousDraft.typedEnabled && previousDraft.type == ValueTypeOption.List
        val isList = updatedDraft.typedEnabled && updatedDraft.type == ValueTypeOption.List

        if (!wasList && isList) {
            listItems = splitListItems(editingField.value, updatedDraft.separator)
            editingField = editingField.copy(value = "")
        } else if (wasList && !isList) {
            editingField = editingField.copy(
                value = joinListItems(listItems, editingField.value, previousDraft.separator)
            )
            listItems = emptyList()
        }
        valueSpecDraft = updatedDraft
    }


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
            FieldValueInput(
                value = editingField.value,
                onValueChange = { editingField = editingField.copy(value = it) },
                draft = valueSpecDraft,
                showError = showErrors,
                listItems = listItems,
                onAddListItem = {
                    editingField.value.trim().takeIf { it.isNotEmpty() }?.let { newItem ->
                        listItems = listItems + newItem
                        editingField = editingField.copy(value = "")
                    }
                },
                onRemoveListItem = { index ->
                    listItems = listItems.filterIndexed { currentIndex, _ -> currentIndex != index }
                },
                onBrowseFile = { filePickerLauncher.launch(arrayOf("*/*")) }
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

                Spacer(Modifier.height(8.dp))

                TypedValueEditor(
                    draft = valueSpecDraft,
                    onChange = ::onSpecDraftChange
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (canRecoverPreviousValue && onRecoverPreviousValue != null) {
                    LongPressHint("Recover previous value for this field") {
                        OutlinedButton(
                            onClick = {
                                onRecoverPreviousValue()
                                Toast.makeText(context, "Previous value recovered", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        ) {
                            Text("Recover value")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
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
                                value = encodeValueFromDraft(
                                    plainValue = currentValueToPersist(),
                                    draft = valueSpecDraft
                                ),
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
