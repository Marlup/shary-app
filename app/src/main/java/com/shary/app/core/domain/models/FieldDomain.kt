package com.shary.app.core.domain.models

import com.shary.app.core.domain.types.enums.UiFieldTag
import java.time.Instant

// --------------------
// Field (Domain Model)
// --------------------
data class FieldDomain(
    val key: String,
    val value: String,
    val keyAlias: String? = null,
    val tag: UiFieldTag = UiFieldTag.Unknown,
    val dateAdded: Instant
) {
    val tagColor get() = tag.toColor()

    companion object {
        fun create(
            key: String,
            value: String,
            keyAlias: String? = null,
            tag: UiFieldTag = UiFieldTag.Unknown
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
                tag = UiFieldTag.Unknown,
                dateAdded = Instant.EPOCH // will be replaced on confirm
            )
        }
    }
}
