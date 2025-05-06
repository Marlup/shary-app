package com.shary.app.repositories.`interface`

import com.shary.app.Field

interface FieldRepository {
    suspend fun getAllFields(): List<Field>
    suspend fun saveField(field: Field)
    suspend fun saveFieldIfNotExists(field: Field): Boolean
    suspend fun deleteField(key: String): Boolean
}
