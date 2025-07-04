package com.shary.app.repositories.impl

import android.content.Context
import androidx.datastore.core.DataStore
import com.shary.app.Field
import com.shary.app.FieldList
import com.shary.app.datastore.fieldListDataStore
import com.shary.app.repositories.`interface`.FieldRepository
import kotlinx.coroutines.flow.first

class FieldRepositoryImpl(
    context: Context
) : FieldRepository {
    private val dataStore: DataStore<FieldList> = context.fieldListDataStore

    override suspend fun getAllFields(): List<Field> {
        val fieldList = dataStore.data.first()
        return fieldList.fieldsList
    }

    override suspend fun saveField(field: Field) {
        dataStore.updateData { current ->
            current.toBuilder()
                .addFields(field)
                .build()
        }
    }

    override suspend fun saveFieldIfNotExists(field: Field): Boolean {
        val existingFields = getAllFields()
        val fieldAlreadyExists = existingFields.any { it.key == field.key }

        if (!fieldAlreadyExists) {
            saveField(field)
            return true
        } else {
            return false
        }
    }

    override suspend fun deleteField(key: String){
        dataStore.updateData { current ->
            val updatedFields = current.fieldsList.filter { it.key != key }
            current.toBuilder()
                .clearFields()
                .addAllFields(updatedFields)
                .build()
        }
    }

    override suspend fun updateValue(key: String, value: String) {
        dataStore.updateData { currentData ->
            val updatedFields = buildList {
                var updated = false
                for (field in currentData.fieldsList) {
                    if (!updated && field.key == key) {
                        add(
                            field.toBuilder()
                                .setValue(value)
                                .build()
                        )
                        updated = true
                    } else {
                        add(field)
                    }
                }
            }

            currentData.toBuilder()
                .clearFields()
                .addAllFields(updatedFields)
                .build()
        }
    }

    override suspend fun updateAlias(key: String, alias: String) {
        dataStore.updateData { currentData ->
            val updatedFields = buildList {
                var updated = false
                for (field in currentData.fieldsList) {
                    if (!updated && field.key == key) {
                        add(
                            field.toBuilder()
                                .setKeyAlias(alias)
                                .build()
                        )
                        updated = true
                    } else {
                        add(field)
                    }
                }
            }

            currentData.toBuilder()
                .clearFields()
                .addAllFields(updatedFields)
                .build()
        }
    }
}
