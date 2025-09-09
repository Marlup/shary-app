// com/shary/app/infrastructure/repositories/TagRepositoryImpl.kt
package com.shary.app.infrastructure.repositories

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.types.enums.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.core.graphics.toColorInt

private const val TAGS_PREFS_NAME = "tags_prefs"
private val Context.dataStore by preferencesDataStore(TAGS_PREFS_NAME)

class TagRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TagRepository {

    companion object {
        private val TAGS_KEY: Preferences.Key<Set<String>> =
            stringSetPreferencesKey("tags") // all tags, built-in + custom



        private fun colorToHex(c: Color): String =
            //String.format("#%06X", 0xFFFFFF and c.toArgb()) // Color(it.toColorInt()) requires
            // an ARGB int, so it interprets the string wrongly if alpha is missing.
            String.format("#%08X", c.toArgb()) // keep alpha too


        private fun parseEntry(entry: String): Tag? {
            val parts = entry.split("|")
            if (parts.size != 2) return null
            val name = parts[0]
            val color = runCatching {
                Color(parts[1].toColorInt())
            }
                .getOrElse { return null }
            return Tag.fromString(name, color)
        }
    }

    // ---------- Flows ----------
    override val allTags: Flow<List<Tag>> =
        context.dataStore.data.map { prefs ->
            prefs[TAGS_KEY].orEmpty()
                .mapNotNull { parseEntry(it) }
                .sortedBy { it.name.lowercase() }
        }

    // ---------- Mutations ----------
    override suspend fun addTag(name: String, color: Color) {
        context.dataStore.edit { prefs ->
            val current = prefs[TAGS_KEY].orEmpty()
            prefs[TAGS_KEY] = current + "${name.trim()}|${colorToHex(c=color)}"
        }
    }

    override suspend fun removeTag(name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[TAGS_KEY].orEmpty()
            prefs[TAGS_KEY] = current.filterNot {
                it.substringBefore("|").equals(name, ignoreCase = true)
            }.toSet()
        }
    }

    override suspend fun updateTag(name: String, newColor: Color) {
        context.dataStore.edit { prefs ->
            val current = prefs[TAGS_KEY].orEmpty()
            val updated = current.map {
                val n = it.substringBefore("|")
                if (n.equals(name, ignoreCase = true)) "$n|${colorToHex(newColor)}" else it
            }.toSet()
            prefs[TAGS_KEY] = updated
        }
    }

    override suspend fun clearTags() {
        context.dataStore.edit { it[TAGS_KEY] = emptySet() }
    }
}
