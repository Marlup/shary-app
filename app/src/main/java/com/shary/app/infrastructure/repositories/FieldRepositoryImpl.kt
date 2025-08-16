package com.shary.app.infrastructure.repositories

import androidx.datastore.core.DataStore
import com.shary.app.FieldList
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.infrastructure.mappers.toDomain
import com.shary.app.infrastructure.mappers.toProto
import com.shary.app.core.domain.interfaces.security.FieldCodec
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class FieldRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<FieldList>,
    private val codec: FieldCodec
) : FieldRepository {

    // =============================== Domain API ============================

    override suspend fun getAllFields(): List<FieldDomain> {
        val fieldProtoList = dataStore.data.first().fieldsList
        return fieldProtoList.map { it.toDomain(codec) }
    }

    override suspend fun getFieldsByTag(tag: UiFieldTag): List<FieldDomain> {
        val wanted = UiFieldTag.toString(tag).lowercase()
        return dataStore.data.first().fieldsList
            .map { it.toDomain(codec) }
            .filter { UiFieldTag.toString(it.tag).lowercase() == wanted }
    }

    override suspend fun saveField(field: FieldDomain) {
        val encrypted = field.toProto(codec)
        dataStore.updateData { current ->
            current.toBuilder()
                .addFields(encrypted)
                .build()
        }
    }

    override suspend fun saveFieldIfNotExists(field: FieldDomain): Boolean {
        // Compare by decrypted key in clear text
        val existing = getAllFields()
        return if (existing.none { it.key.equals(field.key, ignoreCase = true) }) {
            saveField(field)
            true
        } else {
            false
        }
    }

    override suspend fun deleteField(key: String) {
        dataStore.updateData { current ->
            val filtered = current.fieldsList.filter { proto ->
                val domainKey = proto.toDomain(codec).key
                !domainKey.equals(key, ignoreCase = true)
            }
            current.toBuilder()
                .clearFields()
                .addAllFields(filtered)
                .build()
        }
    }

    override suspend fun updateValue(key: String, value: String) {
        dataStore.updateData { current ->
            val updated = current.fieldsList.map { proto ->
                val domain = proto.toDomain(codec)
                if (domain.key.equals(key, ignoreCase = true)) {
                    domain.copy(value = value).toProto(codec)
                } else {
                    proto
                }
            }
            current.toBuilder()
                .clearFields()
                .addAllFields(updated)
                .build()
        }
    }

    override suspend fun updateAlias(key: String, alias: String) {
        dataStore.updateData { current ->
            val updated = current.fieldsList.map { proto ->
                val domain = proto.toDomain(codec)
                if (domain.key.equals(key, ignoreCase = true)) {
                    domain.copy(keyAlias = alias.ifBlank { null }).toProto(codec)
                } else {
                    proto
                }
            }
            current.toBuilder()
                .clearFields()
                .addAllFields(updated)
                .build()
        }
    }

    override suspend fun updateTag(key: String, tag: UiFieldTag) {
        dataStore.updateData { current ->
            val updated = current.fieldsList.map { proto ->
                val domain = proto.toDomain(codec)
                if (domain.key.equals(key, ignoreCase = true)) {
                    domain.copy(tag = tag).toProto(codec)
                } else {
                    proto
                }
            }
            current.toBuilder()
                .clearFields()
                .addAllFields(updated)
                .build()
        }
    }
}
