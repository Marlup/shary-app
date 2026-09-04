package com.shary.app.infrastructure.persistance.repositories

import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.shary.app.core.domain.interfaces.repositories.TagCreateResult
import com.shary.app.core.domain.interfaces.repositories.TagUpdateResult
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.core.domain.types.valueobjects.TagNameInvalidReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagRepositoryImplTest {

    @Test
    fun createNormalizesTheStoredName() = runBlocking {
        val repository = repository()

        val result = repository.addTag("  Project   Alpha ", Color.Blue)

        assertTrue(result is TagCreateResult.Created)
        assertEquals("Project Alpha", repository.allTags.first().single().name)
    }

    @Test
    fun duplicateCreateDoesNotOverwriteExistingColor() = runBlocking {
        val repository = repository()
        repository.addTag("Work", Color.Blue)

        val result = repository.addTag(" work ", Color.Red)

        assertTrue(result is TagCreateResult.AlreadyExists)
        assertEquals(Color.Blue, repository.allTags.first().single().safeColor())
    }

    @Test
    fun renameAtomicallyReplacesTheOriginalTag() = runBlocking {
        val repository = repository()
        repository.addTag("Work", Color.Blue)

        val result = repository.updateTag("WORK", "Personal", Color.Green)

        assertTrue(result is TagUpdateResult.Updated)
        val stored = repository.allTags.first().single()
        assertEquals("Personal", stored.name)
        assertEquals(Color.Green, stored.safeColor())
    }

    @Test
    fun renameCollisionLeavesBothTagsUnchanged() = runBlocking {
        val repository = repository()
        repository.addTag("Work", Color.Blue)
        repository.addTag("Personal", Color.Red)

        val result = repository.updateTag("Work", "personal", Color.Green)

        assertTrue(result is TagUpdateResult.NameAlreadyExists)
        assertEquals(
            listOf("Personal" to Color.Red, "Work" to Color.Blue),
            repository.allTags.first().map { it.name to it.safeColor() }
        )
    }

    @Test
    fun invalidCreateReturnsTheSharedValidationReasonWithoutWriting() = runBlocking {
        val repository = repository()

        val result = repository.addTag("work/private", Color.Blue)

        assertEquals(
            TagCreateResult.Invalid(TagNameInvalidReason.UNSUPPORTED_CHARACTERS),
            result
        )
        assertEquals(emptyList<Any>(), repository.allTags.first())
    }

    private fun repository() = TagRepositoryImpl(InMemoryPreferencesStore())

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
