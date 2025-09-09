package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag

interface FieldRepository {
    suspend fun getAllFields(): List<FieldDomain>
    suspend fun getFieldsByTag(tag: Tag): List<FieldDomain>
    suspend fun saveField(field: FieldDomain)
    suspend fun saveFieldIfNotExists(field: FieldDomain): Boolean
    suspend fun deleteField(key: String)
    suspend fun deleteFields(keys: List<String>)
    suspend fun updateValue(key: String, newValue: String)
    suspend fun updateAlias(key: String, newAlias: String)
    suspend fun updateTag(key: String, newTag: Tag)
}
