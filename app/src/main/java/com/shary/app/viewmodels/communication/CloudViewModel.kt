package com.shary.app.viewmodels.communication

import CloudEvent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val cloudService: CloudService,
    private val requestRepository: RequestRepository
) : ViewModel() {

    /** Loading state */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Cloud events */
    private val _events = MutableSharedFlow<CloudEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CloudEvent> = _events.asSharedFlow()

    /** Upload the user to the backend. */
    fun uploadUser(username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val token = runCatching {
                withContext(Dispatchers.IO) { cloudService.uploadUser(username) }
            }.getOrDefault("")
            _isLoading.value = false
            if (token.isNotBlank()) {
                _events.tryEmit(CloudEvent.UserUploaded(username, token))
            } else {
                _events.tryEmit(CloudEvent.Error(Exception("Upload failed")))
            }
        }
    }

    /** Upload data to the backend. */
    fun uploadData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        consumers: List<UserDomain>,
        isRequest: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val resultMap = runCatching {
                withContext(Dispatchers.IO) {
                    Log.d("CloudViewModel", "uploadData() - ownerUsername: ${owner.username}")
                    cloudService.uploadData(fields, owner, consumers, isRequest)
                }
            }.getOrDefault(emptyMap())
            _isLoading.value = false
            if (isRequest) {
                val request = RequestDomain(
                    fields = fields,
                    sender = owner,
                    recipients = consumers,
                    dateAdded = Instant.now()
                )
                runCatching {
                    withContext(Dispatchers.IO) {
                        requestRepository.saveSentRequest(request)
                    }
                }.onFailure { error ->
                    Log.e("CloudViewModel", "Failed to save sent request: ${error.message}")
                }
            }
            _events.tryEmit(CloudEvent.DataUploaded(resultMap))
        }
    }
}

