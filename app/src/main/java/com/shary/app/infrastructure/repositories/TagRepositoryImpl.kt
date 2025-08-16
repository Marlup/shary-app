// com/shary/app/repositories/tags/TagRepositoryImpl.kt
package com.shary.app.infrastructure.repositories

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.types.enums.UiFieldTag
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAGS_PREFS_NAME = "tags_prefs"
private val Context.dataStore by preferencesDataStore(TAGS_PREFS_NAME)

class TagRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TagRepository {

    companion object {
        private val CUSTOM_TAGS_KEY: Preferences.Key<Set<String>> =
            stringSetPreferencesKey("custom_tags")

        // Soft constraints (tune as you like)
        private const val MAX_LEN = 32
        private val ALLOWED_REGEX = Regex("""[A-Za-z0-9 _\-\p{L}]+""") // allow letters incl. accents, digits, space, _ and -
    }

    // ---------- Flows ----------

    /** Only custom tags (sorted, unique, case-insensitive) */
    override val customTags: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[CUSTOM_TAGS_KEY].orEmpty()
        raw
            .map(::normalize)
            .filter(::isValidName)
            .distinctBy(String::lowercase)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }

    /** Built‑ins + custom as UiFieldTag */
    override val allTags: Flow<List<UiFieldTag>> = customTags.map { customs ->
        val customObjs = customs.map { UiFieldTag.Custom(it) }
        UiFieldTag.entries + customObjs
    }

    // ---------- Mutations ----------

    override suspend fun addCustomTag(name: String) {
        val normalized = normalize(name)
        if (!isValidName(normalized)) return
        context.dataStore.edit { prefs ->
            val current = prefs[CUSTOM_TAGS_KEY].orEmpty()

            // Prevent duplicates (case-insensitive)
            val exists = current.any { it.equals(normalized, ignoreCase = true) }
            val conflictsBuiltIn = UiFieldTag.entries.any { it.name.equals(normalized, ignoreCase = true) }
            if (!exists && !conflictsBuiltIn) {
                prefs[CUSTOM_TAGS_KEY] = current + normalized
            }
        }
    }

    override suspend fun removeCustomTag(name: String) {
        val target = name.trim()
        if (target.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[CUSTOM_TAGS_KEY].orEmpty()
            // remove case-insensitively
            val updated = current.filterNot { it.equals(target, ignoreCase = true) }.toSet()
            prefs[CUSTOM_TAGS_KEY] = updated
        }
    }

    override suspend fun clearCustomTags() {
        context.dataStore.edit { it[CUSTOM_TAGS_KEY] = emptySet() }
    }

    override suspend fun renameCustomTag(oldName: String, newName: String): Boolean {
        val oldN = normalize(oldName)
        val newN = normalize(newName)
        if (!isValidName(newN)) return false

        // Avoid collision with built-ins or existing customs
        val current = context.dataStore.data.first()[CUSTOM_TAGS_KEY].orEmpty()
        val collidesBuiltIn = UiFieldTag.entries.any { it.name.equals(newN, ignoreCase = true) }
        val collidesCustom = current.any { it.equals(newN, ignoreCase = true) }
        if (collidesBuiltIn || collidesCustom) return false

        context.dataStore.edit { prefs ->
            val set = prefs[CUSTOM_TAGS_KEY].orEmpty()
            if (set.none { it.equals(oldN, ignoreCase = true) }) return@edit
            val withoutOld = set.filterNot { it.equals(oldN, ignoreCase = true) }.toMutableSet()
            withoutOld += newN
            prefs[CUSTOM_TAGS_KEY] = withoutOld
        }
        return true
    }

    // ---------- Helpers ----------

    private fun normalize(input: String): String =
        input.trim()
            .replace(Regex("\\s+"), " ")           // collapse multiple spaces
            .take(MAX_LEN)                          // limit length

    private fun isValidName(name: String): Boolean =
        name.isNotEmpty() && ALLOWED_REGEX.matches(name)
}
