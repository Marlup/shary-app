package com.shary.app.repositories.requests

import android.content.Context
import androidx.datastore.core.DataStore
import com.shary.app.Field
import com.shary.app.Request
import com.shary.app.RequestList
import com.shary.app.datastore.requestListDataStore
import kotlinx.coroutines.flow.first

class RequestRepositoryImpl(
    context: Context
    ) : RequestRepository {
        private val dataStore: DataStore<RequestList> = context.requestListDataStore

        override suspend fun computeHash(fields: List<Field>): String {
            return fields.joinToString(separator = ",") { field: Field -> field.key }
        }
        override suspend fun getAllRequests(): List<Request> {
            val requestList = dataStore.data.first()
            return requestList.requestsList
        }

        override suspend fun saveRequest(request: Request) {
            dataStore.updateData { current ->
                current.toBuilder()
                    .addRequests(request)
                    .build()
            }
        }

        override suspend fun saveRequestIfNotExists(request: Request): Boolean {
            val existingRequests = getAllRequests()
            val requestAlreadyExists = existingRequests.any { it.id == request.id }

            if (!requestAlreadyExists) {
                saveRequest(request)
                return true
            } else {
                return false
            }
        }

        override suspend fun deleteRequest(request: Request) {
            dataStore.updateData { current ->
                val updatedRequests = current.requestsList.filter { it.id != request.id }
                current.toBuilder()
                    .clearRequests()
                    .addAllRequests(updatedRequests)
                    .build()
            }
        }

        override suspend fun updateFields(id: String, fields: List<Field>) {
            dataStore.updateData { currentData ->
                val updatedRequests = buildList {
                    var updated = false
                    for (request in currentData.requestsList) {
                        val newId: String = computeHash(fields)
                        if (!updated && request.id == id && newId != id) {
                            add(
                                request
                                    .toBuilder()
                                    .setId(newId)
                                    .addAllFields(fields)
                                    .build()
                            )
                            updated = true
                        } else {
                            add(request)
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