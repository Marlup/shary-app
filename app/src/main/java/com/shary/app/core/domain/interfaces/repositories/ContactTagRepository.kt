package com.shary.app.core.domain.interfaces.repositories

import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.types.enums.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Contact grouping tags. Kept separate from [TagRepository] so the Field tag
 * vocabulary does not mix with the Contact grouping vocabulary.
 */
interface ContactTagRepository {
    /** Seeded default group tags plus any custom tags, sorted by name. */
    val allTags: Flow<List<Tag>>

    /** Add a custom tag without replacing an existing case-insensitive name. */
    suspend fun addTag(name: String, color: Color): TagCreateResult

    /** Atomically rename/recolor the tag identified by its original name. */
    suspend fun updateTag(originalName: String, name: String, color: Color): TagUpdateResult

    /** Remove a tag name (case-insensitive match). */
    suspend fun removeTag(name: String)
}
