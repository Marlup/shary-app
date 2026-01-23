package com.shary.app.viewmodels.field

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.events.FieldEvent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.types.enums.SearchFieldBy
import com.shary.app.core.domain.types.enums.SortByParameter
import com.shary.app.core.domain.types.enums.getSafeTag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

@HiltViewModel
class FieldViewModel @Inject constructor(
    private val fieldRepository: FieldRepository,
    private val cacheSelection: CacheService,
    private val cloudService: CloudService,
) : ViewModel() {

    // Domain state exposed to UI
    private val _fields = MutableStateFlow<List<FieldDomain>>(emptyList())
    val fields: StateFlow<List<FieldDomain>> = _fields.asStateFlow()

    private val _selectedFields = MutableStateFlow<List<FieldDomain>>(emptyList())
    val selectedFields: StateFlow<List<FieldDomain>> = _selectedFields.asStateFlow()

    // Loading flag for long-running operations
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // One-shot events (snackbar/toast/navigation)
    private val _events = MutableSharedFlow<FieldEvent>(extraBufferCapacity = 1)

    private val _searchCriteria = MutableStateFlow("")
    val searchCriteria: StateFlow<String> = _searchCriteria.asStateFlow()

    private val _searchFieldBy = MutableStateFlow(SearchFieldBy.KEY)
    val searchFieldBy: StateFlow<SearchFieldBy> = _searchFieldBy.asStateFlow()

    private val _selectedKeys = MutableStateFlow<Set<String>>(emptySet())

    private val _sortByParameter = MutableStateFlow(SortByParameter.KEY)
    val sortByParameter: StateFlow<SortByParameter> = _sortByParameter.asStateFlow()
    private val _ascendingSortByMap = MutableStateFlow(SortByParameter.entries.associateWith { false })
    val ascendingSortByMap: StateFlow<Map<SortByParameter, Boolean>> = _ascendingSortByMap.asStateFlow()

    private val _ascending = MutableStateFlow(ascendingSortByMap.value[_sortByParameter.value]!!)
    val ascending: StateFlow<Boolean> = _ascending.asStateFlow()

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
        if (field.value == value) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { fieldRepository.updateValue(field.key, value) }
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
        setSelectedFields(_selectedFields.value)
    }

    fun setSelectedFields(fields: List<FieldDomain>) {
        val selectedFields = fields.distinctBy { it.key.lowercase() }
        _selectedFields.value = selectedFields
        cacheSelection.cacheFields(selectedFields)
    }

    fun clearSelectedFields() {
        cacheSelection.clearCachedFields()
        _selectedFields.value = emptyList()
    }

    fun toggleFieldSelection(field: FieldDomain) {
        _selectedFields.update { current -> if (field in current) current - field else current + field }
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

    fun isFilteredFieldsNotEmpty():Boolean = filteredFields.value.isNotEmpty()

    fun refreshFields() {
        refresh()
    }

    // ----------------------------- Internals --------------------------------

    private fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val fields = withContext(Dispatchers.IO) {
                fieldRepository.getAllFields().first()
            }
            _fields.value = fields
            _isLoading.value = false
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
                    // Make both updates atomically with respect to other writes
                    writeMutex.withLock {
                        if (shouldUpdateValue) {
                            fieldRepository.updateValue(field.key, valueTrimmed)
                        }
                        if (shouldUpdateAlias) {
                            fieldRepository.updateAlias(field.key, aliasTrimmed)
                        }
                        if (shouldUpdateTag) {
                            Log.d("FieldViewModel", "Updating tag for ${field.key} to ${newField.tag.getSafeTag()}")
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

    /**
     * Fetches fields from Firebase, decrypts them, and saves them to the local database.
     * Returns the number of fields successfully added.
     */
    fun fetchFieldsFromCloud(email: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = runCatching {
                withContext(Dispatchers.IO) {

                    // payloadData: Result<List<String>>
                    val payloadData = cloudService.fetchPayloadDataFromEmail(email)

                    val jsonStrings: List<String> = payloadData.getOrThrow()
                    if (jsonStrings.isEmpty()) {
                        throw IllegalStateException("No payloads returned from cloud")
                    }

                    var addedCount = 0

                    // One lock for the whole batch to keep repository writes consistent
                    writeMutex.withLock {
                        jsonStrings.forEachIndexed { idx, jsonString ->
                            Log.d("FieldViewModel", "Fetched payload[$idx]: $jsonString")

                            val jsonObject = JSONObject(jsonString)
                            val keys = jsonObject.keys()

                            while (keys.hasNext()) {
                                val key = keys.next()
                                val value = jsonObject.getString(key)

                                val field = FieldDomain(
                                    key = key,
                                    value = value,
                                    keyAlias = "",
                                    tag = Tag.Unknown,
                                    dateAdded = Instant.now()
                                )

                                val created = fieldRepository.saveFieldIfNotExists(normalize(field))
                                if (created) addedCount++

                                Log.d("FieldViewModel", "Fetched key: $key")
                            }
                        }
                    }

                    addedCount
                }
            }

            _isLoading.value = false

            result.onSuccess { count ->
                if (count > 0) {
                    _events.tryEmit(FieldEvent.FetchedFromCloud(count))
                    refresh()
                } else {
                    _events.tryEmit(FieldEvent.NoNewFields)
                }
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.FetchError(e))
            }
        }
    }
}
