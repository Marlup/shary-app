package com.shary.app.viewmodels.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.application.request.RequestInboxDecision
import com.shary.app.application.request.RequestInboxDecisionController
import com.shary.app.application.request.RequestInboxDecisionReceipt
import com.shary.app.application.request.RequestInboxDecisionState
import com.shary.app.application.request.RequestDraftFieldAddOutcome
import com.shary.app.application.request.RequestWorkflowAction
import com.shary.app.application.request.addRequestedFieldToDraft
import com.shary.app.application.request.reduceRequestWorkflow
import com.shary.app.application.request.requireAuthenticatedRequester
import com.shary.app.application.request.normalizeRequestedFieldKeys
import com.shary.app.core.constants.CloudInboxPolicy
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.RequestedFieldDraft
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.models.hasPersistableDraftIdentity
import com.shary.app.core.domain.models.canonicalCloudRequestId
import com.shary.app.core.domain.models.stableImportedRequestId
import com.shary.app.core.domain.types.enums.CloudPayloadDecision
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.core.domain.types.valueobjects.CloudInboxItem
import com.shary.app.core.domain.types.valueobjects.DataPayload
import com.shary.app.infrastructure.services.cloud.CloudRateLimitedException
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessageB64
import com.shary.app.utils.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

@HiltViewModel
class RequestViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val cacheSelection: CacheService,
    private val cloudService: CloudService
) : ViewModel() {

    private val workflowPersistence = Channel<WorkflowPersistence>(Channel.UNLIMITED)

    val requestWorkflow: StateFlow<RequestWorkflowState> =
        cacheSelection.accountState
            .map { it.requestWorkflow }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                cacheSelection.getRequestWorkflow()
            )

    val listMode: StateFlow<RequestListMode> = requestWorkflow
        .map { it.mode }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            cacheSelection.getRequestWorkflow().mode
        )

    val draftFields: StateFlow<List<FieldDomain>> = requestWorkflow
        .map(RequestWorkflowState::visibleDraftFields)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            cacheSelection.getRequestWorkflow().visibleDraftFields()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isWorkflowRestoreComplete = MutableStateFlow(false)
    val isWorkflowRestoreComplete: StateFlow<Boolean> =
        _isWorkflowRestoreComplete.asStateFlow()
    private val _isCloudInboxLoading = MutableStateFlow(false)
    val isCloudInboxLoading: StateFlow<Boolean> = _isCloudInboxLoading.asStateFlow()
    private val _requestInboxItems = MutableStateFlow<List<CloudInboxItem>>(emptyList())
    val requestInboxItems: StateFlow<List<CloudInboxItem>> = _requestInboxItems.asStateFlow()
    private val requestInboxDecisionController = RequestInboxDecisionController()
    val requestInboxDecisionState: StateFlow<RequestInboxDecisionState> =
        requestInboxDecisionController.state

    val receivedRequests: StateFlow<List<RequestDomain>> =
        requestRepository.getReceivedRequests()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sentRequests: StateFlow<List<RequestDomain>> =
        requestRepository.getSentRequests()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<RequestEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RequestEvent> = _events.asSharedFlow()
    private var decisionCooldownUntilEpochSeconds: Long = 0L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (command in workflowPersistence) {
                val result = runCatching {
                    requestRepository.saveWorkflowDraft(command.ownerAddress, command.workflow)
                }.onFailure { error ->
                    AppLogger.error("RequestViewModel", "event=persist_workflow_draft_failed", error)
                }
                command.completion?.complete(result.isSuccess)
            }
        }
        viewModelScope.launch {
            cacheSelection.accountState
                .map { it.owner.email.trim().lowercase() }
                .distinctUntilChanged()
                .collectLatest { ownerAddress ->
                    _isWorkflowRestoreComplete.value = false
                    if (ownerAddress.isNotBlank()) {
                        restorePersistedWorkflow(ownerAddress)
                        _isWorkflowRestoreComplete.value = true
                    }
                }
        }
    }

    private fun dispatch(action: RequestWorkflowAction): RequestWorkflowState {
        val updated = cacheSelection.updateRequestWorkflow { current ->
            reduceRequestWorkflow(current, action)
        }
        queueWorkflowPersistence(updated)
        return updated
    }

    // -------------------- Draft Request --------------------

    fun getCachedRequests(): List<RequestDomain> = cacheSelection.getRequests()
    fun getCachedDraftRequest(): RequestDomain = cacheSelection.getDraftRequest()
    fun anyRequestCached() = cacheSelection.isAnyRequestCached()
    //fun anyDraftRequestCached() = cacheSelection.isAnyDraftRequestCached()


    fun setListMode(mode: RequestListMode) {
        dispatch(RequestWorkflowAction.ChangeMode(mode))
    }

    // -------------------- Draft Fields --------------------

    fun getDraftFields(): List<FieldDomain> =
        cacheSelection.getRequestWorkflow().outgoingRequestDraft.requestedFields
            .map(RequestedFieldDraft::toFieldDomain)

    fun anyDraftFieldCached(): Boolean =
        cacheSelection.getRequestWorkflow().outgoingRequestDraft.requestedFields.isNotEmpty()

    suspend fun setOutgoingRecipients(users: List<UserDomain>): String? {
        val current = cacheSelection.getRequestWorkflow()
        val updated = reduceRequestWorkflow(
            current,
            RequestWorkflowAction.SetOutgoingRecipients(
                recipientAddresses = users.map(UserDomain::email),
                candidateRequestId = UUID.randomUUID().toString()
            )
        )
        val requestId = updated.outgoingRequestDraft
            .takeIf { it.requestedFields.isNotEmpty() && it.recipientAddresses.isNotEmpty() }
            ?.requestId
            ?.takeIf(String::isNotBlank)
            ?: return null
        if (!persistWorkflowNow(updated)) return null
        cacheSelection.cacheRequestWorkflow(updated)
        return requestId
    }

    fun toggleFieldSelection(field: FieldDomain) {
        val current = getDraftFields()
        val updated = if (current.any { it.key.equals(field.key, ignoreCase = true) }) {
            current.filterNot { it.key.equals(field.key, ignoreCase = true) }
        } else {
            current + field
        }
        setDraftFields(updated)
    }

    fun addDraftField(field: FieldDomain): RequestDraftFieldAddOutcome {
        AppLogger.debug("RequestViewModel", "event=add_draft_field")
        val transition = addRequestedFieldToDraft(
            current = cacheSelection.getRequestWorkflow().outgoingRequestDraft.requestedFields,
            candidate = RequestedFieldDraft(
                key = field.key,
                keyAlias = field.keyAlias
            )
        )
        if (transition.outcome == RequestDraftFieldAddOutcome.ADDED) {
            setDraftFields(transition.requestedFields.map(RequestedFieldDraft::toFieldDomain))
        }
        return transition.outcome
    }

    fun removeDraftFields() {
        dispatch(RequestWorkflowAction.ClearOutgoingDraft)
    }

    fun clearDraftFields() {
        when (cacheSelection.getRequestWorkflow().mode) {
            RequestListMode.RECEIVED -> dispatch(RequestWorkflowAction.ClearReceivedWorkflow)
            RequestListMode.SENT -> dispatch(RequestWorkflowAction.ClearOutgoingDraft)
        }
    }

    fun restoreDraftFields(fields: List<FieldDomain>) {
        if (fields.isEmpty()) return
        setDraftFields(fields)
    }

    fun setActiveReceivedRequest(request: RequestDomain): Boolean {
        val updated = dispatch(RequestWorkflowAction.SelectReceivedRequest(request))
        return updated.activeReceivedRequest?.id == request.id.trim()
    }

    fun clearActiveReceivedRequest() {
        dispatch(RequestWorkflowAction.ClearReceivedWorkflow)
    }

    suspend fun prepareResponse(fields: List<FieldDomain>): String? {
        if (fields.isEmpty()) return null
        val current = cacheSelection.getRequestWorkflow()
        val response = current.responseDraft ?: return null
        val updated = reduceRequestWorkflow(
            current,
            RequestWorkflowAction.SetResponseMatches(
                requestId = response.requestId,
                matchedFieldKeys = fields.map { it.key }
            )
        )
        val requestId = updated.responseDraft
            ?.takeIf { it.matchedFieldKeys.isNotEmpty() }
            ?.requestId
            ?: return null
        if (!persistWorkflowNow(updated)) return null
        cacheSelection.cacheRequestWorkflow(updated)
        return requestId
    }

    fun markActiveReceivedRequestAsResponded(requestId: String) {
        val workflow = cacheSelection.getRequestWorkflow()
        val selected = workflow.activeReceivedRequest ?: return
        if (selected.id != requestId || workflow.responseDraft?.requestId != requestId) return

        viewModelScope.launch {
            val markResult = runCatching {
                withContext(Dispatchers.IO) {
                    requestRepository.markReceivedRequestResponded(selected, responded = true)
                }
            }
            val marked = markResult.getOrElse { error ->
                _events.tryEmit(RequestEvent.ResponseStatusUpdateFailed(error))
                return@launch
            }
            if (!marked) {
                _events.tryEmit(
                    RequestEvent.ResponseStatusUpdateFailed(
                        IllegalStateException("Received request no longer exists")
                    )
                )
                return@launch
            }
            dispatch(RequestWorkflowAction.CompleteResponse(requestId))
        }
    }

    fun setDraftFields(fields: List<FieldDomain>) {
        AppLogger.debug("RequestViewModel", "event=set_draft_fields_start")
        val updated = dispatch(
            RequestWorkflowAction.SetOutgoingFields(
                fields = fields,
                candidateRequestId = UUID.randomUUID().toString()
            )
        )
        AppLogger.debug(
            "RequestViewModel",
            "event=set_draft_fields_success count=${updated.outgoingRequestDraft.requestedFields.size}"
        )
    }

    private suspend fun restorePersistedWorkflow(ownerAddress: String) {
        if (cacheSelection.getRequestWorkflow().hasPersistableDraftIdentity()) return
        val restored = runCatching {
            withContext(Dispatchers.IO) {
                requestRepository.findWorkflowDraft(ownerAddress)
            }
        }.onFailure { error ->
            AppLogger.error("RequestViewModel", "event=restore_workflow_draft_failed", error)
        }.getOrNull() ?: return

        val currentOwner = cacheSelection.getOwnerEmail()?.trim()?.lowercase()
        if (
            currentOwner == ownerAddress &&
            !cacheSelection.getRequestWorkflow().hasPersistableDraftIdentity()
        ) {
            cacheSelection.cacheRequestWorkflow(restored)
        }
    }

    private fun queueWorkflowPersistence(workflow: RequestWorkflowState) {
        val ownerAddress = cacheSelection.getOwnerEmail()?.trim()?.lowercase().orEmpty()
        if (ownerAddress.isBlank()) return
        workflowPersistence.trySend(WorkflowPersistence(ownerAddress, workflow))
    }

    private suspend fun persistWorkflowNow(workflow: RequestWorkflowState): Boolean {
        val ownerAddress = cacheSelection.getOwnerEmail()?.trim()?.lowercase().orEmpty()
        if (ownerAddress.isBlank()) return false
        val completion = CompletableDeferred<Boolean>()
        workflowPersistence.send(
            WorkflowPersistence(
                ownerAddress = ownerAddress,
                workflow = workflow,
                completion = completion
            )
        )
        return completion.await()
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
        val verification = item.payload.verification
        if (!requestInboxDecisionController.tryStart(verification, RequestInboxDecision.ACCEPT)) {
            return
        }
        _isCloudInboxLoading.value = true
        viewModelScope.launch {
            var localRequestedKeyCount: Int? = null
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    enforceDecisionCooldownOrThrow()
                    val decryptedJson = cloudService.decryptRequestFromInbox(email, item.payload).getOrThrow()
                    val importedKeyCount = importDecryptedRequestJsons(
                        jsonStrings = listOf(decryptedJson),
                        targetUserEmail = email,
                        authenticatedRequesterIds = listOf(item.payload.user),
                        externalRequestIds = listOf(
                            "${item.payload.verification}:${item.payload.creationAt}"
                        )
                    )
                    localRequestedKeyCount = importedKeyCount
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
                requestInboxDecisionController.succeed(
                    RequestInboxDecisionReceipt(
                        verification = verification,
                        decision = RequestInboxDecision.ACCEPT,
                        localRequestAvailable = true,
                        requestedKeyCount = importedKeyCount,
                        backendAcknowledged = backendAcknowledged
                    )
                )
                _events.tryEmit(
                    RequestEvent.CloudInboxAccepted(
                        importedKeyCount = importedKeyCount,
                        backendAcknowledged = backendAcknowledged
                    )
                )
            }.onFailure { error ->
                captureDecisionCooldown(error)
                requestInboxDecisionController.fail(
                    verification = verification,
                    decision = RequestInboxDecision.ACCEPT,
                    error = error,
                    localRequestAvailable = localRequestedKeyCount != null,
                    requestedKeyCount = localRequestedKeyCount ?: 0
                )
                _events.tryEmit(RequestEvent.FetchError(error))
            }
        }
    }

    fun rejectRequestInboxItem(email: String, item: CloudInboxItem) {
        val verification = item.payload.verification
        if (!requestInboxDecisionController.tryStart(verification, RequestInboxDecision.REJECT)) {
            return
        }
        _isCloudInboxLoading.value = true
        viewModelScope.launch {
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
                requestInboxDecisionController.succeed(
                    RequestInboxDecisionReceipt(
                        verification = verification,
                        decision = RequestInboxDecision.REJECT,
                        localRequestAvailable = false,
                        requestedKeyCount = 0,
                        backendAcknowledged = backendAcknowledged
                    )
                )
                _events.tryEmit(RequestEvent.CloudInboxRejected(backendAcknowledged))
            }.onFailure { error ->
                captureDecisionCooldown(error)
                requestInboxDecisionController.fail(
                    verification = verification,
                    decision = RequestInboxDecision.REJECT,
                    error = error
                )
                _events.tryEmit(RequestEvent.FetchError(error))
            }
        }
    }

    fun consumeRequestInboxDecisionReceipt() {
        requestInboxDecisionController.consumeReceipt()
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
        targetUserEmail: String,
        authenticatedRequesterIds: List<String?> = emptyList(),
        externalRequestIds: List<String?> = emptyList()
    ): Int {
        if (jsonStrings.isEmpty()) return 0

        var totalMatchedFields = 0
        jsonStrings.forEachIndexed { idx, jsonString ->
            AppLogger.debug("RequestViewModel", "event=fetch_request_item index=$idx")
            val request = parseRequestFromJson(
                jsonString = jsonString,
                targetUserEmail = targetUserEmail,
                sourceIndex = idx,
                authenticatedRequesterId = authenticatedRequesterIds.getOrNull(idx),
                externalRequestId = externalRequestIds.getOrNull(idx)
            )
            requestRepository.saveReceivedRequest(request)
            totalMatchedFields += request.fields.size
        }
        return totalMatchedFields
    }

    private fun parseRequestFromJson(
        jsonString: String,
        targetUserEmail: String,
        sourceIndex: Int,
        authenticatedRequesterId: String?,
        externalRequestId: String?
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

        val requestedKeys = normalizeRequestedFieldKeys(
            List(keysJson.length()) { index -> keysJson.optString(index) }
        )
        if (requestedKeys.isEmpty()) {
            throw IllegalStateException("Request[$sourceIndex] has no valid 'keys'")
        }
        val requestedFields = requestedKeys.map { key ->
            FieldDomain.initialize().copy(key = key)
        }
        val verifiedRequesterId = authenticatedRequesterId
            ?.takeIf { it.isNotBlank() }
            ?.let { senderId ->
                requireAuthenticatedRequester(
                    requesterAddress = userEmail,
                    authenticatedRequesterId = senderId,
                    addressHasher = ::hashMessageB64
                )
            }
            .orEmpty()
        val requestId = canonicalCloudRequestId(jsonObject.optString("request_id"))
            ?: stableImportedRequestId(
                requesterAddress = userEmail,
                recipientAddress = targetUserEmail,
                fieldKeys = requestedFields.map { it.key },
                externalReference = externalRequestId
            )
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
            responded = false,
            id = requestId,
            requesterId = verifiedRequesterId
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

private data class WorkflowPersistence(
    val ownerAddress: String,
    val workflow: RequestWorkflowState,
    val completion: CompletableDeferred<Boolean>? = null
)

private fun RequestWorkflowState.visibleDraftFields(): List<FieldDomain> = when (mode) {
    RequestListMode.RECEIVED -> activeReceivedRequest?.fields.orEmpty()
    RequestListMode.SENT -> outgoingRequestDraft.requestedFields.map(RequestedFieldDraft::toFieldDomain)
}

private fun RequestedFieldDraft.toFieldDomain(): FieldDomain = FieldDomain(
    key = key,
    value = "",
    keyAlias = keyAlias,
    dateAdded = Instant.EPOCH
)
