package com.shary.app.infrastructure.persistance.repositories

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shary.app.application.connector.ConnectorMutationAction
import com.shary.app.application.connector.ConnectorMutationType
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.enums.ConnectorAuditDecision
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.enums.ConnectorPolicyMode
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.connectorDataStore by preferencesDataStore("connector_prefs")

@Singleton
class ConnectorRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ConnectorRepository {
    private val dataStore = context.connectorDataStore

    private object Keys {
        val CLIENTS = stringPreferencesKey("connector_clients_json")
        val SESSIONS = stringPreferencesKey("connector_sessions_json")
        val POLICIES = stringPreferencesKey("connector_policies_json")
        val AUDIT_EVENTS = stringPreferencesKey("connector_audit_events_json")
        val PENDING_ACTIONS = stringPreferencesKey("connector_pending_actions_json")
    }

    override val clients: Flow<List<ConnectorClient>> = dataStore.data.map { prefs ->
        parseClients(prefs[Keys.CLIENTS])
    }

    override val sessions: Flow<List<ConnectorSession>> = dataStore.data.map { prefs ->
        parseSessions(prefs[Keys.SESSIONS])
    }

    override val policies: Flow<List<ConnectorPolicy>> = dataStore.data.map { prefs ->
        parsePolicies(prefs[Keys.POLICIES])
    }

    override val auditEvents: Flow<List<ConnectorAuditEvent>> = dataStore.data.map { prefs ->
        parseAuditEvents(prefs[Keys.AUDIT_EVENTS])
    }

    override val pendingActions: Flow<List<ConnectorPendingAction>> = dataStore.data.map { prefs ->
        parsePendingActions(prefs[Keys.PENDING_ACTIONS])
    }

    override suspend fun upsertClient(client: ConnectorClient) {
        dataStore.edit { prefs ->
            val updated = parseClients(prefs[Keys.CLIENTS]).toMutableList()
            val index = updated.indexOfFirst { it.clientId == client.clientId }
            if (index >= 0) updated[index] = client else updated += client
            prefs[Keys.CLIENTS] = writeClients(updated)
        }
    }

    override suspend fun upsertSession(session: ConnectorSession) {
        dataStore.edit { prefs ->
            val updated = parseSessions(prefs[Keys.SESSIONS]).toMutableList()
            val index = updated.indexOfFirst { it.sessionId == session.sessionId }
            if (index >= 0) updated[index] = session else updated += session
            prefs[Keys.SESSIONS] = writeSessions(updated)
        }
    }

    override suspend fun upsertPolicy(policy: ConnectorPolicy) {
        dataStore.edit { prefs ->
            val updated = parsePolicies(prefs[Keys.POLICIES]).toMutableList()
            val index = updated.indexOfFirst { it.clientId == policy.clientId }
            if (index >= 0) updated[index] = policy else updated += policy
            prefs[Keys.POLICIES] = writePolicies(updated)
        }
    }

    override suspend fun appendAuditEvent(event: ConnectorAuditEvent) {
        dataStore.edit { prefs ->
            val updated = parseAuditEvents(prefs[Keys.AUDIT_EVENTS]).toMutableList()
            updated += event
            prefs[Keys.AUDIT_EVENTS] = writeAuditEvents(updated)
        }
    }

    override suspend fun upsertPendingAction(pendingAction: ConnectorPendingAction) {
        dataStore.edit { prefs ->
            val updated = parsePendingActions(prefs[Keys.PENDING_ACTIONS]).toMutableList()
            val index = updated.indexOfFirst { it.pendingId == pendingAction.pendingId }
            if (index >= 0) updated[index] = pendingAction else updated += pendingAction
            prefs[Keys.PENDING_ACTIONS] = writePendingActions(updated)
        }
    }

    override suspend fun getClient(clientId: String): ConnectorClient? =
        clients.first().firstOrNull { it.clientId == clientId }

    override suspend fun getSession(sessionId: String): ConnectorSession? =
        sessions.first().firstOrNull { it.sessionId == sessionId }

    override suspend fun getLatestSessionForClient(clientId: String): ConnectorSession? =
        sessions.first()
            .filter { it.clientId == clientId }
            .maxByOrNull { it.issuedAt }

