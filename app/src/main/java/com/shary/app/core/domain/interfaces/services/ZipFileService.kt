package com.shary.app.core.domain.interfaces.services

import android.net.Uri
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import java.io.File


interface ZipFileService {
    suspend fun getModeFromZip(file: File): DataFileMode?
    suspend fun getFieldsFromZip(file: File): Map<String, String>
    suspend fun getFieldsFromJson(file: File): Map<String, String>
    suspend fun validateZipStructure(file: File): Boolean
    suspend fun copyZipToPrivateStorage(uri: Uri): File?
    fun getFileNameFromUri(uri: Uri): String?
    fun isZipFile(uri: Uri): Boolean
    fun deletePrivateFile(file: File): Boolean
    fun listPrivateZipFiles(): List<File>
    fun listPrivateJsonFiles(): List<File>
    fun loadZipFromUri(uri: Uri): File?
    fun loadJsonFromUri(uri: Uri): File?

    // Builders
    suspend fun createSharyZip(
        fields: List<FieldDomain>,
        metadata: Map<String, String>,
        mode: DataFileMode
    ): File
}