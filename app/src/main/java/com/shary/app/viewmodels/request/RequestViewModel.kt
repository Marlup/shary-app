package com.shary.app.viewmodels.request

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.services.CloudService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

@HiltViewModel
class RequestViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val fieldRepository: FieldRepository,
    private val cloudService: CloudService
) : ViewModel() {

    private val _requests = MutableStateFlow<List<RequestDomain>>(emptyList())
    val requests: StateFlow<List<RequestDomain>> = _requests.asStateFlow()

    private val _events = MutableSharedFlow<RequestEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RequestEvent> = _events.asSharedFlow()

    fun refreshRequests() {
        viewModelScope.launch {
            requestRepository.getAllRequests().collect { requests ->
                _requests.value = requests
            }
        }
    }

    /**
     * Fetches request data from Firebase, processes it, and attempts to match with local fields.
     * If fields exist locally, creates a RequestDomain and saves it.
     */
    fun fetchRequestsFromCloud(username: String, currentUser: UserDomain) {
        viewModelScope.launch {
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
                        val requestedKeys = mutableListOf<String>()
                        for (i in 0 until keysJson.length()) {
                            requestedKeys.add(keysJson.getString(i))
                        }

                        // Get all local fields
                        val allFields = mutableListOf<FieldDomain>()
                        fieldRepository.getAllFields().collect { fields ->
                            allFields.addAll(fields)
                        }

                        // Match requested keys with local fields
                        val matchedFields = allFields.filter { field ->
                            requestedKeys.contains(field.key)
                        }

                        if (matchedFields.isEmpty()) {
                            throw IllegalStateException("No matching fields found locally")
                        }

                        // Create RequestDomain
                        val sender = UserDomain(username = senderUsername)
                        val request = RequestDomain(
                            fields = matchedFields,
                            sender = sender,
                            recipients = listOf(currentUser),
                            dateAdded = Instant.now()
                        )

                        // Save request
                        requestRepository.saveRequest(request)

                        matchedFields.size
                    }
                }
            }

            result.onSuccess { matchedCount ->
                _events.tryEmit(RequestEvent.FetchedFromCloud(matchedCount))
                refreshRequests()
            }.onFailure { e ->
                Log.e("RequestViewModel", "Error fetching requests from cloud: ${e.message}")
                _events.tryEmit(RequestEvent.FetchError(e))
            }
        }
    }
}
