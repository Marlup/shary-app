package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.types.valueobjects.RecoverableFieldValue
import kotlinx.coroutines.flow.Flow

interface FieldValueHistoryRepository {
    val recoverableKeys: Flow<Set<String>>

    suspend fun saveSnapshot(key: String, previousValue: String)
    suspend fun consumeSnapshot(key: String): RecoverableFieldValue?
    suspend fun clearSnapshot(key: String)
    suspend fun clearSnapshots(keys: List<String>)
}

