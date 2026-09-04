package com.shary.app.ui.connector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.shary.app.infrastructure.connector.remote.RemoteApprovalState
import com.shary.app.infrastructure.connector.remote.RemoteMcpCoordinator
import com.shary.app.utils.BiometricAuthManager
import kotlinx.coroutines.launch

@Composable
fun RemoteMcpDialogs(
    activity: FragmentActivity,
    coordinator: RemoteMcpCoordinator,
    enabled: Boolean
) {
    val approvalState by coordinator.approvalState.collectAsState()
    val pendingOperations by coordinator.pendingOperations.collectAsState()
    val renewalRequired by coordinator.renewalRequired.collectAsState()
    val scope = rememberCoroutineScope()
    var alias by rememberSaveable { mutableStateOf("My Shary vault") }
    var localError by remember { mutableStateOf<String?>(null) }
    var selectedScopes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dismissedRenewalGrantId by remember { mutableStateOf<String?>(null) }

    val approvalId = when (val state = approvalState) {
        is RemoteApprovalState.Ready -> state.details.approvalId
        is RemoteApprovalState.Processing -> state.details.approvalId
        is RemoteApprovalState.Failed -> state.details?.approvalId
        else -> null
    }
    LaunchedEffect(approvalId) {
        localError = null
        if (approvalId != null) {
            alias = "My Shary vault"
            val details = when (val state = approvalState) {
                is RemoteApprovalState.Ready -> state.details
                is RemoteApprovalState.Processing -> state.details
                is RemoteApprovalState.Failed -> state.details
                else -> null
            }
            selectedScopes = details?.scopes?.filter { it in DEFAULT_SELECTED_SCOPES }?.toSet().orEmpty()
        }
    }

    when (val state = approvalState) {
        RemoteApprovalState.Idle -> Unit
        RemoteApprovalState.Loading -> ProgressDialog("Loading MCP authorization…")
        is RemoteApprovalState.Processing -> ProgressDialog("Creating the encrypted vault grant…")
        is RemoteApprovalState.Ready -> AlertDialog(
            onDismissRequest = coordinator::dismissApproval,
            title = { Text("Authorize ${state.details.clientName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This creates a 30-day, revocable grant for only these scopes:")
                    Text(
                        "The agent profile may list authorized aliases and invite other owners. " +
                            "Choose what this vault itself exposes:"
                    )
                    state.details.scopes.filterNot { it.startsWith("vaults.") }.forEach { scopeName ->
                        androidx.compose.foundation.layout.Row {
                            Checkbox(
                                checked = scopeName in selectedScopes,
                                onCheckedChange = { checked ->
                                    selectedScopes = if (checked) {
                                        selectedScopes + scopeName
                                    } else {
                                        selectedScopes - scopeName
                                    }
                                }
                            )
                            Text(scopeName, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it.take(80) },
                        label = { Text("Vault alias shown to the agent") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    localError?.let { Text(it) }
                }
            },
            confirmButton = {
                Button(
                    enabled = alias.isNotBlank() && selectedScopes.isNotEmpty(),
                    onClick = {
                        val authError = BiometricAuthManager(activity, activity) {
                            scope.launch { coordinator.approveCurrent(alias, selectedScopes) }
                        }.authenticate()
                        if (authError != null) localError = authError
                    }
                ) { Text("Authorize") }
            },
            dismissButton = {
                TextButton(onClick = coordinator::dismissApproval) { Text("Cancel") }
            }
        )
        is RemoteApprovalState.Completed -> AlertDialog(
            onDismissRequest = coordinator::dismissApproval,
            title = { Text("MCP agent authorized") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = coordinator::dismissApproval) { Text("Done") }
            }
        )
        is RemoteApprovalState.Failed -> AlertDialog(
            onDismissRequest = coordinator::dismissApproval,
            title = { Text("MCP authorization failed") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (state.details == null) {
                            coordinator.dismissApproval()
                        } else {
                            scope.launch { coordinator.approveCurrent(alias, selectedScopes) }
                        }
                    }
                ) { Text(if (state.details == null) "Close" else "Retry") }
            },
            dismissButton = if (state.details == null) null else {
                { TextButton(onClick = coordinator::dismissApproval) { Text("Cancel") } }
            }
        )
    }

    if (enabled && approvalState == RemoteApprovalState.Idle) {
        pendingOperations.firstOrNull()?.let { operation ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Confirm agent change") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(operation.confirmationSummary())
                        Text("The agent cannot apply this change until you approve it.")
                        localError?.let { Text(it) }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val authError = BiometricAuthManager(activity, activity) {
                                scope.launch {
                                    runCatching { coordinator.approveAndExecute(operation.operationId) }
                                        .onFailure { localError = it.message ?: "Operation failed." }
                                }
                            }.authenticate()
                            if (authError != null) localError = authError
                        }
                    ) { Text("Approve change") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                runCatching { coordinator.rejectOperation(operation.operationId) }
                                    .onFailure { localError = it.message ?: "Unable to reject operation." }
                            }
                        }
                    ) { Text("Reject") }
                }
            )
        }
        if (pendingOperations.isEmpty()) {
            renewalRequired.firstOrNull { it.grantId != dismissedRenewalGrantId }?.let { credential ->
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Renew MCP vault access") },
                    text = {
                        Text(
                            "Agent profile …${credential.agentProfileId.takeLast(8)} expires soon. " +
                                "Renew this revocable grant for another 30 days?"
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val authError = BiometricAuthManager(activity, activity) {
                                    scope.launch {
                                        runCatching { coordinator.renewGrant(credential.grantId) }
                                            .onFailure { localError = it.message ?: "Grant renewal failed." }
                                    }
                                }.authenticate()
                                if (authError != null) localError = authError
                            }
                        ) { Text("Renew 30 days") }
                    },
                    dismissButton = {
                        TextButton(onClick = { dismissedRenewalGrantId = credential.grantId }) {
                            Text("Not now")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProgressDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Shary MCP") },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(message)
            }
        },
        confirmButton = {}
    )
}

private val DEFAULT_SELECTED_SCOPES = setOf(
    "fields.metadata.read",
    "users.read",
    "actions.preview"
)
