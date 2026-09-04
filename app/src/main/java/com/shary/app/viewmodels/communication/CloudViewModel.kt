package com.shary.app.viewmodels.communication

import CloudEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.application.cloud.CloudDeliveryAttempt
import com.shary.app.application.cloud.CloudDeliveryController
import com.shary.app.application.cloud.CloudDeliveryKind
import com.shary.app.application.cloud.CloudDeliveryState
import com.shary.app.application.cloud.CloudDeliverySummary
import com.shary.app.application.cloud.toCloudDeliveryReceipt
import com.shary.app.application.cloud.toPersistedRequest
import com.shary.app.application.cloud.toRequestDeliveryOutcomes
import com.shary.app.core.constants.CloudInboxPolicy
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.canonicalCloudRequestId
import com.shary.app.utils.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val cloudService: CloudService,
    private val requestRepository: RequestRepository
) : ViewModel() {

    val cloudState = cloudService.cloudState

    private val deliveryController = CloudDeliveryController()
    val deliveryState: StateFlow<CloudDeliveryState> = deliveryController.state

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
        responseRequestId: String? = null,
    ) {
        val stableResponseRequestId = responseRequestId?.let(::canonicalRequestIdOrReport)
        if (responseRequestId != null && stableResponseRequestId == null) return
        val attempt = deliveryController.begin(
            kind = CloudDeliveryKind.DATA,
            recipients = recipients.map(UserDomain::email),
            transactionId = stableResponseRequestId
        ) ?: return
        launchDataAttempt(attempt, fields, owner, recipients, expiryDays, stableResponseRequestId)
    }

    fun retryFailedData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int = CloudInboxPolicy.DEFAULT_EXPIRY_DAYS,
        responseRequestId: String? = null,
    ) {
        val stableResponseRequestId = responseRequestId?.let(::canonicalRequestIdOrReport)
        if (responseRequestId != null && stableResponseRequestId == null) return
        val attempt = deliveryController.retryFailed(
            CloudDeliveryKind.DATA,
            transactionId = stableResponseRequestId
        ) ?: return
        launchDataAttempt(attempt, fields, owner, recipients, expiryDays, stableResponseRequestId)
    }

    private fun launchDataAttempt(
        attempt: CloudDeliveryAttempt,
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int,
        responseRequestId: String?
    ) {
        val attemptedRecipients = recipients.forAttempt(attempt)
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        cloudService.uploadData(fields, owner, attemptedRecipients, expiryDays)
                    }
                }
                result.onSuccess { resultMap ->
                    val receipt = deliveryController.complete(attempt, resultMap)
                    if (responseRequestId != null && receipt != null) {
                        saveResponseDeliveryHistory(responseRequestId, receipt)
                    }
                    if (resultMap.isEmpty()) {
                        _events.tryEmit(
                            CloudEvent.Error(IllegalStateException("No backend acknowledgement received."))
                        )
                    } else {
                        _events.tryEmit(CloudEvent.DataUploaded(resultMap))
                    }
                }.onFailure { throwable ->
                    val receipt = deliveryController.fail(attempt)
                    if (responseRequestId != null && receipt != null) {
                        saveResponseDeliveryHistory(responseRequestId, receipt)
                    }
                    _events.tryEmit(CloudEvent.Error(throwable))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Upload request to the backend. */
    fun uploadRequest(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        requestId: String,
        expiryDays: Int = CloudInboxPolicy.DEFAULT_EXPIRY_DAYS,
    ) {
        val stableRequestId = canonicalRequestIdOrReport(requestId) ?: return
        val attempt = deliveryController.begin(
            kind = CloudDeliveryKind.REQUEST,
            recipients = recipients.map(UserDomain::email),
            transactionId = stableRequestId
        ) ?: return
        launchRequestAttempt(attempt, fields, owner, recipients, stableRequestId, expiryDays)
    }

    fun retryFailedRequest(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        requestId: String,
        expiryDays: Int = CloudInboxPolicy.DEFAULT_EXPIRY_DAYS,
    ) {
        val stableRequestId = canonicalRequestIdOrReport(requestId) ?: return
        val attempt = deliveryController.retryFailed(
            kind = CloudDeliveryKind.REQUEST,
            transactionId = stableRequestId
        ) ?: return
        launchRequestAttempt(attempt, fields, owner, recipients, stableRequestId, expiryDays)
    }

    fun restoreRequestDelivery(requestId: String) {
        val stableRequestId = canonicalRequestIdOrReport(requestId) ?: return
        if (deliveryController.state.value != CloudDeliveryState.Review) return

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    requestRepository.findSentRequest(stableRequestId)
                }
            }.onSuccess { request ->
                request?.toCloudDeliveryReceipt()?.let(deliveryController::restore)
            }.onFailure { error ->
                AppLogger.error("CloudViewModel", "event=restore_request_delivery_failed", error)
                _events.emit(
                    CloudEvent.Error(
                        IllegalStateException("Saved delivery receipt could not be loaded.", error)
                    )
                )
            }
        }
    }

    fun restoreResponseDelivery(requestId: String) {
        val stableRequestId = canonicalRequestIdOrReport(requestId) ?: return
        if (deliveryController.state.value != CloudDeliveryState.Review) return

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    requestRepository.findReceivedRequest(stableRequestId)
                }
            }.onSuccess { request ->
                request?.toCloudDeliveryReceipt(CloudDeliveryKind.DATA)
                    ?.let(deliveryController::restore)
            }.onFailure { error ->
                AppLogger.error("CloudViewModel", "event=restore_response_delivery_failed", error)
                _events.emit(
                    CloudEvent.Error(
                        IllegalStateException("Saved response receipt could not be loaded.", error)
                    )
                )
            }
        }
    }

    fun resetDeliveryState(): Boolean {
        val reset = deliveryController.reset()
        if (reset) _isLoading.value = false
        return reset
    }

    private fun launchRequestAttempt(
        attempt: CloudDeliveryAttempt,
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        stableRequestId: String,
        expiryDays: Int
    ) {
        val attemptedRecipients = recipients.forAttempt(attempt)
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        cloudService.uploadRequest(
                            fields = fields,
                            owner = owner,
                            recipients = attemptedRecipients,
                            requestId = stableRequestId,
                            expiryDays = expiryDays
                        )
                    }
                }
                result.onSuccess { resultMap ->
                    val receipt = deliveryController.complete(attempt, resultMap)
                    receipt?.let { saveRequestDeliveryHistory(it, fields, owner) }
                    if (resultMap.isEmpty()) {
                        _events.tryEmit(
                            CloudEvent.Error(IllegalStateException("No backend acknowledgement received."))
                        )
                        return@onSuccess
                    }

                    _events.emit(CloudEvent.RequestUploaded(resultMap))
                }.onFailure { throwable ->
                    deliveryController.fail(attempt)?.let { receipt ->
                        saveRequestDeliveryHistory(receipt, fields, owner)
                    }
                    _events.tryEmit(CloudEvent.Error(throwable))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun saveRequestDeliveryHistory(
        receipt: com.shary.app.application.cloud.CloudDeliveryReceipt,
        fields: List<FieldDomain>,
        owner: UserDomain
    ) {
        val request = receipt.toPersistedRequest(
            fields = fields,
            ownerEmail = owner.email,
            updatedAt = Instant.now()
        ) ?: return
        runCatching {
            withContext(Dispatchers.IO) {
                requestRepository.saveSentRequest(request)
            }
        }.onFailure { error ->
            AppLogger.error("CloudViewModel", "event=save_sent_request_failed", error)
            _events.emit(
                CloudEvent.Error(
                    IllegalStateException(
                        "The delivery result is visible, but its history could not be saved.",
                        error
                    )
                )
            )
        }
    }

    private suspend fun saveResponseDeliveryHistory(
        requestId: String,
        receipt: com.shary.app.application.cloud.CloudDeliveryReceipt
    ) {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                requestRepository.updateReceivedRequestDelivery(
                    requestId = requestId,
                    channel = RequestDeliveryChannel.CLOUD,
                    outcomes = receipt.toRequestDeliveryOutcomes(),
                    updatedAt = Instant.now()
                )
            }
        }
        result.onSuccess { updated ->
            if (!updated) {
                _events.emit(
                    CloudEvent.Error(
                        IllegalStateException(
                            "The response result is visible, but its Request history was not found."
                        )
                    )
                )
            }
        }.onFailure { error ->
            AppLogger.error("CloudViewModel", "event=save_response_delivery_failed", error)
            _events.emit(
                CloudEvent.Error(
                    IllegalStateException(
                        "The response result is visible, but its history could not be saved.",
                        error
                    )
                )
            )
        }
    }

    private fun canonicalRequestIdOrReport(requestId: String): String? {
        return canonicalCloudRequestId(requestId) ?: run {
            _events.tryEmit(
                CloudEvent.Error(IllegalStateException("Outgoing request identity is unavailable."))
            )
            null
        }
    }
}

private fun List<UserDomain>.forAttempt(attempt: CloudDeliveryAttempt): List<UserDomain> {
    val usersByAddress = linkedMapOf<String, UserDomain>()
    forEach { user ->
        val normalizedAddress = user.email.trim().lowercase(Locale.ROOT)
        if (normalizedAddress.isNotEmpty()) usersByAddress.putIfAbsent(normalizedAddress, user)
    }
    return attempt.attemptedRecipients.mapNotNull { address ->
        usersByAddress[address.trim().lowercase(Locale.ROOT)]
    }
}
