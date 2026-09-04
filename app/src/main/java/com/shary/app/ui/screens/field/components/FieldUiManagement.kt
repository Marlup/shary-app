package com.shary.app.ui.screens.field.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.FieldDomain
import java.time.Instant
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.PredefinedKey
import com.shary.app.core.domain.types.enums.Tag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.FieldValueMeta
import com.shary.app.core.domain.types.valueobjects.FieldValueSpec
import com.shary.app.core.domain.types.valueobjects.TaskItem
import com.shary.app.core.domain.types.valueobjects.TaskStatus
import com.shary.app.core.domain.types.valueobjects.TaskValueContract
import com.shary.app.core.domain.types.valueobjects.TaskValueEncodingResult
import com.shary.app.core.domain.types.valueobjects.TaskValueValidationIssue
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet900
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private enum class ValueTypeOption(val wireKind: String, val label: String) {
    Text("text", "Text"),
    List("list", "List"),
    Email("email", "Email"),
    LinkUrl("link_url", "Link/URL"),
    Json("json", "JSON"),
    Questionnaire("questionnaire", "Questionnaire"),
    Yaml("yaml", "YAML"),
    File("file", "File"),
    Tasks("tasks", "Tasks"),
    Sensitive("sensitive", "Sensitive");

    companion object {
        fun fromWire(kind: String?): ValueTypeOption =
            when (kind.orEmpty().trim().lowercase()) {
                "link", "url" -> LinkUrl
                "" -> Text
                else -> entries.firstOrNull { it.wireKind.equals(kind.orEmpty(), ignoreCase = true) } ?: Text
            }
    }
}

