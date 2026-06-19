package com.shary.app.viewmodels.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.application.connector.PairConnectorRequest
import com.shary.app.application.connector.PairConnectorResult
import com.shary.app.application.connector.PairConnectorUseCase
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

    fun pairReadOnlyConnector(displayName: String = "Local Assistant (Read-only)") {
        viewModelScope.launch {
            val result = pairConnectorUseCase(
                PairConnectorRequest(
                    displayName = displayName,
                    allowedCapabilities = setOf(
                        ConnectorCapability.FIELDS_READ,
                        ConnectorCapability.USERS_READ,
                        ConnectorCapability.ACTIONS_PREVIEW
                    )
                )
            )
            _lastPairing.value = result.toUiState(modeLabel = "Read-only")
        }
    }

    fun pairReadWriteConnector(displayName: String = "Local Assistant (Read-Write)") {
        viewModelScope.launch {
            val result = pairConnectorUseCase(
                PairConnectorRequest(
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
            )
            _lastPairing.value = result.toUiState(modeLabel = "Read-write")
        }
    }

    fun startMcpServer(port: Int = 8765) {
        mcpHttpServer.start(port)
    }

    fun stopMcpServer() {
        mcpHttpServer.stop()
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
