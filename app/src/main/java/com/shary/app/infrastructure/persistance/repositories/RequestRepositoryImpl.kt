package com.shary.app.infrastructure.persistance.repositories

import androidx.datastore.core.DataStore
import com.shary.app.RequestList
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.infrastructure.mappers.toDomain
import com.shary.app.infrastructure.mappers.toProto
import com.shary.app.utils.log.AppLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
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
        AppLogger.debug("RequestRepositoryImpl", "event=save_received_request")
        val encrypted = request.toProto(codec)
        dataStore.updateData { current ->
            current.toBuilder()
                .addReceivedRequests(encrypted)
                .build()
        }
    }

    override suspend fun saveSentRequest(request: RequestDomain) {
        val encrypted = request.toProto(codec)
        dataStore.updateData { current ->
            current.toBuilder()
                .addSentRequests(encrypted)
                .build()
        }
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
        val thisKeys = fields.map { it.key.trim().lowercase() }.sorted()
        val otherKeys = other.fields.map { it.key.trim().lowercase() }.sorted()
        val thisRecipients = recipients.map { it.trim().lowercase() }.sorted()
        val otherRecipients = other.recipients.map { it.trim().lowercase() }.sorted()

        return user.trim().equals(other.user.trim(), ignoreCase = true) &&
                dateAdded.toEpochMilli() == other.dateAdded.toEpochMilli() &&
                thisKeys == otherKeys &&
                thisRecipients == otherRecipients
    }
}
