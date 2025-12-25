package com.shary.app.infrastructure.persistance.repositories

import androidx.datastore.core.DataStore
import com.shary.app.RequestList
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.infrastructure.mappers.toDomain
import com.shary.app.infrastructure.mappers.toProto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RequestRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<RequestList>,
    private val codec: FieldCodec
) : RequestRepository {

    override suspend fun getAllRequests(): Flow<List<RequestDomain>> {
        return dataStore.data.map { requestProtoList ->
            requestProtoList.requestsList.map { it.toDomain(codec) }
        }
    }

    override suspend fun saveRequest(request: RequestDomain) {
        val encrypted = request.toProto(codec)
        dataStore.updateData { current ->
            current.toBuilder()
                .addRequests(encrypted)
                .build()
        }
    }
}
