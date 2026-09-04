package com.shary.app.viewmodels.user

import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.ContactSortByParameter
import com.shary.app.core.domain.types.enums.Tag
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactListPolicyTest {

    @Test
    fun queryMatchesAliasOrEmailCaseInsensitively() {
        val users = listOf(
            contact("Alice", "alice@example.com"),
            contact("Bob", "bob@example.com"),
            contact("Carol", "carol@corp.io"),
        )

        assertEquals(
            listOf("alice@example.com"),
            filterUsersByQuery(users, "AL").map { it.email }
        )
    }

    @Test
    fun blankQueryReturnsEveryUser() {
        val users = listOf(contact("Alice", "alice@example.com"))

        assertEquals(users, filterUsersByQuery(users, "   "))
    }

    @Test
    fun tagFilterKeepsOnlyMatchingGroup() {
        val friends = contact("Alice", "alice@example.com").copy(tag = Tag.Entry("Friends", Color.Blue))
        val family = contact("Bob", "bob@example.com").copy(tag = Tag.Entry("Family", Color.Red))
        val untagged = contact("Carol", "carol@corp.io")

        val result = filterUsersByTag(listOf(friends, family, untagged), "Friends")

        assertEquals(listOf("Alice"), result.map { it.username })
    }

    @Test
    fun allFilterReturnsEveryUser() {
        val users = listOf(contact("Alice", "alice@example.com"))

        assertEquals(users, filterUsersByTag(users, "All"))
    }

    @Test
    fun sortByNameIsCaseInsensitiveAndAscending() {
        val users = listOf(
            contact("bob", "bob@example.com"),
            contact("Alice", "alice@example.com"),
            contact("carol", "carol@corp.io"),
        )

        assertEquals(
            listOf("Alice", "bob", "carol"),
            sortUsers(users, ContactSortByParameter.NAME, ascending = true).map { it.username }
        )
    }

    @Test
    fun sortByTagGroupsAlphabetically() {
        val family = contact("Bob", "bob@example.com").copy(tag = Tag.Entry("Family", Color.Red))
        val friends = contact("Alice", "alice@example.com").copy(tag = Tag.Entry("Friends", Color.Blue))
        val untagged = contact("Carol", "carol@corp.io")

        assertEquals(
            listOf("family", "friends", "unknown"),
            sortUsers(listOf(friends, family, untagged), ContactSortByParameter.TAG, true)
                .map { it.tag.toTagString().lowercase() }
        )
    }

    @Test
    fun sortByDateAddedCanDescend() {
        val older = contact("Alice", "alice@example.com")
            .copy(dateAdded = Instant.parse("2026-01-01T00:00:00Z"))
        val newer = contact("Bob", "bob@example.com")
            .copy(dateAdded = Instant.parse("2026-09-01T00:00:00Z"))

        assertEquals(
            listOf("Bob", "Alice"),
            sortUsers(listOf(older, newer), ContactSortByParameter.DATE_ADDED, false)
                .map { it.username }
        )
    }

    private fun contact(username: String, email: String) = UserDomain(
        username = username,
        email = email,
        dateAdded = Instant.parse("2026-07-19T10:15:30Z")
    )
}
