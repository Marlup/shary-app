package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain

interface RequestRepository {
    suspend fun computeHash(fields: List<FieldDomain>): String
    suspend fun getAllRequests(): List<RequestDomain>
    suspend fun saveRequest(request: RequestDomain)
    suspend fun saveRequestIfNotExists(request: RequestDomain): Boolean
    suspend fun deleteRequest(request: RequestDomain)
    suspend fun updateFields(id: String, fields: List<FieldDomain>)
}
