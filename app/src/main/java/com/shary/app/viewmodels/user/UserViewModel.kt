package com.shary.app.viewmodels.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.interfaces.repositories.ContactUpdateResult
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.types.enums.ContactSortByParameter
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.application.session.SessionDataCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appCache: CacheService,
    private val sessionDataCoordinator: SessionDataCoordinator
) : ViewModel() {

    // Domain state exposed to UI
    private val _users = MutableStateFlow<List<UserDomain>>(emptyList())
    val users: StateFlow<List<UserDomain>> = _users.asStateFlow()

    private val _selectedEmails = MutableStateFlow<Set<String>>(emptySet())
    val selectedUsers: StateFlow<List<UserDomain>> =
        combine(_users, _selectedEmails) { allUsers, selectedEmails ->
            allUsers.filter { user -> selectedEmails.contains(user.email.trim().lowercase()) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedPhoneNumber = MutableStateFlow<String?>(null)
    val selectedPhoneNumber: StateFlow<String?> = _selectedPhoneNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchCriteria = MutableStateFlow("")
    val searchCriteria: StateFlow<String> = _searchCriteria.asStateFlow()

    private val _sortByParameter = MutableStateFlow(ContactSortByParameter.NAME)
    val sortByParameter: StateFlow<ContactSortByParameter> = _sortByParameter.asStateFlow()

    private val _ascendingSortByMap =
        MutableStateFlow(ContactSortByParameter.entries.associateWith { true })
    val ascendingSortByMap: StateFlow<Map<ContactSortByParameter, Boolean>> =
        _ascendingSortByMap.asStateFlow()

    private val _ascending = MutableStateFlow(true)
    val ascending: StateFlow<Boolean> = _ascending.asStateFlow()

    /** Combined search + sort. Tag filtering is applied in the screen layer. */
    val filteredUsers: StateFlow<List<UserDomain>> =
        combine(
            _users,
            _searchCriteria,
            _sortByParameter,
            _ascending
        ) { list, query, sortParam, asc ->
            sortUsers(filterUsersByQuery(list, query), sortParam, asc)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // One-shot events for UI (snackbar/toast)
    private val _events = MutableSharedFlow<UserEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UserEvent> = _events.asSharedFlow()
    private val editorOperationController = ContactEditorOperationController()
    val editorOperationState: StateFlow<ContactEditorOperationState> = editorOperationController.state

    // Serialize writes to avoid concurrent edits on the same store
    private val writeMutex = Mutex()

    init {
        viewModelScope.launch {
            sessionDataCoordinator.state.collect { sessionState ->
                _users.value = sessionState.users
            }
        }
        if (!sessionDataCoordinator.state.value.started) refresh()
    }

    // ------------------------- Selection helpers -------------------------

    fun anyCachedUser() = appCache.isAnyUserCached()
    fun getOwner() = appCache.getOwner()
    fun getOwnerEmail() = appCache.getOwnerEmail()
    fun getOwnerUsername() = appCache.getOwnerUsername()

    fun toggleUser(user: UserDomain) = _selectedEmails.update { current ->
        val normalized = user.email.trim().lowercase()
        if (current.contains(normalized)) current - normalized else current + normalized
    }

    fun cacheUsers(users: List<UserDomain>) {
        val normalizedEmails = users.map { it.email.trim().lowercase() }.toSet()
        _selectedEmails.value = normalizedEmails
        val selectedByEmail = _users.value.filter { normalizedEmails.contains(it.email.trim().lowercase()) }
        appCache.cacheUsers(selectedByEmail)
    }

    fun getCachedUsers(): List<UserDomain> = appCache.getUsers()

    fun setPhoneNumber(number: String?) {
        _selectedPhoneNumber.value = number
        appCache.cachePhoneNumber(number) // opcional para WhatsApp/Telegram
    }
    fun clearSelectedUsers() {
        _selectedEmails.value = emptySet()
        appCache.clearCachedUsers()
    }

    // ----------------------------- Loading -------------------------------

    private fun refresh() {
        sessionDataCoordinator.refreshUsers()
    }

    fun refreshUsers() {
        refresh()
    }

    fun isTagInUse(tag: Tag): Boolean = _users.value.any { it.tag == tag }

    fun updateSearch(query: String) {
        _searchCriteria.value = query
    }

    fun updateSort(sortByParameter: ContactSortByParameter, ascending: Boolean) {
        _ascendingSortByMap.update { currentMap ->
            currentMap + (sortByParameter to ascending)
        }
        _sortByParameter.value = sortByParameter
        _ascending.value = ascending
    }

    fun updateTag(user: UserDomain, newTag: Tag) {
        if (user.tag == newTag) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { userRepository.updateTag(user.email, newTag) }
                }
            }
            _isLoading.value = false
            result.onSuccess {
                refresh()
            }.onFailure { e ->
                _events.tryEmit(UserEvent.Error(e))
            }
        }
    }

    // ----------------------------- Commands ------------------------------

    /** Save user if not exists. Screen calls this method directly (no coroutines in UI). */
    fun saveUser(user: UserDomain) {
        saveUser(user, editorOperation = false)
    }

    fun submitEditorAddUser(user: UserDomain) {
        saveUser(user, editorOperation = true)
    }

    fun resetEditorOperationState() {
        editorOperationController.reset()
    }

    private fun saveUser(user: UserDomain, editorOperation: Boolean) {
        val normalized = user.copy(
            username = user.username.trim(),
            email = user.email.trim()
        )
        if (editorOperation && !editorOperationController.claim(
                ContactEditorOperation.ADD,
                normalized.email
            )
        ) {
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { userRepository.saveUserIfNotExists(normalized) }
                }
            }
            _isLoading.value = false

            result.onSuccess { created ->
                if (created) {
                    if (editorOperation) {
                        editorOperationController.succeed(
                            ContactEditorOperation.ADD,
                            normalized.email
                        )
                    }
                    _events.tryEmit(UserEvent.Saved(normalized))
                    refresh()
                } else {
                    if (editorOperation) {
                        editorOperationController.fail(
                            ContactEditorOperation.ADD,
                            normalized.email,
                            "A contact with this email already exists."
                        )
                    }
                    _events.tryEmit(UserEvent.AlreadyExists(normalized.email))
                }
            }.onFailure { e ->
                if (editorOperation) {
                    editorOperationController.fail(
                        ContactEditorOperation.ADD,
                        normalized.email,
                        e.message.orEmpty()
                    )
                }
                _events.tryEmit(UserEvent.Error(e))
            }
        }
    }

    fun submitEditorUpdateUser(original: UserDomain, updated: UserDomain) {
        val normalized = updated.copy(
            username = updated.username.trim(),
            email = updated.email.trim(),
            dateAdded = original.dateAdded
        )
        if (!editorOperationController.claim(ContactEditorOperation.UPDATE, original.email)) return

        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        userRepository.updateUser(original.email, normalized)
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess { updateResult ->
                when (updateResult) {
                    ContactUpdateResult.Updated,
                    ContactUpdateResult.Unchanged -> {
                        editorOperationController.succeed(
                            ContactEditorOperation.UPDATE,
                            original.email
                        )
                        _events.tryEmit(UserEvent.Saved(normalized))
                        refresh()
                    }
                    ContactUpdateResult.EmailAlreadyExists -> {
                        editorOperationController.fail(
                            ContactEditorOperation.UPDATE,
                            original.email,
                            "A contact with this email already exists."
                        )
                        _events.tryEmit(UserEvent.AlreadyExists(normalized.email))
                    }
                    ContactUpdateResult.NotFound -> {
                        val error = IllegalStateException("The original contact no longer exists.")
                        editorOperationController.fail(
                            ContactEditorOperation.UPDATE,
                            original.email,
                            error.message.orEmpty()
                        )
                        _events.tryEmit(UserEvent.Error(error))
                    }
                }
            }.onFailure { error ->
                editorOperationController.fail(
                    ContactEditorOperation.UPDATE,
                    original.email,
                    error.message.orEmpty()
                )
                _events.tryEmit(UserEvent.Error(error))
            }
        }
    }

    /** Upsert convenience: replace if email exists, otherwise add. */
    fun upsertUser(user: UserDomain) {
        val normalized = user.copy(
            username = user.username.trim(),
            email = user.email.trim()
        )
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { userRepository.upsertUser(normalized) } // returns created:boolean
                }
            }
            _isLoading.value = false

            result.onSuccess { created ->
                if (created) {
                    _events.tryEmit(UserEvent.Saved(normalized))
                }
                // You could emit a specific "Updated" event if desired
                refresh()
            }.onFailure { e ->
                _events.tryEmit(UserEvent.Error(e))
            }
        }
    }

    /** Delete by full user (uses email as key). */
    fun deleteUser(user: UserDomain) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { userRepository.deleteUser(user.email) }
                }
            }
            _isLoading.value = false

            result.onSuccess { removed ->
                if (removed) {
                    _events.tryEmit(UserEvent.Deleted(user.email))
                    refresh()
                } else {
                    // Not found; optional: emit a specific event
                }
            }.onFailure { e ->
                _events.tryEmit(UserEvent.Error(e))
            }
        }
    }

    /** Batch delete by emails. */
    fun deleteUsers(users: Collection<UserDomain>) {
        if (users.isEmpty()) return
        val emails = users.map { it.email }
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { userRepository.deleteUsers(emails) }
                }
            }
            _isLoading.value = false

            result.onSuccess { removedCount ->
                if (removedCount > 0) refresh()
            }.onFailure { e ->
                _events.tryEmit(UserEvent.Error(e))
            }
        }
    }

    fun restoreDeletedUsers(users: Collection<UserDomain>) {
        if (users.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        users.forEach { user ->
                            userRepository.saveUserIfNotExists(
                                user.copy(
                                    username = user.username.trim(),
                                    email = user.email.trim()
                                )
                            )
                        }
                    }
                }
            }
            _isLoading.value = false
            result.onSuccess {
                refresh()
            }.onFailure { e ->
                _events.tryEmit(UserEvent.Error(e))
            }
        }
    }
}

// UI events for UserViewModel
sealed interface UserEvent {
    data class Saved(val user: UserDomain) : UserEvent
    data class AlreadyExists(val email: String) : UserEvent
    data class Deleted(val email: String) : UserEvent
    data class Error(val throwable: Throwable) : UserEvent
}
