package com.shary.app.infrastructure.services.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.shary.app.core.domain.interfaces.services.JsonFileService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Implementation of [JsonFileService] for handling JSON-based
 * Shary data files. These files contain two top-level objects:
 *
 * - `metadata`: a JSON object with descriptive properties such as `mode`.
 * - `fields`: a JSON object where each entry is a key/value pair representing a field.
 *
 * This service is responsible for:
 * - Copying/importing JSON files from SAF into the app's private storage.
 * - Validating JSON structure.
 * - Reading/writing metadata and fields.
 * - Creating new Shary JSON files from domain models.
 * - Listing and deleting JSON files in the private sandbox.
 */
class JsonFileServiceImpl(
    private val context: Context
) : JsonFileService {

    /**
     * Reads the `mode` field from the `metadata` object of a JSON file.
     *
     * @return the parsed [DataFileMode] if present and valid, otherwise `null`.
     */
    override suspend fun getModeFromJson(file: File): DataFileMode? = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(file.readText())
            val metaObj = root.optJSONObject("metadata") ?: return@runCatching null
            val mode = metaObj.optString("mode", "")
            DataFileMode.fromString(mode)
        }.getOrElse { ex ->
            Log.e("JsonFileServiceImpl", "Invalid metadata in JSON", ex)
            null
        }
    }

    /**
     * Extracts all fields from the `fields` object of the JSON file.
     *
     * @return a [Map] of key/value pairs, or empty if the structure is missing/invalid.
     */
    override suspend fun getFieldsFromJson(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(file.readText())
            val fieldsObj = root.optJSONObject("fields") ?: return@runCatching emptyMap<String, String>()
            buildMap {
                fieldsObj.keys().forEach { key ->
                    put(key, fieldsObj.optString(key, ""))
                }
            }
        }.getOrElse { ex ->
            Log.e("JsonFileServiceImpl", "Invalid fields in JSON", ex)
            emptyMap()
        }
    }

    /**
     * Validates that the JSON file has both `metadata` and `fields` top-level objects.
     *
     * @return true if structure is valid, false otherwise.
     */
    override suspend fun validateJsonStructure(file: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(file.readText())
            root.has("metadata") && root.has("fields")
        }.getOrDefault(false)
    }

    /**
     * Copies a JSON file from a SAF [Uri] into the app's private storage,
     * generating a unique filename to avoid collisions.
     *
     * @return the [File] reference if successful, or null if copy failed.
     */
    override suspend fun copyJsonToPrivateStorage(uri: Uri): File? = withContext(Dispatchers.IO) {
        val originalName = getFileNameFromUri(uri) ?: "import.json"
        val safeName = originalName.takeLast(100).ifBlank { "import.json" }
        val destFile = File(context.filesDir, uniqueName(safeName))

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            } ?: return@withContext null
            destFile
        } catch (e: Exception) {
            Log.e("JsonFileServiceImpl", "Failed to copy JSON", e)
            null
        }
    }

    /**
     * Resolves the display name of a file from a SAF [Uri].
     *
     * @return the file name, or null if not resolvable.
     */
    override fun getFileNameFromUri(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return it.getString(index)
            }
        }
        return null
    }

    /**
     * Lists all `.json` files currently stored in the app's private sandbox,
     * ordered by modification date (newest first).
     */
    override fun listPrivateJsonFiles(): List<File> {
        return context.filesDir
            .listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Builds and writes a Shary JSON file to the cache directory.
     *
     * @param fields the list of domain fields to include.
     * @param metadata additional metadata key/value pairs.
     * @param mode the operating mode, written under `metadata.mode`.
     * @return the created [File].
     */
    override suspend fun createSharyJson(
        fields: List<FieldDomain>,
        metadata: Map<String, String>,
        mode: DataFileMode
    ): File = withContext(Dispatchers.IO) {
        val file = File.createTempFile("shary_", ".json", context.cacheDir)
        val json = JSONObject().apply {
            put("metadata", JSONObject().apply {
                metadata.forEach { (k, v) -> put(k, v) }
                put("mode", DataFileMode.toString(mode))
            })
            put("fields", JSONObject().apply {
                fields.forEach { put(it.key, it.value) }
            })
        }
        file.writeText(json.toString())
        file
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Alternative import method: copies a JSON file from SAF [Uri] into
     * the private storage, using `lastPathSegment` as the file name.
     *
     * Useful when uniqueness is not enforced externally.
     */
    override fun loadJsonFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val destFile = File(context.filesDir, uri.lastPathSegment ?: uniqueName("import.json"))
            inputStream.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            destFile
        } catch (e: Exception) {
            Log.e("JsonFileServiceImpl", "Failed to load JSON", e)
            null
        }
    }

    /**
     * Deletes a JSON file from private storage.
     *
     * @return true if deletion succeeded, false otherwise.
     */
    override fun deletePrivateFile(file: File): Boolean =
        runCatching { file.delete() }.getOrDefault(false)

    /**
     * Generates a unique file name by appending a short UUID suffix before the extension.
     */
    private fun uniqueName(base: String): String {
        val dot = base.lastIndexOf('.')
        val id = UUID.randomUUID().toString().take(8)
        return if (dot > 0) {
            val name = base.take(dot)
            val ext = base.substring(dot)
            "${name}_$id$ext"
        } else {
            "${base}_$id"
        }
    }
}