    override suspend fun getSessionByAccessTokenHash(accessTokenHash: String): ConnectorSession? =
        sessions.first().firstOrNull { it.accessTokenHash == accessTokenHash }

    override suspend fun getPolicy(clientId: String): ConnectorPolicy? =
        policies.first().firstOrNull { it.clientId == clientId }

    private fun parseClients(raw: String?): List<ConnectorClient> = readArray(raw).mapNotNull { obj ->
        runCatching {
            ConnectorClient(
                clientId = obj.optString("client_id", ""),
                displayName = obj.optString("display_name", ""),
                createdAt = obj.optLong("created_at", 0L),
                status = ConnectorClientStatus.valueOf(obj.optString("status", ConnectorClientStatus.PAIRED_LIMITED.name)),
                lastActivityAt = if (obj.has("last_activity_at")) obj.optLong("last_activity_at") else null
            )
        }.getOrNull()
    }

    private fun parseSessions(raw: String?): List<ConnectorSession> = readArray(raw).mapNotNull { obj ->
        runCatching {
            ConnectorSession(
                sessionId = obj.optString("session_id", ""),
                clientId = obj.optString("client_id", ""),
                accessTokenHash = obj.optString("access_token_hash", ""),
                refreshSecretHash = obj.optString("refresh_secret_hash", ""),
                issuedAt = obj.optLong("issued_at", 0L),
                expiresAt = obj.optLong("expires_at", 0L),
                revokedAt = if (obj.has("revoked_at")) obj.optLong("revoked_at") else null
            )
        }.getOrNull()
    }

    private fun parsePolicies(raw: String?): List<ConnectorPolicy> = readArray(raw).mapNotNull { obj ->
        runCatching {
            ConnectorPolicy(
                clientId = obj.optString("client_id", ""),
                mode = ConnectorPolicyMode.valueOf(obj.optString("mode", ConnectorPolicyMode.STRICT.name)),
                askEveryTime = obj.optBoolean("ask_every_time", true),
                allowedCapabilities = readArray(obj.optJSONArray("allowed_capabilities")?.toString())
                    .mapNotNull { capObj ->
                        runCatching {
                            ConnectorCapability.valueOf(capObj.optString("value"))
                        }.getOrNull()
                    }.toSet(),
                updatedAt = obj.optLong("updated_at", 0L)
            )
        }.getOrNull()
    }

