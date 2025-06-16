package com.shary.app.repositories.fields

import android.content.Context
import androidx.datastore.core.DataStore
import com.shary.app.Field
import com.shary.app.FieldList
import com.shary.app.datastore.fieldListDataStore
import com.shary.app.services.security.CryptoManager
import kotlinx.coroutines.flow.first

class FieldRepositoryImpl(
    context: Context,
    private val cryptographyManager: CryptoManager
) : FieldRepository {
    private val dataStore: DataStore<FieldList> = context.fieldListDataStore
    
    private fun encryptField(field: Field): Field {
        val encryptedKey = cryptographyManager.encryptKeyRSA(field.key)
        val encryptedValue = cryptographyManager.encryptValueAES(field.value)
        val encryptedAlias = cryptographyManager.encryptValueAES(field.keyAlias)

        return field.toBuilder()
            .setKey(encryptedKey)
            .setValue(encryptedValue)
            .setKeyAlias(encryptedAlias)
            .build()
    }

    private fun decryptField(field: Field): Field {
        return try {
            val decryptedKey = cryptographyManager.decryptKeyRSA(field.key)
            val decryptedValue = cryptographyManager.decryptValueAES(field.value)
            val decryptedAlias = cryptographyManager.decryptValueAES(field.keyAlias)

            field.toBuilder()
                .setKey(decryptedKey)
                .setValue(decryptedValue)
                .setKeyAlias(decryptedAlias)
                .build()
        } catch (e: Exception) {
            // Corrupted or mismatched key — return raw
            field
        }
    }

    override suspend fun getAllFields(): List<Field> {
        return dataStore.data.first().fieldsList.map { decryptField(it) }
    }

    override suspend fun saveField(field: Field) {
        val encrypted = encryptField(field)
        dataStore.updateData { current ->
            current.toBuilder()
                .addFields(encrypted)
                .build()
        }
    }

    override suspend fun saveFieldIfNotExists(field: Field): Boolean {
        val existing = getAllFields()
        if (existing.none { it.key == field.key }) {
            saveField(field)
            return true
        }
        return false
    }

    override suspend fun deleteField(key: String) {
        dataStore.updateData { current ->
            val filtered = current.fieldsList.filter {
                try {
                    decryptField(it).key != key
                } catch (_: Exception) {
                    true
                }
            }
            current.toBuilder()
                .clearFields()
                .addAllFields(filtered)
                .build()
        }
    }

    override suspend fun updateValue(key: String, value: String) {
        dataStore.updateData { currentData ->
            val updatedFields = currentData.fieldsList.map { encryptedField ->
                try {
                    val decrypted = decryptField(encryptedField)
                    if (decrypted.key == key) {
                        encryptField(decrypted.toBuilder().setValue(value).build())
                    } else {
                        encryptedField
                    }
                } catch (e: Exception) {
                    encryptedField
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
            val updatedFields = currentData.fieldsList.map { encryptedField ->
                try {
                    val decrypted = decryptField(encryptedField)
                    if (decrypted.key == key) {
                        encryptField(decrypted.toBuilder().setKeyAlias(alias).build())
                    } else {
                        encryptedField
                    }
                } catch (e: Exception) {
                    encryptedField
                }
            }

            currentData.toBuilder()
                .clearFields()
                .addAllFields(updatedFields)
                .build()
        }
    }
}
