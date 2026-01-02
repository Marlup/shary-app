package com.shary.app.viewmodels.request

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.ui.screens.request.RequestsScreen
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
    private val cacheSelection: CacheService,
    private val cloudService: CloudService
) : ViewModel() {

    private val _listMode = MutableStateFlow(RequestListMode.RECEIVED)
    val listMode: StateFlow<RequestListMode> = _listMode.asStateFlow()

    private val _draftFields = MutableStateFlow<List<FieldDomain>>(emptyList())
    private val _draftRequest = MutableStateFlow(RequestDomain.initialize())
    val draftFields: StateFlow<List<FieldDomain>> = _draftFields.asStateFlow()
    val draftRequest: StateFlow<RequestDomain> = _draftRequest.asStateFlow()

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

    // -------------------- Draft Request --------------------

    fun getCachedRequests(): List<RequestDomain> = cacheSelection.getRequests()
    fun getCachedDraftRequest(): RequestDomain = cacheSelection.getDraftRequest()
    fun anyRequestCached() = cacheSelection.isAnyRequestCached()
    //fun anyDraftRequestCached() = cacheSelection.isAnyDraftRequestCached()


    fun setListMode(mode: RequestListMode) {
        _listMode.value = mode
    }

    // -------------------- Draft Fields --------------------

    fun getDraftFields(): List<FieldDomain> = cacheSelection.getDraftFields()
    fun anyDraftFieldCached() = cacheSelection.isAnyDraftFieldCached()

    fun addDraftField(field: FieldDomain) {
        Log.d("RequestViewModel", "[3] Adding draft field: $field")
        _draftFields.update { current ->
            val exists = current.any { it.key.equals(field.key, ignoreCase = true) }
            if (exists) current else current + field
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

    fun updateDraftRequest() {
        Log.d("RequestViewModel", "[3] Before Adding draft request: ${_draftFields.value}")
        if (draftFields.value.isNotEmpty()){
            cacheSelection.cacheDraftRequest(
                RequestDomain.initialize().copy(fields = draftFields.value)
            )
            Log.d("RequestViewModel", "[4] After Adding draft request: ${_draftFields.value}")
        }
    }

    fun setDraftFields() {
        setDraftFields(_draftFields.value)
    }

    fun setDraftFields(fields: List<FieldDomain>) {
        Log.d("RequestViewModel", "[3] Before Updating draft request: ${_draftFields.value}")
        val draftFields = fields.distinctBy { it.key.lowercase() }
        _draftFields.value = draftFields
        Log.d("RequestViewModel", "[4] After Updating draft fields: ${_draftFields.value}")
        cacheSelection.cacheDraftFields(draftFields)
    }

    /**
     * Fetches request data from Firebase and saves it as a received request.
     */
    fun fetchRequestsFromCloud(username: String, currentUser: UserDomain) {
        Log.d("RequestViewModel", "[3] Fetching requests from cloud for user: $username")
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val requestData = cloudService.fetchRequestData(username)

                    requestData.getOrThrow().let { jsonString ->
                        Log.d("RequestViewModel", "Fetched request data: $jsonString")
                        val jsonObject = JSONObject(jsonString)

                        // Extract user
                        val username = jsonObject.optString("user", "")
                        // Extract keys
                        val keysJson = jsonObject.optJSONArray("keys")

                        Log.d("RequestViewModel", "[4] Fetched keys: $keysJson")
                        if (keysJson == null || keysJson.length() == 0) {
                            throw IllegalStateException("No keys found in request")
                        }
                        // Extract keys
                        val requestedFields = mutableListOf<FieldDomain>()
                        for (i in 0 until keysJson.length()) {
                            requestedFields.add(
                                FieldDomain.initialize().copy(key = keysJson.getString(i))
                            )
                        }
                        Log.d("RequestViewModel", "[5] Fetched keys: $requestedFields")

                        // Create RequestDomain
                        val user = UserDomain(username = username)
                        val request = RequestDomain(
                            fields = requestedFields,
                            user = user,
                            recipients = listOf(currentUser),
                            dateAdded = Instant.now(),
                            owned = false,
                            responded = false
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
