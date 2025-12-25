package com.shary.app.infrastructure.services.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.shary.app.core.constants.Constants.VALID_METADATA_FILENAMES
import com.shary.app.core.domain.interfaces.services.ZipFileService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class ZipFileServiceImpl(
    private val context: Context
): ZipFileService {

    // Hard limits (defense-in-depth)
    private val maxEntries = 200
    private val maxEntryBytes = 5 * 1024 * 1024 // 5 MB per entry

    // -------------------- Public API (all suspend/IO) --------------------

    override suspend fun getModeFromZip(file: File): DataFileMode? = withContext(Dispatchers.IO) {
        extractTextEntry(file, "metadata.txt")?.let { meta ->
            Regex("""(?im)^\s*mode\s*=\s*([A-Za-z0-9_-]+)\s*$""")
                .find(meta)
                ?.groupValues?.getOrNull(1)
                ?.let { DataFileMode.fromString(it) }
                ?.also { Log.d("getModeFromZip", it.name) }
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
            Log.e("ZipFileServiceImpl", "Invalid content.json", ex)
            emptyMap()
        }
    }

    override suspend fun getFieldsFromJson(file: File): Map<String, String> = withContext(Dispatchers.IO) {
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
            Log.e("ZipFileServiceImpl", "Invalid content.json", ex)
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
            Log.e("ZipFileServiceImpl", "Failed to copy ZIP", e)
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
    override fun deletePrivateFile(file: File): Boolean =
        runCatching { file.delete() }.getOrDefault(false)

    override suspend fun createSharyZip(
        fields: List<FieldDomain>,
        metadata: Map<String, String>,
        mode: DataFileMode
    ): File = withContext(Dispatchers.IO) {
        val zipFile = File.createTempFile("shary_", ".zip", context.cacheDir)
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            val metaText = buildString {
                metadata.forEach { (k, v) -> append("$k=$v\n") }
                append("mode=${DataFileMode.toString(mode)}")
            }
            zip.putNextEntry(ZipEntry("metadata.txt"))
            zip.write(metaText.toByteArray())
            zip.closeEntry()

            val fieldsObj = JSONObject().apply {
                fields.forEach { put(it.key, it.value) }
            }
            val content = JSONObject(mapOf("fields" to fieldsObj))
            zip.putNextEntry(ZipEntry("content.json"))
            zip.write(content.toString().toByteArray())
            zip.closeEntry()
        }
        zipFile
    }


    // -------------------- Internals --------------------

    // In ZipFileServiceImpl
    override fun listPrivateZipFiles(): List<File> {
        return context.filesDir
            .listFiles { f -> f.extension == "zip" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    // In ZipFileServiceImpl
    override fun listPrivateJsonFiles(): List<File> {
        return context.filesDir
            .listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    // copiar ZIP desde SAF a sandbox privado
    /** Copy File from SAF to private sandbox */
    override fun loadZipFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val destFile = File(context.filesDir, uri.lastPathSegment!!)
            inputStream.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Copy File from SAF to private sandbox */
    override fun loadJsonFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val destFile = File(context.filesDir, uri.lastPathSegment!!)
            inputStream.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun extractTextEntry(file: File, fileName: String): String? =
        withContext(Dispatchers.IO) {
            var result: String? = null
            useZip(file) { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = safeZipName(entry.name)
                    if (VALID_METADATA_FILENAMES.contains(name)
                        || name == fileName) {
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
            val name = base.take(dot)
            val ext = base.substring(dot)
            "${name}_$id$ext"
        } else {
            "${base}_$id"
        }
    }
}
