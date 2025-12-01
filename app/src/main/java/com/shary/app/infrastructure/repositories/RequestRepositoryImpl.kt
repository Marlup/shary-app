package com.shary.app.infrastructure.repositories

import androidx.datastore.core.DataStore
import com.shary.app.RequestList
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.infrastructure.mappers.toDomain
import com.shary.app.infrastructure.mappers.toProto
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import kotlinx.coroutines.flow.first


class RequestRepositoryImpl(
    private val dataStore: DataStore<RequestList>,
    private val codec: FieldCodec
) : RequestRepository {

    // ------------------------------ Domain API ------------------------------

    /** Compute a request "hash" from field keys. Keep behavior identical to the original. */
    override suspend fun computeHash(fields: List<FieldDomain>): String {
        return fields.joinToString(separator = ",") { it.key }
    }

    override suspend fun getAllRequests(): List<RequestDomain> {
        val requestProtoList = dataStore.data.first().requestsList
        return requestProtoList.map { it.toDomain(codec) }
    }

    override suspend fun saveRequest(request: RequestDomain) {
        dataStore.updateData { current ->
            current.toBuilder()
                .addRequests(request.toProto(codec))
                .build()
        }
    }

    override suspend fun saveRequestIfNotExists(request: RequestDomain): Boolean {
        val existing = getAllRequests()
        val already = existing.any { it.id == request.id }
        return if (!already) {
            saveRequest(request)
            true
        } else {
            false
        }
    }

    override suspend fun deleteRequest(request: RequestDomain) {
        dataStore.updateData { current ->
            val updated = current.requestsList.filter { it.id != request.id }
            current.toBuilder()
                .clearRequests()
                .addAllRequests(updated)
                .build()
        }
    }

    override suspend fun updateFields(id: String, fields: List<FieldDomain>) {
        dataStore.updateData { currentData ->
            val updatedRequests = buildList {
                var updated = false
                for (req in currentData.requestsList) {
                    // Keep original behavior: new ID is derived from (new) fields
                    val newId = computeHash(fields)
                    if (!updated && req.id == id && newId != id) {
                        add(
                            req.toBuilder()
                                .setId(newId)
                                .clearFields()
                                .addAllFields(fields.map { it.toProto(codec) })
                                .build()
                        )
                        updated = true
                    } else {
                        add(req)
                    }
                }
            }

            currentData.toBuilder()
                .clearRequests()
                .addAllRequests(updatedRequests)
                .build()
        }
    }
}
