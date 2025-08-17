// com/shary/app/viewmodels/request/RequestListViewModel.kt
package com.shary.app.viewmodels.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class RequestListViewModel @Inject constructor(
    private val requestRepository: RequestRepository
) : ViewModel() {

    // Exposed state to UI
    private val _requests = MutableStateFlow<List<RequestDomain>>(emptyList())
    val requests: StateFlow<List<RequestDomain>> = _requests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // One-shot events for UI (snackbar/toast/navigation)
    private val _events = MutableSharedFlow<RequestEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RequestEvent> = _events

    // Serialize writes to avoid concurrent edits on the same store
    private val writeMutex = Mutex()

    init { refresh() }

    // ------------------------------ Queries ------------------------------

    /** Reload the whole list from the repository. UI never launches coroutines directly. */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) { requestRepository.getAllRequests() }
            _requests.value = list
            _isLoading.value = false
        }
    }

    // ----------------------------- Commands ------------------------------

    /** Add a request already built in domain shape. De-duplicates by id. */
    fun addRequest(request: RequestDomain) {
        val normalized = request.copy(
            id = request.id.trim(),
            dateAdded = if (request.dateAdded == Instant.EPOCH) Instant.now() else request.dateAdded
        )
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        requestRepository.saveRequestIfNotExists(normalized)
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess { created ->
                if (created) {
                    _events.tryEmit(RequestEvent.Saved)
                    refresh()
                } else {
                    _events.tryEmit(RequestEvent.AlreadyExists)
                }
            }.onFailure { e ->
                _events.tryEmit(RequestEvent.Error(e))
            }
        }
    }

    /**
     * Build and add a request from a list of fields.
     * The id is computed with the repository (same behavior you had: join of keys).
     */
    fun addRequestFromFields(fields: List<FieldDomain>) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        val id = requestRepository.computeHash(fields)
                        val request = RequestDomain(
                            id = id,
                            fields = fields,
                            dateAdded = Instant.now()
                        )
                        requestRepository.saveRequestIfNotExists(request)
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess { created ->
                if (created) {
                    _events.tryEmit(RequestEvent.Saved) // id might be long; keep generic
                    refresh()
                } else {
                    _events.tryEmit(RequestEvent.AlreadyExists)
                }
            }.onFailure { e ->
                _events.tryEmit(RequestEvent.Error(e))
            }
        }
    }

    /** Delete a request by its domain object (uses id). */
    fun deleteRequest(request: RequestDomain) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { requestRepository.deleteRequest(request) }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(RequestEvent.Deleted(request.id))
                refresh()
            }.onFailure { e ->
                _events.tryEmit(RequestEvent.Error(e))
            }
        }
    }

    /**
     * Update the fields of the request identified by [id].
     * Repository will recompute the new id (hash of keys) and replace it.
     */
    fun updateFields(id: String, fields: List<FieldDomain>) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { requestRepository.updateFields(id, fields) }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(RequestEvent.FieldsUpdated(id))
                refresh()
            }.onFailure { e ->
                _events.tryEmit(RequestEvent.Error(e))
            }
        }
    }
}

// UI events for RequestListViewModel
sealed interface RequestEvent {
    data object Saved : RequestEvent
    data object AlreadyExists : RequestEvent
    data class Deleted(val id: String) : RequestEvent
    data class FieldsUpdated(val id: String) : RequestEvent
    data class Error(val throwable: Throwable) : RequestEvent
}
