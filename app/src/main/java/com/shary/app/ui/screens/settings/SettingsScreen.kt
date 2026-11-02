package com.shary.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.components.SharyTopBar
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.theme.SurfaceMid
import com.shary.app.ui.theme.TextMuted
import com.shary.app.ui.theme.Violet50
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.configuration.VaultTransferMode
import com.shary.app.viewmodels.configuration.VaultTransferUiState
import com.shary.app.viewmodels.user.UserViewModel
import java.time.LocalDate

@Composable
fun SettingsScreen(navController: NavHostController) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsState()
    val transfer by settingsViewModel.vaultTransfer.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) settingsViewModel.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) settingsViewModel.inspectImport(uri)
    }

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
                title = "Settings",
                subtitle = "Privacy, cloud and local preferences",
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable { navController.popBackStack() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Assistant Connectors",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Manage MCP capability, agent approvals and vault grants from the dedicated Connect tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SharySoftButton(
                        text = "Open Connect",
                        onClick = { navController.navigate(Screen.Connectors.route) }
                    )
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
                        text = "Vault backup",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Export or import an encrypted copy of fields, contacts, " +
                            "requests, tags, and safe preferences. Shary cannot recover " +
                            "the passphrase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SharySoftButton(
                            text = "Export vault",
                            onClick = settingsViewModel::beginExport,
                            modifier = Modifier.weight(1f)
                        )
                        SharySoftButton(
                            text = "Import vault",
                            onClick = settingsViewModel::beginImport,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    transfer.message?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (transfer.mode == null) {
                        transfer.error?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
                        Text(
                            text = "Remember email at login",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            SelectionContainer {
                                Text(
                                    text = emailLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                        subtitle = "Show known sender name or email instead of hash",
                        checked = settings.showFriendlySenderIdentity,
                        onCheckedChange = settingsViewModel::setShowFriendlySenderIdentity
                    )
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Require biometric for cloud inbox",
                        subtitle = "Require biometric or device PIN before viewing cloud inbox",
                        checked = settings.requireBiometricForCloudInbox,
                        onCheckedChange = settingsViewModel::setRequireBiometricForCloudInbox
                    )
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Compact list mode",
                        subtitle = "Reduce vertical spacing in fields, contacts and requests lists",
                        checked = settings.compactListMode,
                        onCheckedChange = settingsViewModel::setCompactListMode
                    )
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Wi-Fi only cloud sync",
                        subtitle = "Block cloud send and download actions outside Wi-Fi",
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

            Text(
                text = "Cloud expiry defaults support 7 or 14 days. Senders can still choose 14 days when needed.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }

    VaultPassphraseDialog(
        state = transfer,
        onPassphraseChange = settingsViewModel::updatePassphrase,
        onConfirmPassphraseChange = settingsViewModel::updateConfirmPassphrase,
        onDismiss = settingsViewModel::cancelTransfer,
        onContinue = {
            when (transfer.mode) {
                VaultTransferMode.EXPORT -> {
                    if (settingsViewModel.validateExportPassphrase()) {
                        exportLauncher.launch(
                            "shary-vault-${LocalDate.now()}.sharyvault"
                        )
                    }
                }

                VaultTransferMode.IMPORT -> {
                    if (settingsViewModel.validateImportPassphrase()) {
                        importLauncher.launch(arrayOf("*/*"))
                    }
                }

                else -> Unit
            }
        }
    )

    VaultImportPreviewDialog(
        state = transfer,
        onDismiss = settingsViewModel::cancelTransfer,
        onConfirm = settingsViewModel::confirmImport
    )
}

@Composable
private fun VaultPassphraseDialog(
    state: VaultTransferUiState,
    onPassphraseChange: (String) -> Unit,
    onConfirmPassphraseChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    if (state.mode != VaultTransferMode.EXPORT && state.mode != VaultTransferMode.IMPORT) {
        return
    }

    val isExport = state.mode == VaultTransferMode.EXPORT
    AlertDialog(
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        title = {
            Text(if (isExport) "Export encrypted vault" else "Import encrypted vault")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isExport) {
                        "Choose a passphrase for the backup file. It is not stored " +
                            "and cannot be recovered."
                    } else {
                        "Enter the passphrase used when the backup was created."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.passphrase,
                    onValueChange = onPassphraseChange,
                    label = { Text("Passphrase") },
                    singleLine = true,
                    enabled = !state.isBusy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isExport) {
                    OutlinedTextField(
                        value = state.confirmPassphrase,
                        onValueChange = onConfirmPassphraseChange,
                        label = { Text("Confirm passphrase") },
                        singleLine = true,
                        enabled = !state.isBusy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                state.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (state.isBusy) {
                    Text(
                        text = if (isExport) "Encrypting..." else "Reading backup...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue, enabled = !state.isBusy) {
                Text("Choose file")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isBusy) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun VaultImportPreviewDialog(
    state: VaultTransferUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (state.mode != VaultTransferMode.IMPORT_PREVIEW) return
    val preview = state.preview ?: return

    AlertDialog(
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        title = { Text("Import vault backup?") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Fields: ${preview.fieldCount}")
                Text("Contacts: ${preview.contactCount}")
                Text("Requests: ${preview.requestCount}")
                Text("Tags: ${preview.tagCount}")
                Text(
                    text = "Existing items are kept. Duplicate keys, emails, and " +
                        "request IDs are skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (state.isBusy) {
                    Text(
                        text = "Importing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.isBusy) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isBusy) {
                Text("Cancel")
            }
        }
    )
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
                text = "Hold a card for ${selectedMillis}ms to copy value or email",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                SettingsChoiceChip(
                    label = "${option}ms",
                    selected = selectedMillis == option,
                    onClick = { onSelect(option) }
                )
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                SettingsChoiceChip(
                    label = if (option == 0) "Off" else "${option}s",
                    selected = selectedSeconds == option,
                    onClick = { onSelect(option) }
                )
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsChoiceChip(
                label = "7d",
                selected = selectedDays == 7,
                onClick = { onSelect(7) }
            )
            SettingsChoiceChip(
                label = "14d",
                selected = selectedDays == 14,
                onClick = { onSelect(14) }
            )
        }
    }
}

@Composable
private fun SettingsChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.semantics {
            stateDescription = if (selected) "Selected" else "Not selected"
        }
    )
}
