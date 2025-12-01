package com.shary.app.infrastructure.repositories

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.deserialize
import com.shary.app.core.domain.types.enums.key
import com.shary.app.core.domain.types.enums.serialize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Initialize one instance of DataStore<Preferences> global and unique
private val Context.dataStore by preferencesDataStore(name = "tags_prefs")

class TagRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TagRepository {

    private val dataStore = context.dataStore

    companion object {
        private val CUSTOM_TAGS_KEY: Preferences.Key<Set<String>> =
            stringSetPreferencesKey("custom_tags") // "name|#AARRGGBB" o "name" legacy

        private const val MAX_LEN = 32
        private val ALLOWED_REGEX = Regex("""[A-Za-z0-9 _\-\p{L}]+""")
    }

    override val allTags: Flow<List<Tag>> =
        dataStore.data.map { prefs ->
            val raw = prefs[CUSTOM_TAGS_KEY].orEmpty()
            raw.map {
                if ('|' in it) Tag.deserialize(it)
                else Tag.fromString(it, Tag.Unknown.toColor())
            }
                .distinctBy { it.key }
                .sortedBy { it.name.lowercase() }
        }

    override suspend fun addTag(name: String, color: Color) {
        val normalized = normalize(name)
        if (!isValidName(normalized)) return
        dataStore.edit { prefs ->
            val current = prefs[CUSTOM_TAGS_KEY].orEmpty()
            val entry = Tag.fromString(normalized, color).serialize()
            val withoutSameName = current.filterNot {
                it.substringBefore('|', it).equals(normalized, true)
            }.toMutableSet()
            withoutSameName += entry
            prefs[CUSTOM_TAGS_KEY] = withoutSameName
        }
    }

    override suspend fun updateTag(name: String, color: Color) {
        addTag(name, color) // overwrite if exists
    }

    override suspend fun removeTag(name: String) {
        val target = name.trim()
        if (target.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[CUSTOM_TAGS_KEY].orEmpty()
            val updated = current.filterNot {
                it.substringBefore('|', it).equals(target, ignoreCase = true)
            }.toSet()
            prefs[CUSTOM_TAGS_KEY] = updated
        }
    }

    private fun normalize(input: String): String =
        input.trim().replace(Regex("\\s+"), " ").take(MAX_LEN)

    private fun isValidName(name: String): Boolean =
        name.isNotEmpty() && ALLOWED_REGEX.matches(name)
}
