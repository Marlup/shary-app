package com.shary.app.infrastructure.services.cache

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.ui.screens.field.FieldsScreen
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * SelectionCacheService
 *
 * In-memory cache for selected fields, users, and phone numbers.
 *
 * Responsibilities:
 * - Keeps cached lists of FieldDomain and UserDomain objects.
 * - Provides reactive flows for UI components to observe cache changes.
 * - Deduplicates entries on cache insertions (by key/email).
 * - Offers both fast (non-synchronized) and safe (Mutex-locked) cache updates.
 * - Can be reset or cleared selectively (fields, users, all).
 *
 * This service is a singleton; it is not persisted across app restarts.
 */
@Singleton
class SelectionCacheService @Inject constructor(
) : CacheService {

    /** Used to serialize writes when calling safe methods */
    private val lock = Mutex()

    /** Cached fields, exposed as reactive flow */
    private val _fields = MutableStateFlow<List<FieldDomain>>(emptyList())

    /** Cached requests, exposed as reactive flow */
    private val _requests = MutableStateFlow<List<RequestDomain>>(emptyList())

    /** Cached draft request, exposed as reactive flow */
    private val _draftRequest = MutableStateFlow(RequestDomain.initialize())

    /** Cached draft request, exposed as reactive flow */
    private val _draftFields = MutableStateFlow(emptyList<FieldDomain>())

    /** Cached users, exposed as reactive flow */
    private val _users  = MutableStateFlow<List<UserDomain>>(emptyList())

    /** Optional Owner as Userdomain structure as cached alongside fields/users */
    private val _owner  = MutableStateFlow(UserDomain())

    /** Optional phone number cached alongside fields/users */
    private val _ownerPhone  = MutableStateFlow<String?>(null)

    /** Optional phone number cached alongside fields/users */
    private val _ownerEmail  = MutableStateFlow<String?>(null)    /** Optional phone number cached alongside fields/users */

    /** Optional username cached alongside fields/users */
    private val _ownerUsername  = MutableStateFlow<String?>(null)

    // -------------------- Public Flows --------------------

    override val fieldsFlow: StateFlow<List<FieldDomain>> get() = _fields
    override val requestsFlow: StateFlow<List<RequestDomain>> get() = _requests
    override val usersFlow: StateFlow<List<UserDomain>> get() = _users

    // -------------------- Read-only accessors --------------------

    override fun getFields(): List<FieldDomain> = _fields.value
    override fun getRequests(): List<RequestDomain> = _requests.value
    override fun getDraftFields(): List<FieldDomain> = _draftFields.value
    override fun getDraftRequest(): RequestDomain = _draftRequest.value
    override fun getUsers(): List<UserDomain> = _users.value

    // -------------------- Cache mutations --------------------

    /**
     * Replaces cached fields with new list, deduplicated by key (case-insensitive).
     */
    override fun cacheFields(fields: List<FieldDomain>) {
        val unique = fields.distinctBy { it.key.trim().lowercase() }
        _fields.value = unique
    }

    override fun cacheRequests(requests: List<RequestDomain>) {
        val unique = requests.distinctBy { it.recipients }
        _requests.value = unique
    }

    override fun cacheDraftRequest(draftRequest: RequestDomain) {
        _draftRequest.value = draftRequest
    }


    override fun clearCachedDraftFields() {
        _draftFields.value = emptyList()
    }
    override fun cacheDraftFields(draftFields: List<FieldDomain>) {
        _draftFields.value = draftFields
    }
    override fun isAnyFieldCached(): Boolean = getFields().isNotEmpty()
    override fun isAnyRequestCached(): Boolean = getRequests().isNotEmpty()
    override fun isAnyDraftFieldCached(): Boolean = _draftFields.value.isNotEmpty()

    /**
     * Replaces cached users with new list, deduplicated by normalized email.
     */
    override fun cacheUsers(users: List<UserDomain>) {
        val unique = users.distinctBy { it.email.trim().lowercase() }
        _users.value = unique
    }

    override fun isAnyUserCached(): Boolean = getUsers().isNotEmpty()

    /** Clears fields */
    override fun clearCachedFields() { _fields.value = emptyList() }

    /** Clears requests */
    override fun clearCachedRequests() { _requests.value = emptyList() }

    /** Clears draft request */
    override fun clearCachedDraftRequest() { _draftRequest.value = RequestDomain.initialize() }

    /** Clears users */
    override fun clearCachedUsers()  { _users.value = emptyList() }

    /** Clears all cached values (fields, users, phone) */
    override fun clearAllCaches() {
        _fields.value = emptyList()
        _users.value = emptyList()
        _ownerPhone.value = null
    }

    /** Sets or clears cached phone number */
    override fun cachePhoneNumber(number: String?) { _ownerPhone.value = number }
    override fun getPhoneNumber(): String? = _ownerPhone.value

    /** Sets or clears cached email and username*/
    override fun cacheOwner(owner: UserDomain) { _owner.value = owner }
    override fun cacheOwnerUsername(username: String?) {
        if (username != null) {
            _owner.value.username = username
        }
    }
    override fun cacheOwnerEmail(email: String?) {
        if (email != null) {
            _owner.value.email = email
        }
    }
    override fun getOwner(): UserDomain = _owner.value
    override fun getOwnerUsername(): String = _owner.value.username
    override fun getOwnerEmail(): String = _owner.value.email

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
