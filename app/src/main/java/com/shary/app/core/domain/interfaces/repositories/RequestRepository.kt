package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.RequestDomain
import kotlinx.coroutines.flow.Flow

interface RequestRepository {
    suspend fun getAllRequests(): Flow<List<RequestDomain>>
    suspend fun saveRequest(request: RequestDomain)
}
