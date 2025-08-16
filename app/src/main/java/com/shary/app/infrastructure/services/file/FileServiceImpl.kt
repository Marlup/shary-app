package com.shary.app.infrastructure.services.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.shary.app.core.domain.interfaces.services.FileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class FileServiceImpl(
    private val context: Context
): FileService {

    // Hard limits (defense-in-depth)
    private val maxEntries = 200
    private val maxEntryBytes = 5 * 1024 * 1024 // 5 MB per entry

    // -------------------- Public API (all suspend/IO) --------------------

    override suspend fun getModeFromZip(file: File): String? = withContext(Dispatchers.IO) {
        extractTextEntry(file, "meta.txt")?.let { meta ->
            Regex("""(?im)^\s*mode\s*=\s*([A-Za-z0-9_-]+)\s*$""")
                .find(meta)?.groupValues?.getOrNull(1)?.also {
                    Log.d("getModeFromZip", it)
                }
        }
    }

    override suspend fun validateZipStructure(file: File): Boolean = withContext(Dispatchers.IO) {
        val required = setOf("meta.txt", "content.json")
        val found = mutableSetOf<String>()
        useZip(file) { zip ->
            var count = 0
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = safeZipName(entry.name) ?: return@useZip // invalid entry → ignore
                found += name
                count++
                if (count > maxEntries) return@withContext false
                entry = zip.nextEntry
            }
        }
        required.all { it in found }
    }

    override suspend fun getFieldsFromZip(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        val json = extractTextEntry(file, "content.json") ?: return@withContext emptyMap()
        runCatching {
            val root = JSONObject(json)
            val fieldsObj = root.optJSONObject("fields") ?: return@runCatching emptyMap<String, String>()
            buildMap {
                fieldsObj.keys().forEach { key ->
                    put(key, fieldsObj.optString(key, ""))
                }
            }
        }.getOrElse { ex ->
            Log.e("FileServiceImpl", "Invalid content.json", ex)
            emptyMap()
        }
    }

    override suspend fun copyZipToPrivateStorage(uri: Uri): File? = withContext(Dispatchers.IO) {
        val originalName = getFileNameFromUri(uri) ?: "import.zip"
        val safeName = originalName.takeLast(100).ifBlank { "import.zip" }
        val destFile = File(context.filesDir, uniqueName(safeName))

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            destFile
        } catch (e: Exception) {
            Log.e("FileServiceImpl", "Failed to copy ZIP", e)
            null
        }
    }

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

    private fun getMimeType(uri: Uri): String? = context.contentResolver.getType(uri)

    override fun isZipFile(uri: Uri): Boolean {
        val mime = getMimeType(uri)
        if (mime == "application/zip" || mime == "application/x-zip-compressed") return true
        // Fallback by extension if MIME is missing/wrong
        val name = getFileNameFromUri(uri)?.lowercase() ?: return false
        return name.endsWith(".zip")
    }

    /** Optional utility to clean up a copied file */
    override fun deletePrivateFile(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    // -------------------- Internals --------------------

    private suspend fun extractTextEntry(file: File, fileName: String): String? =
        withContext(Dispatchers.IO) {
            var result: String? = null
            useZip(file) { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = safeZipName(entry.name)
                    if (name == fileName) {
                        result = readEntryAsString(zip)
                        break
                    }
                    entry = zip.nextEntry
                }
            }
            result
        }

    private inline fun <T> useZip(file: File, block: (ZipInputStream) -> T): T {
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            return block(zip)
        }
    }

    /** Stops Zip Slip (../../) and absolute paths; returns normalized name or null */
    private fun safeZipName(raw: String): String? {
        val norm = raw.replace('\\', '/')
        if (norm.contains("..") || norm.startsWith("/") ) return null
        return norm
    }

    /** Read current entry from ZipInputStream into String with size cap */
    private fun readEntryAsString(input: InputStream): String {
        val buffer = ByteArray(8 * 1024)
        var read: Int
        var total = 0
        val out = StringBuilder()
        while (true) {
            read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxEntryBytes) {
                throw IllegalStateException("ZIP entry too large")
            }
            out.append(String(buffer, 0, read, Charsets.UTF_8))
        }
        return out.toString()
    }

    private fun uniqueName(base: String): String {
        val dot = base.lastIndexOf('.')
        val id = UUID.randomUUID().toString().take(8)
        return if (dot > 0) {
            val name = base.substring(0, dot)
            val ext = base.substring(dot)
            "${name}_$id$ext"
        } else {
            "${base}_$id"
        }
    }
}
