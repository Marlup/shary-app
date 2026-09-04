package com.shary.app.infrastructure.persistance.repositories

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shary.app.core.domain.interfaces.repositories.FieldValueHistoryRepository
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.core.domain.types.valueobjects.RecoverableFieldValue
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.fieldValueHistoryDataStore by preferencesDataStore(name = "field_value_history_prefs")

@Singleton
class FieldValueHistoryRepositoryImpl @Inject constructor(
    private val context: Context,
    private val codec: FieldCodec
) : FieldValueHistoryRepository {

    private val dataStore = context.fieldValueHistoryDataStore

    companion object {
        private val SNAPSHOTS_JSON_KEY: Preferences.Key<String> =
            stringPreferencesKey("field_value_snapshots_json")
    }

    override val recoverableKeys: Flow<Set<String>> =
        dataStore.data.map { prefs ->
            val snapshotsMap = readSnapshotsMap(prefs[SNAPSHOTS_JSON_KEY])
            snapshotsMap.values.mapNotNull { encoded ->
                decodeSnapshot(encoded)?.key
                    ?.let { normalizeKey(it) }
                    ?.takeIf { it.isNotBlank() }
            }.toSet()
        }

    override suspend fun saveSnapshot(key: String, previousValue: String) {
        val normalized = normalizeKey(key)
        if (normalized.isBlank()) return

        val payload = JSONObject()
            .put("key", normalized)
            .put("value", previousValue)
            .put("updated_at", System.currentTimeMillis())
            .toString()
        val encrypted = codec.encode(payload, Purpose.ValueHistory)

        dataStore.edit { prefs ->
            val snapshots = readSnapshotsMap(prefs[SNAPSHOTS_JSON_KEY]).toMutableMap()
            snapshots[hashKey(normalized)] = encrypted
            prefs[SNAPSHOTS_JSON_KEY] = writeSnapshotsMap(snapshots)
        }
    }

    override suspend fun consumeSnapshot(key: String): RecoverableFieldValue? {
        val normalized = normalizeKey(key)
        if (normalized.isBlank()) return null

        val snapshots = readSnapshotsMap(dataStore.data.first()[SNAPSHOTS_JSON_KEY])
        val hashed = hashKey(normalized)
        val encoded = snapshots[hashed] ?: return null
        val snapshot = decodeSnapshot(encoded)
        clearSnapshot(normalized)
        return snapshot
    }

    override suspend fun clearSnapshot(key: String) {
        val normalized = normalizeKey(key)
        if (normalized.isBlank()) return

        dataStore.edit { prefs ->
            val snapshots = readSnapshotsMap(prefs[SNAPSHOTS_JSON_KEY]).toMutableMap()
            snapshots.remove(hashKey(normalized))
            prefs[SNAPSHOTS_JSON_KEY] = writeSnapshotsMap(snapshots)
        }
    }

    override suspend fun clearSnapshots(keys: List<String>) {
        val normalizedHashes = keys
            .map { normalizeKey(it) }
            .filter { it.isNotBlank() }
            .map { hashKey(it) }
            .toSet()

        if (normalizedHashes.isEmpty()) return

        dataStore.edit { prefs ->
            val snapshots = readSnapshotsMap(prefs[SNAPSHOTS_JSON_KEY]).toMutableMap()
            normalizedHashes.forEach { snapshots.remove(it) }
            prefs[SNAPSHOTS_JSON_KEY] = writeSnapshotsMap(snapshots)
        }
    }

    private fun normalizeKey(key: String): String = key.trim().lowercase()

    private fun hashKey(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun readSnapshotsMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            mutableMapOf<String, String>().apply {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val entryKey = keys.next()
                    val value = json.optString(entryKey, "")
                    if (value.isNotBlank()) put(entryKey, value)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun writeSnapshotsMap(map: Map<String, String>): String {
        val json = JSONObject()
        map.forEach { (key, value) -> json.put(key, value) }
        return json.toString()
    }

    private fun decodeSnapshot(encoded: String): RecoverableFieldValue? {
        return runCatching {
            val decoded = codec.decode(encoded, Purpose.ValueHistory)
            val json = JSONObject(decoded)
            RecoverableFieldValue(
                key = json.optString("key", ""),
                value = json.optString("value", ""),
                updatedAt = json.optLong("updated_at", 0L)
            )
        }.getOrNull()
    }
}
