package com.shary.app.core.domain.types.valueobjects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

const val SENSITIVE_MASK = "*****"

@Serializable
data class FieldValueEnvelope(
    val v: Int = 1,
    val data: String,
    val spec: FieldValueSpec? = null,
    val meta: FieldValueMeta? = null
)

@Serializable
data class FieldValueSpec(
    val kind: String,
    val options: Map<String, String> = emptyMap()
)

@Serializable
data class FieldValueMeta(
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val sha256: String? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null
)

enum class FieldValueMode {
    PLAIN,
    TYPED
}

data class ParsedFieldValue(
    val mode: FieldValueMode,
    val plainData: String,
    val envelope: FieldValueEnvelope? = null
)

data class FieldValueValidationResult(
    val isValid: Boolean,
    val hint: String? = null
)

data class FileTypeCheck(
    val allowed: Boolean,
    val reason: String? = null
)

@Serializable
enum class TaskStatus(val wireName: String) {
    @SerialName("todo")
    TODO("todo"),

    @SerialName("doing")
    DOING("doing"),

    @SerialName("done")
    DONE("done"),

    @SerialName("review")
    REVIEW("review"),

    @SerialName("cancel")
    CANCEL("cancel")
}

@Serializable
data class TaskItem(
    val title: String,
    val status: TaskStatus = TaskStatus.TODO
)

enum class TaskValueValidationIssue(val message: String) {
    EMPTY("Add at least one task."),
    BLANK_TITLE("Task titles cannot be blank."),
    ENCODING_FAILED("Tasks could not be encoded.")
}

sealed interface TaskValueEncodingResult {
    data class Success(val encodedValue: String) : TaskValueEncodingResult

    data class Invalid(
        val issue: TaskValueValidationIssue,
        val invalidIndices: List<Int> = emptyList()
    ) : TaskValueEncodingResult
}

object TaskValueContract {
    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }
    private val serializer = ListSerializer(TaskItem.serializer())

    fun tryEncode(items: List<TaskItem>): TaskValueEncodingResult {
        if (items.isEmpty()) {
            return TaskValueEncodingResult.Invalid(TaskValueValidationIssue.EMPTY)
        }

        val normalized = items.map { item ->
            item.copy(title = item.title.trim())
        }
        val invalidIndices = normalized.mapIndexedNotNull { index, item ->
            index.takeIf { item.title.isBlank() }
        }
        if (invalidIndices.isNotEmpty()) {
            return TaskValueEncodingResult.Invalid(
                issue = TaskValueValidationIssue.BLANK_TITLE,
                invalidIndices = invalidIndices
            )
        }

        return runCatching { json.encodeToString(serializer, normalized) }
            .fold(
                onSuccess = TaskValueEncodingResult::Success,
                onFailure = {
                    TaskValueEncodingResult.Invalid(TaskValueValidationIssue.ENCODING_FAILED)
                }
            )
    }

    fun encode(items: List<TaskItem>): String = when (val result = tryEncode(items)) {
        is TaskValueEncodingResult.Success -> result.encodedValue
        is TaskValueEncodingResult.Invalid -> throw IllegalArgumentException(result.issue.message)
    }

    fun decode(data: String): Result<List<TaskItem>> = runCatching {
        json.decodeFromString(serializer, data).also { items ->
            require(items.isNotEmpty()) { "Add at least one task." }
            require(items.all { it.title.trim().isNotBlank() }) { "Task titles cannot be blank." }
        }
    }

    fun normalize(data: String): String = encode(decode(data).getOrThrow())
}

enum class FieldValueType(
    val wireName: String,
    val description: String
) {
    PLAIN("plain", "Unstructured text."),
    EMAIL("email", "Email address."),
    LIST("list", "Ordered text values."),
    LINK_URL("link_url", "Web, FTP, or mailto link."),
    JSON("json", "JSON object or array."),
    QUESTIONNAIRE("questionnaire", "Questionnaire value and answer configuration."),
    YAML("yaml", "YAML document."),
    FILE("file", "Allowed file name or content URI reference."),
    TASKS("tasks", "Task titles with todo, doing, done, review, or cancel status."),
    SENSITIVE("sensitive", "Locally protected value requiring device authentication.");

    companion object {
        fun fromWireName(value: String?): FieldValueType? = when (value.orEmpty().trim().lowercase()) {
            "link", "url" -> LINK_URL
            "yml" -> YAML
            else -> entries.firstOrNull { it.wireName == value.orEmpty().trim().lowercase() }
        }
    }
}

data class FieldValueTypeInfo(
    val name: String,
    val description: String
)

object FieldValueTypes {
    val available: List<FieldValueTypeInfo> = FieldValueType.entries.map {
        FieldValueTypeInfo(name = it.wireName, description = it.description)
    }

    fun encodeForStorage(value: String, requestedType: String?): Result<String> = runCatching {
        val type = FieldValueType.fromWireName(requestedType)
            ?: throw IllegalArgumentException("Unsupported value_type='${requestedType.orEmpty()}'.")
        when (type) {
            FieldValueType.PLAIN -> value
            FieldValueType.TASKS -> FieldValueContract.encode(
                data = TaskValueContract.normalize(value),
                spec = FieldValueSpec(kind = type.wireName)
            )
            else -> FieldValueContract.encode(
                data = value,
                spec = FieldValueSpec(kind = type.wireName)
            ).also { encoded ->
                val validation = FieldValueContract.validate(encoded, strictModeEnabled = true)
                require(validation.isValid) { validation.hint ?: "Invalid ${type.wireName} value." }
            }
        }
    }

