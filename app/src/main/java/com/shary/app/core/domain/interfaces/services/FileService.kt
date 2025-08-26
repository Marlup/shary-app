package com.shary.app.core.domain.interfaces.services

import android.net.Uri
import com.shary.app.core.domain.types.enums.DataFileMode
import java.io.File


interface FileService {
    suspend fun getModeFromZip(file: File): DataFileMode?
    suspend fun validateZipStructure(file: File): Boolean
    suspend fun getFieldsFromZip(file: File): Map<String, String>
    suspend fun copyZipToPrivateStorage(uri: Uri): File?
    fun getFileNameFromUri(uri: Uri): String?
    fun isZipFile(uri: Uri): Boolean
    fun deletePrivateFile(file: File): Boolean
    fun listPrivateZipFiles(): List<File>
}