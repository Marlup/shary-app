package com.shary.app.infrastructure.persistance.repositories

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shary.app.core.domain.interfaces.repositories.ContactTagRepository
import com.shary.app.core.domain.interfaces.repositories.TagCreateResult
import com.shary.app.core.domain.interfaces.repositories.TagUpdateResult
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.deserialize
import com.shary.app.core.domain.types.enums.key
import com.shary.app.core.domain.types.enums.serialize
import com.shary.app.core.domain.types.valueobjects.TagNameContract
import com.shary.app.core.domain.types.valueobjects.TagNameValidation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.contactTagsDataStore by preferencesDataStore(name = "contact_tags_prefs")

@Singleton
class ContactTagRepositoryImpl internal constructor(
    private val dataStore: DataStore<Preferences>
) : ContactTagRepository {

    @Inject
    constructor(context: Context) : this(context.contactTagsDataStore)

    companion object {
        private val CONTACT_TAGS_KEY: Preferences.Key<Set<String>> =
            stringSetPreferencesKey("contact_tags")

        /** Seeded grouping tags shown until the user edits the set. */
        val DefaultTags: List<Tag> = listOf(
            Tag.Entry("Friends", Color(0xFF4C8BF5)),
            Tag.Entry("Colleagues", Color(0xFF34A853)),
            Tag.Entry("Family", Color(0xFFF2994A)),
            Tag.Entry("Vendor", Color(0xFF9B51E0)),
        )

        private fun seeded(): Set<String> = DefaultTags.map { it.serialize() }.toSet()
    }

    override val allTags: Flow<List<Tag>> =
        dataStore.data.map { prefs ->
            effectiveStored(prefs)
                .map { stored ->
                    if ('|' in stored) Tag.deserialize(stored)
                    else Tag.fromString(stored, Tag.Unknown.toColor())
                }
                .distinctBy { it.key }
                .sortedBy { it.name.lowercase() }
        }

    override suspend fun addTag(name: String, color: Color): TagCreateResult {
        val validation = TagNameContract.validate(name)
        if (validation is TagNameValidation.Invalid) {
            return TagCreateResult.Invalid(validation.reason)
        }
        val normalized = (validation as TagNameValidation.Valid).normalizedName
        var result: TagCreateResult? = null
        dataStore.edit { prefs ->
            val current = effectiveStored(prefs)
            val existing = current.firstOrNull { storedName(it).equals(normalized, true) }
            if (existing != null) {
                result = TagCreateResult.AlreadyExists(deserializeStoredTag(existing))
            } else {
                val tag = Tag.fromString(normalized, color)
                prefs[CONTACT_TAGS_KEY] = current + tag.serialize()
                result = TagCreateResult.Created(tag)
            }
        }
        return checkNotNull(result)
    }

    override suspend fun updateTag(
        originalName: String,
        name: String,
        color: Color
    ): TagUpdateResult {
        val validation = TagNameContract.validate(name)
        if (validation is TagNameValidation.Invalid) {
            return TagUpdateResult.Invalid(validation.reason)
        }
        val normalizedOriginal = originalName.trim().replace(Regex("\\s+"), " ")
        val normalizedName = (validation as TagNameValidation.Valid).normalizedName
        var result: TagUpdateResult? = null
        dataStore.edit { prefs ->
            val current = effectiveStored(prefs)
            val original = current.firstOrNull {
                storedName(it).equals(normalizedOriginal, ignoreCase = true)
            }
            if (original == null) {
                result = TagUpdateResult.NotFound
                return@edit
            }
            val collision = current.firstOrNull { stored ->
                stored != original && storedName(stored).equals(normalizedName, ignoreCase = true)
            }
            if (collision != null) {
                result = TagUpdateResult.NameAlreadyExists(deserializeStoredTag(collision))
                return@edit
            }

            val updatedTag = Tag.fromString(normalizedName, color)
            val serialized = updatedTag.serialize()
            if (serialized == original) {
                result = TagUpdateResult.Unchanged(updatedTag)
            } else {
                prefs[CONTACT_TAGS_KEY] = current - original + serialized
                result = TagUpdateResult.Updated(updatedTag)
            }
        }
        return checkNotNull(result)
    }

    override suspend fun removeTag(name: String) {
        val target = name.trim()
        if (target.isEmpty()) return
        dataStore.edit { prefs ->
            val current = effectiveStored(prefs)
            val updated = current.filterNot {
                it.substringBefore('|', it).equals(target, ignoreCase = true)
            }.toSet()
            prefs[CONTACT_TAGS_KEY] = updated
        }
    }

    private fun effectiveStored(prefs: Preferences): Set<String> =
        prefs[CONTACT_TAGS_KEY] ?: seeded()

    private fun storedName(serialized: String): String = serialized.substringBefore('|', serialized)

    private fun deserializeStoredTag(serialized: String): Tag =
        if ('|' in serialized) Tag.deserialize(serialized)
        else Tag.fromString(serialized, Tag.Unknown.toColor())
}
