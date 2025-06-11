package com.shary.app.repositories.requests

import com.shary.app.Field
import com.shary.app.Request

interface RequestRepository {
    suspend fun computeHash(fields: List<Field>): String
    suspend fun getAllRequests(): List<Request>
    suspend fun saveRequest(request: Request)
    suspend fun saveRequestIfNotExists(request: Request): Boolean
    suspend fun deleteRequest(request: Request)
    suspend fun updateFields(id: String, fields: List<Field>)
}