    fun wireTypeOf(storedValue: String): String {
        val parsed = FieldValueContract.parse(storedValue)
        return parsed.envelope?.spec?.kind
            ?.let(FieldValueType::fromWireName)
            ?.wireName
            ?: FieldValueType.PLAIN.wireName
    }

    fun isSensitive(storedValue: String): Boolean =
        wireTypeOf(storedValue) == FieldValueType.SENSITIVE.wireName

    fun displayData(storedValue: String, revealSensitive: Boolean = false): String {
        if (isSensitive(storedValue) && !revealSensitive) return SENSITIVE_MASK
        return FieldValueContract.parse(storedValue).plainData
    }
}

object FieldFilePolicy {
    private val allowedExtensions = setOf(
        "txt", "csv", "xlsx", "pdf", "yml", "yaml", "json", "docx", "md", "ppt", "pptx"
    )

    private val deniedExtensions = setOf(
        "exe", "dll", "so", "bin", "apk", "jar", "class", "bat", "cmd", "com",
        "msi", "vbs", "ps1", "sh", "scr", "sys", "dmg", "pkg"
    )

    fun inspect(fileName: String): FileTypeCheck {
        val ext = fileName
            .substringAfterLast('.', "")
            .trim()
            .lowercase()

        if (ext.isBlank()) {
            return FileTypeCheck(
                allowed = false,
                reason = "File name must include an extension."
            )
        }
        if (ext in deniedExtensions) {
            return FileTypeCheck(
                allowed = false,
                reason = "File type '.$ext' is blocked for security."
            )
        }
        if (ext !in allowedExtensions) {
            return FileTypeCheck(
                allowed = false,
                reason = "File type '.$ext' is not in the allowed file list."
            )
        }
        return FileTypeCheck(allowed = true)
    }
}

object FieldValueContract {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(
        data: String,
        spec: FieldValueSpec? = null,
        meta: FieldValueMeta? = null
    ): String {
        if (spec == null && meta == null) return data
        return json.encodeToString(
            FieldValueEnvelope.serializer(),
            FieldValueEnvelope(data = data, spec = spec, meta = meta)
        )
    }

    fun parse(value: String): ParsedFieldValue {
        val trimmed = value.trim()
        if (trimmed.isBlank() || !(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return ParsedFieldValue(
                mode = FieldValueMode.PLAIN,
                plainData = value
            )
        }

        val envelope = runCatching {
            json.decodeFromString(FieldValueEnvelope.serializer(), trimmed)
        }.getOrNull()

        return if (envelope == null) {
            ParsedFieldValue(
                mode = FieldValueMode.PLAIN,
                plainData = value
            )
        } else {
            ParsedFieldValue(
                mode = FieldValueMode.TYPED,
                plainData = envelope.data,
                envelope = envelope
            )
        }
    }

    fun validate(value: String, strictModeEnabled: Boolean): FieldValueValidationResult {
        if (!strictModeEnabled) return FieldValueValidationResult(isValid = true)

        val parsed = parse(value)
        if (parsed.mode == FieldValueMode.PLAIN) {
            return FieldValueValidationResult(isValid = true)
        }

        val spec = parsed.envelope?.spec ?: return FieldValueValidationResult(isValid = true)
        return when (spec.kind.trim().lowercase()) {
            "file" -> validateFileTypedValue(parsed.plainData, parsed.envelope.meta)
            "email" -> validateEmailTypedValue(parsed.plainData)
            "tasks" -> validateTasksTypedValue(parsed.plainData)
            "json", "yaml", "yml", "list", "questionnaire", "link_url", "link", "url", "sensitive" -> {
                FieldValueValidationResult(isValid = true)
            }
            else -> FieldValueValidationResult(
                isValid = false,
                hint = "Unsupported spec.kind='${spec.kind}'."
            )
        }
    }

    private fun validateFileTypedValue(
        data: String,
        meta: FieldValueMeta?
    ): FieldValueValidationResult {
        val fileName = meta?.fileName?.trim().orEmpty().ifBlank { data.trim() }
        if (fileName.isBlank()) {
            return FieldValueValidationResult(
                isValid = false,
                hint = "For spec.kind=file, value.data must contain a filename or reference."
            )
        }

        val check = FieldFilePolicy.inspect(fileName)
        if (!check.allowed) {
            return FieldValueValidationResult(
                isValid = false,
                hint = check.reason
            )
        }
        return FieldValueValidationResult(isValid = true)
    }

    private fun validateEmailTypedValue(data: String): FieldValueValidationResult {
        val email = data.trim()
        val valid = email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
        return if (valid) {
            FieldValueValidationResult(isValid = true)
        } else {
            FieldValueValidationResult(
                isValid = false,
                hint = "Enter a valid email address."
            )
        }
    }

    private fun validateTasksTypedValue(data: String): FieldValueValidationResult =
        TaskValueContract.decode(data).fold(
            onSuccess = { FieldValueValidationResult(isValid = true) },
            onFailure = {
                FieldValueValidationResult(
                    isValid = false,
                    hint = it.message ?: "Enter a valid task list."
                )
            }
        )
}
