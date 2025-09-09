package com.shary.app.core.domain.models

import com.shary.app.core.domain.types.enums.FieldAttribute
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.safeColor
import java.time.Instant

// --------------------
// Field (Domain Model)
// --------------------
data class FieldDomain(
    val key: String,
    val value: String,
    val keyAlias: String? = null,
    val tag: Tag = Tag.Unknown,
    val dateAdded: Instant
) {

    companion object {
        fun create(
            key: String,
            value: String,
            keyAlias: String? = null,
            tag: Tag = Tag.Unknown
        ): FieldDomain {
            return FieldDomain(
                key = key.trim(),
                value = value.trim(),
                keyAlias = keyAlias?.trim(),
                tag = tag,
                dateAdded = Instant.now()
            )
        }

        fun initialize(): FieldDomain {
            return FieldDomain(
                key = "",
                value = "",
                keyAlias = null,
                tag = Tag.Unknown,
                dateAdded = Instant.EPOCH // will be replaced on confirm
            )
        }
    }

    fun matchBy(criteria: String, searchBy: FieldAttribute): Boolean {
        return when (searchBy) {
            FieldAttribute.Key -> key.contains(criteria, ignoreCase = true)
            FieldAttribute.Alias -> keyAlias.orEmpty().contains(criteria, ignoreCase = true)
            FieldAttribute.Tag -> tag.toString().orEmpty().contains(criteria, ignoreCase = true)
            FieldAttribute.Date -> dateAdded.toString().contains(criteria, ignoreCase = true)
        }
    }
}
