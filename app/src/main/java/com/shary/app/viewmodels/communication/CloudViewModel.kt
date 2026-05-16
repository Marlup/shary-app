package com.shary.app.viewmodels.communication

import CloudEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.constants.CloudInboxPolicy
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.utils.log.AppLogger
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

    val cloudState = cloudService.cloudState

    fun refreshOnlineStatus() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { cloudService.sendPing() }
            }
        }
    }

    /** Loading state */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Cloud events */
    private val _events = MutableSharedFlow<CloudEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CloudEvent> = _events.asSharedFlow()

    /** Upload the user to the backend. */
    fun uploadUser(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) { cloudService.uploadUser(email) }
            }
            _isLoading.value = false
            result.onSuccess { token ->
                if (token.isNotBlank()) {
                    _events.tryEmit(CloudEvent.UserUploaded(email, token))
                } else {
                    _events.tryEmit(CloudEvent.Error(IllegalStateException("Identity registration returned empty token.")))
                }
            }.onFailure { error ->
                _events.tryEmit(CloudEvent.Error(error))
            }
        }
    }

    /** Upload data to the backend. */
    fun uploadData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int = CloudInboxPolicy.DEFAULT_EXPIRY_DAYS,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    cloudService.uploadData(fields, owner, recipients, expiryDays)
                }
            }
            _isLoading.value = false
            result.onSuccess { resultMap ->
                if (resultMap.isEmpty()) {
                    _events.tryEmit(CloudEvent.Error(IllegalStateException("No backend acknowledgement received.")))
                } else {
                    _events.tryEmit(CloudEvent.DataUploaded(resultMap))
                }
            }.onFailure { throwable ->
                _events.tryEmit(CloudEvent.Error(throwable))
            }
        }
    }

    /** Upload request to the backend. */
    fun uploadRequest(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int = CloudInboxPolicy.DEFAULT_EXPIRY_DAYS,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    cloudService.uploadRequest(fields, owner, recipients, expiryDays)
                }
            }
            _isLoading.value = false
            result.onSuccess { resultMap ->
                if (resultMap.isEmpty()) {
                    _events.tryEmit(CloudEvent.Error(IllegalStateException("No backend acknowledgement received.")))
                    return@onSuccess
                }

                val request = RequestDomain(
                    fields = fields,
                    user = owner.email,
                    recipients = recipients.map { it.email },
                    dateAdded = Instant.now(),
                    owned = true,
                    responded = false
                )
                runCatching {
                    withContext(Dispatchers.IO) {
                        requestRepository.saveSentRequest(request)
                    }
                }.onFailure { error ->
                    AppLogger.error("CloudViewModel", "event=save_sent_request_failed", error)
                }

                _events.tryEmit(CloudEvent.RequestUploaded(resultMap))
            }.onFailure { throwable ->
                _events.tryEmit(CloudEvent.Error(throwable))
            }
        }
    }
}
