package com.shary.app.viewmodels.fileVisualizer

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.services.JsonFileService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.valueobjects.ParsedJson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

@HiltViewModel
class FileVisualizerViewModel @Inject constructor(
    private val jsonFileService: JsonFileService
) : ViewModel() {

    private val _items = MutableStateFlow<List<ParsedJson>>(emptyList())
    val items: StateFlow<List<ParsedJson>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 2)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    sealed interface Event {
        data class Info(val message: String) : Event
        data class Error(val message: String) : Event
    }

    private val fileChannel = MutableSharedFlow<File>(extraBufferCapacity = 10)

    init {
        viewModelScope.launch {
            fileChannel.collect { file -> refreshFromFile(file) }
        }
    }

    fun refreshFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val found = jsonFileService.listPrivateJsonFiles()
                val parsed = found.mapNotNull { f -> processFileSafe(f) }
                _items.value = parsed
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Import from SAF */
    fun importJsonFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("importJsonFromUri", uri.toString())
            jsonFileService.loadJsonFromUri(uri)
            refreshFiles()
        }
    }

    private suspend fun processFileSafe(file: File): ParsedJson? =
        withContext(Dispatchers.IO) {
            try {
                val valid = jsonFileService.validateJsonStructure(file)
                val mode = jsonFileService.getModeFromJson(file)
                val fieldsMap = jsonFileService.getFieldsFromJson(file)

                val fieldsDomain = fieldsMap.map { (key, value) ->
                    FieldDomain(key = key, value = value, dateAdded = Instant.MIN)
                }

                ParsedJson(
                    file = file,
                    fileName = file.name,
                    mode = mode,
                    fields = fieldsDomain,
                    isValidStructure = valid
                )
            } catch (e: Exception) {
                _events.tryEmit(Event.Error("Error parsing file: ${file.name} -> ${e.message}"))
                null
            }
        }

    private suspend fun refreshFromFile(file: File) {
        _isLoading.value = true
        try {
            val mode = jsonFileService.getModeFromJson(file) ?: return
            val fieldsMap = jsonFileService.getFieldsFromJson(file)

            val domainFields = fieldsMap.map { (k, v) -> FieldDomain.create(k, v) }

            val parsed = ParsedJson(
                file = file,
                fileName = file.name,
                mode = mode,
                fields = domainFields,
                isValidStructure = jsonFileService.validateJsonStructure(file)
            )
            _items.value = listOf(parsed)
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun processUriSafe(uri: Uri): ParsedJson? =
        withContext(Dispatchers.IO) {
            try {
                val copied = jsonFileService.copyJsonToPrivateStorage(uri)
                    ?: return@withContext null.also {
                        _events.tryEmit(Event.Error("Cannot copy JSON from $uri"))
                    }

                val valid = jsonFileService.validateJsonStructure(copied)
                if (!valid) {
                    _events.tryEmit(Event.Error("Invalid JSON structure: ${copied.name}"))
                }

                val mode = jsonFileService.getModeFromJson(copied)
                val fieldsMap = jsonFileService.getFieldsFromJson(copied)

                val fieldsDomain = fieldsMap.map { (key, value) ->
                    FieldDomain(key = key, value = value, dateAdded = Instant.MIN)
                }

                ParsedJson(
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
