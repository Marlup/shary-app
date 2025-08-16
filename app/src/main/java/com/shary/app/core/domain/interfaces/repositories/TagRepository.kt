// com/shary/app/repositories/tags/TagRepository.kt
package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.types.enums.UiFieldTag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    /** Built‑ins + custom, as UiFieldTag */
    val allTags: Flow<List<UiFieldTag>>

    /** Only custom tag names (strings) */
    val customTags: Flow<List<String>>

    /** Add a custom tag name (no‑op if invalid or already exists, case‑insensitive) */
    suspend fun addCustomTag(name: String)

    /** Remove a custom tag name (case‑insensitive match) */
    suspend fun removeCustomTag(name: String)

    /** Optional helpers */
    suspend fun clearCustomTags()
    suspend fun renameCustomTag(oldName: String, newName: String): Boolean
}
