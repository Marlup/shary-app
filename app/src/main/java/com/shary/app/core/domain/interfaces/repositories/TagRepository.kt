package com.shary.app.core.domain.interfaces.repositories

import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.types.enums.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    /** Built-ins + custom tags */
    val allTags: Flow<List<Tag>>

    /** Add a custom tag name (no-op if invalid or already exists, case-insensitive) */
    suspend fun addTag(name: String, color: Color)

    /** Update a custom tag name (case-insensitive match) */
    suspend fun updateTag(name: String, color: Color)

    /** Remove a custom tag name (case-insensitive match) */
    suspend fun removeTag(name: String)
}
