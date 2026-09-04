package com.shary.app.viewmodels.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.application.connector.ManageConnectorsUseCase
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.interfaces.repositories.SettingsRepository
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.infrastructure.connector.remote.PendingRemoteOperation
import com.shary.app.infrastructure.connector.remote.RemoteGrantCredential
import com.shary.app.infrastructure.connector.remote.RemoteMcpCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectorSettingsViewModel @Inject constructor(
    private val connectorRepository: ConnectorRepository,
    private val settingsRepository: SettingsRepository,
    private val manageConnectorsUseCase: ManageConnectorsUseCase,
    private val remoteMcpCoordinator: RemoteMcpCoordinator
) : ViewModel() {

    val connectorClients: StateFlow<List<ConnectorClient>> = connectorRepository.clients.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val connectorAuditEvents: StateFlow<List<ConnectorAuditEvent>> = connectorRepository.auditEvents.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val pendingOperations: StateFlow<List<PendingRemoteOperation>> = remoteMcpCoordinator.pendingOperations
    val renewalRequired: StateFlow<List<RemoteGrantCredential>> = remoteMcpCoordinator.renewalRequired
    val mcpEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.mcpEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _connectorMessage = MutableStateFlow<String?>(null)
    val connectorMessage: StateFlow<String?> = _connectorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            revokeLegacyLocalConnectorsIfNeeded()
        }
    }

    fun revokeConnector(clientId: String) {
        viewModelScope.launch {
            val client = connectorRepository.getClient(clientId)
            val isRemote = isRemoteClient(client)
            val revoked = if (isRemote) {
                val remoteClient = requireNotNull(client)
                runCatching {
                    remoteMcpCoordinator.revokeAccess(remoteClient.grantId, remoteClient.clientId)
                    true
                }.getOrElse {
                    _connectorMessage.value = it.message ?: "Remote MCP revocation failed"
                    false
                }
            } else {
                manageConnectorsUseCase.revoke(clientId)
            }
            if (revoked) {
                _connectorMessage.value = if (isRemote) {
                    "Vault grant revoked"
                } else {
                    "Unsupported legacy local connector revoked"
                }
            }
        }
    }

    fun revokeAllConnectors() {
        viewModelScope.launch {
            var revokedCount = 0
            var failedCount = 0
            connectorClients.value
                .filter { it.status != ConnectorClientStatus.REVOKED }
                .forEach { client ->
                    val result = if (isRemoteClient(client)) {
                        runCatching { remoteMcpCoordinator.revokeAccess(client.grantId, client.clientId) }
                    } else {
                        runCatching { manageConnectorsUseCase.revoke(client.clientId) }
                    }
                    if (result.isSuccess) {
                        revokedCount += 1
                    } else {
                        failedCount += 1
                    }
                }
            _connectorMessage.value = if (failedCount > 0) {
                "Revoked $revokedCount grant(s); $failedCount remote revocation(s) failed"
            } else if (revokedCount == 0) {
                "No active vault grants to revoke"
            } else {
                "Revoked $revokedCount vault grant(s)"
            }
        }
    }

    fun showConnectorMessage(message: String) {
        _connectorMessage.value = message
    }

    fun setMcpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMcpEnabled(enabled)
            remoteMcpCoordinator.applyCapabilityChange(enabled)
            _connectorMessage.value = if (enabled) {
                "MCP capability enabled"
            } else {
                "MCP capability disabled"
            }
        }
    }

    private fun isRemoteClient(client: ConnectorClient?): Boolean =
        client != null && client.grantId.startsWith("grant_")

    private suspend fun revokeLegacyLocalConnectorsIfNeeded() {
        val legacyClients = connectorRepository.clients.first()
            .filter { client ->
                client.status != ConnectorClientStatus.REVOKED && !isRemoteClient(client)
            }
        if (legacyClients.isEmpty()) return

        var revokedCount = 0
        legacyClients.forEach { client ->
            if (manageConnectorsUseCase.revoke(client.clientId)) {
                revokedCount += 1
            }
        }
        if (revokedCount > 0) {
            _connectorMessage.value =
                "Legacy local MCP access was deprecated. Revoked $revokedCount local connector(s); use cloud OAuth to reconnect."
        }
    }
}
