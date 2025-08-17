package com.shary.app.viewmodels.field

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.core.session.Session
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.TagRepository // <- simple abstraction: addCustomTag(name)
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class FieldViewModel @Inject constructor(
    private val fieldRepository: FieldRepository,
    private val tagRepository: TagRepository, // wrap your TagViewModel or implement repo
    private val session: Session
) : ViewModel() {

    // Domain state exposed to UI
    private val _fields = MutableStateFlow<List<FieldDomain>>(emptyList())
    val fields: StateFlow<List<FieldDomain>> = _fields.asStateFlow()

    private val _selectedFields = MutableStateFlow<List<FieldDomain>>(emptyList())
    val selectedFields: StateFlow<List<FieldDomain>> = _selectedFields.asStateFlow()

    private val _selectedKeys = MutableStateFlow<List<String>>(emptyList())
    val selectedKeys: StateFlow<List<String>> = _selectedKeys.asStateFlow()

    // Loading flag for long-running operations
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // One-shot events (snackbar/toast/navigation)
    private val _events = MutableSharedFlow<FieldEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FieldEvent> = _events.asSharedFlow()

    // Serialize writes to avoid concurrent edits on the same store
    private val writeMutex = Mutex()

    init { refresh() }

    // ------------------- Public API (called from Screen) -------------------

    /** Add a new field. Handles custom tag persistence and de-duplication. */
    fun addField(field: FieldDomain) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        val normalized = normalize(field)
                        // persist custom tag if needed
                        if (normalized.tag is UiFieldTag.Custom) {
                            val name = UiFieldTag.toString(normalized.tag)
                            tagRepository.addCustomTag(name)
                        }
                        // save if not exists
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

    fun deleteField(field: FieldDomain) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock { fieldRepository.deleteField(field.key) }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(FieldEvent.Deleted(field.key))
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
        if (field.keyAlias.orEmpty() == alias) return
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

    fun updateTag(field: FieldDomain, tag: UiFieldTag) {
        if (field.tag == tag) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    writeMutex.withLock {
                        // ensure custom tag is registered before updating
                        if (tag is UiFieldTag.Custom) {
                            tagRepository.addCustomTag(UiFieldTag.toString(tag))
                        }
                        fieldRepository.updateTag(field.key, tag)
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                _events.tryEmit(FieldEvent.TagUpdated(field.key, tag))
                refresh()
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }

    fun setSelectedFields(fields: List<FieldDomain>) {
        _selectedFields.value = fields.distinctBy { it.key.lowercase() }
        session.setSelectedFields(_selectedFields.value) // <— persistencia cross-screen
    }

    fun clearSelectedFields() { session.resetSelectedFields() }

    fun toggleFieldSelection(key: String) {
        _selectedKeys.update { current -> if (key in current) current - key else current + key }
    }
    fun clearSelectedKeys() { _selectedKeys.value = emptyList() }

    // ----------------------------- Internals --------------------------------

    private fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) { fieldRepository.getAllFields() }
            _fields.value = list
            _isLoading.value = false
        }
    }

    /** Normalize trims and ensure dateAdded is set. */
    private fun normalize(field: FieldDomain): FieldDomain {
        val trimmed = field.copy(
            key = field.key.trim(),
            value = field.value.trim(),
            keyAlias = field.keyAlias?.trim()
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
    fun updateField(field: FieldDomain, newValue: String, newAlias: String) {
        val valueTrimmed = newValue.trim()
        val aliasTrimmed = newAlias.trim()

        val shouldUpdateValue = field.value != valueTrimmed
        val shouldUpdateAlias = field.keyAlias.orEmpty() != aliasTrimmed

        if (!shouldUpdateValue && !shouldUpdateAlias) return

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
                    }
                }
            }
            _isLoading.value = false

            result.onSuccess {
                if (shouldUpdateValue) _events.tryEmit(FieldEvent.ValueUpdated(field.key))
                if (shouldUpdateAlias) _events.tryEmit(FieldEvent.AliasUpdated(field.key))
                // Single refresh after performing all updates
                refresh()
            }.onFailure { e ->
                _events.tryEmit(FieldEvent.Error(e))
            }
        }
    }
}

// UI events surfaced by the ViewModel
sealed interface FieldEvent {
    // Keep payloads small and stable for UI; prefer keys instead of whole objects when possible
    data class Saved(val field: FieldDomain) : FieldEvent
    data class AlreadyExists(val key: String) : FieldEvent
    data class Deleted(val key: String) : FieldEvent
    data class ValueUpdated(val key: String) : FieldEvent
    data class AliasUpdated(val key: String) : FieldEvent
    data class TagUpdated(val key: String, val tag: UiFieldTag) : FieldEvent
    data class Error(val throwable: Throwable) : FieldEvent
}
