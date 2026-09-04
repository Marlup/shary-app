package com.shary.app.infrastructure.services.cache

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.OutgoingRequestDraft
import com.shary.app.core.domain.models.RequestedFieldDraft
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.ResponseDraft
import com.shary.app.core.domain.models.UserDomain
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionCacheServiceTest {

    @Test
    fun clearAllCachesResetsTheCompleteAccountScopedInventory() {
        val cache = SelectionCacheService()
        val field = FieldDomain.create(key = "secret", value = "account-a")
        val request = RequestDomain(
            fields = listOf(field),
            user = "alice@example.com",
            recipients = listOf("owner@example.com"),
            dateAdded = Instant.EPOCH,
            owned = false,
            responded = false
        )
        val contact = UserDomain(
            username = "Alice",
            email = "alice@example.com",
            dateAdded = Instant.EPOCH
        )
        val owner = UserDomain(
            username = "Owner A",
            email = "owner-a@example.com",
            dateAdded = Instant.EPOCH
        )

        cache.cacheFields(listOf(field))
        cache.cacheRequests(listOf(request))
        cache.cacheDraftFields(listOf(field))
        cache.cacheDraftRequest(request)
        cache.cacheUsers(listOf(contact))
        cache.cacheOwner(owner)
        cache.cachePhoneNumber("+34123456789")
        cache.cacheRequestWorkflow(
            RequestWorkflowState(
                outgoingRequestDraft = OutgoingRequestDraft(listOf(RequestedFieldDraft(field.key)))
            )
        )

        cache.clearAllCaches()

        assertTrue(cache.getFields().isEmpty())
        assertTrue(cache.getRequests().isEmpty())
        assertTrue(cache.getDraftFields().isEmpty())
        assertTrue(cache.getDraftRequest().fields.isEmpty())
        assertTrue(cache.getDraftRequest().user.isBlank())
        assertTrue(cache.getUsers().isEmpty())
        assertTrue(cache.getOwner().username.isBlank())
        assertTrue(cache.getOwner().email.isBlank())
        assertEquals(null, cache.getOwnerEmail())
        assertEquals(null, cache.getOwnerUsername())
        assertEquals(null, cache.getPhoneNumber())
        assertEquals(RequestWorkflowState(), cache.getRequestWorkflow())
        assertFalse(cache.isAnyFieldCached())
        assertFalse(cache.isAnyRequestCached())
        assertFalse(cache.isAnyDraftFieldCached())
        assertFalse(cache.isAnyUserCached())
    }

    @Test
    fun ownerUpdatesReplaceStateWithoutMutatingTheSuppliedObject() {
        val cache = SelectionCacheService()
        val suppliedOwner = UserDomain(
            username = "Original",
            email = "original@example.com",
            dateAdded = Instant.EPOCH
        )

        cache.cacheOwner(suppliedOwner)
        cache.cacheOwnerUsername("Updated")
        cache.cacheOwnerEmail("updated@example.com")

        assertEquals("Original", suppliedOwner.username)
        assertEquals("original@example.com", suppliedOwner.email)
        assertEquals("Updated", cache.getOwnerUsername())
        assertEquals("updated@example.com", cache.getOwnerEmail())
    }

    @Test
    fun cachedStateDoesNotRetainCallerOwnedMutableCollections() {
        val cache = SelectionCacheService()
        val fields = mutableListOf(FieldDomain.create(key = "first", value = "value"))
        val matchedKeys = mutableListOf("first")

        cache.cacheFields(fields)
        cache.cacheRequestWorkflow(
            RequestWorkflowState(
                responseDraft = ResponseDraft(
                    requestId = "request-1",
                    requesterId = "sender-hash",
                    requesterAddress = "alice@example.com",
                    matchedFieldKeys = matchedKeys
                )
            )
        )
        fields += FieldDomain.create(key = "second", value = "value")
        matchedKeys += "second"

        assertEquals(listOf("first"), cache.getFields().map { it.key })
        assertEquals(listOf("first"), cache.getRequestWorkflow().responseDraft?.matchedFieldKeys)
        assertFalse(cache.accountState is MutableStateFlow<*>)
    }
}
