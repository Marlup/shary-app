package com.shary.app.viewmodels.fileVisualizer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.services.FileService
import com.shary.app.core.domain.models.FieldDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant


@HiltViewModel
class FileVisualizerViewModel @Inject constructor(
    private val fileService: FileService
) : ViewModel() {

    data class ParsedZip(
        val file: File,
        val fileName: String,
        val mode: String?,               // from meta.txt
        val fields: List<FieldDomain>,   // parsed from content.json
        val isValidStructure: Boolean
    )

    private val _items = MutableStateFlow<List<ParsedZip>>(emptyList())
    val items: StateFlow<List<ParsedZip>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 2)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    sealed interface Event {
        data class Info(val message: String) : Event
        data class Error(val message: String) : Event
    }

    fun addZips(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = uris.map { uri ->
                    async(Dispatchers.IO) { processUriSafe(uri) }
                }.awaitAll()

                val successes = results.filterNotNull()
                val failures = results.size - successes.size

                if (successes.isNotEmpty()) {
                    _items.value = _items.value + successes
                    _events.tryEmit(Event.Info("Added ${successes.size} ZIP(s)"))
                }
                if (failures > 0) {
                    _events.tryEmit(Event.Info("$failures ZIP(s) skipped"))
                }
            } catch (e: Exception) {
                _events.tryEmit(Event.Error("Error adding ZIPs: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeByName(fileName: String, deleteFile: Boolean = false) {
        val toRemove = _items.value.firstOrNull { it.fileName == fileName } ?: return
        _items.value = _items.value.filterNot { it.fileName == fileName }
        if (deleteFile) fileService.deletePrivateFile(toRemove.file)
    }

    fun clearAll(deleteFiles: Boolean = false) {
        if (deleteFiles) _items.value.forEach { fileService.deletePrivateFile(it.file) }
        _items.value = emptyList()
    }

    private suspend fun processUriSafe(uri: Uri): ParsedZip? {
        return withContext(Dispatchers.IO) {
            try {
                if (!fileService.isZipFile(uri)) {
                    _events.tryEmit(Event.Info("Skipped (not a ZIP): ${fileService.getFileNameFromUri(uri) ?: uri}"))
                    return@withContext null
                }

                val copied = fileService.copyZipToPrivateStorage(uri)
                    ?: return@withContext null.also {
                        _events.tryEmit(Event.Error("Cannot copy ZIP from $uri"))
                    }

                val valid = fileService.validateZipStructure(copied)
                if (!valid) {
                    _events.tryEmit(Event.Error("Invalid ZIP structure: ${copied.name}"))
                }

                val mode = fileService.getModeFromZip(copied)
                val fieldsMap = fileService.getFieldsFromZip(copied)

                val fieldsDomain = fieldsMap.map { (key, value) ->
                    FieldDomain(key = key, value = value, dateAdded = Instant.MIN)
                }

                ParsedZip(
                    file = copied,
                    fileName = copied.name,
                    mode = mode,
                    fields = fieldsDomain,
                    isValidStructure = valid
                )
            } catch (e: Exception) {
                _events.tryEmit(Event.Error("Error parsing: ${e.message}"))
                null
            }
        }
    }
}
