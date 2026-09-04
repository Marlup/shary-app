package com.shary.app.infrastructure.persistance.repositories

import androidx.datastore.core.DataStore
import androidx.compose.ui.graphics.Color
import com.shary.app.UserList
import com.shary.app.core.domain.interfaces.repositories.ContactUpdateResult
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.infrastructure.mappers.toProto
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UserRepositoryImplTest {

    @Test
    fun emailRenameReplacesTheOriginalContactWithoutCreatingADuplicate() = runBlocking {
        val original = contact("Alice", "alice@example.com")
        val repository = repositoryWith(original)

        val result = repository.updateUser(
            originalEmail = original.email,
            updatedUser = original.copy(
                username = "Alice Primary",
                email = "alice.new@example.com"
            )
        )

        assertEquals(ContactUpdateResult.Updated, result)
        assertEquals(
            listOf(contact("Alice Primary", "alice.new@example.com")),
            repository.getAllUsers()
        )
    }

    @Test
    fun emailCollisionLeavesBothExistingContactsUnchanged() = runBlocking {
        val alice = contact("Alice", "alice@example.com")
        val bob = contact("Bob", "bob@example.com")
        val repository = repositoryWith(alice, bob)

        val result = repository.updateUser(
            originalEmail = alice.email,
            updatedUser = alice.copy(email = " BOB@example.com ")
        )

        assertEquals(ContactUpdateResult.EmailAlreadyExists, result)
        assertEquals(listOf(alice, bob), repository.getAllUsers())
    }

    @Test
    fun aliasOnlyUpdateTargetsTheOriginalEmail() = runBlocking {
        val original = contact("Alice", "alice@example.com")
        val repository = repositoryWith(original)

        val result = repository.updateUser(
            originalEmail = " ALICE@example.com ",
            updatedUser = original.copy(username = "Local Alice")
        )

        assertEquals(ContactUpdateResult.Updated, result)
        assertEquals(
            listOf(original.copy(username = "Local Alice")),
            repository.getAllUsers()
        )
    }

    @Test
    fun missingOriginalReturnsNotFoundWithoutCreatingAContact() = runBlocking {
        val existing = contact("Alice", "alice@example.com")
        val repository = repositoryWith(existing)

        val result = repository.updateUser(
            originalEmail = "missing@example.com",
            updatedUser = contact("Missing", "new@example.com")
        )

        assertEquals(ContactUpdateResult.NotFound, result)
        assertEquals(listOf(existing), repository.getAllUsers())
    }

    @Test
    fun tagRoundTripsThroughTheProtoStore() = runBlocking {
        val tagged = contact("Alice", "alice@example.com")
            .copy(tag = Tag.Entry("Friends", Color.Blue))
        val repository = repositoryWith(tagged)

        assertEquals("Friends", repository.getAllUsers().single().tag.toTagString())
    }

    @Test
    fun updateTagPersistsTheNewGroup() = runBlocking {
        val original = contact("Alice", "alice@example.com")
        val repository = repositoryWith(original)

        repository.updateTag(original.email, Tag.Entry("Family", Color.Red))

        assertEquals("Family", repository.getAllUsers().single().tag.toTagString())
    }

    private fun repositoryWith(vararg contacts: UserDomain): UserRepositoryImpl {
        val initial = UserList.newBuilder()
            .addAllUsers(contacts.map { it.toProto() })
            .build()
        return UserRepositoryImpl(InMemoryUserStore(initial))
    }

    private fun contact(username: String, email: String) = UserDomain(
        username = username,
        email = email,
        dateAdded = Instant.parse("2026-07-19T10:15:30Z")
    )

    private class InMemoryUserStore(initial: UserList) : DataStore<UserList> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<UserList> = state

        override suspend fun updateData(transform: suspend (t: UserList) -> UserList): UserList {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
