package com.shary.app.core.domain.interfaces.services

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.UserDomain
import kotlinx.coroutines.flow.StateFlow

data class AccountScopedCacheState(
    val fields: List<FieldDomain> = emptyList(),
    val requests: List<RequestDomain> = emptyList(),
    val draftFields: List<FieldDomain> = emptyList(),
    val draftRequest: RequestDomain = RequestDomain.initialize(),
    val users: List<UserDomain> = emptyList(),
    val owner: UserDomain = UserDomain(),
    val ownerPhone: String? = null,
    val requestWorkflow: RequestWorkflowState = RequestWorkflowState()
)

/**
 * Process-local account and cross-screen selection state.
 *
 * [accountState] is the authoritative inventory. Session teardown replaces it
 * in one assignment so observers cannot see a partially cleared account.
 */
interface CacheService {

    val accountState: StateFlow<AccountScopedCacheState>


    // Fields
    fun clearCachedFields()
    fun cacheFields(fields: List<FieldDomain>)
    fun getFields(): List<FieldDomain>
    fun isAnyFieldCached(): Boolean

    // Requests
    fun clearCachedRequests()
    fun cacheRequests(requests: List<RequestDomain>)
    fun getRequests(): List<RequestDomain>
    fun isAnyRequestCached(): Boolean

    // Draft Fields
    fun clearCachedDraftFields()
    fun cacheDraftFields(draftFields: List<FieldDomain>)
    fun getDraftFields(): List<FieldDomain>
    fun isAnyDraftFieldCached(): Boolean

    // Draft Request
    fun clearCachedDraftRequest()
    fun cacheDraftRequest(draftRequest: RequestDomain)
    fun getDraftRequest(): RequestDomain
    //fun isAnyDraftRequestCached(): Boolean


    // Users
    fun clearCachedUsers()
    fun cacheUsers(users: List<UserDomain>)
    fun getUsers(): List<UserDomain>
    fun isAnyUserCached(): Boolean


    // Owner
    fun cacheOwner(owner: UserDomain)
    fun cacheOwnerUsername(username: String?)
    fun cacheOwnerEmail(email: String?)
    fun cachePhoneNumber(number: String?)
    fun getOwner(): UserDomain
    fun getOwnerUsername(): String?
    fun getOwnerEmail(): String?
    fun getPhoneNumber(): String?

    // Request workflow
    fun getRequestWorkflow(): RequestWorkflowState
    fun cacheRequestWorkflow(state: RequestWorkflowState)
    fun updateRequestWorkflow(
        transform: (RequestWorkflowState) -> RequestWorkflowState
    ): RequestWorkflowState


    // General
    fun clearAllCaches()
}
