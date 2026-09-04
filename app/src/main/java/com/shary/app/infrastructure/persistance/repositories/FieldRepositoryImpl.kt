package com.shary.app.infrastructure.persistance.repositories

import androidx.datastore.core.DataStore
import com.shary.app.FieldList
import com.shary.app.core.domain.interfaces.repositories.FieldLoadSnapshot
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.infrastructure.mappers.toDomainResult
import com.shary.app.infrastructure.mappers.toProto
import com.shary.app.core.domain.interfaces.security.FieldCodec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Singleton
class FieldRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<FieldList>,
    private val codec: FieldCodec
) : FieldRepository {

    // =============================== Domain API ============================

    override suspend fun getAllFields(): Flow<List<FieldDomain>> {
        return dataStore.data.map { fieldProtoList ->
            fieldProtoList.fieldsList.mapNotNull { it.toDomainResult(codec).getOrNull() }
        }
    }

    override suspend fun getAllFieldsProgressive(
        firstBatchSize: Int,
        chunkSize: Int
    ): Flow<List<FieldDomain>> =
        getFieldLoadSnapshots(firstBatchSize, chunkSize).map { it.fields }

    override suspend fun getFieldLoadSnapshots(
        firstBatchSize: Int,
        chunkSize: Int
    ): Flow<FieldLoadSnapshot> = flow {
        val safeFirstBatch = firstBatchSize.coerceAtLeast(1)
        val safeChunkSize = chunkSize.coerceAtLeast(1)

        val encryptedFields = dataStore.data.first().fieldsList
        if (encryptedFields.isEmpty()) {
            emit(
                FieldLoadSnapshot(
                    fields = emptyList(),
                    attemptedCount = 0,
                    totalCount = 0,
                    failureCount = 0,
                    complete = true
                )
            )
            return@flow
        }

        val progressive = ArrayList<FieldDomain>(encryptedFields.size)
        var failures = 0
        encryptedFields.forEachIndexed { index, proto ->
            proto.toDomainResult(codec).fold(
                onSuccess = { progressive += it },
                onFailure = { failures += 1 }
            )
            val attemptedCount = index + 1
            val isWarmupReady = progressive.size >= safeFirstBatch
            val isChunkBoundary = isWarmupReady &&
                ((progressive.size - safeFirstBatch) % safeChunkSize == 0)
            val isLast = attemptedCount == encryptedFields.size
            if (isWarmupReady || isChunkBoundary || isLast) {
                emit(
                    FieldLoadSnapshot(
                        fields = progressive.toList(),
                        attemptedCount = attemptedCount,
                        totalCount = encryptedFields.size,
                        failureCount = failures,
                        complete = isLast
                    )
                )
            }
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun getFieldsByTag(tag: Tag): List<FieldDomain> {
        val wanted = tag.toTagString().lowercase()
        return dataStore.data.first().fieldsList
            .mapNotNull { it.toDomainResult(codec).getOrNull() }
            .filter { it.tag.toTagString().lowercase() == wanted }
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
        val existing = dataStore.data.first().fieldsList.mapNotNull {
            it.toDomainResult(codec).getOrNull()
        }
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
                val domainKey = proto.toDomainResult(codec).getOrNull()?.key
                domainKey == null || !domainKey.equals(key, ignoreCase = true)
            }
            current.toBuilder()
                .clearFields()
                .addAllFields(filtered)
                .build()
        }
    }

    override suspend fun deleteFields(keys: List<String>) {
        dataStore.updateData { current ->
            val filtered = current.fieldsList.filter { proto ->
                val domainKey = proto.toDomainResult(codec).getOrNull()?.key
                domainKey == null || keys.none { it.equals(domainKey, ignoreCase = true) }
            }
            current.toBuilder()
                .clearFields()
                .addAllFields(filtered)
                .build()
        }
    }


    override suspend fun updateValue(key: String, newValue: String) {
        dataStore.updateData { current ->
            val updated = current.fieldsList.map { proto ->
                val domain = proto.toDomainResult(codec).getOrNull()
                if (domain != null && domain.key.equals(key, ignoreCase = true)) {
                    domain.copy(value = newValue).toProto(codec)
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

    override suspend fun updateAlias(key: String, newAlias: String) {
        dataStore.updateData { current ->
            val updated = current.fieldsList.map { proto ->
                val domain = proto.toDomainResult(codec).getOrNull()
                if (domain != null && domain.key.equals(key, ignoreCase = true)) {
                    domain.copy(keyAlias = newAlias).toProto(codec)
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

    override suspend fun updateTag(key: String, newTag: Tag) {
        dataStore.updateData { current ->
            val updated = current.fieldsList.map { proto ->
                val domain = proto.toDomainResult(codec).getOrNull()
                if (domain != null && domain.key.equals(key, ignoreCase = true)) {
                    domain.copy(tag = newTag).toProto(codec)
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

    override suspend fun updateMcpShareable(key: String, shareable: Boolean) {
        dataStore.updateData { current ->
            val updated = current.fieldsList.map { proto ->
                val domain = proto.toDomainResult(codec).getOrNull()
                if (domain != null && domain.key.equals(key, ignoreCase = true)) {
                    domain.copy(isMcpShareable = shareable).toProto(codec)
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
