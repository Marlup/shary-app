package com.shary.app.core.domain.models

import java.time.Instant


// --------------------
// Request (Domain Model)
// --------------------
data class RequestDomain(
    val id: String,
    val fields: List<FieldDomain> = emptyList(),
    val dateAdded: Instant
) {
    val fieldCount get() = fields.size

    fun addField(field: FieldDomain): RequestDomain =
        copy(fields = fields + field)

    fun removeField(key: String): RequestDomain =
        copy(fields = fields.filterNot { it.key.equals(key, ignoreCase = true) })
}
