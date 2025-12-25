package com.shary.app.viewmodels.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.interfaces.services.CacheService
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
    private val appCache: CacheService
) : ViewModel() {

    // Domain state exposed to UI
    private val _users = MutableStateFlow<List<UserDomain>>(emptyList())
    val users: StateFlow<List<UserDomain>> = _users.asStateFlow()

    private val _cachedUsers = MutableStateFlow<List<UserDomain>>(emptyList())
    val selectedUsers: StateFlow<List<UserDomain>> = _cachedUsers.asStateFlow()

    private val _selectedPhoneNumber = MutableStateFlow<String?>(null)
    val selectedPhoneNumber: StateFlow<String?> = _selectedPhoneNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // One-shot events for UI (snackbar/toast)
    private val _events = MutableSharedFlow<UserEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UserEvent> = _events.asSharedFlow()

    // Serialize writes to avoid concurrent edits on the same store
    private val writeMutex = Mutex()

    init { refresh() }

    // ------------------------- Selection helpers -------------------------

    fun anyCachedUser() = appCache.isAnyUserCached()
    fun getOwner() = appCache.getOwner()
    fun getOwnerEmail() = appCache.getOwnerEmail()
    fun getOwnerUsername() = appCache.getOwnerUsername()

    fun toggleUser(user: UserDomain) = _cachedUsers.update { current ->
        if (user in current) current - user else current + user
    }

    fun cacheUsers(users: List<UserDomain>) {
        _cachedUsers.value = users.distinctBy { it.email.trim().lowercase() }
        appCache.cacheUsers(_cachedUsers.value) // <— persistencia cross-screen
    }

    fun getCachedUsers(): List<UserDomain> = appCache.getUsers()

    fun setPhoneNumber(number: String?) {
        _selectedPhoneNumber.value = number
        appCache.cachePhoneNumber(number) // opcional para WhatsApp/Telegram
    }
    fun clearSelectedUsers() { appCache.clearCachedUsers() }

    // ----------------------------- Loading -------------------------------

    private fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) { userRepository.getAllUsers() }
            _users.value = list
            _isLoading.value = false
        }
    }

    fun refreshUsers() {
        refresh()
    }

    // ----------------------------- Commands ------------------------------

    /** Save user if not exists. Screen calls this method directly (no coroutines in UI). */
    fun saveUser(user: UserDomain) {
        val normalized = user.copy(
            username = user.username.trim(),
            email = user.email.trim()
        )
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
                    _events.tryEmit(UserEvent.Saved(normalized))
                    refresh()
                } else {
                    _events.tryEmit(UserEvent.AlreadyExists(normalized.email))
                }
            }.onFailure { e ->
                _events.tryEmit(UserEvent.Error(e))
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
}

// UI events for UserViewModel
sealed interface UserEvent {
    data class Saved(val user: UserDomain) : UserEvent
    data class AlreadyExists(val email: String) : UserEvent
    data class Deleted(val email: String) : UserEvent
    data class Error(val throwable: Throwable) : UserEvent
}
