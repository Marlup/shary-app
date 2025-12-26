package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.RequestDomain
import kotlinx.coroutines.flow.Flow

interface RequestRepository {
    fun getReceivedRequests(): Flow<List<RequestDomain>>
    fun getSentRequests(): Flow<List<RequestDomain>>
    suspend fun saveReceivedRequest(request: RequestDomain)
    suspend fun saveSentRequest(request: RequestDomain)
}
