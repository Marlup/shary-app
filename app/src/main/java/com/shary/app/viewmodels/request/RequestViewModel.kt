package com.shary.app.viewmodels.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.constants.CloudInboxPolicy
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.CloudPayloadDecision
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.core.domain.types.valueobjects.CloudInboxItem
import com.shary.app.core.domain.types.valueobjects.DataPayload
import com.shary.app.infrastructure.services.cloud.CloudRateLimitedException
import com.shary.app.utils.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val _isCloudInboxLoading = MutableStateFlow(false)
    val isCloudInboxLoading: StateFlow<Boolean> = _isCloudInboxLoading.asStateFlow()
    private val _requestInboxItems = MutableStateFlow<List<CloudInboxItem>>(emptyList())
    val requestInboxItems: StateFlow<List<CloudInboxItem>> = _requestInboxItems.asStateFlow()

    val receivedRequests: StateFlow<List<RequestDomain>> =
        requestRepository.getReceivedRequests()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sentRequests: StateFlow<List<RequestDomain>> =
        requestRepository.getSentRequests()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<RequestEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RequestEvent> = _events.asSharedFlow()
    private var decisionCooldownUntilEpochSeconds: Long = 0L

    // -------------------- Draft Request --------------------

    fun getCachedRequests(): List<RequestDomain> = cacheSelection.getRequests()
    fun getCachedDraftRequest(): RequestDomain = cacheSelection.getDraftRequest()
    fun anyRequestCached() = cacheSelection.isAnyRequestCached()
    //fun anyDraftRequestCached() = cacheSelection.isAnyDraftRequestCached()


    fun setListMode(mode: RequestListMode) {
        _listMode.value = mode
        if (mode == RequestListMode.SENT) {
            clearActiveReceivedRequest()
        }
    }

    // -------------------- Draft Fields --------------------

    fun getDraftFields(): List<FieldDomain> = cacheSelection.getDraftFields()
    fun anyDraftFieldCached() = cacheSelection.isAnyDraftFieldCached()
    fun toggleFieldSelection(field: FieldDomain) {
        _draftFields.update { current -> if (field in current) current - field else current + field }
    }

    fun addDraftField(field: FieldDomain) {
        AppLogger.debug("RequestViewModel", "event=add_draft_field")
        _draftFields.update { current ->
            val exists = current.any { it.key.equals(field.key, ignoreCase = true) }
            if (exists) current else current + field
        }
    }

    fun removeDraftFields() {
        if (_draftFields.value.isEmpty()) return
        val toRemove = _draftFields.value.toSet()
        _draftFields.update { current -> current.filterNot { it in toRemove } }
    }

    fun clearDraftFields() {
        _draftFields.value = emptyList()
    }

    fun restoreDraftFields(fields: List<FieldDomain>) {
        if (fields.isEmpty()) return
        setDraftFields(fields)
    }

    fun setActiveReceivedRequest(request: RequestDomain) {
        _draftRequest.value = request
        cacheSelection.cacheDraftRequest(request)
    }

    fun clearActiveReceivedRequest() {
        _draftRequest.value = RequestDomain.initialize()
        cacheSelection.clearCachedDraftRequest()
    }

    fun markActiveReceivedRequestAsResponded() {
        val selected = _draftRequest.value
        if (selected.fields.isEmpty() || selected.user.isBlank()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                requestRepository.markReceivedRequestResponded(selected, responded = true)
            }
            _draftRequest.value = selected.copy(responded = true)
            cacheSelection.cacheDraftRequest(_draftRequest.value)
            clearActiveReceivedRequest()
        }
    }

    fun updateDraftRequest() {
        AppLogger.debug("RequestViewModel", "event=update_draft_request_start")
        if (draftFields.value.isNotEmpty()){
            cacheSelection.cacheDraftRequest(
                RequestDomain.initialize().copy(fields = draftFields.value)
            )
            AppLogger.debug("RequestViewModel", "event=update_draft_request_success count=${_draftFields.value.size}")
        }
    }

    fun setDraftFields() {
        setDraftFields(_draftFields.value)
    }

    fun setDraftFields(fields: List<FieldDomain>) {
        AppLogger.debug("RequestViewModel", "event=set_draft_fields_start")
        val draftFields = fields.distinctBy { it.key.lowercase() }
        _draftFields.value = draftFields
        AppLogger.debug("RequestViewModel", "event=set_draft_fields_success count=${_draftFields.value.size}")
        cacheSelection.cacheDraftFields(draftFields)
    }

    /**
     * Fetches request data from Firebase and saves it as a received request.
     */
    fun fetchRequestsFromCloud(targetUser: UserDomain) {
        AppLogger.info("RequestViewModel", "event=fetch_requests_start")

        viewModelScope.launch {
            _isLoading.value = true

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val requestsData = cloudService.fetchRequestDataFromEmail(targetUser.email)
                    importDecryptedRequestJsons(
                        jsonStrings = requestsData.getOrThrow(),
                        targetUserEmail = targetUser.email
                    )
                }
            }

            _isLoading.value = false

            result.onSuccess { totalMatchedFields ->
                _events.tryEmit(RequestEvent.FetchedFromCloud(totalMatchedFields))
            }.onFailure { e ->
                AppLogger.error("RequestViewModel", "event=fetch_requests_failed", e)
                _events.tryEmit(RequestEvent.FetchError(e))
            }
        }
    }

    /**
     * Fetches pending encrypted request payloads and keeps them in-memory for user review.
     */
    fun loadRequestInbox(email: String) {
        viewModelScope.launch {
            _isCloudInboxLoading.value = true
            _requestInboxItems.value = emptyList()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val payloads = cloudService.fetchRequestInboxFromEmail(email).getOrThrow()
                    payloads
                        .sortedByDescending { it.creationAt }
                        .map { payload ->
                            val previewKeys = if (CloudInboxPolicy.ENABLE_FIELD_NAME_PREVIEW) {
                                cloudService.decryptRequestFromInbox(email, payload)
                                    .getOrNull()
                                    ?.let(::extractPreviewRequestKeys)
                                    .orEmpty()
                            } else {
                                emptyList()
                            }
                            CloudInboxItem(
                                payload = payload,
                                previewFieldNames = previewKeys
                            )
                        }
                }
            }
            _isCloudInboxLoading.value = false

            result.onSuccess { inbox ->
                _requestInboxItems.value = inbox
                if (inbox.isEmpty()) {
                    _events.tryEmit(RequestEvent.CloudInboxEmpty)
                } else {
                    _events.tryEmit(RequestEvent.CloudInboxLoaded(inbox.size))
                }
            }.onFailure { error ->
                _events.tryEmit(RequestEvent.FetchError(error))
            }
        }
    }

    fun acceptRequestInboxItem(email: String, item: CloudInboxItem) {
        viewModelScope.launch {
            _isCloudInboxLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    enforceDecisionCooldownOrThrow()
                    val decryptedJson = cloudService.decryptRequestFromInbox(email, item.payload).getOrThrow()
                    val importedKeyCount = importDecryptedRequestJsons(
                        jsonStrings = listOf(decryptedJson),
                        targetUserEmail = email
                    )
                    val backendAcknowledged = cloudService.sendRequestDecision(
                        email = email,
                        payload = item.payload,
                        decision = CloudPayloadDecision.ACCEPT,
                        notifySender = false
                    ).getOrThrow()
                    importedKeyCount to backendAcknowledged
                }
            }
            _isCloudInboxLoading.value = false

            result.onSuccess { (importedKeyCount, backendAcknowledged) ->
                removeRequestInboxItem(item.payload)
                _events.tryEmit(
                    RequestEvent.CloudInboxAccepted(
                        importedKeyCount = importedKeyCount,
                        backendAcknowledged = backendAcknowledged
                    )
                )
            }.onFailure { error ->
                captureDecisionCooldown(error)
                _events.tryEmit(RequestEvent.FetchError(error))
            }
        }
    }

    fun rejectRequestInboxItem(email: String, item: CloudInboxItem) {
        viewModelScope.launch {
            _isCloudInboxLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    enforceDecisionCooldownOrThrow()
                    cloudService.sendRequestDecision(
                        email = email,
                        payload = item.payload,
                        decision = CloudPayloadDecision.REJECT,
                        notifySender = CloudInboxPolicy.ENABLE_REJECT_NOTIFY_SENDER
                    ).getOrThrow()
                }
            }
            _isCloudInboxLoading.value = false

            result.onSuccess { backendAcknowledged ->
                removeRequestInboxItem(item.payload)
                _events.tryEmit(RequestEvent.CloudInboxRejected(backendAcknowledged))
            }.onFailure { error ->
                captureDecisionCooldown(error)
                _events.tryEmit(RequestEvent.FetchError(error))
            }
        }
    }

    private fun enforceDecisionCooldownOrThrow() {
        val remaining = remainingDecisionCooldownSeconds()
        if (remaining > 0) {
            throw CloudRateLimitedException(
                retryAfterSeconds = remaining,
                message = "Decision cooldown is still active."
            )
        }
    }

    private fun captureDecisionCooldown(error: Throwable) {
        val rateLimited = error as? CloudRateLimitedException ?: return
        val retryAfter = rateLimited.retryAfterSeconds ?: return
        if (retryAfter <= 0) return
        val cooldownUntil = Instant.now().epochSecond + retryAfter
        if (cooldownUntil > decisionCooldownUntilEpochSeconds) {
            decisionCooldownUntilEpochSeconds = cooldownUntil
        }
    }

    private fun remainingDecisionCooldownSeconds(): Long {
        val now = Instant.now().epochSecond
        val remaining = decisionCooldownUntilEpochSeconds - now
        return if (remaining > 0) remaining else 0
    }

    private suspend fun importDecryptedRequestJsons(
        jsonStrings: List<String>,
        targetUserEmail: String
    ): Int {
        if (jsonStrings.isEmpty()) return 0

        var totalMatchedFields = 0
        jsonStrings.forEachIndexed { idx, jsonString ->
            AppLogger.debug("RequestViewModel", "event=fetch_request_item index=$idx")
            val request = parseRequestFromJson(
                jsonString = jsonString,
                targetUserEmail = targetUserEmail,
                sourceIndex = idx
            )
            requestRepository.saveReceivedRequest(request)
            totalMatchedFields += request.fields.size
        }
        return totalMatchedFields
    }

    private fun parseRequestFromJson(
        jsonString: String,
        targetUserEmail: String,
        sourceIndex: Int
    ): RequestDomain {
        val jsonObject = JSONObject(jsonString)

        val userEmail = jsonObject.optString("user", "").trim()
        if (userEmail.isBlank()) {
            throw IllegalStateException("Request[$sourceIndex] has empty 'user'")
        }

        val keysJson = jsonObject.optJSONArray("keys")
        if (keysJson == null || keysJson.length() == 0) {
            throw IllegalStateException("Request[$sourceIndex] has no 'keys'")
        }

        val requestedFields = MutableList(keysJson.length()) { i ->
            FieldDomain.initialize().copy(key = keysJson.getString(i))
        }
        AppLogger.debug(
            "RequestViewModel",
            "event=fetch_request_keys index=$sourceIndex count=${requestedFields.size}"
        )

        return RequestDomain(
            fields = requestedFields,
            user = userEmail,
            recipients = listOf(targetUserEmail),
            dateAdded = Instant.now(),
            owned = false,
            responded = false
        )
    }

    private fun removeRequestInboxItem(payload: DataPayload) {
        _requestInboxItems.update { current ->
            current.filterNot { item ->
                item.payload.verification == payload.verification &&
                    item.payload.user == payload.user &&
                    item.payload.recipient == payload.recipient
            }
        }
    }

    private fun extractPreviewRequestKeys(jsonString: String): List<String> = runCatching {
        val jsonObject = JSONObject(jsonString)
        val keysJson = jsonObject.optJSONArray("keys") ?: return@runCatching emptyList()
        buildList {
            repeat(keysJson.length()) { idx ->
                val key = keysJson.optString(idx).trim()
                if (key.isNotBlank()) add(key)
            }
        }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }.take(12)
    }.getOrDefault(emptyList())
}
