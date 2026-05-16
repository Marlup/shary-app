package com.shary.app.viewmodels.field

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.application.security.ChangePasswordAndRewrapDataKeyUseCase
import com.shary.app.core.constants.CloudInboxPolicy
import com.shary.app.core.domain.interfaces.events.FieldEvent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.CloudPayloadDecision
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.FieldValueHistoryRepository
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.types.enums.SearchFieldBy
import com.shary.app.core.domain.types.enums.SortByParameter
import com.shary.app.core.domain.types.enums.getSafeTag
import com.shary.app.core.domain.types.valueobjects.CloudInboxItem
import com.shary.app.core.domain.types.valueobjects.DataPayload
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.infrastructure.services.cloud.CloudRateLimitedException
import com.shary.app.utils.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

@HiltViewModel
class FieldViewModel @Inject constructor(
    private val fieldRepository: FieldRepository,
    private val fieldValueHistoryRepository: FieldValueHistoryRepository,
    private val cacheSelection: CacheService,
    private val cloudService: CloudService,
    private val authenticationService: AuthenticationService,
    private val changePasswordAndRewrapDataKeyUseCase: ChangePasswordAndRewrapDataKeyUseCase
) : ViewModel() {
    private data class CloudFetchSummary(
        val downloadedKeys: List<String>,
        val loadedKeys: List<String>,
        val preDownloadedKeys: List<String>
    )

    // Domain state exposed to UI
    private val _fields = MutableStateFlow<List<FieldDomain>>(emptyList())
    val fields: StateFlow<List<FieldDomain>> = _fields.asStateFlow()

    private val _selectedKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedFields: StateFlow<List<FieldDomain>> =
        combine(_fields, _selectedKeys) { allFields, selectedKeys ->
            allFields.filter { field -> selectedKeys.contains(field.key.trim().lowercase()) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Loading flag for long-running operations
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isCloudInboxLoading = MutableStateFlow(false)
    val isCloudInboxLoading: StateFlow<Boolean> = _isCloudInboxLoading.asStateFlow()
    private val _cloudInboxItems = MutableStateFlow<List<CloudInboxItem>>(emptyList())
    val cloudInboxItems: StateFlow<List<CloudInboxItem>> = _cloudInboxItems.asStateFlow()

    // One-shot events (snackbar/toast/navigation)
    private val _events = MutableSharedFlow<FieldEvent>(extraBufferCapacity = 1)

    private val _searchCriteria = MutableStateFlow("")
    val searchCriteria: StateFlow<String> = _searchCriteria.asStateFlow()

    private val _searchFieldBy = MutableStateFlow(SearchFieldBy.KEY)
    val searchFieldBy: StateFlow<SearchFieldBy> = _searchFieldBy.asStateFlow()

    private val _sortByParameter = MutableStateFlow(SortByParameter.KEY)
    val sortByParameter: StateFlow<SortByParameter> = _sortByParameter.asStateFlow()
    private val _ascendingSortByMap = MutableStateFlow(SortByParameter.entries.associateWith { false })
    val ascendingSortByMap: StateFlow<Map<SortByParameter, Boolean>> = _ascendingSortByMap.asStateFlow()
    private val _strictModeEnabled = MutableStateFlow(false)
    val strictModeEnabled: StateFlow<Boolean> = _strictModeEnabled.asStateFlow()

    private val _ascending = MutableStateFlow(ascendingSortByMap.value[_sortByParameter.value]!!)
    val ascending: StateFlow<Boolean> = _ascending.asStateFlow()
    val recoverableKeys: StateFlow<Set<String>> =
        fieldValueHistoryRepository.recoverableKeys
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val events: SharedFlow<FieldEvent> = _events.asSharedFlow()

    /** Combined filtering + sorting logic **/
    val filteredFields: StateFlow<List<FieldDomain>> =
        combine(
            _fields,
            _searchCriteria,
            _searchFieldBy,
            _sortByParameter,
            _ascending
        ) { list, query, attribute, sortFieldBy, asc ->
            list.filter { it.matchBy(query, attribute) }
                .sortedWith(compareBy<FieldDomain> {
                    when (sortFieldBy) {
                        SortByParameter.KEY ->  it.key
                        SortByParameter.TAG -> it.tag.toString()
                        SortByParameter.DATE_ADDED -> it.dateAdded
                    }
                }.let {
                    if (asc) it else it.reversed() })
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Serialize writes to avoid concurrent edits on the same store
    private val writeMutex = Mutex()
    private var refreshJob: Job? = null
    private var refreshSequence: Long = 0L
    private var decisionCooldownUntilEpochSeconds: Long = 0L

    init {
        refresh()
    }

    // ------------------- Public API (called from Screen) -------------------

    fun getCachedFields(): List<FieldDomain> = cacheSelection.getFields()
    fun anyFieldCached() = cacheSelection.isAnyFieldCached()

    /**
     * Checks if any field uses the specified tag.
     * Returns true if the tag is in use, false otherwise.
     */
    fun isTagInUse(tag: Tag): Boolean {
        return _fields.value.any { it.tag == tag }
    }

    /** Add a new field. Handles custom tag persistence and de-duplication. */
    fun addField(field: FieldDomain) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        val normalized = normalize(field)
                        validateValueOrThrow(normalized.value)
                        fieldRepository.saveFieldIfNotExists(normalized)
                    }
                }
            }
            _isLoading.value = false
            result.onSuccess { created ->
                if (created) {
                    _events.tryEmit(FieldEvent.Saved(field))
                    refresh()
                } else {
                    _events.tryEmit(FieldEvent.AlreadyExists(field.key))
                }
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }

    fun deleteFields(fields: List<FieldDomain>) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        val keys = fields.map { it.key }
                        fieldRepository.deleteFields(keys)
                        fieldValueHistoryRepository.clearSnapshots(keys)
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(FieldEvent.MultiDeleted(fields))
                refresh()
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }

    fun updateValue(field: FieldDomain, value: String) {
        val normalizedValue = value.trim()
        if (field.value == normalizedValue) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    validateValueOrThrow(normalizedValue)
                    writeMutex.withLock {
                        fieldValueHistoryRepository.saveSnapshot(field.key, field.value)
                        fieldRepository.updateValue(field.key, normalizedValue)
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(FieldEvent.ValueUpdated(field.key))
                refresh()
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }

    fun updateAlias(field: FieldDomain, alias: String) {
        if (field.keyAlias == alias) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { fieldRepository.updateAlias(field.key, alias) }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(FieldEvent.AliasUpdated(field.key))
                refresh()
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }

    fun setSelectedFields() {
        setSelectedFields(selectedFields.value)
    }

    fun setSelectedFields(fields: List<FieldDomain>) {
        val normalizedKeys = fields
            .map { it.key.trim().lowercase() }
            .toSet()
        _selectedKeys.value = normalizedKeys
        val selectedByKey = _fields.value.filter { normalizedKeys.contains(it.key.trim().lowercase()) }
        cacheSelection.cacheFields(selectedByKey)
    }

    fun clearSelectedFields() {
        _selectedKeys.value = emptySet()
        cacheSelection.clearCachedFields()
    }

    fun toggleFieldSelection(field: FieldDomain) {
        val normalized = field.key.trim().lowercase()
        _selectedKeys.update { current ->
            if (current.contains(normalized)) current - normalized else current + normalized
        }
    }

    fun updateSearch(query: String, attribute: SearchFieldBy) {
        _searchCriteria.value = query
        _searchFieldBy.value = attribute
    }
    fun updateSearchField(searchFieldBy: SearchFieldBy) {
        _searchFieldBy.value = searchFieldBy
    }

    fun updateSort(sortByParameter: SortByParameter, ascending: Boolean) {
        //_ascendingSortByMap.value = _ascendingSortByMap.value + (sortByParameter to ascending)
        _ascendingSortByMap.update { currentMap ->
            currentMap + (sortByParameter to ascending)
        }
        _sortByParameter.value = sortByParameter
        _ascending.value = ascending
    }

    fun setStrictModeEnabled(enabled: Boolean) {
        _strictModeEnabled.value = enabled
    }

    fun isFilteredFieldsNotEmpty():Boolean = filteredFields.value.isNotEmpty()

    fun refreshFields() {
        refresh()
    }

    // ----------------------------- Internals --------------------------------

    private fun refresh() {
        refreshJob?.cancel()
        val sequence = ++refreshSequence
        refreshJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                fieldRepository
                    .getAllFieldsProgressive(firstBatchSize = 1, chunkSize = 1)
                    .collect { progressivelyLoaded ->
                        _fields.value = progressivelyLoaded
                    }
            } catch (_: CancellationException) {
                return@launch
            } catch (throwable: Throwable) {
                _events.tryEmit(FieldEvent.Error(throwable))
            } finally {
                if (refreshSequence == sequence) {
                    _isLoading.value = false
                }
            }
        }
    }

    /** Normalize trims and ensure dateAdded is set. */
    private fun normalize(field: FieldDomain): FieldDomain {
        val trimmed = field.copy(
            key = field.key.trim(),
            value = field.value.trim(),
            keyAlias = field.keyAlias.trim()
        )
        return if (trimmed.dateAdded == Instant.EPOCH) {
            trimmed.copy(dateAdded = Instant.now())
        } else trimmed
    }

    private fun validateValueOrThrow(value: String) {
        val result = FieldValueContract.validate(value, strictModeEnabled.value)
        if (!result.isValid) {
            throw IllegalArgumentException(
                result.hint ?: "Value does not satisfy strict mode validation."
            )
        }
    }

    // Add this to your FieldViewModel

    /**
     * Update both value and alias in a single coroutine to avoid multiple refreshes.
     * - Trims inputs before comparing/applying changes.
     * - Emits specific events for each changed attribute.
     * - Refreshes only once at the end if something changed.
     */
    fun updateField(field: FieldDomain, newField: FieldDomain) {
        val valueTrimmed = newField.value.trim()
        val aliasTrimmed = newField.keyAlias.trim()

        val shouldUpdateValue = field.value != valueTrimmed
        val shouldUpdateAlias = field.keyAlias != aliasTrimmed
        val shouldUpdateTag = field.tag != newField.tag.getSafeTag()

        if (!shouldUpdateValue && !shouldUpdateAlias && !shouldUpdateTag) return

        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    if (shouldUpdateValue) validateValueOrThrow(valueTrimmed)
                    // Make both updates atomically with respect to other writes
                    writeMutex.withLock {
                        if (shouldUpdateValue) {
                            fieldValueHistoryRepository.saveSnapshot(field.key, field.value)
                            fieldRepository.updateValue(field.key, valueTrimmed)
                        }
                        if (shouldUpdateAlias) {
                            fieldRepository.updateAlias(field.key, aliasTrimmed)
                        }
                        if (shouldUpdateTag) {
                            AppLogger.debug("FieldViewModel", "event=update_tag key=${field.key}")
                            fieldRepository.updateTag(field.key, newField.tag.getSafeTag())
                        }
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                if (shouldUpdateValue) _events.tryEmit(FieldEvent.ValueUpdated(field.key))
                if (shouldUpdateAlias) _events.tryEmit(FieldEvent.AliasUpdated(field.key))
                if (shouldUpdateTag) _events.tryEmit(FieldEvent.TagUpdated(field.key,
                    newField.tag.getSafeTag()
                ))
                // Single refresh after performing all updates
                refresh()
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }

    fun recoverPreviousValue(field: FieldDomain) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        val snapshot = fieldValueHistoryRepository.consumeSnapshot(field.key)
                            ?: throw IllegalStateException("No recoverable value found for '${field.key}'.")

                        validateValueOrThrow(snapshot.value)
                        fieldRepository.updateValue(field.key, snapshot.value)
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(FieldEvent.ValueRecovered(field.key))
                refresh()
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }

    fun restoreDeletedFields(fields: List<FieldDomain>) {
        if (fields.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        fields.forEach { field ->
                            fieldRepository.saveFieldIfNotExists(normalize(field))
                        }
                    }
                }
            }
            _isLoading.value = false
            result.onSuccess {
                refresh()
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }

    /**
     * Fetches pending encrypted payloads and keeps them in-memory for user review.
     */
    fun loadCloudInbox(email: String) {
        viewModelScope.launch {
            _isCloudInboxLoading.value = true
            _cloudInboxItems.value = emptyList()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val payloads = cloudService.fetchPayloadInboxFromEmail(email).getOrThrow()
                    payloads
                        .sortedByDescending { it.creationAt }
                        .map { payload ->
                            val previewNames = if (CloudInboxPolicy.ENABLE_FIELD_NAME_PREVIEW) {
                                cloudService.decryptPayloadFromInbox(email, payload)
                                    .getOrNull()
                                    ?.let(::extractPreviewFieldNames)
                                    .orEmpty()
                            } else {
                                emptyList()
                            }
                            CloudInboxItem(
                                payload = payload,
                                previewFieldNames = previewNames
                            )
                        }
                }
            }
            _isCloudInboxLoading.value = false

            result.onSuccess { inbox ->
                _cloudInboxItems.value = inbox
                if (inbox.isEmpty()) {
                    _events.tryEmit(FieldEvent.CloudInboxEmpty)
                } else {
                    _events.tryEmit(FieldEvent.CloudInboxLoaded(inbox.size))
                }
            }.onFailure { error ->
                _events.tryEmit(FieldEvent.FetchError(error))
            }
        }
    }

    fun acceptCloudInboxItem(email: String, item: CloudInboxItem) {
        viewModelScope.launch {
            _isCloudInboxLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    enforceDecisionCooldownOrThrow()
                    val decryptedJson = cloudService.decryptPayloadFromInbox(email, item.payload).getOrThrow()
                    val summary = importDecryptedPayloadJsons(listOf(decryptedJson))
                    val backendAcknowledged = cloudService.sendPayloadDecision(
                        email = email,
                        payload = item.payload,
                        decision = CloudPayloadDecision.ACCEPT,
                        notifySender = false
                    ).getOrThrow()
                    summary to backendAcknowledged
                }
            }
            _isCloudInboxLoading.value = false

            result.onSuccess { (summary, backendAcknowledged) ->
                removeCloudInboxItem(item.payload)
                emitCloudImportSummary(summary)
                _events.tryEmit(FieldEvent.CloudInboxAccepted(backendAcknowledged))
            }.onFailure { error ->
                captureDecisionCooldown(error)
                _events.tryEmit(FieldEvent.FetchError(error))
            }
        }
    }

    fun rejectCloudInboxItem(email: String, item: CloudInboxItem) {
        viewModelScope.launch {
            _isCloudInboxLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    enforceDecisionCooldownOrThrow()
                    cloudService.sendPayloadDecision(
                        email = email,
                        payload = item.payload,
                        decision = CloudPayloadDecision.REJECT,
                        notifySender = CloudInboxPolicy.ENABLE_REJECT_NOTIFY_SENDER
                    ).getOrThrow()
                }
            }
            _isCloudInboxLoading.value = false

            result.onSuccess { backendAcknowledged ->
                removeCloudInboxItem(item.payload)
                _events.tryEmit(FieldEvent.CloudInboxRejected(backendAcknowledged))
            }.onFailure { error ->
                captureDecisionCooldown(error)
                _events.tryEmit(FieldEvent.FetchError(error))
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

    /**
     * Legacy immediate-sync entrypoint. Kept for compatibility, but current UI uses inbox review flow.
     */
    fun fetchFieldsFromCloud(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val payloads = cloudService.fetchPayloadInboxFromEmail(email).getOrThrow()
                    val jsonStrings = payloads.map { payload ->
                        cloudService.decryptPayloadFromInbox(email, payload).getOrThrow()
                    }
                    importDecryptedPayloadJsons(jsonStrings)
                }
            }
            _isLoading.value = false

            result.onSuccess(::emitCloudImportSummary).onFailure { error ->
                _events.tryEmit(FieldEvent.FetchError(error))
            }
        }
    }

    private suspend fun importDecryptedPayloadJsons(jsonStrings: List<String>): CloudFetchSummary {
        if (jsonStrings.isEmpty()) {
            return CloudFetchSummary(
                downloadedKeys = emptyList(),
                loadedKeys = emptyList(),
                preDownloadedKeys = emptyList()
            )
        }

        val downloadedKeys = mutableListOf<String>()
        val loadedKeys = mutableListOf<String>()
        val preDownloadedKeys = mutableListOf<String>()

        writeMutex.withLock {
            val existingKeys = fieldRepository
                .getAllFields()
                .first()
                .map { it.key.trim().lowercase() }
                .toMutableSet()

            jsonStrings.forEachIndexed { idx, jsonString ->
                AppLogger.debug("FieldViewModel", "event=fetched_payload index=$idx")

                val jsonObject = JSONObject(jsonString)
                val keys = jsonObject.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    val normalizedKey = key.trim()
                    downloadedKeys += normalizedKey

                    val rawValue = jsonObject.opt(key)
                    val (value, alias) = when (rawValue) {
                        is JSONObject -> {
                            val nestedValue = rawValue.optString("value")
                            val nestedAlias = rawValue.optString("alias")
                            nestedValue to nestedAlias
                        }
                        is String -> rawValue to ""
                        is JSONArray -> rawValue.toString() to ""
                        null -> "" to ""
                        else -> rawValue.toString() to ""
                    }

                    if (existingKeys.contains(normalizedKey.lowercase())) {
                        preDownloadedKeys += normalizedKey
                        continue
                    }

                    val field = FieldDomain(
                        key = normalizedKey,
                        value = value,
                        keyAlias = alias,
                        tag = Tag.Unknown,
                        dateAdded = Instant.now()
                    )

                    val created = fieldRepository.saveFieldIfNotExists(normalize(field))
                    if (created) {
                        loadedKeys += normalizedKey
                        existingKeys += normalizedKey.lowercase()
                    } else {
                        preDownloadedKeys += normalizedKey
                    }

                    AppLogger.debug("FieldViewModel", "event=fetched_key key=$key")
                }
            }
        }

        return CloudFetchSummary(
            downloadedKeys = downloadedKeys.distinctBy { it.lowercase() },
            loadedKeys = loadedKeys.distinctBy { it.lowercase() },
            preDownloadedKeys = preDownloadedKeys.distinctBy { it.lowercase() }
        )
    }

    private fun emitCloudImportSummary(summary: CloudFetchSummary) {
        if (summary.loadedKeys.isNotEmpty()) {
            _events.tryEmit(
                FieldEvent.FetchedFromCloud(
                    count = summary.loadedKeys.size,
                    loadedKeys = summary.loadedKeys,
                    preDownloadedKeys = summary.preDownloadedKeys
                )
            )
            refresh()
        } else if (summary.downloadedKeys.isNotEmpty()) {
            _events.tryEmit(
                FieldEvent.FetchedFromCloud(
                    count = 0,
                    loadedKeys = emptyList(),
                    preDownloadedKeys = summary.preDownloadedKeys
                )
            )
        } else {
            _events.tryEmit(FieldEvent.NoNewFields)
        }
    }

    private fun removeCloudInboxItem(payload: DataPayload) {
        _cloudInboxItems.update { current ->
            current.filterNot { item ->
                item.payload.verification == payload.verification &&
                    item.payload.user == payload.user &&
                    item.payload.recipient == payload.recipient
            }
        }
    }

    private fun extractPreviewFieldNames(jsonString: String): List<String> = runCatching {
        val jsonObject = JSONObject(jsonString)
        val names = mutableListOf<String>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            names += keys.next()
        }
        names.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }.take(12)
    }.getOrDefault(emptyList())

    fun changePasswordAndRewrapDataKey(
        context: Context,
        oldPassword: String,
        newPassword: String,
        repeatNewPassword: String
    ) {
        if (oldPassword.isBlank()) {
            _events.tryEmit(FieldEvent.Error(IllegalArgumentException("Old password is required.")))
            return
        }
        if (newPassword.isBlank()) {
            _events.tryEmit(FieldEvent.Error(IllegalArgumentException("New password is required.")))
            return
        }
        if (newPassword != repeatNewPassword) {
            _events.tryEmit(FieldEvent.Error(IllegalArgumentException("New passwords do not match.")))
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        changePasswordAndRewrapDataKeyUseCase(
                            context = context,
                            oldPassword = oldPassword,
                            newPassword = newPassword
                        )
                    }
                }
                authenticationService.logoutForRelogin()
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(FieldEvent.PasswordChanged)
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }
}
