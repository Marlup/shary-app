package com.shary.app.core.domain.models

import com.shary.app.core.domain.types.enums.FieldAttribute
import com.shary.app.core.domain.types.enums.SearchFieldBy
import com.shary.app.core.domain.types.enums.Tag
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

    fun matchBy(queryCriteria: String, searchBy: SearchFieldBy): Boolean {
        return when (searchBy) {
            SearchFieldBy.KEY -> key.contains(queryCriteria, ignoreCase = true)
            SearchFieldBy.ALIAS -> keyAlias.orEmpty().contains(queryCriteria, ignoreCase = true)
            SearchFieldBy.TAG -> tag.toString().orEmpty().contains(queryCriteria, ignoreCase = true)
            SearchFieldBy.DATE_ADDED -> dateAdded.toString().contains(queryCriteria, ignoreCase = true)
        }
    }
}
