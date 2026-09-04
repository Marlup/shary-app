package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import kotlinx.coroutines.flow.Flow

data class FieldLoadSnapshot(
    val fields: List<FieldDomain>,
    val attemptedCount: Int,
    val totalCount: Int,
    val failureCount: Int,
    val complete: Boolean
)

interface FieldRepository {
    suspend fun getAllFields(): Flow<List<FieldDomain>>
    suspend fun getAllFieldsProgressive(
        firstBatchSize: Int = 5,
        chunkSize: Int = 25
    ): Flow<List<FieldDomain>>
    suspend fun getFieldLoadSnapshots(
        firstBatchSize: Int = 5,
        chunkSize: Int = 25
    ): Flow<FieldLoadSnapshot>
    suspend fun getFieldsByTag(tag: Tag): List<FieldDomain>
    suspend fun saveField(field: FieldDomain)
    suspend fun saveFieldIfNotExists(field: FieldDomain): Boolean
    suspend fun deleteField(key: String)
    suspend fun deleteFields(keys: List<String>)
    suspend fun updateValue(key: String, newValue: String)
    suspend fun updateAlias(key: String, newAlias: String)
    suspend fun updateTag(key: String, newTag: Tag)
    suspend fun updateMcpShareable(key: String, shareable: Boolean)
}
