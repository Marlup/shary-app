package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.RequestWorkflowState
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface RequestRepository {
    fun getReceivedRequests(): Flow<List<RequestDomain>>
    fun getSentRequests(): Flow<List<RequestDomain>>
    suspend fun findReceivedRequest(requestId: String): RequestDomain?
    suspend fun findSentRequest(requestId: String): RequestDomain?
    suspend fun saveReceivedRequest(request: RequestDomain)
    suspend fun saveSentRequest(request: RequestDomain)
    suspend fun findWorkflowDraft(ownerAddress: String): RequestWorkflowState?
    suspend fun saveWorkflowDraft(ownerAddress: String, workflow: RequestWorkflowState)
    suspend fun clearWorkflowDraft(ownerAddress: String)
    suspend fun updateReceivedRequestDelivery(
        requestId: String,
        channel: RequestDeliveryChannel,
        outcomes: List<RequestDeliveryOutcome>,
        updatedAt: Instant
    ): Boolean
    suspend fun markReceivedRequestResponded(request: RequestDomain, responded: Boolean = true): Boolean
}
