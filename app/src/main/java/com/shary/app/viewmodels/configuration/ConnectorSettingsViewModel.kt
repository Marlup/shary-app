package com.shary.app.viewmodels.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.application.connector.PairConnectorRequest
import com.shary.app.application.connector.PairConnectorResult
import com.shary.app.application.connector.PairConnectorUseCase
import com.shary.app.application.connector.ManageConnectorsUseCase
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.infrastructure.connector.mcp.SharyMcpHttpServer
import com.shary.app.infrastructure.connector.mcp.SharyMcpHttpServerState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConnectorPairingUiState(
    val clientId: String,
    val sessionId: String,
    val accessToken: String,
    val refreshSecret: String,
    val expiresAtEpochSeconds: Long,
    val modeLabel: String
)

@HiltViewModel
class ConnectorSettingsViewModel @Inject constructor(
    private val connectorRepository: ConnectorRepository,
    private val pairConnectorUseCase: PairConnectorUseCase,
    private val manageConnectorsUseCase: ManageConnectorsUseCase,
    private val mcpHttpServer: SharyMcpHttpServer
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
    val mcpServerState: StateFlow<SharyMcpHttpServerState> = mcpHttpServer.state

    private val _lastPairing = MutableStateFlow<ConnectorPairingUiState?>(null)
    val lastPairing: StateFlow<ConnectorPairingUiState?> = _lastPairing.asStateFlow()

    private val _connectorMessage = MutableStateFlow<String?>(null)
    val connectorMessage: StateFlow<String?> = _connectorMessage.asStateFlow()

    private val _pairingInProgress = MutableStateFlow(false)
    val pairingInProgress: StateFlow<Boolean> = _pairingInProgress.asStateFlow()

    fun pairReadOnlyConnector(displayName: String = "Local Assistant (Read-only)") {
        pairConnector(
            request = PairConnectorRequest(
                displayName = displayName,
                allowedCapabilities = setOf(
                    ConnectorCapability.FIELDS_READ,
                    ConnectorCapability.USERS_READ,
                    ConnectorCapability.ACTIONS_PREVIEW
                )
            )
        ) { it.toUiState(modeLabel = "Read-only") }
    }

    fun pairReadWriteConnector(displayName: String = "Local Assistant (Read-Write)") {
        pairConnector(
            request = PairConnectorRequest(
                displayName = displayName,
                allowedCapabilities = setOf(
                    ConnectorCapability.FIELDS_READ,
                    ConnectorCapability.USERS_READ,
                    ConnectorCapability.FIELDS_WRITE,
                    ConnectorCapability.USERS_WRITE,
                    ConnectorCapability.ACTIONS_PREVIEW,
                    ConnectorCapability.ACTIONS_COMMIT
                )
            )
        ) { it.toUiState(modeLabel = "Read-write") }
    }

    fun revokeConnector(clientId: String) {
        viewModelScope.launch {
            val revoked = manageConnectorsUseCase.revoke(clientId)
            if (_lastPairing.value?.clientId == clientId) _lastPairing.value = null
            _connectorMessage.value = if (revoked) "Connector revoked" else "Connector was already revoked"
        }
    }

    fun revokeAllConnectors() {
        viewModelScope.launch {
            val revokedCount = manageConnectorsUseCase.revokeAll()
            _lastPairing.value = null
            _connectorMessage.value = if (revokedCount == 0) {
                "No active connectors to revoke"
            } else {
                "Revoked $revokedCount connector(s)"
            }
        }
    }

    fun showConnectorMessage(message: String) {
        _connectorMessage.value = message
    }

    fun startMcpServer(port: Int = 8765) {
        mcpHttpServer.start(port)
    }

    fun stopMcpServer() {
        mcpHttpServer.stop()
    }

    private fun pairConnector(
        request: PairConnectorRequest,
        toUiState: (PairConnectorResult) -> ConnectorPairingUiState
    ) {
        if (_pairingInProgress.value) return
        if (!mcpHttpServer.state.value.running) {
            _connectorMessage.value = "Start MCP before pairing a connector"
            return
        }
        viewModelScope.launch {
            _pairingInProgress.value = true
            runCatching { pairConnectorUseCase(request) }
                .onSuccess { result ->
                    _lastPairing.value = toUiState(result)
                    _connectorMessage.value = "Connector paired. Copy the bearer token now."
                }
                .onFailure { error ->
                    _connectorMessage.value = error.message ?: "Connector pairing failed"
                }
            _pairingInProgress.value = false
        }
    }

    private fun PairConnectorResult.toUiState(modeLabel: String): ConnectorPairingUiState =
        ConnectorPairingUiState(
            clientId = clientId,
            sessionId = sessionId,
            accessToken = accessToken,
            refreshSecret = refreshSecret,
            expiresAtEpochSeconds = expiresAtEpochSeconds,
            modeLabel = modeLabel
        )
}
