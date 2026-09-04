package com.shary.app.infrastructure.persistance.repositories

import androidx.datastore.core.DataStore
import com.shary.app.RequestList
import com.shary.app.PersistedOutgoingRequestDraft
import com.shary.app.PersistedRequestWorkflow
import com.shary.app.PersistedResponseDraft
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.OutgoingRequestDraft
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.RequestedFieldDraft
import com.shary.app.core.domain.models.ResponseDraft
import com.shary.app.core.domain.models.hasPersistableDraftIdentity
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.models.stableRequestId
import com.shary.app.infrastructure.mappers.toDomain
import com.shary.app.infrastructure.mappers.toDomainFields
import com.shary.app.infrastructure.mappers.toProto
import com.shary.app.infrastructure.mappers.toProtoFields
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.core.domain.types.valueobjects.Purpose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class RequestRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<RequestList>,
    private val codec: FieldCodec
) : RequestRepository {

    override fun getReceivedRequests(): Flow<List<RequestDomain>> {
        return dataStore.data.map { requestProtoList ->
            (requestProtoList.receivedRequestsList + requestProtoList.requestsList)
                .map { it.toDomain(codec) }
        }
    }

    override fun getSentRequests(): Flow<List<RequestDomain>> {
        return dataStore.data.map { requestProtoList ->
            requestProtoList.sentRequestsList.map { it.toDomain(codec) }
        }
    }

    override suspend fun saveReceivedRequest(request: RequestDomain) {
        val encrypted = request.toProto(codec)
        dataStore.updateData { current ->
            if (current.receivedRequestsList.any { it.id == encrypted.id }) {
                current
            } else {
                current.toBuilder()
                    .addReceivedRequests(encrypted)
                    .build()
            }
        }
    }

    override suspend fun findReceivedRequest(requestId: String): RequestDomain? {
        val stableId = requestId.trim()
        if (stableId.isEmpty()) return null
        val current = dataStore.data.first()
        return (current.receivedRequestsList + current.requestsList)
            .firstNotNullOfOrNull { proto ->
                proto.toDomain(codec).takeIf { it.id == stableId }
            }
    }

    override suspend fun findSentRequest(requestId: String): RequestDomain? {
        val stableId = requestId.trim()
        if (stableId.isEmpty()) return null
        return dataStore.data.first().sentRequestsList
            .firstOrNull { it.id == stableId }
            ?.toDomain(codec)
    }

    override suspend fun saveSentRequest(request: RequestDomain) {
        val encrypted = request.toProto(codec)
        dataStore.updateData { current ->
            val sent = current.sentRequestsList
                .filterNot { it.id == encrypted.id } + encrypted
            current.toBuilder()
                .clearSentRequests()
                .addAllSentRequests(sent)
                .build()
        }
    }

    override suspend fun findWorkflowDraft(ownerAddress: String): RequestWorkflowState? {
        val owner = ownerAddress.normalizedAddress()
        if (owner.isEmpty()) return null
        val current = dataStore.data.first()
        val persisted = current.workflowDraftsList.firstOrNull {
            it.ownerAddress.normalizedAddress() == owner
        } ?: return null

        val outgoing = if (persisted.hasOutgoingDraft()) {
            val stored = persisted.outgoingDraft
            OutgoingRequestDraft(
                requestedFields = stored.requestedFieldsList
                    .toDomainFields(codec)
                    .map { field ->
                        RequestedFieldDraft(
                            key = field.key.trim(),
                            keyAlias = field.keyAlias.trim()
                        )
                    }
                    .filter { it.key.isNotBlank() }
                    .distinctBy { it.key.lowercase() },
                requestId = stored.requestId.trim(),
                recipientAddresses = stored.recipientAddressesList
                    .map(String::normalizedAddress)
                    .filter(String::isNotBlank)
                    .distinct()
            )
        } else {
            OutgoingRequestDraft()
        }

        val storedResponse = persisted.responseDraft.takeIf { persisted.hasResponseDraft() }
        val activeRequest = storedResponse?.let { response ->
            (current.receivedRequestsList + current.requestsList)
                .firstNotNullOfOrNull { proto ->
                    proto.toDomain(codec).takeIf { request ->
                        request.id == response.requestId.trim() &&
                            request.requesterId == response.requesterId.trim() &&
                            request.user.normalizedAddress() ==
                            response.requesterAddress.normalizedAddress()
                    }
                }
        }
        val response = storedResponse?.takeIf { activeRequest != null }?.let { stored ->
            ResponseDraft(
                requestId = stored.requestId.trim(),
                requesterId = stored.requesterId.trim(),
                requesterAddress = stored.requesterAddress.normalizedAddress(),
                matchedFieldKeys = stored.matchedFieldKeysList
                    .map { codec.decode(it, Purpose.Key).trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .distinct(),
                completed = stored.completed || activeRequest?.responded == true
            )
        }
        val mode = runCatching { RequestListMode.valueOf(persisted.mode) }
            .getOrDefault(RequestListMode.RECEIVED)
        val restored = RequestWorkflowState(
            mode = mode,
            activeReceivedRequest = activeRequest,
            responseDraft = response,
            outgoingRequestDraft = outgoing
        )
        return restored.takeIf(RequestWorkflowState::hasPersistableDraftIdentity)
    }

    override suspend fun saveWorkflowDraft(
        ownerAddress: String,
        workflow: RequestWorkflowState
    ) {
        val owner = ownerAddress.normalizedAddress()
        if (owner.isEmpty()) return
        if (!workflow.hasPersistableDraftIdentity()) {
            clearWorkflowDraft(owner)
            return
        }

        val persisted = PersistedRequestWorkflow.newBuilder()
            .setOwnerAddress(owner)
            .setMode(workflow.mode.name)
            .apply {
                workflow.outgoingRequestDraft
                    .takeIf { it.requestId.isNotBlank() }
                    ?.let { outgoing ->
                        outgoingDraft = PersistedOutgoingRequestDraft.newBuilder()
                            .setRequestId(outgoing.requestId.trim())
                            .addAllRequestedFields(
                                outgoing.requestedFields.map { field ->
                                    FieldDomain.initialize().copy(
                                        key = field.key,
                                        value = "",
                                        keyAlias = field.keyAlias,
                                        dateAdded = Instant.EPOCH
                                    )
                                }.toProtoFields(codec)
                            )
                            .addAllRecipientAddresses(
                                outgoing.recipientAddresses
                                    .map(String::normalizedAddress)
                                    .filter(String::isNotBlank)
                                    .distinct()
                            )
                            .build()
                    }
                workflow.responseDraft?.let { response ->
                    responseDraft = PersistedResponseDraft.newBuilder()
                        .setRequestId(response.requestId.trim())
                        .setRequesterId(response.requesterId.trim())
                        .setRequesterAddress(response.requesterAddress.normalizedAddress())
                        .addAllMatchedFieldKeys(
                            response.matchedFieldKeys
                                .map { it.trim().lowercase() }
                                .filter { it.isNotBlank() }
                                .distinct()
                                .map { codec.encode(it, Purpose.Key) }
                        )
                        .setCompleted(response.completed)
                        .build()
                }
            }
            .build()

        dataStore.updateData { current ->
            val snapshots = current.workflowDraftsList
                .filterNot { it.ownerAddress.normalizedAddress() == owner } + persisted
            current.toBuilder()
                .clearWorkflowDrafts()
                .addAllWorkflowDrafts(snapshots)
                .build()
        }
    }

    override suspend fun clearWorkflowDraft(ownerAddress: String) {
        val owner = ownerAddress.normalizedAddress()
        if (owner.isEmpty()) return
        dataStore.updateData { current ->
            current.toBuilder()
                .clearWorkflowDrafts()
                .addAllWorkflowDrafts(
                    current.workflowDraftsList.filterNot {
                        it.ownerAddress.normalizedAddress() == owner
                    }
                )
                .build()
        }
    }

    override suspend fun updateReceivedRequestDelivery(
        requestId: String,
        channel: RequestDeliveryChannel,
        outcomes: List<RequestDeliveryOutcome>,
        updatedAt: Instant
    ): Boolean {
        val stableId = requestId.trim()
        if (stableId.isEmpty() || outcomes.isEmpty()) return false
        var updated = false
        dataStore.updateData { current ->
            fun updateMatching(requests: List<com.shary.app.Request>): List<com.shary.app.Request> =
                requests.map { proto ->
                    val domain = proto.toDomain(codec)
                    if (!updated && domain.id == stableId) {
                        updated = true
                        domain.copy(
                            deliveryChannel = channel,
                            deliveryOutcomes = outcomes,
                            deliveryUpdatedAt = updatedAt
                        ).toProto(codec)
                    } else {
                        proto
                    }
                }

            current.toBuilder()
                .clearReceivedRequests()
                .addAllReceivedRequests(updateMatching(current.receivedRequestsList))
                .clearRequests()
                .addAllRequests(updateMatching(current.requestsList))
                .build()
        }
        return updated
    }

    override suspend fun markReceivedRequestResponded(
        request: RequestDomain,
        responded: Boolean
    ): Boolean {
        var updated = false
        dataStore.updateData { current ->
            val updatedReceived = current.receivedRequestsList.map { proto ->
                val domain = proto.toDomain(codec)
                if (!updated && domain.matchesIdentityOf(request)) {
                    updated = true
                    domain.copy(responded = responded).toProto(codec)
                } else {
                    proto
                }
            }

            val updatedLegacy = current.requestsList.map { proto ->
                val domain = proto.toDomain(codec)
                if (!updated && domain.matchesIdentityOf(request)) {
                    updated = true
                    domain.copy(responded = responded).toProto(codec)
                } else {
                    proto
                }
            }

            current.toBuilder()
                .clearReceivedRequests()
                .addAllReceivedRequests(updatedReceived)
                .clearRequests()
                .addAllRequests(updatedLegacy)
                .build()
        }
        return updated
    }

    private fun RequestDomain.matchesIdentityOf(other: RequestDomain): Boolean {
        return stableRequestId() == other.stableRequestId()
    }
}

private fun String.normalizedAddress(): String = trim().lowercase()
