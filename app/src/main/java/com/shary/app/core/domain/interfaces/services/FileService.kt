package com.shary.app.core.domain.interfaces.services

import android.net.Uri
import java.io.File

interface FileService {
    suspend fun getModeFromZip(file: File): String?
    suspend fun validateZipStructure(file: File): Boolean
    suspend fun getFieldsFromZip(file: File): Map<String, String>
    suspend fun copyZipToPrivateStorage(uri: Uri): File?
    fun getFileNameFromUri(uri: Uri): String?
    fun isZipFile(uri: Uri): Boolean
    fun deletePrivateFile(file: File): Boolean
}