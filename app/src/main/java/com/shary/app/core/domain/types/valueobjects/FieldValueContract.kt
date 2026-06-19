package com.shary.app.core.domain.types.valueobjects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
            "json", "yaml", "yml", "list", "questionnaire", "link_url", "link", "url" -> {
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
}
