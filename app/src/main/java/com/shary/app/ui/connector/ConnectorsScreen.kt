package com.shary.app.ui.connector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.shary.app.BuildConfig
import com.shary.app.core.domain.interfaces.events.FieldEvent
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import com.shary.app.ui.components.PrimaryTabOverflowMenu
import com.shary.app.ui.components.SectionTab
import com.shary.app.ui.components.SharySectionNavigationBar
import com.shary.app.ui.components.SharyTopBar
import com.shary.app.ui.screens.field.components.ChangePasswordDialog
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.theme.SurfaceMid
import com.shary.app.ui.theme.TextMuted
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet700
import com.shary.app.viewmodels.configuration.ConnectorSettingsViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ConnectorsScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {}
) {
    val connectorSettingsViewModel: ConnectorSettingsViewModel = hiltViewModel()
    val fieldViewModel: FieldViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    val connectorClients by connectorSettingsViewModel.connectorClients.collectAsState()
    val connectorAuditEvents by connectorSettingsViewModel.connectorAuditEvents.collectAsState()
    val pendingOperations by connectorSettingsViewModel.pendingOperations.collectAsState()
    val renewalRequired by connectorSettingsViewModel.renewalRequired.collectAsState()
    val connectorMessage by connectorSettingsViewModel.connectorMessage.collectAsState()
    val mcpEnabled by connectorSettingsViewModel.mcpEnabled.collectAsState()

    val activeRemoteConnectorClients = connectorClients
        .filter { it.status != ConnectorClientStatus.REVOKED && it.grantId.startsWith("grant_") }
        .sortedByDescending { it.createdAt }

    var openChangePasswordDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        fieldViewModel.events.collect { event ->
            when (event) {
                FieldEvent.PasswordChanged -> {
                    openChangePasswordDialog = false
                    navController.navigate(Screen.Login.routeWithPasswordChanged(true)) {
                        popUpTo(Screen.Fields.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is FieldEvent.Error -> {
                    if (openChangePasswordDialog) {
                        snackbarMessage = event.throwable.message ?: "Unable to change password"
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Violet50),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                SharyTopBar(
                    title = "",
                    subtitle = if (mcpEnabled) {
                        "${activeRemoteConnectorClients.size} active grant(s)"
                    } else {
                        "MCP capability disabled"
                    },
                    actions = {
                        PrimaryTabOverflowMenu(
                            onOpenSettings = { navController.navigate(Screen.Settings.route) },
                            onChangePassword = { openChangePasswordDialog = true },
                            onLogout = {
                                fieldViewModel.clearSelectedFields()
                                onLogout()
                            }
                        )
                    }
                )
            }
        },
        bottomBar = {
            SharySectionNavigationBar(
                currentTab = SectionTab.CONNECT,
                onTabSelected = { tab ->
                    when (tab) {
                        SectionTab.FIELDS -> navController.navigate(Screen.Fields.route) {
                            launchSingleTop = true
                        }
                        SectionTab.CONNECT -> Unit
                        SectionTab.USERS -> navController.navigate(Screen.Users.route) {
                            launchSingleTop = true
                        }
                        SectionTab.REQUESTS -> navController.navigate(Screen.Requests.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Violet50)
                .padding(horizontal = 18.dp)
        ) {
            ConnectorsContent(
                mcpEnabled = mcpEnabled,
                connectorMessage = connectorMessage,
                pendingApprovalCount = pendingOperations.size,
                renewalRequiredCount = renewalRequired.size,
                activeRemoteConnectorClients = activeRemoteConnectorClients,
                recentActivityLabels = connectorAuditEvents.takeLast(5).reversed().map { event ->
                    "${event.operation} • ${event.decision.name.lowercase()}"
                },
                onToggleMcp = connectorSettingsViewModel::setMcpEnabled,
                onRevokeConnector = { connectorSettingsViewModel.revokeConnector(it) },
                onRevokeAll = connectorSettingsViewModel::revokeAllConnectors
            )
        }
    }

    if (openChangePasswordDialog) {
        val isLoading by fieldViewModel.isLoading.collectAsState()
        ChangePasswordDialog(
            isLoading = isLoading,
            onDismiss = { openChangePasswordDialog = false },
            onAccept = { oldPassword, newPassword, repeatNewPassword ->
                fieldViewModel.changePasswordAndRewrapDataKey(
                    context = navController.context,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    repeatNewPassword = repeatNewPassword
                )
            }
        )
    }
}

@Composable
internal fun ConnectorsContent(
    mcpEnabled: Boolean,
    connectorMessage: String?,
    pendingApprovalCount: Int,
    renewalRequiredCount: Int,
    activeRemoteConnectorClients: List<ConnectorClient>,
    recentActivityLabels: List<String>,
    onToggleMcp: (Boolean) -> Unit,
    onRevokeConnector: (String) -> Unit,
    onRevokeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceMid
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable MCP capability",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Allow OAuth approval links, background agent sync, and vault grant execution on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = mcpEnabled,
                        onCheckedChange = onToggleMcp,
                        modifier = Modifier.semantics {
                            contentDescription = "Enable MCP capability switch"
                        }
                    )
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Public MCP endpoint",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SelectionContainer {
                        Text(
                            text = BuildConfig.SHARY_MCP_BASE_URL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (mcpEnabled) {
                            "Connect from Codex, Claude, or another OAuth-capable MCP client. Complete OAuth there, then approve scopes in Shary."
                        } else {
                            "MCP is currently disabled. Approval links, background sync, and grant execution stay inactive until you enable this capability."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pending owner approvals: $pendingApprovalCount | Grants near renewal: $renewalRequiredCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    connectorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Violet700
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceMid
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Active vault grants: ${activeRemoteConnectorClients.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (activeRemoteConnectorClients.isEmpty()) {
                    Text(
                        text = "No active vault grants. Start OAuth from your MCP client, then approve the requested scopes in Shary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activeRemoteConnectorClients.forEach { client ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.displayName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = buildGrantSummary(client),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = { onRevokeConnector(client.clientId) }) {
                                Text("Revoke")
                            }
                        }
                    }
                    Button(onClick = onRevokeAll) {
                        Text("Revoke all")
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceMid
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Recent MCP activity: ${recentActivityLabels.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                recentActivityLabels.forEach { label ->
                    Text(
                        text = "- $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (recentActivityLabels.isEmpty()) {
                    Text(
                        text = "No MCP activity yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Writes stay approval-gated on this device. Renewal prompts appear during the final 7 days of a grant when MCP capability is enabled.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

private fun buildGrantSummary(client: ConnectorClient): String {
    val statusLabel = client.status.name.lowercase().replace('_', ' ')
    val grantSuffix = if (client.grantId.isBlank()) "" else "Grant ...${client.grantId.takeLast(8)}"
    val lastActivity = client.lastActivityAt?.let { "Last activity ${formatEpochSeconds(it)}" }.orEmpty()
    return listOf(statusLabel, grantSuffix, lastActivity)
        .filter { it.isNotBlank() }
        .joinToString(" • ")
}

private fun formatEpochSeconds(epochSeconds: Long): String = runCatching {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(epochSeconds))
}.getOrDefault(epochSeconds.toString())
