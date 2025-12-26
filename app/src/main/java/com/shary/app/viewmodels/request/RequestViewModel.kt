package com.shary.app.viewmodels.request

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

@HiltViewModel
class RequestViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val cloudService: CloudService
) : ViewModel() {

    enum class RequestListMode {
        RECEIVED,
        SENT
    }

    private val _listMode = MutableStateFlow(RequestListMode.RECEIVED)
    val listMode: StateFlow<RequestListMode> = _listMode.asStateFlow()

    private val _draftFields = MutableStateFlow<List<FieldDomain>>(emptyList())
    val draftFields: StateFlow<List<FieldDomain>> = _draftFields.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val receivedRequests: StateFlow<List<RequestDomain>> =
        requestRepository.getReceivedRequests()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sentRequests: StateFlow<List<RequestDomain>> =
        requestRepository.getSentRequests()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<RequestEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RequestEvent> = _events.asSharedFlow()

    fun setListMode(mode: RequestListMode) {
        _listMode.value = mode
    }

    fun addDraftField(field: FieldDomain) {
        _draftFields.update { current ->
            val trimmed = field.copy(key = field.key.trim(), keyAlias = field.keyAlias.trim())
            val exists = current.any { it.key.equals(trimmed.key, ignoreCase = true) }
            if (exists) current else current + trimmed
        }
    }

    fun removeDraftFields(fields: List<FieldDomain>) {
        if (fields.isEmpty()) return
        val toRemove = fields.toSet()
        _draftFields.update { current -> current.filterNot { it in toRemove } }
    }

    fun clearDraftFields() {
        _draftFields.value = emptyList()
    }

    /**
     * Fetches request data from Firebase and saves it as a received request.
     */
    fun fetchRequestsFromCloud(username: String, currentUser: UserDomain) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val fetchResult = cloudService.fetchData(username)

                    fetchResult.getOrThrow().let { jsonString ->
                        Log.d("RequestViewModel", "Fetched request data: $jsonString")
                        val jsonObject = JSONObject(jsonString)

                        // Check if it's a request by looking for "mode" field
                        val mode = jsonObject.optString("mode", "")
                        if (mode != "request") {
                            throw IllegalStateException("Fetched data is not a request")
                        }

                        val senderUsername = jsonObject.optString("sender", "")
                        val keysJson = jsonObject.optJSONArray("keys")

                        if (keysJson == null || keysJson.length() == 0) {
                            throw IllegalStateException("No keys found in request")
                        }

                        // Extract requested keys
                        val requestedFields = mutableListOf<FieldDomain>()
                        for (i in 0 until keysJson.length()) {
                            val key = keysJson.getString(i)
                            requestedFields.add(
                                FieldDomain(
                                    key = key,
                                    keyAlias = "",
                                    value = "",
                                    tag = com.shary.app.core.domain.types.enums.Tag.Unknown,
                                    dateAdded = Instant.now()
                                )
                            )
                        }

                        // Create RequestDomain
                        val sender = UserDomain(username = senderUsername)
                        val request = RequestDomain(
                            fields = requestedFields,
                            sender = sender,
                            recipients = listOf(currentUser),
                            dateAdded = Instant.now()
                        )

                        // Save request
                        requestRepository.saveReceivedRequest(request)

                        requestedFields.size
                    }
                }
            }

            _isLoading.value = false
            result.onSuccess { matchedCount ->
                _events.tryEmit(RequestEvent.FetchedFromCloud(matchedCount))
            }.onFailure { e ->
                Log.e("RequestViewModel", "Error fetching requests from cloud: ${e.message}")
                _events.tryEmit(RequestEvent.FetchError(e))
            }
        }
    }
}
