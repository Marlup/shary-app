package com.shary.app.viewmodels.communication

import CloudEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val cloudService: CloudService
) : ViewModel() {

    /** Loading state */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Cloud events */
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

    /** Check if the user is registered in the backend. */
    fun checkUserRegistered(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val registered = runCatching {
                withContext(Dispatchers.IO) { cloudService.isUserRegisteredInCloud(email) }
            }.getOrElse { false }
            _isLoading.value = false
            _events.tryEmit(CloudEvent.UserRegisteredResult(email, registered))
        }
    }

    /** Upload the user to the backend. */
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

    /** Delete the user from the backend. */
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

    /** Upload data to the backend. */
    fun uploadData(
        fields: List<FieldDomain>,
        ownerEmail: String?,
        consumers: List<UserDomain>,
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

    /** Fetch the public key for the user. */
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

    /** Establish anonymous session in Firebase and emits the uuid. Idempotent. */
    fun ensureAnonymousSession() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = cloudService.ensureAnonymousSession()
            _isLoading.value = false

            res.onSuccess { uid ->
                _events.tryEmit(CloudEvent.AnonymousReady(uid))
            }.onFailure { e ->
                _events.tryEmit(CloudEvent.Error(e))
            }
        }
    }

    /** Refresh token ID and make it available (for example. in Session). */
    fun refreshCloudIdToken() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = cloudService.refreshIdToken()
            _isLoading.value = false

            res.onSuccess { token ->
                _events.tryEmit(CloudEvent.TokenRefreshed(token))
            }.onFailure { e ->
                _events.tryEmit(CloudEvent.Error(e))
            }
        }
    }

    /** Close the anonymous session (and clean the token session). */
    fun signOutCloud() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = cloudService.signOutCloud()
            _isLoading.value = false

            res.onSuccess {
                _events.tryEmit(CloudEvent.CloudSignedOut)
            }.onFailure { e ->
                _events.tryEmit(CloudEvent.Error(e))
            }
        }
    }
}


