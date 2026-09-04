package com.shary.app.application.cloud

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

data class IncomingCloudField(
    val key: String,
    val value: String,
    val alias: String
) {
    fun toFieldDomain(now: Instant): FieldDomain = FieldDomain(
        key = key,
        value = value,
        keyAlias = alias,
        tag = Tag.Unknown,
        dateAdded = now
    )
}

data class CloudImportDiff(
    val addedKeys: List<String> = emptyList(),
    val duplicateKeys: List<String> = emptyList(),
    val changedKeys: List<String> = emptyList(),
    val unchangedKeys: List<String> = emptyList(),
    val ignoredKeys: List<String> = emptyList()
) {
    val requiresDetailedReview: Boolean
        get() = duplicateKeys.isNotEmpty() || changedKeys.isNotEmpty() || ignoredKeys.isNotEmpty()

    val hasIncomingFields: Boolean
        get() = addedKeys.isNotEmpty() ||
            duplicateKeys.isNotEmpty() ||
            changedKeys.isNotEmpty() ||
            unchangedKeys.isNotEmpty()
}

data class CloudFieldImportPlan(
    val incomingFields: List<IncomingCloudField>,
    val fieldsToCreate: List<IncomingCloudField>,
    val diff: CloudImportDiff
)

object CloudFieldImportPlanner {
    private val json = Json { ignoreUnknownKeys = true }

    fun plan(
        jsonStrings: List<String>,
        existingFields: List<FieldDomain>
    ): CloudFieldImportPlan {
        val existingByKey = existingFields.associateBy { it.key.normalizedKey() }
        val seenIncomingKeys = mutableSetOf<String>()
        val incomingFields = mutableListOf<IncomingCloudField>()
        val fieldsToCreate = mutableListOf<IncomingCloudField>()
        val addedKeys = mutableListOf<String>()
        val duplicateKeys = mutableListOf<String>()
        val changedKeys = mutableListOf<String>()
        val unchangedKeys = mutableListOf<String>()
        val ignoredKeys = mutableListOf<String>()

        jsonStrings.forEach { jsonString ->
            parseIncomingFields(jsonString).forEach { incoming ->
                val normalized = incoming.key.normalizedKey()
                if (normalized.isBlank()) {
                    ignoredKeys += incoming.key
                    return@forEach
                }
                if (!seenIncomingKeys.add(normalized)) {
                    duplicateKeys += incoming.key
                    return@forEach
                }

                incomingFields += incoming
                val existing = existingByKey[normalized]
                if (existing == null) {
                    fieldsToCreate += incoming
                    addedKeys += incoming.key
                } else {
                    duplicateKeys += incoming.key
                    if (
                        existing.value.trim() != incoming.value.trim() ||
                        existing.keyAlias.trim() != incoming.alias.trim()
                    ) {
                        changedKeys += incoming.key
                    } else {
                        unchangedKeys += incoming.key
                    }
                }
            }
        }

        return CloudFieldImportPlan(
            incomingFields = incomingFields,
            fieldsToCreate = fieldsToCreate,
            diff = CloudImportDiff(
                addedKeys = addedKeys.distinctNormalized(),
                duplicateKeys = duplicateKeys.distinctNormalized(),
                changedKeys = changedKeys.distinctNormalized(),
                unchangedKeys = unchangedKeys.distinctNormalized(),
                ignoredKeys = ignoredKeys.distinctNormalized()
            )
        )
    }

    fun previewKeys(jsonString: String): List<String> =
        parseIncomingFields(jsonString)
            .map { it.key }
            .filter { it.isNotBlank() }
            .distinctBy { it.normalizedKey() }
            .sortedBy { it.normalizedKey() }
            .take(12)

    private fun parseIncomingFields(jsonString: String): List<IncomingCloudField> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        return jsonObject.entries.map { (rawKey, rawValue) ->
            val key = rawKey.trim()
            val (value, alias) = when (rawValue) {
                is JsonObject -> {
                    val nestedValue = rawValue.stringField("value")
                    val nestedAlias = rawValue.stringField("alias")
                    nestedValue to nestedAlias
                }
                is JsonPrimitive -> rawValue.contentOrNull.orEmpty() to ""
                else -> rawValue.toString() to ""
            }
            IncomingCloudField(
                key = key,
                value = value,
                alias = alias
            )
        }
    }

    private fun String.normalizedKey(): String = trim().lowercase()

    private fun List<String>.distinctNormalized(): List<String> =
        distinctBy { it.normalizedKey() }

    private fun JsonObject.stringField(name: String): String =
        this[name]?.jsonPrimitive?.contentOrNull.orEmpty()
}
