package com.shary.app.ui.screens.settings

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.shary.app.core.constants.ConnectorPolicyConfig
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.ui.components.SharyTopBar
import com.shary.app.ui.theme.SurfaceMid
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.viewmodels.configuration.ConnectorSettingsViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.user.UserViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavHostController) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val connectorSettingsViewModel: ConnectorSettingsViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsState()
    val connectorClients by connectorSettingsViewModel.connectorClients.collectAsState()
    val connectorAuditEvents by connectorSettingsViewModel.connectorAuditEvents.collectAsState()
    val mcpServerState by connectorSettingsViewModel.mcpServerState.collectAsState()
    val lastPairing by connectorSettingsViewModel.lastPairing.collectAsState()
    val connectorMessage by connectorSettingsViewModel.connectorMessage.collectAsState()
    val pairingInProgress by connectorSettingsViewModel.pairingInProgress.collectAsState()
    val activeConnectorClients = connectorClients
        .filter { it.status != ConnectorClientStatus.REVOKED }
        .sortedByDescending { it.createdAt }
    val connectorLimitReached =
        activeConnectorClients.size >= ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var connectorClipboardClearJob by remember { mutableStateOf<Job?>(null) }

    val ownerEmail = userViewModel.getOwner().email.trim()
    val emailLabel = when {
        ownerEmail.isNotBlank() -> ownerEmail
        settings.rememberedEmail.isNotBlank() -> settings.rememberedEmail
        else -> "No email available"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Violet50)
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            SharyTopBar(
                title = "Configuration",
                subtitle = "Privacy, cloud and UX preferences",
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable { navController.popBackStack() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
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
                        Text(
                            text = "Remember email at login",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = emailLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Switch(
                                checked = settings.rememberEmailAtLogin,
                                onCheckedChange = settingsViewModel::setRememberEmailAtLogin
                            )
                        }
                    }
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Auto-open cloud inbox on start",
                        subtitle = "Open pending cloud items when Fields screen starts",
                        checked = settings.autoOpenCloudInboxOnStart,
                        onCheckedChange = settingsViewModel::setAutoOpenCloudInboxOnStart
                    )
                    HorizontalDivider()
                    SettingsExpiryRow(
                        selectedDays = settings.defaultCloudExpiryDays,
                        onSelect = settingsViewModel::setDefaultCloudExpiryDays
                    )
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Friendly sender labels",
                        subtitle = "Show known sender name/email instead of hash",
                        checked = settings.showFriendlySenderIdentity,
                        onCheckedChange = settingsViewModel::setShowFriendlySenderIdentity
                    )
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Require biometric for cloud inbox",
                        subtitle = "Require biometric/PIN before viewing cloud inbox",
                        checked = settings.requireBiometricForCloudInbox,
                        onCheckedChange = settingsViewModel::setRequireBiometricForCloudInbox
                    )
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Compact list mode",
                        subtitle = "Reduce vertical spacing in lists",
                        checked = settings.compactListMode,
                        onCheckedChange = settingsViewModel::setCompactListMode
                    )
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Wi-Fi only cloud sync",
                        subtitle = "Block cloud send/download outside Wi-Fi",
                        checked = settings.wifiOnlyCloudSync,
                        onCheckedChange = settingsViewModel::setWifiOnlyCloudSync
                    )
                    HorizontalDivider()
                    SettingsCopyLongPressRow(
                        selectedMillis = settings.copyLongPressMillis,
                        onSelect = settingsViewModel::setCopyLongPressMillis
                    )
                    HorizontalDivider()
                    SettingsClipboardAutoClearRow(
                        selectedSeconds = settings.clipboardAutoClearSeconds,
                        onSelect = settingsViewModel::setClipboardAutoClearSeconds
                    )
                }
            }

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
                                text = "Assistant Connectors",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Pair and supervise local assistant access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MCP server",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (mcpServerState.running) {
                                "Running at ${mcpServerState.endpoint}"
                            } else {
                                "Stopped. Start it before connecting Codex or another MCP client."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        mcpServerState.lastError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = !mcpServerState.running,
                                onClick = { connectorSettingsViewModel.startMcpServer() }
                            ) {
                                Text("Start MCP")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = mcpServerState.running,
                                onClick = { connectorSettingsViewModel.stopMcpServer() }
                            ) {
                                Text("Stop MCP")
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = mcpServerState.running &&
                                !pairingInProgress &&
                                !connectorLimitReached,
                            onClick = { connectorSettingsViewModel.pairReadOnlyConnector() }
                        ) {
                            Text("Pair read-only")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = mcpServerState.running &&
                                !pairingInProgress &&
                                !connectorLimitReached,
                            onClick = { connectorSettingsViewModel.pairReadWriteConnector() }
                        ) {
                            Text("Pair read-write")
                        }
                    }
                    Text(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        text = when {
                            !mcpServerState.running -> "Start MCP before pairing. Stopping MCP pauses access but does not revoke existing connectors."
                            connectorLimitReached -> "Connector limit reached. Revoke one before pairing another."
                            else -> "Pairing creates a persistent connector credential. Use Revoke when it is no longer needed."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    lastPairing?.let { pairing ->
                        HorizontalDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Latest ${pairing.modeLabel} MCP credentials",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            SelectionContainer {
                                Text(
                                    text = "Endpoint: ${mcpServerState.endpoint}\nAuthorization: Bearer ${pairing.accessToken}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    val bearerValue = "Bearer ${pairing.accessToken}"
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            ClipEntry(
                                                ClipData.newPlainText(
                                                    "shary_mcp_bearer",
                                                    bearerValue
                                                )
                                            )
                                        )
                                    }
                                    connectorClipboardClearJob?.cancel()
                                    if (settings.clipboardAutoClearSeconds > 0) {
                                        connectorClipboardClearJob = scope.launch {
                                            delay(settings.clipboardAutoClearSeconds * 1000L)
                                            clipboard.setClipEntry(
                                                ClipEntry(ClipData.newPlainText("", ""))
                                            )
                                        }
                                    }
                                    connectorSettingsViewModel.showConnectorMessage(
                                        if (settings.clipboardAutoClearSeconds > 0) {
                                            "Bearer copied; clipboard clears in ${settings.clipboardAutoClearSeconds}s"
                                        } else {
                                            "Bearer copied to clipboard"
                                        }
                                    )
                                }
                            ) {
                                Text("Copy bearer")
                            }
                            Text(
                                text = "Expires at epoch ${pairing.expiresAtEpochSeconds}. Re-pair when expired.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Active connectors: ${activeConnectorClients.size}/${ConnectorPolicyConfig.MAX_ACTIVE_CONNECTORS}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        activeConnectorClients.forEach { client ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = client.displayName,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = client.status.name.lowercase().replace('_', ' '),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        connectorSettingsViewModel.revokeConnector(client.clientId)
                                    }
                                ) {
                                    Text("Revoke")
                                }
                            }
                        }
                        if (activeConnectorClients.isEmpty()) {
                            Text(
                                text = "No active connector. Start MCP, then pair one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Button(
                                onClick = connectorSettingsViewModel::revokeAllConnectors
                            ) {
                                Text("Revoke all")
                            }
                        }
                        connectorMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Violet600
                            )
                        }
                    }
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Recent activity: ${connectorAuditEvents.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        connectorAuditEvents.takeLast(3).reversed().forEach { event ->
                            Text(
                                text = "- ${event.operation} • ${event.decision.name.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (connectorAuditEvents.isEmpty()) {
                            Text(
                                text = "No connector activity yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                text = "Cloud expiry default supports 7 or 14 days. Sender can choose 14 days when needed.",
                style = MaterialTheme.typography.bodySmall,
                color = Violet500
            )
        }
    }
}

