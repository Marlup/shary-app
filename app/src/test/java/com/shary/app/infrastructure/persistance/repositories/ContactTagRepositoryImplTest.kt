package com.shary.app.infrastructure.persistance.repositories

import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.shary.app.core.domain.interfaces.repositories.TagCreateResult
import com.shary.app.core.domain.interfaces.repositories.TagUpdateResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactTagRepositoryImplTest {

    @Test
    fun seededDefaultsAreAvailableWithoutAnyWrite() = runBlocking {
        val repository = repository()

        assertEquals(
            listOf("Colleagues", "Family", "Friends", "Vendor"),
            repository.allTags.first().map { it.name }
        )
    }

    @Test
    fun customTagAddsAlongsideSeededDefaults() = runBlocking {
        val repository = repository()

        val result = repository.addTag("Neighbors", Color.Cyan)

        assertTrue(result is TagCreateResult.Created)
        val names = repository.allTags.first().map { it.name }
        assertTrue(names.containsAll(listOf("Colleagues", "Family", "Friends", "Vendor", "Neighbors")))
    }

    @Test
    fun removeSeededTagPersistsExclusion() = runBlocking {
        val repository = repository()

        repository.removeTag("Friends")

        assertEquals(
            listOf("Colleagues", "Family", "Vendor"),
            repository.allTags.first().map { it.name }
        )
    }

    @Test
    fun updateSeededTagRenamesAndRecolors() = runBlocking {
        val repository = repository()

        val result = repository.updateTag("Friends", "Close Friends", Color.Green)

        assertTrue(result is TagUpdateResult.Updated)
        val names = repository.allTags.first().map { it.name }
        assertTrue("Close Friends" in names)
        assertTrue("Friends" !in names)
    }

    private fun repository() = ContactTagRepositoryImpl(InMemoryPreferencesStore())

    private class InMemoryPreferencesStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
