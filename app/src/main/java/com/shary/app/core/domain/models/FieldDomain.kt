package com.shary.app.core.domain.models

import com.shary.app.core.domain.models.FieldDomain.Companion.initialize
import com.shary.app.core.domain.types.enums.SearchFieldBy
import com.shary.app.core.domain.types.enums.Tag
import java.time.Instant

// --------------------
// Field (Domain Model)
// --------------------
data class FieldDomain(
    val key: String,
    val value: String,
    val keyAlias: String = "",
    val tag: Tag = Tag.Unknown,
    val dateAdded: Instant
) {

    companion object {
        fun FieldDomain?.orEmpty(): FieldDomain =
            this ?: initialize()

        fun create(
            key: String,
            value: String,
            keyAlias: String = "",
            tag: Tag = Tag.Unknown
        ): FieldDomain {
            return FieldDomain(
                key = key.trim(),
                value = value.trim(),
                keyAlias = keyAlias.trim(),
                tag = tag,
                dateAdded = Instant.now()
            )
        }

        fun initialize(): FieldDomain = FieldDomain(
            key = "",
            value = "",
            keyAlias = "",
            tag = Tag.Unknown,
            //dateAdded = Instant.EPOCH
            dateAdded = Instant.now()
        )
    }

    fun matchBy(queryCriteria: String, searchBy: SearchFieldBy): Boolean {
        return when (searchBy) {
            SearchFieldBy.KEY -> key.contains(queryCriteria, ignoreCase = true)
            SearchFieldBy.ALIAS -> keyAlias.orEmpty().contains(queryCriteria, ignoreCase = true)
            SearchFieldBy.TAG -> tag.toString().contains(queryCriteria, ignoreCase = true)
            SearchFieldBy.DATE_ADDED -> dateAdded.toString().contains(queryCriteria, ignoreCase = true)
        }
    }
}

fun FieldDomain?.orEmpty(): FieldDomain =
    this ?: initialize()

fun FieldDomain.reset(): FieldDomain = this.copy(
    key = "",
    value = "",
    keyAlias = "",
    tag = Tag.Unknown,
    dateAdded = Instant.EPOCH
)