@Composable
private fun SettingsCopyLongPressRow(
    selectedMillis: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(400, 600, 900)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Copy long-press time",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Hold card for ${selectedMillis}ms to copy value/email",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        options.forEachIndexed { index, option ->
            ExpiryChip(
                label = "${option}ms",
                selected = selectedMillis == option,
                onClick = { onSelect(option) }
            )
            if (index < options.lastIndex) {
                Box(modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun SettingsClipboardAutoClearRow(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(0, 15, 30, 60)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Clipboard auto-clear",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (selectedSeconds <= 0) {
                    "Sensitive copied values stay until replaced"
                } else {
                    "Clear copied values after ${selectedSeconds}s"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        options.forEachIndexed { index, option ->
            val label = if (option == 0) "Off" else "${option}s"
            ExpiryChip(
                label = label,
                selected = selectedSeconds == option,
                onClick = { onSelect(option) }
            )
            if (index < options.lastIndex) {
                Box(modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitleMaxLines: Int = 2
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = subtitleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsExpiryRow(
    selectedDays: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Default cloud expiry",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Sender-side default retention",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExpiryChip(
            label = "7d",
            selected = selectedDays == 7,
            onClick = { onSelect(7) }
        )
        Box(modifier = Modifier.padding(horizontal = 4.dp))
        ExpiryChip(
            label = "14d",
            selected = selectedDays == 14,
            onClick = { onSelect(14) }
        )
    }
}

@Composable
private fun ExpiryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Violet600 else Color.Transparent,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Violet500
        )
    }
}
