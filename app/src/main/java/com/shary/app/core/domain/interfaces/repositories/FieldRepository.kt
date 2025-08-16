package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.UiFieldTag

interface FieldRepository {
    suspend fun getAllFields(): List<FieldDomain>
    suspend fun getFieldsByTag(tag: UiFieldTag): List<FieldDomain>
    suspend fun saveField(field: FieldDomain)
    suspend fun saveFieldIfNotExists(field: FieldDomain): Boolean
    suspend fun deleteField(key: String)
    suspend fun updateValue(key: String, value: String)
    suspend fun updateAlias(key: String, alias: String)
    suspend fun updateTag(key: String, tag: UiFieldTag)
}
