package com.shary.app.viewmodels

import CloudEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CloudServiceViewModel @Inject constructor(
    private val cloudService: CloudService
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<CloudEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CloudEvent> = _events.asSharedFlow()

    /** Ping the backend and emit result */
    fun sendPing() {
        viewModelScope.launch {
            _isLoading.value = true
            val ok = runCatching {
                withContext(Dispatchers.IO) { cloudService.sendPing() }
            }.getOrElse { false }
            _isLoading.value = false
            _events.tryEmit(CloudEvent.PingResult(ok))
        }
    }

    fun checkUserRegistered(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val registered = runCatching {
                withContext(Dispatchers.IO) { cloudService.isUserRegistered(email) }
            }.getOrElse { false }
            _isLoading.value = false
            _events.tryEmit(CloudEvent.UserRegisteredResult(email, registered))
        }
    }

    fun uploadUser(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val token = runCatching {
                withContext(Dispatchers.IO) { cloudService.uploadUser(email) }
            }.getOrDefault("")
            _isLoading.value = false
            if (token.isNotBlank()) {
                _events.tryEmit(CloudEvent.UserUploaded(email, token))
            } else {
                _events.tryEmit(CloudEvent.Error(Exception("Upload failed")))
            }
        }
    }

    fun deleteUser(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val deleted = runCatching {
                withContext(Dispatchers.IO) { cloudService.deleteUser(email) }
            }.getOrElse { false }
            _isLoading.value = false
            if (deleted) {
                _events.tryEmit(CloudEvent.UserDeleted(email))
            } else {
                _events.tryEmit(CloudEvent.Error(Exception("Delete failed")))
            }
        }
    }

    fun uploadData(
        fields: List<FieldDomain>,
        ownerEmail: String,
        consumers: List<String>,
        isRequest: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val resultMap = runCatching {
                withContext(Dispatchers.IO) {
                    cloudService.uploadData(fields, ownerEmail, consumers, isRequest)
                }
            }.getOrDefault(emptyMap())
            _isLoading.value = false
            _events.tryEmit(CloudEvent.DataUploaded(resultMap))
        }
    }

    fun getPubKey(userHash: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val pubKey = runCatching {
                withContext(Dispatchers.IO) { cloudService.getPubKey(userHash) }
            }.getOrNull()
            _isLoading.value = false
            if (!pubKey.isNullOrBlank()) {
                _events.tryEmit(CloudEvent.PubKeyFetched(userHash, pubKey))
            } else {
                _events.tryEmit(CloudEvent.Error(Exception("No pubkey found")))
            }
        }
    }
}


