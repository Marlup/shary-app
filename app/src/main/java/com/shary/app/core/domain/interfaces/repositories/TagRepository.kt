package com.shary.app.core.domain.interfaces.repositories

import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.TagNameInvalidReason
import kotlinx.coroutines.flow.Flow

sealed interface TagCreateResult {
    data class Created(val tag: Tag) : TagCreateResult
    data class AlreadyExists(val existing: Tag) : TagCreateResult
    data class Invalid(val reason: TagNameInvalidReason) : TagCreateResult
}

sealed interface TagUpdateResult {
    data class Updated(val tag: Tag) : TagUpdateResult
    data class Unchanged(val tag: Tag) : TagUpdateResult
    data class NameAlreadyExists(val existing: Tag) : TagUpdateResult
    data object NotFound : TagUpdateResult
    data class Invalid(val reason: TagNameInvalidReason) : TagUpdateResult
}

interface TagRepository {
    /** Built-ins + custom tags */
    val allTags: Flow<List<Tag>>

    /** Add a custom tag without replacing an existing case-insensitive name. */
    suspend fun addTag(name: String, color: Color): TagCreateResult

    /** Atomically rename/recolor the custom tag identified by its original name. */
    suspend fun updateTag(originalName: String, name: String, color: Color): TagUpdateResult

    /** Remove a custom tag name (case-insensitive match) */
    suspend fun removeTag(name: String)
}
