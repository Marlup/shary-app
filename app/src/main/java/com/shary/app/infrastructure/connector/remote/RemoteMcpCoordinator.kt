package com.shary.app.infrastructure.connector.remote

import android.content.Context
import android.net.Uri
import com.google.firebase.messaging.FirebaseMessaging
import com.shary.app.BuildConfig
import com.shary.app.application.connector.ConnectorOwnerResolver
import com.shary.app.application.connector.ManageConnectorsUseCase
import com.shary.app.application.connector.PairConnectorRequest
import com.shary.app.application.connector.PairConnectorUseCase
import com.shary.app.application.connector.RefreshConnectorSessionRequest
import com.shary.app.application.connector.RefreshConnectorSessionUseCase
import com.shary.app.core.domain.interfaces.repositories.SettingsRepository
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.valueobjects.ConnectorOwnerBinding
import com.shary.app.infrastructure.connector.mcp.SharyMcpDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

@Singleton
class RemoteMcpCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val api: RemoteMcpApi,
    private val secureStore: RemoteMcpSecureStore,
    private val ownerResolver: ConnectorOwnerResolver,
    private val pairConnector: PairConnectorUseCase,
    private val refreshConnectorSession: RefreshConnectorSessionUseCase,
    private val manageConnectors: ManageConnectorsUseCase,
    private val dispatcher: SharyMcpDispatcher,
    private val settingsRepository: SettingsRepository
) {
    private val preferences = context.getSharedPreferences("remote_mcp_state", Context.MODE_PRIVATE)
    private val operationMutex = Mutex()

    private val _approvalState = MutableStateFlow<RemoteApprovalState>(RemoteApprovalState.Idle)
    val approvalState: StateFlow<RemoteApprovalState> = _approvalState.asStateFlow()

    private val _pendingOperations = MutableStateFlow(secureStore.pendingOperations())
    val pendingOperations: StateFlow<List<PendingRemoteOperation>> = _pendingOperations.asStateFlow()
    private val _renewalRequired = MutableStateFlow(expiringCredentials())
    val renewalRequired: StateFlow<List<RemoteGrantCredential>> = _renewalRequired.asStateFlow()

    suspend fun inspectApprovalLink(uri: Uri) {
        if (!isCapabilityEnabled()) {
            _approvalState.value = RemoteApprovalState.Failed(
                "MCP access is disabled. Open Connect and enable MCP capability before approving an agent."
            )
            return
        }
        val authorizationId = uri.getQueryParameter("authorization_id")
        val invitationId = uri.getQueryParameter("invitation_id")
        val kind: RemoteApprovalKind
        val approvalId: String
        when {
            !authorizationId.isNullOrBlank() -> {
                kind = RemoteApprovalKind.OAUTH
                approvalId = authorizationId
            }
            !invitationId.isNullOrBlank() -> {
                kind = RemoteApprovalKind.INVITATION
                approvalId = invitationId
            }
            else -> return
        }
        _approvalState.value = RemoteApprovalState.Loading
        runCatching { api.approvalDetails(kind, approvalId) }
            .onSuccess { _approvalState.value = RemoteApprovalState.Ready(it) }
            .onFailure {
                _approvalState.value = RemoteApprovalState.Failed(
                    it.message ?: "Unable to inspect this MCP authorization request."
                )
            }
    }

    suspend fun approveCurrent(ownerAlias: String, approvedScopes: Set<String>) {
        if (!isCapabilityEnabled()) {
            _approvalState.value = RemoteApprovalState.Failed(
                "MCP access is disabled. Enable MCP capability to continue.",
                details = when (val state = _approvalState.value) {
                    is RemoteApprovalState.Ready -> state.details
                    is RemoteApprovalState.Failed -> state.details
                    else -> null
                }
            )
            return
        }
        val ready = when (val state = _approvalState.value) {
            is RemoteApprovalState.Ready -> state.details
            is RemoteApprovalState.Failed -> state.details
            else -> null
        } ?: return
        require(ownerAlias.isNotBlank()) { "Vault alias cannot be blank." }
        require(approvedScopes.isNotEmpty()) { "Select at least one MCP scope." }
        _approvalState.value = RemoteApprovalState.Processing(ready)
        runCatching {
            registerDevice()
            val binding = api.approve(
                details = ready,
                vaultId = ownerResolver.vaultId(),
                deviceId = ownerResolver.deviceId(),
                ownerAlias = ownerAlias.trim(),
                approvedScopes = approvedScopes
            )
            bindLocalConnector(ready.clientName, binding)
            RemoteMcpWorkScheduler.schedule(context)
        }.onSuccess {
            _approvalState.value = RemoteApprovalState.Completed(
                "This agent can now access the approved Shary vault scopes."
            )
        }.onFailure {
            _approvalState.value = RemoteApprovalState.Failed(
                message = it.message ?: "MCP authorization failed.",
                details = ready
            )
        }
    }

    fun dismissApproval() {
        _approvalState.value = RemoteApprovalState.Idle
    }

    suspend fun registerDevice(displayName: String = android.os.Build.MODEL) {
        registerDevice(displayName, scheduleWork = true)
    }

    private suspend fun registerDevice(displayName: String, scheduleWork: Boolean) {
        if (!isCapabilityEnabled()) return
        val fcmToken = FirebaseMessaging.getInstance().token.await()
        api.registerDevice(
            deviceId = ownerResolver.deviceId(),
            vaultId = ownerResolver.vaultId(),
            displayName = displayName,
            appVersion = BuildConfig.VERSION_NAME,
            fcmToken = fcmToken
        )
        preferences.edit().putBoolean(KEY_ENABLED, true).apply()
        if (scheduleWork) RemoteMcpWorkScheduler.schedule(context)
    }

    suspend fun sync() = operationMutex.withLock {
        if (!preferences.getBoolean(KEY_ENABLED, false) || !isCapabilityEnabled()) return@withLock
        registerDevice(android.os.Build.MODEL, scheduleWork = false)
        var firstFailure: Throwable? = null
        api.claimOperations(ownerResolver.deviceId()).forEach { operation ->
            runCatching {
                val toolName = operation.command.optString("tool_name")
                if (operation.requiresConfirmation && operation.status == "awaiting_confirmation") {
                    secureStore.savePending(
                        PendingRemoteOperation(
                            operationId = operation.operationId,
                            grantId = operation.grantId,
                            toolName = toolName,
                            commandJson = operation.command.toString(),
                            expiresAt = operation.expiresAt
                        )
                    )
                } else {
                    executeOperation(operation.operationId, operation.grantId, operation.command)
                    secureStore.removePending(operation.operationId)
                }
            }.onFailure { if (firstFailure == null) firstFailure = it }
        }
        refreshPendingState()
        refreshRenewalState()
        firstFailure?.let { throw it }
    }

    suspend fun approveAndExecute(operationId: String) = operationMutex.withLock {
        val pending = secureStore.pendingOperations().firstOrNull { it.operationId == operationId }
            ?: error("Pending MCP operation not found.")
        api.approveOperation(ownerResolver.deviceId(), operationId)
        executeOperation(operationId, pending.grantId, JSONObject(pending.commandJson))
        secureStore.removePending(operationId)
        refreshPendingState()
    }

    suspend fun rejectOperation(operationId: String) = operationMutex.withLock {
        api.rejectOperation(ownerResolver.deviceId(), operationId)
        secureStore.removePending(operationId)
        refreshPendingState()
    }

    suspend fun renewGrant(grantId: String) = operationMutex.withLock {
        val credential = secureStore.credential(grantId) ?: error("Local MCP grant not found.")
        val expiresAt = api.renewGrant(grantId)
        secureStore.saveCredential(credential.copy(grantExpiresAt = expiresAt))
        refreshRenewalState()
    }

    suspend fun revokeAccess(grantId: String, clientId: String) = operationMutex.withLock {
        api.revokeGrant(grantId)
        secureStore.removeCredential(grantId)
        manageConnectors.revoke(clientId)
        refreshRenewalState()
    }

    fun applyCapabilityChange(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            RemoteMcpWorkScheduler.schedule(context)
        } else {
            RemoteMcpWorkScheduler.cancel(context)
            RemoteMcpNotification.clear(context)
            if (_approvalState.value !is RemoteApprovalState.Idle) {
                _approvalState.value = RemoteApprovalState.Failed(
                    "MCP access is disabled. Re-enable MCP capability from Connect to continue."
                )
            }
        }
        refreshPendingState()
        refreshRenewalState()
    }

    private suspend fun bindLocalConnector(clientName: String, binding: RemoteGrantBinding) {
        secureStore.credential(binding.grantId)?.let { existing ->
            api.bindGrant(binding.grantId, existing.clientId)
            return
        }
        val capabilities = binding.scopes.mapNotNull(::capabilityForScope).toSet()
            .ifEmpty { setOf(ConnectorCapability.VAULT_ACCESS) }
        val pairing = pairConnector(
            PairConnectorRequest(
                displayName = "Remote ${clientName.take(80)}",
                allowedCapabilities = capabilities,
                sessionTtlSeconds = 24 * 60 * 60,
                ownerBinding = ConnectorOwnerBinding(
                    ownerUid = requireNotNull(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser).uid,
                    vaultId = binding.vaultId,
                    agentProfileId = binding.agentProfileId,
                    grantId = binding.grantId
                )
            )
        )
        secureStore.saveCredential(
            RemoteGrantCredential(
                grantId = binding.grantId,
                agentProfileId = binding.agentProfileId,
                clientId = pairing.clientId,
                sessionId = pairing.sessionId,
                accessToken = pairing.accessToken,
                refreshSecret = pairing.refreshSecret,
                expiresAt = pairing.expiresAtEpochSeconds,
                grantExpiresAt = binding.expiresAt
            )
        )
        try {
            api.bindGrant(binding.grantId, pairing.clientId)
        } catch (error: Throwable) {
            secureStore.removeCredential(binding.grantId)
            manageConnectors.revoke(pairing.clientId)
            runCatching { api.revokeGrant(binding.grantId) }
            throw error
        }
    }

    private suspend fun executeOperation(
        operationId: String,
        grantId: String,
        command: JSONObject
    ) {
        val credential = activeCredential(grantId)
        val toolName = command.getString("tool_name")
        val arguments = command.optJSONObject("arguments") ?: JSONObject()
        if (toolName in WRITE_TOOLS) arguments.put("confirm", true)
        val rpc = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", command.optString("request_id", operationId))
            .put("method", "tools/call")
            .put(
                "params",
                JSONObject()
                    .put("name", toolName)
                    .put("arguments", arguments)
            )
        val response = runCatching {
            dispatcher.dispatch(
                rawBody = rpc.toString(),
                headers = mapOf("Authorization" to "Bearer ${credential.accessToken}")
            )
        }
        response.onFailure { error ->
            api.completeOperation(
                deviceId = ownerResolver.deviceId(),
                operationId = operationId,
                error = JSONObject()
                    .put("code", "device_execution_failed")
                    .put("message", error.message ?: "Device execution failed.")
            )
        }.onSuccess { transport ->
            val rpcResponse = runCatching { JSONObject(transport.body ?: "{}") }.getOrDefault(JSONObject())
            val result = rpcResponse.optJSONObject("result")
            if (transport.statusCode >= 400 || result == null || result.optBoolean("isError")) {
                val structured = result?.optJSONObject("structuredContent")
                val connectorError = structured?.optJSONObject("error")
                api.completeOperation(
                    deviceId = ownerResolver.deviceId(),
                    operationId = operationId,
                    error = JSONObject()
                        .put("code", connectorError?.optString("code", "connector_error") ?: "connector_error")
                        .put(
                            "message",
                            connectorError?.optString("message", "The Shary connector rejected the operation.")
                                ?: "The Shary connector rejected the operation."
                        )
                )
            } else {
                api.completeOperation(
                    deviceId = ownerResolver.deviceId(),
                    operationId = operationId,
                    result = result
                )
            }
        }
    }

    private suspend fun activeCredential(grantId: String): RemoteGrantCredential {
        val current = secureStore.credential(grantId)
            ?: error("No local connector credential exists for this MCP grant.")
        val now = System.currentTimeMillis() / 1000
        if (current.expiresAt > now + 60) return current
        val refreshed = refreshConnectorSession(
            RefreshConnectorSessionRequest(
                sessionId = current.sessionId,
                refreshSecret = current.refreshSecret,
                sessionTtlSeconds = 24 * 60 * 60
            )
        ).getOrThrow()
        return current.copy(
            accessToken = refreshed.accessToken,
            refreshSecret = refreshed.refreshSecret,
            expiresAt = refreshed.expiresAtEpochSeconds
        ).also(secureStore::saveCredential)
    }

    private fun refreshPendingState() {
        val enabled = preferences.getBoolean(KEY_ENABLED, false)
        val pending = if (enabled) secureStore.pendingOperations() else emptyList()
        _pendingOperations.value = pending
        if (pending.isEmpty()) {
            RemoteMcpNotification.clear(context)
        } else {
            RemoteMcpNotification.showPendingWrites(context, pending.size)
        }
    }

    private fun refreshRenewalState() {
        _renewalRequired.value = expiringCredentials()
    }

    private fun expiringCredentials(): List<RemoteGrantCredential> {
        if (!preferences.getBoolean(KEY_ENABLED, false)) return emptyList()
        val threshold = System.currentTimeMillis() / 1000 + RENEWAL_WINDOW_SECONDS
        return secureStore.credentials().filter { it.grantExpiresAt <= threshold }
    }

    private suspend fun isCapabilityEnabled(): Boolean =
        settingsRepository.settings.first().mcpEnabled

    private fun capabilityForScope(scope: String): ConnectorCapability? = when (scope) {
        "vaults.list", "vaults.invite" -> ConnectorCapability.VAULT_ACCESS
        "fields.metadata.read" -> ConnectorCapability.FIELDS_READ
        "fields.value.read" -> ConnectorCapability.FIELDS_VALUE_READ
        "fields.write" -> ConnectorCapability.FIELDS_WRITE
        "users.read" -> ConnectorCapability.USERS_READ
        "users.write" -> ConnectorCapability.USERS_WRITE
        "actions.preview" -> ConnectorCapability.ACTIONS_PREVIEW
        "actions.commit" -> ConnectorCapability.ACTIONS_COMMIT
        else -> null
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val RENEWAL_WINDOW_SECONDS = 7 * 24 * 60 * 60
        val WRITE_TOOLS = setOf(
            "shary.fields.create",
            "shary.fields.update",
            "shary.users.create",
            "shary.users.update",
            "shary.actions.commit"
        )
    }
}