    private fun parseAuditEvents(raw: String?): List<ConnectorAuditEvent> = readArray(raw).mapNotNull { obj ->
        runCatching {
            val metadataObj = obj.optJSONObject("metadata") ?: JSONObject()
            val metadata = mutableMapOf<String, String>()
            val keys = metadataObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                metadata[key] = metadataObj.optString(key, "")
            }

            ConnectorAuditEvent(
                eventId = obj.optString("event_id", ""),
                timestamp = obj.optLong("timestamp", 0L),
                clientId = obj.optString("client_id", ""),
                requestId = obj.optString("request_id", ""),
                operation = obj.optString("operation", ""),
                targetType = obj.optString("target_type", ""),
                targetIds = readStringArray(obj.optJSONArray("target_ids")),
                decision = ConnectorAuditDecision.valueOf(
                    obj.optString("decision", ConnectorAuditDecision.DENIED.name)
                ),
                reasonCode = obj.optString("reason_code", ""),
                metadata = metadata
            )
        }.getOrNull()
    }

    private fun parsePendingActions(raw: String?): List<ConnectorPendingAction> = readArray(raw).mapNotNull { obj ->
        runCatching {
            val summaryObj = obj.optJSONObject("preview_summary") ?: JSONObject()
            val summary = mutableMapOf<String, String>()
            val summaryKeys = summaryObj.keys()
            while (summaryKeys.hasNext()) {
                val key = summaryKeys.next()
                summary[key] = summaryObj.optString(key, "")
            }

            ConnectorPendingAction(
                pendingId = obj.optString("pending_id", ""),
                clientId = obj.optString("client_id", ""),
                requestId = obj.optString("request_id", ""),
                proposedActions = parseActionsArray(obj.optJSONArray("proposed_actions")),
                previewSummary = summary,
                status = obj.optString("status", "previewed"),
                updatedAt = obj.optLong("updated_at", 0L)
            )
        }.getOrNull()
    }

    private fun writeClients(items: List<ConnectorClient>): String = JSONArray().apply {
        items.forEach { item ->
            put(
                JSONObject()
                    .put("client_id", item.clientId)
                    .put("display_name", item.displayName)
                    .put("created_at", item.createdAt)
                    .put("status", item.status.name)
                    .put("last_activity_at", item.lastActivityAt)
            )
        }
    }.toString()

    private fun writeSessions(items: List<ConnectorSession>): String = JSONArray().apply {
        items.forEach { item ->
            put(
                JSONObject()
                    .put("session_id", item.sessionId)
                    .put("client_id", item.clientId)
                    .put("access_token_hash", item.accessTokenHash)
                    .put("refresh_secret_hash", item.refreshSecretHash)
                    .put("issued_at", item.issuedAt)
                    .put("expires_at", item.expiresAt)
                    .put("revoked_at", item.revokedAt)
            )
        }
    }.toString()

    private fun writePolicies(items: List<ConnectorPolicy>): String = JSONArray().apply {
        items.forEach { item ->
            val caps = JSONArray()
            item.allowedCapabilities.forEach { cap ->
                caps.put(JSONObject().put("value", cap.name))
            }
            put(
                JSONObject()
                    .put("client_id", item.clientId)
                    .put("mode", item.mode.name)
                    .put("ask_every_time", item.askEveryTime)
                    .put("allowed_capabilities", caps)
                    .put("updated_at", item.updatedAt)
            )
        }
    }.toString()

    private fun writeAuditEvents(items: List<ConnectorAuditEvent>): String = JSONArray().apply {
        items.forEach { item ->
            val metadata = JSONObject()
            item.metadata.forEach { (k, v) -> metadata.put(k, v) }
            put(
                JSONObject()
                    .put("event_id", item.eventId)
                    .put("timestamp", item.timestamp)
                    .put("client_id", item.clientId)
                    .put("request_id", item.requestId)
                    .put("operation", item.operation)
                    .put("target_type", item.targetType)
                    .put("target_ids", JSONArray(item.targetIds))
                    .put("decision", item.decision.name)
                    .put("reason_code", item.reasonCode)
                    .put("metadata", metadata)
            )
        }
    }.toString()

    private fun writePendingActions(items: List<ConnectorPendingAction>): String = JSONArray().apply {
        items.forEach { item ->
            val summary = JSONObject()
            item.previewSummary.forEach { (k, v) -> summary.put(k, v) }
            put(
                JSONObject()
                    .put("pending_id", item.pendingId)
                    .put("client_id", item.clientId)
                    .put("request_id", item.requestId)
                    .put("proposed_actions", writeActionsArray(item.proposedActions))
                    .put("preview_summary", summary)
                    .put("status", item.status)
                    .put("updated_at", item.updatedAt)
            )
        }
    }.toString()

    private fun readArray(raw: String?): List<JSONObject> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.getJSONObject(i))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) add(arr.optString(i, ""))
        }.filter { it.isNotBlank() }
    }

    private fun parseActionsArray(arr: JSONArray?): List<ConnectorMutationAction> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val type = runCatching { ConnectorMutationType.valueOf(obj.optString("type")) }.getOrNull()
                    ?: continue
                add(
                    ConnectorMutationAction(
                        type = type,
                        key = obj.optString("key").takeIf { it.isNotBlank() },
                        value = obj.optString("value").takeIf { it.isNotBlank() },
                        keyAlias = obj.optString("key_alias").takeIf { it.isNotBlank() },
                        tag = obj.optString("tag").takeIf { it.isNotBlank() },
                        email = obj.optString("email").takeIf { it.isNotBlank() },
                        username = obj.optString("username").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun writeActionsArray(actions: List<ConnectorMutationAction>): JSONArray = JSONArray().apply {
        actions.forEach { action ->
            put(
                JSONObject()
                    .put("type", action.type.name)
                    .put("key", action.key)
                    .put("value", action.value)
                    .put("key_alias", action.keyAlias)
                    .put("tag", action.tag)
                    .put("email", action.email)
                    .put("username", action.username)
            )
        }
    }
}
