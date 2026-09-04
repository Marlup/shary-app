package com.shary.app.infrastructure.connector.mcp

import com.shary.app.core.domain.types.valueobjects.ConnectorErrorEnvelope
import com.shary.app.core.domain.types.valueobjects.ConnectorErrorPayload
import com.shary.app.core.domain.types.valueobjects.ConnectorSuccessEnvelope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal object McpJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    @Suppress("UNCHECKED_CAST")
    fun parseObject(raw: String): Map<String, Any?> {
        val parsed = json.parseToJsonElement(raw)
        return parsed.toKotlin() as? Map<String, Any?>
            ?: throw IllegalArgumentException("Expected JSON object")
    }

    fun stringify(value: Any?): String = encode(normalize(value))

    fun normalize(value: Any?): Any? = when (value) {
        null -> null
        is ConnectorSuccessEnvelope<*> -> linkedMapOf(
            "request_id" to value.requestId,
            "schema_version" to value.schemaVersion,
            "timestamp" to value.timestamp,
            "data" to normalize(value.data)
        )
        is ConnectorErrorEnvelope -> linkedMapOf(
            "request_id" to value.requestId,
            "schema_version" to value.schemaVersion,
            "error" to normalize(value.error)
        )
        is ConnectorErrorPayload -> linkedMapOf(
            "code" to value.code,
            "message" to value.message,
            "details" to value.details,
            "retryable" to value.retryable
        )
        is Map<*, *> -> value.entries.associate { (key, item) ->
            key.toString() to normalize(item)
        }
        is Iterable<*> -> value.map { normalize(it) }
        is Array<*> -> value.map { normalize(it) }
        is Enum<*> -> value.name
        is Number, is Boolean, is String -> value
        else -> value.toString()
    }

    @Suppress("UNCHECKED_CAST")
    fun mapAt(value: Any?, fieldName: String): Map<String, Any?> {
        return value as? Map<String, Any?>
            ?: throw IllegalArgumentException("$fieldName must be an object")
    }

    private fun JsonElement.toKotlin(): Any? = when (this) {
        is JsonNull -> null
        is JsonObject -> entries.associate { (key, value) -> key to value.toKotlin() }
        is JsonArray -> map { it.toKotlin() }
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> booleanOrNull
            longOrNull != null -> longOrNull
            doubleOrNull != null -> doubleOrNull
            else -> contentOrNull
        }
    }

    private fun encode(value: Any?): String = when (value) {
        null -> "null"
        is Boolean -> value.toString()
        is Number -> value.toString()
        is String -> quote(value)
        is Map<*, *> -> value.entries.joinToString(
            prefix = "{",
            postfix = "}"
        ) { (key, item) ->
            "${quote(key.toString())}:${encode(item)}"
        }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { encode(it) }
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { encode(it) }
        else -> quote(value.toString())
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
        }
        append('"')
    }
}
