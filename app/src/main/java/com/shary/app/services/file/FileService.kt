package com.shary.app.services.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class FileService(private val context: Context) {

    fun getModeFromZip(file: File): String? {
        val metaContent = extractFileFromZip(file, "meta.txt") ?: return null
        val regex = Regex("mode=(\\w+)")
        val valueMode = regex.find(metaContent)?.groups?.get(1)?.value

        if (valueMode != null) {
            Log.d("getModeFromZip", valueMode)
        }

        return valueMode
    }

    fun validateZipStructure(file: File): Boolean {
        val required = setOf("meta.txt", "content.json")
        val found = mutableSetOf<String>()

        ZipInputStream(file.inputStream()).use { zip ->
            var entry: ZipEntry?
            while (zip.nextEntry.also { entry = it } != null) {
                entry?.name?.let { found.add(it) }
            }
        }
        return required.all { it in found }
    }

    fun getFieldsFromZip(file: File): Map<String, Map<String, String>> {
        val json = extractFileFromZip(file, "content.json") ?: return emptyMap()
        val parsed = JSONObject(json)
        val dataObj = parsed.optJSONObject("fields") ?: JSONObject()
        val data = mutableMapOf<String, String>()
        dataObj.keys().forEach { key ->
            data[key] = dataObj.getString(key)
        }
        return mapOf("fields" to data)
    }

    private fun extractFileFromZip(file: File, fileName: String): String? {
        ZipInputStream(file.inputStream()).use { zipStream ->
            var entry: ZipEntry?
            while (zipStream.nextEntry.also { entry = it } != null) {
                if (entry?.name == fileName) {
                    return zipStream.bufferedReader().use { it.readText() }
                }
            }
        }
        return null
    }

    fun copyZipToPrivateStorage(uri: Uri): File? {
        val fileName = getFileNameFromUri(uri) ?: return null
        val destFile = File(context.filesDir, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            Log.e("FileService", "Failed to copy ZIP to private storage", e)
            null
        }
    }

    fun getFileNameFromUri(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return it.getString(index)
                }
            }
        }
        return null
    }

    fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    fun isZipFile(uri: Uri): Boolean {
        return getMimeType(uri) == "application/zip"
    }

    fun makePublicResourcesDirectory() {
        // No-op for SAF-based storage
    }
}
