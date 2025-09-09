package com.shary.app.viewmodels.fileVisualizer

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.services.JsonFileService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    private val jsonFileService: JsonFileService
) : ViewModel() {

    data class ParsedJson(
        val file: File,
        val fileName: String,
        val mode: DataFileMode?,        // from metadata.mode
        val fields: List<FieldDomain>,  // parsed from fields{}
        val isValidStructure: Boolean
    )

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

    fun onNewFile(file: File) {
        fileChannel.tryEmit(file)
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

    fun addJsons(uris: List<Uri>) {
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
                    _items.value += successes
                    _events.tryEmit(Event.Info("Added ${successes.size} JSON file(s)"))
                }
                if (failures > 0) {
                    _events.tryEmit(Event.Info("$failures JSON file(s) skipped"))
                }
            } catch (e: Exception) {
                _events.tryEmit(Event.Error("Error adding JSONs: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeByName(fileName: String, deleteFile: Boolean = false) {
        val toRemove = _items.value.firstOrNull { it.fileName == fileName } ?: return
        _items.value = _items.value.filterNot { it.fileName == fileName }
        if (deleteFile) jsonFileService.deletePrivateFile(toRemove.file)
    }

    fun clearAll(deleteFiles: Boolean = false) {
        if (deleteFiles) _items.value.forEach { jsonFileService.deletePrivateFile(it.file) }
        _items.value = emptyList()
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


/*
package com.shary.app.viewmodels.fileVisualizer

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.services.ZipFileService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    private val zipFileService: ZipFileService
) : ViewModel() {

    data class ParsedZip(
        val file: File,
        val fileName: String,
        val mode: DataFileMode?,               // from meta.txt
        val fields: List<FieldDomain>,   // parsed from content.json
        val isValidStructure: Boolean
    )

    data class ParsedJson(
        val file: File,
        val fileName: String,
        val mode: DataFileMode?,               // from meta.txt
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

    // concurrent-safe queue of files to process
    private val fileChannel = MutableSharedFlow<File>(extraBufferCapacity = 10)

    init {
        // Launch a consumer coroutine once at VM init
        viewModelScope.launch {
            fileChannel.collect { file ->
                refreshFromFile(file)
            }
        }
    }

    fun refreshFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val found = zipFileService.listPrivateJsonFiles()
                val parsed = found.mapNotNull { f -> processFileSafe(f) }
                _items.value = parsed
            } finally {
                _isLoading.value = false
            }
        }
    }


    /** Import from SAF */
    fun importZipFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("importZipFromUri", uri.toString())
            uri.path?.let { Log.d("importZipFromUri - path", it) }
            uri.path?.let { uri.lastPathSegment?.let { it1 ->
                Log.d("importZipFromUri - last seg",
                    it1
                )
            } }

            zipFileService.loadZipFromUri(uri)
            refreshFiles() // vuelve a refrescar para que aparezca en la lista
        }
    }

    /** Import from SAF */
    fun importJsonFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("importZipFromUri", uri.toString())
            uri.path?.let { Log.d("importZipFromUri - path", it) }
            uri.path?.let { uri.lastPathSegment?.let { it1 ->
                Log.d("importZipFromUri - last seg",
                    it1
                )
            } }

            zipFileService.loadJsonFromUri(uri)
            refreshFiles() // vuelve a refrescar para que aparezca en la lista
        }
    }


    /** Parse an already-copied File */
    private suspend fun processFileSafe(file: File): ParsedZip? {
        return withContext(Dispatchers.IO) {
            try {
                val valid = zipFileService.validateZipStructure(file)
                val mode = zipFileService.getModeFromZip(file)
                val fieldsMap = zipFileService.getFieldsFromZip(file)

                val fieldsDomain = fieldsMap.map { (key, value) ->
                    FieldDomain(key = key, value = value, dateAdded = Instant.MIN)
                }

                ParsedZip(
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
    }


    /** Parse an already-copied File */
    private suspend fun processJsonFileSafe(file: File): ParsedJson? {
        return withContext(Dispatchers.IO) {
            try {
                val valid = zipFileService.validateZipStructure(file)
                val mode = zipFileService.getModeFromZip(file)
                val fieldsMap = zipFileService.getFieldsFromZip(file)

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
    }

    /** Called when a new file is added from UI (after copyZipToPrivateStorage). */
    fun onNewFile(file: File) {
        fileChannel.tryEmit(file) // push file into processing queue
    }

    /** Process a single file safely (runs inside collect). */
    private suspend fun refreshFromFile(file: File) {
        _isLoading.value = true
        try {
            val mode = zipFileService.getModeFromZip(file) ?: return
            val fieldsMap = zipFileService.getFieldsFromZip(file)

            val domainFields = fieldsMap.map { (k, v) ->
                FieldDomain.create(k, v)
            }

            val parsed = ParsedZip(
                file = file,
                fileName = file.name,
                mode = mode,
                fields = domainFields,
                isValidStructure = zipFileService.validateZipStructure(file)
            )

            // replace items list with the latest file only
            _items.value = listOf(parsed)
        } finally {
            _isLoading.value = false
        }
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
        if (deleteFile) zipFileService.deletePrivateFile(toRemove.file)
    }

    fun clearAll(deleteFiles: Boolean = false) {
        if (deleteFiles) _items.value.forEach { zipFileService.deletePrivateFile(it.file) }
        _items.value = emptyList()
    }

    private suspend fun processUriSafe(uri: Uri): ParsedZip? {
        return withContext(Dispatchers.IO) {
            try {
                if (!zipFileService.isZipFile(uri)) {
                    _events.tryEmit(Event.Info("Skipped (not a ZIP): ${zipFileService.getFileNameFromUri(uri) ?: uri}"))
                    return@withContext null
                }

                val copied = zipFileService.copyZipToPrivateStorage(uri)
                    ?: return@withContext null.also {
                        _events.tryEmit(Event.Error("Cannot copy ZIP from $uri"))
                    }

                val valid = zipFileService.validateZipStructure(copied)
                if (!valid) {
                    _events.tryEmit(Event.Error("Invalid ZIP structure: ${copied.name}"))
                }

                val mode = zipFileService.getModeFromZip(copied)
                val fieldsMap = zipFileService.getFieldsFromZip(copied)

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
*/