private data class ValueSpecDraft(
    val typedEnabled: Boolean = true,
    val type: ValueTypeOption = ValueTypeOption.Text,
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
private val httpUrlRegex = Regex("^(https?|ftp)://\\S+$", RegexOption.IGNORE_CASE)
private val mailtoRegex = Regex("^mailto:\\S+@\\S+\\.\\S+$", RegexOption.IGNORE_CASE)
private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", RegexOption.IGNORE_CASE)
private val fileUriRegex = Regex("^(content|file)://\\S+$", RegexOption.IGNORE_CASE)

private fun autoDetectValueSpec(plainValue: String): ValueSpecDraft? {
    val candidate = plainValue.trim()
    if (candidate.isBlank()) return null

    if (fileUriRegex.matches(candidate)) {
        return ValueSpecDraft(
            typedEnabled = true,
            type = ValueTypeOption.File
        )
    }
    if (emailRegex.matches(candidate)) {
        return ValueSpecDraft(
            typedEnabled = true,
            type = ValueTypeOption.Email
        )
    }
    if (httpUrlRegex.matches(candidate) || mailtoRegex.matches(candidate)) {
        return ValueSpecDraft(
            typedEnabled = true,
            type = ValueTypeOption.LinkUrl
        )
    }
    val jsonDetected = runCatching {
        val parsed = valueSpecJson.decodeFromString(JsonElement.serializer(), candidate)
        parsed is JsonObject || parsed is JsonArray
    }.getOrDefault(false)
    if (jsonDetected) {
        return ValueSpecDraft(
            typedEnabled = true,
            type = ValueTypeOption.Json
        )
    }

    val commaItems = candidate.split(",")
        .map(String::trim)
        .filter { it.isNotEmpty() }
    if (commaItems.size >= 2) {
        return ValueSpecDraft(
            typedEnabled = true,
            type = ValueTypeOption.List,
            separator = ","
        )
    }

    return null
}

private fun parseValueDraft(rawValue: String): ParsedValueDraft {
    val parsed = FieldValueContract.parse(rawValue)
    if (parsed.envelope?.spec == null) {
        return ParsedValueDraft(
            plainValue = rawValue,
            specDraft = ValueSpecDraft(typedEnabled = true, type = ValueTypeOption.Text)
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
    if (!draft.typedEnabled || draft.type == ValueTypeOption.Text) return plainValue

    if (draft.type == ValueTypeOption.Tasks) {
        return FieldValueContract.encode(
            data = plainValue,
            spec = FieldValueSpec(kind = draft.type.wireKind)
        )
    }

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
private fun TaskStatusSelector(
    status: TaskStatus,
    onStatusSelected: (TaskStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(status.wireName)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TaskStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.wireName) },
                    onClick = {
                        onStatusSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun encodeTaskDraft(
    taskItems: List<TaskItem>,
    pendingTitle: String
): TaskValueEncodingResult {
    val pendingTask = pendingTitle.trim()
        .takeIf(String::isNotEmpty)
        ?.let(::TaskItem)
    val itemsToEncode = if (pendingTask == null) taskItems else taskItems + pendingTask
    return TaskValueContract.tryEncode(itemsToEncode)
}

@Composable
private fun FieldValueInput(
    value: String,
    onValueChange: (String) -> Unit,
    draft: ValueSpecDraft,
    showError: Boolean,
    listItems: List<String>,
    onAddListItem: () -> Unit,
    onUpdateListItem: (Int, String) -> Unit,
    onRemoveListItem: (Int) -> Unit,
    taskItems: List<TaskItem>,
    taskValidation: TaskValueEncodingResult.Invalid?,
    onAddTask: () -> Unit,
    onUpdateTask: (Int, TaskItem) -> Unit,
    onRemoveTask: (Int) -> Unit,
    onBrowseFile: () -> Unit
) {
    when {
        draft.typedEnabled && draft.type == ValueTypeOption.Tasks -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text("(required) Task title") },
                        supportingText = {
                            when {
                                showError && taskValidation?.issue == TaskValueValidationIssue.EMPTY ->
                                    Text(TaskValueValidationIssue.EMPTY.message)
                                showError && taskValidation?.issue == TaskValueValidationIssue.BLANK_TITLE ->
                                    Text("Fix the blank task titles below")
                                showError && taskValidation?.issue == TaskValueValidationIssue.ENCODING_FAILED ->
                                    Text(TaskValueValidationIssue.ENCODING_FAILED.message)
                                else -> Text("New tasks start as todo")
                            }
                        },
                        isError = showError &&
                            (taskValidation?.issue == TaskValueValidationIssue.EMPTY ||
                                taskValidation?.issue == TaskValueValidationIssue.ENCODING_FAILED),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("field-task-pending-title")
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onAddTask,
                        enabled = value.isNotBlank(),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("+")
                    }
                }

                if (taskItems.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        taskItems.forEachIndexed { index, task ->
                            val hasBlankTitle = showError &&
                                index in taskValidation?.invalidIndices.orEmpty()
                            Surface(
                                tonalElevation = 1.dp,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = task.title,
                                        onValueChange = {
                                            onUpdateTask(index, task.copy(title = it))
                                        },
                                        label = { Text("Task ${index + 1}") },
                                        supportingText = if (hasBlankTitle) {
                                            { Text(TaskValueValidationIssue.BLANK_TITLE.message) }
                                        } else {
                                            null
                                        },
                                        isError = hasBlankTitle,
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("field-task-title-$index")
                                    )
                                    TaskStatusSelector(
                                        status = task.status,
                                        onStatusSelected = {
                                            onUpdateTask(index, task.copy(status = it))
                                        }
                                    )
                                    IconButton(onClick = { onRemoveTask(index) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove task"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        draft.typedEnabled && draft.type == ValueTypeOption.List -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text("(required) Enter value") },
                        supportingText = {
                            when {
                                showError && listItems.isEmpty() && value.isBlank() -> Text("Add at least one element")
                                else -> Text("Write one element and tap + to include it in the list")
                            }
                        },
                        isError = showError && listItems.isEmpty() && value.isBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("field-list-pending-value")
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
                                    OutlinedTextField(
                                        value = item,
                                        onValueChange = { updatedItem ->
                                            onUpdateListItem(index, updatedItem)
                                        },
                                        label = { Text("Element ${index + 1}") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("field-list-element-$index")
                                    )
                                    Spacer(Modifier.width(8.dp))
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
            val invalidEmail = draft.typedEnabled &&
                draft.type == ValueTypeOption.Email &&
                value.isNotBlank() &&
                !emailRegex.matches(value.trim())
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("(required) Enter value") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (draft.typedEnabled && draft.type == ValueTypeOption.Email) {
                        KeyboardType.Email
                    } else {
                        KeyboardType.Text
                    }
                ),
                isError = showError && (value.isBlank() || invalidEmail),
                supportingText = {
                    when {
                        showError && value.isBlank() -> Text("Required")
                        showError && invalidEmail -> Text("Enter a valid email address")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSpecificOptions(
    draft: ValueSpecDraft,
    onChange: (ValueSpecDraft) -> Unit
) {
    when (draft.type) {
        ValueTypeOption.Text -> Unit
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
        ValueTypeOption.Json,
        ValueTypeOption.Yaml,
        ValueTypeOption.LinkUrl,
        ValueTypeOption.Email,
        ValueTypeOption.Tasks,
        ValueTypeOption.Sensitive -> {
            Text(
                "No extra options for this type.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactValueTypeSelector(
    draft: ValueSpecDraft,
    onChange: (ValueSpecDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SharySoftButton(
            text = draft.type.label,
            icon = Icons.Default.Category,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field-value-type-selector"),
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ValueTypeOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    modifier = Modifier.testTag("field-value-type-option-${option.wireKind}"),
                    onClick = {
                        onChange(draft.copy(type = option, typedEnabled = true))
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FieldMetadataSelectors(
    draft: ValueSpecDraft,
    onDraftChange: (ValueSpecDraft) -> Unit,
    selectedTag: Tag,
    onTagSelected: (Tag) -> Unit,
    availableTags: List<Tag>?
) {
    Column(
        modifier = Modifier.width(132.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactValueTypeSelector(
            draft = draft,
            onChange = onDraftChange,
            modifier = Modifier.fillMaxWidth()
        )
        CompactTagSelector(
            selectedTag = selectedTag,
            onTagSelected = onTagSelected,
            availableTags = availableTags,
            modifier = Modifier.fillMaxWidth()
        )
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
    availableTags: List<Tag>? = null,
    isSaving: Boolean = false,
    errorMessage: String? = null,
) {
    EditorAddFieldDialog(
        onDismiss = onDismiss,
        onConfirm = onAddField,
        confirmLabel = "Add",
        availableTags = availableTags,
        isSaving = isSaving,
        errorMessage = errorMessage
    )
}

@Composable
fun UpdateFieldDialog(
    targetField: FieldDomain,
    onDismiss: () -> Unit,
    onUpdateField: (FieldDomain) -> Unit,
    canRecoverPreviousValue: Boolean = false,
    onRecoverPreviousValue: (() -> Unit)? = null,
    availableTags: List<Tag>? = null,
    isSaving: Boolean = false,
    errorMessage: String? = null
) {
    EditorUpdateFieldDialog(
        initialField = targetField,
        title = "Update ${targetField.key}",
        onDismiss = onDismiss,
        onConfirm = onUpdateField,
        confirmLabel = "Save",
        canRecoverPreviousValue = canRecoverPreviousValue,
        onRecoverPreviousValue = onRecoverPreviousValue,
        availableTags = availableTags,
        isSaving = isSaving,
        errorMessage = errorMessage
    )
}

@Composable
fun AddCopiedFieldDialog(
    targetField: FieldDomain,
    onDismiss: () -> Unit,
    onAddCopiedField: (FieldDomain) -> Unit,
    availableTags: List<Tag>? = null,
    isSaving: Boolean = false,
    errorMessage: String? = null
) {
    val initialField = targetField.copy(
        key = "${targetField.key} - Copy",
        dateAdded = Instant.EPOCH,
        isMcpShareable = false
    )

    EditorUpdateFieldDialog(
        initialField = initialField,
        title = "Update Field ${initialField.key}",
        onDismiss = onDismiss,
        onConfirm = onAddCopiedField,
        confirmLabel = "Save",
        availableTags = availableTags,
        isSaving = isSaving,
        errorMessage = errorMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorAddFieldDialog(
    onDismiss: () -> Unit,
    onConfirm: (FieldDomain) -> Unit,
    confirmLabel: String,
    availableTags: List<Tag>? = null,
    isSaving: Boolean = false,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val sheetMaxHeight = with(density) { (windowInfo.containerSize.height.toDp() * 0.7f) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Seed empty domain model (dateAdded set on confirm inside FieldEditorDialog)
    var editingField by remember { mutableStateOf<FieldDomain>(FieldDomain.initialize()) }
    var showErrors by remember { mutableStateOf(false) }
    var valueSpecDraft by remember { mutableStateOf(ValueSpecDraft()) }
    var listItems by remember { mutableStateOf(emptyList<String>()) }
    var taskItems by remember { mutableStateOf(emptyList<TaskItem>()) }
    var typedSpecTouched by remember { mutableStateOf(false) }
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
    fun currentTaskEncoding(): TaskValueEncodingResult =
        encodeTaskDraft(taskItems, editingField.value)

    fun currentValueToPersist(): String? =
        when {
            valueSpecDraft.typedEnabled && valueSpecDraft.type == ValueTypeOption.List ->
                joinListItems(listItems, editingField.value, valueSpecDraft.separator)
            valueSpecDraft.typedEnabled && valueSpecDraft.type == ValueTypeOption.Tasks ->
                (currentTaskEncoding() as? TaskValueEncodingResult.Success)?.encodedValue
            else -> editingField.value.trim()
        }

    fun isValid(): Boolean {
        val value = currentValueToPersist() ?: return false
        if (editingField.key.isBlank() || value.isBlank()) return false
        return !valueSpecDraft.typedEnabled ||
            valueSpecDraft.type != ValueTypeOption.Email ||
            emailRegex.matches(value.trim())
    }
    fun onSpecDraftChange(updatedDraft: ValueSpecDraft) {
        typedSpecTouched = true
        val previousDraft = valueSpecDraft
        val wasList = previousDraft.typedEnabled && previousDraft.type == ValueTypeOption.List
        val isList = updatedDraft.typedEnabled && updatedDraft.type == ValueTypeOption.List
        val wasTasks = previousDraft.typedEnabled && previousDraft.type == ValueTypeOption.Tasks
        val isTasks = updatedDraft.typedEnabled && updatedDraft.type == ValueTypeOption.Tasks

        if (!wasList && isList) {
            listItems = splitListItems(editingField.value, updatedDraft.separator)
            editingField = editingField.copy(value = "")
        } else if (wasList && !isList) {
            editingField = editingField.copy(
                value = joinListItems(listItems, editingField.value, previousDraft.separator)
            )
            listItems = emptyList()
        }
        if (!wasTasks && isTasks) {
            taskItems = emptyList()
        } else if (wasTasks && !isTasks) {
            when (val result = currentTaskEncoding()) {
                is TaskValueEncodingResult.Success -> {
                    editingField = editingField.copy(value = result.encodedValue)
                }
                is TaskValueEncodingResult.Invalid -> {
                    if (result.issue != TaskValueValidationIssue.EMPTY) {
                        showErrors = true
                        return
                    }
                    editingField = editingField.copy(value = "")
                }
            }
            taskItems = emptyList()
        }
        if (updatedDraft.type != ValueTypeOption.Sensitive ||
            previousDraft.type != ValueTypeOption.Sensitive
        ) {
            editingField = editingField.copy(isMcpShareable = false)
        }
        valueSpecDraft = updatedDraft
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        shape = SharyRadius.sheet,
        containerColor = SurfaceLight,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Violet200, RoundedCornerShape(99.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add New Field",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Violet900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                FieldMetadataSelectors(
                    draft = valueSpecDraft,
                    onDraftChange = ::onSpecDraftChange,
                    selectedTag = editingField.tag,
                    onTagSelected = { selectedTag ->
                        editingField = editingField.copy(tag = selectedTag)
                    },
                    availableTags = availableTags
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Category.${valueSpecDraft.type.label}",
                style = MaterialTheme.typography.labelSmall,
                color = Violet500
            )

            Spacer(Modifier.height(8.dp))

            // --- Key with suggestions (same component you already use) ---
            InputWithSuggestions(
                key = editingField.key,
                onKeyChange = { editingField = editingField.copy(key = it) },
                predefinedKeys = PredefinedKey.entries.map { it.key },
                label = "(required) Enter key",
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
                onUpdateListItem = { index, updatedItem ->
                    listItems = listItems.mapIndexed { currentIndex, currentItem ->
                        if (currentIndex == index) updatedItem else currentItem
                    }
                },
                onRemoveListItem = { index ->
                    listItems = listItems.filterIndexed { currentIndex, _ -> currentIndex != index }
                },
                taskItems = taskItems,
                taskValidation = currentTaskEncoding() as? TaskValueEncodingResult.Invalid,
                onAddTask = {
                    editingField.value.trim().takeIf { it.isNotEmpty() }?.let { title ->
                        taskItems = taskItems + TaskItem(title = title)
                        editingField = editingField.copy(value = "")
                    }
                },
                onUpdateTask = { index, updatedTask ->
                    taskItems = taskItems.mapIndexed { currentIndex, currentTask ->
                        if (currentIndex == index) updatedTask else currentTask
                    }
                },
                onRemoveTask = { index ->
                    taskItems = taskItems.filterIndexed { currentIndex, _ -> currentIndex != index }
                },
                onBrowseFile = { filePickerLauncher.launch(arrayOf("*/*")) }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = editingField.keyAlias,
                onValueChange = { editingField = editingField.copy(keyAlias = it) },
                label = { Text("(Optional) Alias") },
                modifier = Modifier.fillMaxWidth()
            )

            if (valueSpecDraft.type == ValueTypeOption.Sensitive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow MCP access")
                        Text(
                            "Off by default. Local authentication is still required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editingField.isMcpShareable,
                        onCheckedChange = {
                            editingField = editingField.copy(isMcpShareable = it)
                        }
                    )
                }
            }

            if (valueSpecDraft.type != ValueTypeOption.Text) {
                Spacer(Modifier.height(8.dp))
                TypeSpecificOptions(
                    draft = valueSpecDraft,
                    onChange = ::onSpecDraftChange
                )
            }

            Spacer(Modifier.height(20.dp))

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SharySoftButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                )
                SharyPrimaryButton(
                    text = if (isSaving) "Saving..." else confirmLabel,
                    onClick = {
                        showErrors = true
                        if (!isValid()) return@SharyPrimaryButton
                        val persistedPlainValue = currentValueToPersist()
                            ?: return@SharyPrimaryButton
                        val draftToPersist =
                            if (!typedSpecTouched && !valueSpecDraft.typedEnabled) {
                                autoDetectValueSpec(persistedPlainValue) ?: valueSpecDraft
                            } else {
                                valueSpecDraft
                            }
                        val normalizedField = editingField.copy(
                            key = editingField.key.trim(),
                            value = encodeValueFromDraft(
                                plainValue = persistedPlainValue,
                                draft = draftToPersist
                            ),
                            keyAlias = editingField.keyAlias.trim(),
                            tag = editingField.tag,
                            dateAdded = if (editingField.dateAdded == Instant.EPOCH) Instant.now() else editingField.dateAdded,
                            isMcpShareable = valueSpecDraft.type == ValueTypeOption.Sensitive &&
                                editingField.isMcpShareable
                        )
                        onConfirm(normalizedField)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving && (!showErrors || isValid())
                )
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
    onRecoverPreviousValue: (() -> Unit)? = null,
    availableTags: List<Tag>? = null,
    isSaving: Boolean = false,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val sheetMaxHeight = with(density) { (windowInfo.containerSize.height.toDp() * 0.7f) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val parsedDraft = remember(initialField) { parseValueDraft(initialField.value) }

    // Keep a working draftField of the field being edited/created
    var editingField by remember(initialField) {
        mutableStateOf(
            initialField.copy(
                value = if (
                    parsedDraft.specDraft.typedEnabled &&
                    parsedDraft.specDraft.type in setOf(ValueTypeOption.List, ValueTypeOption.Tasks)
                ) "" else parsedDraft.plainValue
            )
        )
    }
    var showErrors by remember { mutableStateOf(false) }
    var valueSpecDraft by remember(initialField) { mutableStateOf(parsedDraft.specDraft) }
    var typedSpecTouched by remember(initialField) { mutableStateOf(false) }
    var listItems by remember(initialField) {
        mutableStateOf(
            if (parsedDraft.specDraft.typedEnabled && parsedDraft.specDraft.type == ValueTypeOption.List) {
                splitListItems(parsedDraft.plainValue, parsedDraft.specDraft.separator)
            } else {
                emptyList()
            }
        )
    }
    var taskItems by remember(initialField) {
        mutableStateOf(
            if (parsedDraft.specDraft.typedEnabled &&
                parsedDraft.specDraft.type == ValueTypeOption.Tasks
            ) {
                TaskValueContract.decode(parsedDraft.plainValue).getOrDefault(emptyList())
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
    fun currentTaskEncoding(): TaskValueEncodingResult =
        encodeTaskDraft(taskItems, editingField.value)

    fun currentValueToPersist(): String? =
        when {
            valueSpecDraft.typedEnabled && valueSpecDraft.type == ValueTypeOption.List ->
                joinListItems(listItems, editingField.value, valueSpecDraft.separator)
            valueSpecDraft.typedEnabled && valueSpecDraft.type == ValueTypeOption.Tasks ->
                (currentTaskEncoding() as? TaskValueEncodingResult.Success)?.encodedValue
            else -> editingField.value.trim()
        }

    fun isValid(): Boolean {
        val value = currentValueToPersist() ?: return false
        if (editingField.key.isBlank() || value.isBlank()) return false
        return !valueSpecDraft.typedEnabled ||
            valueSpecDraft.type != ValueTypeOption.Email ||
            emailRegex.matches(value.trim())
    }
    fun onSpecDraftChange(updatedDraft: ValueSpecDraft) {
        typedSpecTouched = true
        val previousDraft = valueSpecDraft
        val wasList = previousDraft.typedEnabled && previousDraft.type == ValueTypeOption.List
        val isList = updatedDraft.typedEnabled && updatedDraft.type == ValueTypeOption.List
        val wasTasks = previousDraft.typedEnabled && previousDraft.type == ValueTypeOption.Tasks
        val isTasks = updatedDraft.typedEnabled && updatedDraft.type == ValueTypeOption.Tasks

        if (!wasList && isList) {
            listItems = splitListItems(editingField.value, updatedDraft.separator)
            editingField = editingField.copy(value = "")
        } else if (wasList && !isList) {
            editingField = editingField.copy(
                value = joinListItems(listItems, editingField.value, previousDraft.separator)
            )
            listItems = emptyList()
        }
        if (!wasTasks && isTasks) {
            taskItems = emptyList()
        } else if (wasTasks && !isTasks) {
            when (val result = currentTaskEncoding()) {
                is TaskValueEncodingResult.Success -> {
                    editingField = editingField.copy(value = result.encodedValue)
                }
                is TaskValueEncodingResult.Invalid -> {
                    if (result.issue != TaskValueValidationIssue.EMPTY) {
                        showErrors = true
                        return
                    }
                    editingField = editingField.copy(value = "")
                }
            }
            taskItems = emptyList()
        }
        if (updatedDraft.type != ValueTypeOption.Sensitive ||
            previousDraft.type != ValueTypeOption.Sensitive
        ) {
            editingField = editingField.copy(isMcpShareable = false)
        }
        valueSpecDraft = updatedDraft
    }


    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        shape = SharyRadius.sheet,
        containerColor = SurfaceLight,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Violet200, RoundedCornerShape(99.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Violet900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                FieldMetadataSelectors(
                    draft = valueSpecDraft,
                    onDraftChange = ::onSpecDraftChange,
                    selectedTag = editingField.tag,
                    onTagSelected = { selectedTag ->
                        editingField = editingField.copy(tag = selectedTag)
                    },
                    availableTags = availableTags
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Category.${valueSpecDraft.type.label}",
                style = MaterialTheme.typography.labelSmall,
                color = Violet500
            )

            Spacer(Modifier.height(8.dp))

            // --- Key with suggestions (same component you already use) ---
            InputWithSuggestions(
                key = editingField.key,
                onKeyChange = { editingField = editingField.copy(key = it) },
                predefinedKeys = PredefinedKey.entries.map { it.key },
                label = "(required) Enter key",
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
                onUpdateListItem = { index, updatedItem ->
                    listItems = listItems.mapIndexed { currentIndex, currentItem ->
                        if (currentIndex == index) updatedItem else currentItem
                    }
                },
                onRemoveListItem = { index ->
                    listItems = listItems.filterIndexed { currentIndex, _ -> currentIndex != index }
                },
                taskItems = taskItems,
                taskValidation = currentTaskEncoding() as? TaskValueEncodingResult.Invalid,
                onAddTask = {
                    editingField.value.trim().takeIf { it.isNotEmpty() }?.let { title ->
                        taskItems = taskItems + TaskItem(title = title)
                        editingField = editingField.copy(value = "")
                    }
                },
                onUpdateTask = { index, updatedTask ->
                    taskItems = taskItems.mapIndexed { currentIndex, currentTask ->
                        if (currentIndex == index) updatedTask else currentTask
                    }
                },
                onRemoveTask = { index ->
                    taskItems = taskItems.filterIndexed { currentIndex, _ -> currentIndex != index }
                },
                onBrowseFile = { filePickerLauncher.launch(arrayOf("*/*")) }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = editingField.keyAlias,
                onValueChange = { editingField = editingField.copy(keyAlias = it) },
                label = { Text("(Optional) Alias") },
                modifier = Modifier.fillMaxWidth()
            )

            if (valueSpecDraft.type == ValueTypeOption.Sensitive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow MCP access")
                        Text(
                            "Off by default. Local authentication is still required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editingField.isMcpShareable,
                        onCheckedChange = {
                            editingField = editingField.copy(isMcpShareable = it)
                        }
                    )
                }
            }

            if (valueSpecDraft.type != ValueTypeOption.Text) {
                Spacer(Modifier.height(8.dp))
                TypeSpecificOptions(
                    draft = valueSpecDraft,
                    onChange = ::onSpecDraftChange
                )
            }

            Spacer(Modifier.height(20.dp))

            if (canRecoverPreviousValue && onRecoverPreviousValue != null) {
                SharySoftButton(
                    text = "Recover value",
                    onClick = onRecoverPreviousValue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Spacer(Modifier.height(8.dp))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SharySoftButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                )
                SharyPrimaryButton(
                    text = if (isSaving) "Saving..." else confirmLabel,
                    onClick = {
                        showErrors = true
                        if (!isValid()) return@SharyPrimaryButton
                        val persistedPlainValue = currentValueToPersist()
                            ?: return@SharyPrimaryButton
                        val draftToPersist =
                            if (!typedSpecTouched && !valueSpecDraft.typedEnabled) {
                                autoDetectValueSpec(persistedPlainValue) ?: valueSpecDraft
                            } else {
                                valueSpecDraft
                            }
                        val normalizedField = editingField.copy(
                            key = editingField.key.trim(),
                            value = encodeValueFromDraft(
                                plainValue = persistedPlainValue,
                                draft = draftToPersist
                            ),
                            keyAlias = editingField.keyAlias.trim(),
                            tag = editingField.tag,
                            dateAdded = if (editingField.dateAdded == Instant.EPOCH) Instant.now() else editingField.dateAdded,
                            isMcpShareable = valueSpecDraft.type == ValueTypeOption.Sensitive &&
                                editingField.isMcpShareable
                        )
                        onConfirm(normalizedField)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving && (!showErrors || isValid())
                )
            }
        }
    }
}
