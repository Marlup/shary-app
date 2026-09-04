package com.shary.app.infrastructure.services.cache

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.interfaces.services.AccountScopedCacheState
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.stableRequestId
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * SelectionCacheService
 *
 * In-memory cache for selected fields, users, and phone numbers.
 *
 * Responsibilities:
 * - Keeps account-scoped selections and workflow handoff state.
 * - Publishes one immutable state so complete teardown is atomic to observers.
 * - Deduplicates entries on cache insertions (by key/email).
 * - Offers both direct and Mutex-serialized updates.
 * - Can be reset or cleared selectively.
 *
 * This service is a singleton; it is not persisted across app restarts.
 */
@Singleton
class SelectionCacheService @Inject constructor(
) : CacheService {

    /** Used to serialize writes when calling safe methods */
    private val lock = Mutex()

    private val _accountState = MutableStateFlow(AccountScopedCacheState())
    override val accountState: StateFlow<AccountScopedCacheState> = _accountState.asStateFlow()

    // -------------------- Read-only accessors --------------------

    override fun getFields(): List<FieldDomain> = _accountState.value.fields
    override fun getRequests(): List<RequestDomain> = _accountState.value.requests
    override fun getDraftFields(): List<FieldDomain> = _accountState.value.draftFields
    override fun getDraftRequest(): RequestDomain = _accountState.value.draftRequest
    override fun getUsers(): List<UserDomain> = _accountState.value.users.map { it.copy() }

    // -------------------- Cache mutations --------------------

    /**
     * Replaces cached fields with new list, deduplicated by key (case-insensitive).
     */
    override fun cacheFields(fields: List<FieldDomain>) {
        val unique = fields
            .distinctBy { it.key.trim().lowercase() }
            .immutableCopy()
        _accountState.update { it.copy(fields = unique) }
    }

    override fun cacheRequests(requests: List<RequestDomain>) {
        val unique = requests
            .distinctBy(RequestDomain::stableRequestId)
            .map(RequestDomain::cacheSnapshot)
            .immutableCopy()
        _accountState.update { it.copy(requests = unique) }
    }

    override fun cacheDraftRequest(draftRequest: RequestDomain) {
        _accountState.update { it.copy(draftRequest = draftRequest.cacheSnapshot()) }
    }


    override fun clearCachedDraftFields() {
        _accountState.update { it.copy(draftFields = emptyList()) }
    }
    override fun cacheDraftFields(draftFields: List<FieldDomain>) {
        _accountState.update { it.copy(draftFields = draftFields.immutableCopy()) }
    }
    override fun isAnyFieldCached(): Boolean = getFields().isNotEmpty()
    override fun isAnyRequestCached(): Boolean = getRequests().isNotEmpty()
    override fun isAnyDraftFieldCached(): Boolean = getDraftFields().isNotEmpty()

    /**
     * Replaces cached users with new list, deduplicated by normalized email.
     */
    override fun cacheUsers(users: List<UserDomain>) {
        val unique = users
            .distinctBy { it.email.trim().lowercase() }
            .map { it.copy() }
            .immutableCopy()
        _accountState.update { it.copy(users = unique) }
    }

    override fun isAnyUserCached(): Boolean = getUsers().isNotEmpty()

    /** Clears fields */
    override fun clearCachedFields() {
        _accountState.update { it.copy(fields = emptyList()) }
    }

    /** Clears requests */
    override fun clearCachedRequests() {
        _accountState.update { it.copy(requests = emptyList()) }
    }

    /** Clears draft request */
    override fun clearCachedDraftRequest() {
        _accountState.update { it.copy(draftRequest = RequestDomain.initialize()) }
    }

    /** Clears users */
    override fun clearCachedUsers() {
        _accountState.update { it.copy(users = emptyList()) }
    }

    /** Atomically clears the complete process-local account inventory. */
    override fun clearAllCaches() {
        _accountState.value = AccountScopedCacheState()
    }

    /** Sets or clears cached phone number */
    override fun cachePhoneNumber(number: String?) {
        _accountState.update { it.copy(ownerPhone = number) }
    }
    override fun getPhoneNumber(): String? = _accountState.value.ownerPhone

    override fun getRequestWorkflow(): RequestWorkflowState = _accountState.value.requestWorkflow

    override fun cacheRequestWorkflow(state: RequestWorkflowState) {
        _accountState.update { it.copy(requestWorkflow = state.cacheSnapshot()) }
    }

    override fun updateRequestWorkflow(
        transform: (RequestWorkflowState) -> RequestWorkflowState
    ): RequestWorkflowState {
        var updated = getRequestWorkflow()
        _accountState.update { state ->
            updated = transform(state.requestWorkflow).cacheSnapshot()
            state.copy(requestWorkflow = updated)
        }
        return updated
    }

    /** Replaces owner identity without retaining a caller-owned mutable object. */
    override fun cacheOwner(owner: UserDomain) {
        _accountState.update { it.copy(owner = owner.copy()) }
    }
    override fun cacheOwnerUsername(username: String?) {
        _accountState.update { state ->
            state.copy(owner = state.owner.copy(username = username.orEmpty()))
        }
    }
    override fun cacheOwnerEmail(email: String?) {
        _accountState.update { state ->
            state.copy(owner = state.owner.copy(email = email.orEmpty()))
        }
    }
    override fun getOwner(): UserDomain = _accountState.value.owner.copy()
    override fun getOwnerUsername(): String? = _accountState.value.owner.username.ifBlank { null }
    override fun getOwnerEmail(): String? = _accountState.value.owner.email.ifBlank { null }

    // -------------------- Thread-safe variants --------------------

    /**
     * Safe version of cacheFields() that serializes writes using Mutex.
     * Prefer when multiple coroutines might update cache concurrently.
     */
    suspend fun cacheFieldsSafe(fields: List<FieldDomain>) =
        lock.withLock { cacheFields(fields) }

    /**
     * Safe version of cacheUsers() that serializes writes using Mutex.
     */
    suspend fun cacheUsersSafe(users: List<UserDomain>) =
        lock.withLock { cacheUsers(users) }
}

private fun RequestDomain.cacheSnapshot(): RequestDomain = copy(
    fields = fields.immutableCopy(),
    recipients = recipients.immutableCopy()
)

private fun RequestWorkflowState.cacheSnapshot(): RequestWorkflowState = copy(
    activeReceivedRequest = activeReceivedRequest?.cacheSnapshot(),
    responseDraft = responseDraft?.copy(
        matchedFieldKeys = responseDraft.matchedFieldKeys.immutableCopy()
    ),
    outgoingRequestDraft = outgoingRequestDraft.copy(
        requestedFields = outgoingRequestDraft.requestedFields.immutableCopy(),
        recipientAddresses = outgoingRequestDraft.recipientAddresses.immutableCopy()
    )
)

private fun <T> List<T>.immutableCopy(): List<T> =
    Collections.unmodifiableList(ArrayList(this))
