package com.shary.app.core.domain.interfaces.services

import android.net.Uri
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import java.io.File


interface JsonFileService {
    suspend fun getModeFromJson(file: File): DataFileMode?
    suspend fun getFieldsFromJson(file: File): Map<String, String>
    suspend fun validateJsonStructure(file: File): Boolean
    suspend fun copyJsonToPrivateStorage(uri: Uri): File?
    fun getFileNameFromUri(uri: Uri): String?
    fun listPrivateJsonFiles(): List<File>

    // Builders
    suspend fun createSharyJson(
        fields: List<FieldDomain>,
        metadata: Map<String, String>,
        mode: DataFileMode
    ): File

    // Extras (needed for ViewModel)
    fun loadJsonFromUri(uri: Uri): File?
    fun deletePrivateFile(file: File): Boolean
}
