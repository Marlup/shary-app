package com.shary.app.ui.screens.field

import com.shary.app.core.domain.models.FieldDomain

data class FieldTagCount(
    val tagName: String,
    val count: Int
)

internal fun buildFieldTagCounts(fields: List<FieldDomain>): List<FieldTagCount> =
    fields
        .groupingBy { field -> field.tag.toTagString().trim().ifBlank { "Unknown" } }
        .eachCount()
        .map { (tagName, count) -> FieldTagCount(tagName = tagName, count = count) }
        .sortedWith(compareByDescending<FieldTagCount> { it.count }.thenBy { it.tagName.lowercase() })
