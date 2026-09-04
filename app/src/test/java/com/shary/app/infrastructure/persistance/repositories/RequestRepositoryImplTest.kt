package com.shary.app.infrastructure.persistance.repositories

import androidx.datastore.core.DataStore
import com.shary.app.RequestList
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.OutgoingRequestDraft
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.RequestedFieldDraft
import com.shary.app.core.domain.models.ResponseDraft
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.infrastructure.mappers.toProto
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestRepositoryImplTest {

    @Test
    fun repeatedReceivedSaveIsAnImmutableIdempotentRetry() = runBlocking {
        val store = InMemoryRequestStore(RequestList.getDefaultInstance())
        val repository = RequestRepositoryImpl(store, IdentityCodec)
        val original = request(id = "request-alice-1").copy(responded = true)
        val retry = original.copy(
            user = "mallory@example.com",
            fields = listOf(FieldDomain.create(key = "replacement", value = "")),
            responded = false,
            dateAdded = original.dateAdded.plusSeconds(60)
        )

        repository.saveReceivedRequest(original)
        repository.saveReceivedRequest(retry)

        val stored = repository.getReceivedRequests().first().single()
        assertTrue(stored.responded)
        assertEquals(original.dateAdded, stored.dateAdded)
        assertEquals(original.user, stored.user)
        assertEquals(original.fields.map { it.key }, stored.fields.map { it.key })
    }

    @Test
    fun responseUpdateTargetsOnlyTheMatchingRequestId() = runBlocking {
        val first = request(id = "request-alice-1")
        val second = request(id = "request-alice-2")
        val repository = RequestRepositoryImpl(
            dataStore = InMemoryRequestStore(
                RequestList.newBuilder()
                    .addReceivedRequests(first.toProto(IdentityCodec))
                    .addReceivedRequests(second.toProto(IdentityCodec))
                    .build()
            ),
            codec = IdentityCodec
        )

        assertTrue(repository.markReceivedRequestResponded(second))

        val stored = repository.getReceivedRequests().first()
        assertFalse(stored.single { it.id == first.id }.responded)
        assertTrue(stored.single { it.id == second.id }.responded)
    }

    @Test
    fun sentRequestUpsertPersistsLatestPerRecipientDeliveryResults() = runBlocking {
        val repository = RequestRepositoryImpl(
            InMemoryRequestStore(RequestList.getDefaultInstance()),
            IdentityCodec
        )
        val first = request("request-alice-1").copy(
            deliveryChannel = RequestDeliveryChannel.CLOUD,
            deliveryOutcomes = listOf(
                RequestDeliveryOutcome("alice@example.com", StatusDataSentDb.SUCCESS),
                RequestDeliveryOutcome("bob@example.com", StatusDataSentDb.ERROR)
            ),
            deliveryUpdatedAt = Instant.parse("2026-07-20T09:00:00Z")
        )
        val retried = first.copy(
            deliveryOutcomes = listOf(
                RequestDeliveryOutcome("alice@example.com", StatusDataSentDb.SUCCESS),
                RequestDeliveryOutcome("bob@example.com", StatusDataSentDb.STORED)
            ),
            deliveryUpdatedAt = Instant.parse("2026-07-20T09:05:00Z")
        )

        repository.saveSentRequest(first)
        repository.saveSentRequest(retried)

        val stored = repository.findSentRequest(first.id)
        assertEquals(retried.id, stored?.id)
        assertEquals(retried.deliveryChannel, stored?.deliveryChannel)
        assertEquals(retried.deliveryOutcomes, stored?.deliveryOutcomes)
        assertEquals(retried.deliveryUpdatedAt, stored?.deliveryUpdatedAt)
        assertEquals(1, repository.getSentRequests().first().size)
    }

    @Test
    fun responseDeliveryUpdateTargetsStableReceivedRequestAndPreservesWorkflowState() = runBlocking {
        val first = request("request-alice-1")
        val second = request("request-alice-2").copy(responded = true)
        val repository = RequestRepositoryImpl(
            InMemoryRequestStore(
                RequestList.newBuilder()
                    .addReceivedRequests(first.toProto(IdentityCodec))
                    .addReceivedRequests(second.toProto(IdentityCodec))
                    .build()
            ),
            IdentityCodec
        )
        val outcomes = listOf(
            RequestDeliveryOutcome("alice@example.com", StatusDataSentDb.SUCCESS),
            RequestDeliveryOutcome("bob@example.com", StatusDataSentDb.ERROR)
        )
        val updatedAt = Instant.parse("2026-07-20T09:15:00Z")

        assertTrue(
            repository.updateReceivedRequestDelivery(
                requestId = second.id,
                channel = RequestDeliveryChannel.CLOUD,
                outcomes = outcomes,
                updatedAt = updatedAt
            )
        )

        val unchanged = repository.findReceivedRequest(first.id)
        val updated = repository.findReceivedRequest(second.id)
        assertTrue(updated?.responded == true)
        assertEquals(RequestDeliveryChannel.CLOUD, updated?.deliveryChannel)
        assertEquals(outcomes, updated?.deliveryOutcomes)
        assertEquals(updatedAt, updated?.deliveryUpdatedAt)
        assertEquals(emptyList<RequestDeliveryOutcome>(), unchanged?.deliveryOutcomes)
    }

    @Test
    fun workflowDraftRoundTripIsOwnerScopedAndRestoresStableRequestIdentities() = runBlocking {
        val store = InMemoryRequestStore(RequestList.getDefaultInstance())
        val repository = RequestRepositoryImpl(store, IdentityCodec)
        val received = request("request-alice-1").copy(
            user = "alice@example.com",
            requesterId = "sender-hash-alice"
        )
        repository.saveReceivedRequest(received)
        val workflow = RequestWorkflowState(
            activeReceivedRequest = received,
            responseDraft = ResponseDraft(
                requestId = received.id,
                requesterId = received.requesterId,
                requesterAddress = received.user,
                matchedFieldKeys = listOf(" Secret ", "address")
            ),
            outgoingRequestDraft = OutgoingRequestDraft(
                requestedFields = listOf(RequestedFieldDraft("account", "Account number")),
                requestId = "0f870d06-ff21-4eb3-8e21-465773882293",
                recipientAddresses = listOf(" Bob@Example.com ")
            )
        )

        repository.saveWorkflowDraft(" OWNER@Example.com ", workflow)
        repository.saveWorkflowDraft(
            "other@example.com",
            RequestWorkflowState(
                outgoingRequestDraft = OutgoingRequestDraft(
                    requestedFields = listOf(RequestedFieldDraft("other")),
                    requestId = "087f4f24-e6f1-45f0-8ed3-31ef5178d428"
                )
            )
        )

        val recreatedRepository = RequestRepositoryImpl(store, IdentityCodec)
        val restored = recreatedRepository.findWorkflowDraft("owner@example.com")
        assertEquals(received.id, restored?.activeReceivedRequest?.id)
        assertEquals(received.id, restored?.responseDraft?.requestId)
        assertEquals(listOf("secret", "address"), restored?.responseDraft?.matchedFieldKeys)
        assertEquals(
            "0f870d06-ff21-4eb3-8e21-465773882293",
            restored?.outgoingRequestDraft?.requestId
        )
        assertEquals(
            listOf(RequestedFieldDraft("account", "Account number")),
            restored?.outgoingRequestDraft?.requestedFields
        )
        assertEquals(
            listOf("bob@example.com"),
            restored?.outgoingRequestDraft?.recipientAddresses
        )
        assertEquals(null, recreatedRepository.findWorkflowDraft("unknown@example.com"))

        recreatedRepository.clearWorkflowDraft("owner@example.com")
        assertEquals(null, recreatedRepository.findWorkflowDraft("owner@example.com"))
        assertEquals(
            "087f4f24-e6f1-45f0-8ed3-31ef5178d428",
            recreatedRepository.findWorkflowDraft("other@example.com")
                ?.outgoingRequestDraft
                ?.requestId
        )
    }

    @Test
    fun mismatchedPersistedRequesterCannotRestoreResponseIdentity() = runBlocking {
        val repository = RequestRepositoryImpl(
            InMemoryRequestStore(RequestList.getDefaultInstance()),
            IdentityCodec
        )
        val received = request("request-alice-1").copy(
            user = "alice@example.com",
            requesterId = "sender-hash-alice"
        )
        repository.saveReceivedRequest(received)
        repository.saveWorkflowDraft(
            "owner@example.com",
            RequestWorkflowState(
                activeReceivedRequest = received,
                responseDraft = ResponseDraft(
                    requestId = received.id,
                    requesterId = "sender-hash-mallory",
                    requesterAddress = received.user,
                    matchedFieldKeys = listOf("secret")
                )
            )
        )

        assertEquals(null, repository.findWorkflowDraft("owner@example.com"))
    }

    private fun request(id: String) = RequestDomain(
        fields = listOf(FieldDomain.create(key = "secret", value = "")),
        user = "alice@example.com",
        recipients = listOf("owner@example.com"),
        dateAdded = Instant.parse("2026-07-18T10:15:30Z"),
        owned = false,
        responded = false,
        id = id
    )

    private class InMemoryRequestStore(initial: RequestList) : DataStore<RequestList> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<RequestList> = state

        override suspend fun updateData(transform: suspend (t: RequestList) -> RequestList): RequestList {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private data object IdentityCodec : FieldCodec {
        override fun encode(message: String, purpose: Purpose): String = message
        override fun decode(message: String, purpose: Purpose): String = message
    }
}
