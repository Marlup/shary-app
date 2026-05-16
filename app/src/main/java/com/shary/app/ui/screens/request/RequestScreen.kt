package com.shary.app.ui.screens.request

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessageB64
import com.shary.app.ui.components.DockAction
import com.shary.app.ui.components.EmptyState
import com.shary.app.ui.components.RequestCard
import com.shary.app.ui.components.RequestStatusTone
import com.shary.app.ui.components.SharyCommandDock
import com.shary.app.ui.components.SharyIconButton
import com.shary.app.ui.components.SharySectionNavigationBar
import com.shary.app.ui.components.SharyTopBar
import com.shary.app.ui.components.SectionTab
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.request.utils.AddRequestDialog
import com.shary.app.ui.screens.request.utils.RequestInboxReviewDialog
import com.shary.app.ui.screens.utils.cloudErrorMessage
import com.shary.app.ui.screens.utils.FieldMatchingDialog
import com.shary.app.ui.theme.SurfaceMid
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.utils.BiometricAuthManager
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.request.RequestViewModel
import com.shary.app.viewmodels.user.UserViewModel
import com.shary.app.ui.utils.isOnWifiNetwork
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun RequestsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val fields by fieldViewModel.fields.collectAsState()
    val users by userViewModel.users.collectAsState()
    val listMode by requestViewModel.listMode.collectAsState()
    val draftFields by requestViewModel.draftFields.collectAsState()
    val receivedRequests by requestViewModel.receivedRequests.collectAsState()
    val sentRequests by requestViewModel.sentRequests.collectAsState()
    val requestInboxItems by requestViewModel.requestInboxItems.collectAsState()
    val isRequestInboxLoading by requestViewModel.isCloudInboxLoading.collectAsState()
    val cloudState by cloudViewModel.cloudState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val cloudReady = cloudState.isOnline && cloudState.isUserValidated

    val requestsToShow = if (listMode == RequestListMode.SENT) sentRequests else receivedRequests
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showAddRequestSheet by remember { mutableStateOf(false) }
    var showMatchDialog by remember { mutableStateOf(false) }
    var openRequestInboxDialog by remember { mutableStateOf(false) }
    var autoInboxAttempted by rememberSaveable { mutableStateOf(false) }
    var selectedReceivedIndex by remember { mutableStateOf<Int?>(null) }
    val cardVerticalPadding = if (settings.compactListMode) 2.dp else 4.dp

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    fun openRequestInbox() {
        val ownerEmail = userViewModel.getOwner().email
        if (ownerEmail.isBlank()) {
            snackbarMessage = "User not logged in"
            return
        }
        if (settings.wifiOnlyCloudSync && !isOnWifiNetwork(context)) {
            snackbarMessage = "Wi-Fi only sync is enabled. Connect to Wi-Fi to continue."
            return
        }

        val runLoad = {
            openRequestInboxDialog = true
            requestViewModel.loadRequestInbox(ownerEmail)
        }

        if (settings.requireBiometricForCloudInbox) {
            val hostActivity = context as? FragmentActivity
            if (hostActivity == null) {
                snackbarMessage = "Biometric check unavailable on this screen"
                return
            }
            val biometric = BiometricAuthManager(
                context = context,
                activity = hostActivity,
                onAuthSuccess = runLoad
            )
            val biometricError = biometric.authenticate()
            if (biometricError != null) {
                snackbarMessage = biometricError
            }
        } else {
            runLoad()
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    LaunchedEffect(Unit) {
        requestViewModel.events.collect { event ->
            when (event) {
                is RequestEvent.FetchedFromCloud -> {
                    snackbarMessage = if (event.matchedCount > 0) {
                        "Fetched and matched ${event.matchedCount} fields from request"
                    } else {
                        "No requests found in cloud"
                    }
                }
                is RequestEvent.FetchError -> {
                    snackbarMessage = cloudErrorMessage(event.throwable)
                }
                is RequestEvent.CloudInboxLoaded -> {
                    snackbarMessage = "Pending cloud requests: ${event.count}. Review one by one."
                }
                RequestEvent.CloudInboxEmpty -> {
                    snackbarMessage = "No pending request items"
                }
                is RequestEvent.CloudInboxRejected -> {
                    snackbarMessage = if (event.backendAcknowledged) {
                        "Request rejected and removed from cloud inbox"
                    } else {
                        "Request rejected locally. Backend acknowledgement unavailable."
                    }
                }
                is RequestEvent.CloudInboxAccepted -> {
                    snackbarMessage = if (event.backendAcknowledged) {
                        "Accepted request and imported ${event.importedKeyCount} keys"
                    } else {
                        "Accepted request and imported ${event.importedKeyCount} keys. Backend acknowledgement unavailable."
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                autoInboxAttempted = false
                if (listMode == RequestListMode.SENT) {
                    requestViewModel.setDraftFields()
                }
            }
        }
        lifecycleOwner.value.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.value.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings.autoOpenCloudInboxOnStart, cloudReady) {
        if (settings.autoOpenCloudInboxOnStart && cloudReady && !autoInboxAttempted) {
            autoInboxAttempted = true
            openRequestInbox()
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
                    actions = {
                        SharyIconButton(
                            icon = Icons.Default.CloudDownload,
                            contentDescription = "Sync requests from cloud",
                            enabled = cloudReady,
                            onClick = ::openRequestInbox
                        )
                        SharyIconButton(
                            icon = if (listMode == RequestListMode.SENT) Icons.Default.Add else Icons.Default.Compare,
                            contentDescription = if (listMode == RequestListMode.SENT) "Add requested key" else "Match fields",
                            onClick = {
                                if (listMode == RequestListMode.SENT) {
                                    showAddRequestSheet = true
                                } else if (draftFields.isNotEmpty()) {
                                    showMatchDialog = true
                                } else {
                                    snackbarMessage = "Select a received request first"
                                }
                            }
                        )
                    }
                )
                AnimatedContent(targetState = listMode, label = "requests-mode-switcher") { mode ->
                    RequestsModeSwitcher(
                        listMode = mode,
                        receivedCount = receivedRequests.size,
                        onModeChange = requestViewModel::setListMode
                    )
                }
            }
        },
        bottomBar = {
            Column {
                SharyCommandDock(
                    selectedCount = draftFields.size,
                    onClearSelection = {
                        if (listMode == RequestListMode.SENT) {
                            requestViewModel.clearDraftFields()
                        } else {
                            selectedReceivedIndex = null
                            requestViewModel.clearDraftFields()
                            requestViewModel.clearActiveReceivedRequest()
                        }
                    },
                    primaryAction = DockAction(
                        label = if (listMode == RequestListMode.RECEIVED) "Match fields →" else "Add requested key",
                        enabled = if (listMode == RequestListMode.RECEIVED) draftFields.isNotEmpty() else true
                    ),
                    secondaryActions = emptyList(),
                    destructiveAction = DockAction(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        enabled = listMode == RequestListMode.SENT && draftFields.isNotEmpty()
                    ) {
                        if (listMode == RequestListMode.SENT && draftFields.isNotEmpty()) {
                            val deletedSnapshot = draftFields.toList()
                            requestViewModel.clearDraftFields()
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Deleted ${deletedSnapshot.size} drafted fields",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    requestViewModel.restoreDraftFields(deletedSnapshot)
                                }
                            }
                        }
                    },
                    onPrimaryClick = {
                        if (listMode == RequestListMode.RECEIVED) {
                            if (draftFields.isNotEmpty()) showMatchDialog = true
                            else snackbarMessage = "Select a request to match"
                        } else {
                            showAddRequestSheet = true
                        }
                    }
                )
                SharySectionNavigationBar(
                    currentTab = SectionTab.REQUESTS,
                    onTabSelected = { tab ->
                        when (tab) {
                            SectionTab.FIELDS -> navController.navigate(Screen.Fields.route) {
                                launchSingleTop = true
                            }
                            SectionTab.USERS -> navController.navigate(Screen.Users.route) {
                                launchSingleTop = true
                            }
                            SectionTab.REQUESTS -> Unit
                        }
                    }
                )
            }
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
            if (requestsToShow.isEmpty()) {
                val (title, body, action) = if (listMode == RequestListMode.SENT) {
                    Triple(
                        "No sent requests",
                        "Define what you need from others and send a request",
                        "Add requested key"
                    )
                } else {
                    Triple(
                        "No received requests",
                        "Incoming requests will appear here",
                        "Sync from cloud"
                    )
                }
                EmptyState(
                    title = title,
                    body = body,
                    primaryAction = action,
                    onPrimaryAction = {
                        if (listMode == RequestListMode.SENT) showAddRequestSheet = true
                        else {
                            if (!cloudReady) {
                                snackbarMessage = "Cloud unavailable or not verified"
                                return@EmptyState
                            }
                            openRequestInbox()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(requestsToShow) { index, request ->
                        val isActive = selectedReceivedIndex == index && listMode == RequestListMode.RECEIVED
                        RequestCard(
                            senderName = request.user.ifBlank { "Unknown sender" },
                            timestamp = request.dateAdded.formatRelativeLabel(),
                            keys = request.fields.map { it.key },
                            fieldCount = request.fields.size,
                            statusLabel = if (listMode == RequestListMode.RECEIVED) {
                                if (request.responded) "Responded" else "Pending"
                            } else {
                                "Sent"
                            },
                            statusTone = if (listMode == RequestListMode.RECEIVED) {
                                if (request.responded) RequestStatusTone.RECEIVED else RequestStatusTone.PENDING
                            } else {
                                RequestStatusTone.SENT
                            },
                            isActive = isActive,
                            onClick = {
                                if (listMode == RequestListMode.RECEIVED) {
                                    selectedReceivedIndex = index
                                    requestViewModel.clearDraftFields()
                                    requestViewModel.setActiveReceivedRequest(request)
                                    request.fields.forEach { requestViewModel.toggleFieldSelection(it) }
                                }
                            },
                            onMatchClick = if (listMode == RequestListMode.RECEIVED) {
                                { showMatchDialog = true }
                            } else {
                                null
                            },
                            modifier = Modifier.padding(vertical = cardVerticalPadding)
                        )
                    }
                }
            }
        }
    }

    if (showAddRequestSheet && listMode == RequestListMode.SENT) {
        AddRequestDialog(
            onDismiss = { showAddRequestSheet = false },
            onAddRequest = { key, keyAlias ->
                if (key.isNotBlank()) {
                    requestViewModel.addDraftField(
                        FieldDomain.initialize().copy(
                            key = key.trim(),
                            value = keyAlias.trim()
                        )
                    )
                    showAddRequestSheet = false
                    snackbarMessage = "Requested key '$key' added"
                } else {
                    snackbarMessage = "Requested key is required"
                }
            }
        )
    }

    if (showMatchDialog && listMode == RequestListMode.RECEIVED) {
        FieldMatchingDialog(
            storedFields = fields,
            requestFields = draftFields,
            onDismiss = { showMatchDialog = false },
            onAccept = { selected ->
                if (userViewModel.anyCachedUser()) {
                    fieldViewModel.setSelectedFields(selected)
                    navController.navigate(Screen.SummaryField.route)
                } else {
                    snackbarMessage = "Select recipient(s) first from Users"
                }
            },
            onAddField = { field -> fieldViewModel.addField(field) }
        )
    }

    if (openRequestInboxDialog) {
        val knownSendersByHash = remember(users) {
            users.associateBy { user -> hashMessageB64(user.email.trim().lowercase()) }
        }
        RequestInboxReviewDialog(
            isLoading = isRequestInboxLoading,
            pendingItems = requestInboxItems,
            resolveSenderLabel = { senderHash ->
                if (settings.showFriendlySenderIdentity) {
                    val matched = knownSendersByHash[senderHash]
                    if (matched != null) {
                        if (matched.username.isNotBlank()) {
                            "${matched.username} <${matched.email}>"
                        } else {
                            matched.email
                        }
                    } else {
                        "Unknown sender (${senderHash.take(12)}...)"
                    }
                } else {
                    "Sender hash: ${senderHash.take(16)}..."
                }
            },
            onAccept = { item ->
                val ownerEmail = userViewModel.getOwner().email
                if (ownerEmail.isNotBlank()) {
                    requestViewModel.acceptRequestInboxItem(ownerEmail, item)
                } else {
                    snackbarMessage = "User not logged in"
                }
            },
            onReject = { item ->
                val ownerEmail = userViewModel.getOwner().email
                if (ownerEmail.isNotBlank()) {
                    requestViewModel.rejectRequestInboxItem(ownerEmail, item)
                } else {
                    snackbarMessage = "User not logged in"
                }
            },
            onCancel = { openRequestInboxDialog = false }
        )
    }
}

@Composable
private fun RequestsModeSwitcher(
    listMode: RequestListMode,
    receivedCount: Int,
    onModeChange: (RequestListMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .background(SurfaceMid, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ModeButton(
            text = if (receivedCount > 0) "Received ($receivedCount)" else "Received",
            selected = listMode == RequestListMode.RECEIVED,
            onClick = { onModeChange(RequestListMode.RECEIVED) },
            modifier = Modifier.weight(1f)
        )
        ModeButton(
            text = "Sent",
            selected = listMode == RequestListMode.SENT,
            onClick = { onModeChange(RequestListMode.SENT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier.background(
            if (selected) Violet600 else androidx.compose.ui.graphics.Color.Transparent,
            RoundedCornerShape(12.dp)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) androidx.compose.ui.graphics.Color.White else Violet500
        )
    }
}

private fun java.time.Instant.formatRelativeLabel(): String {
    return DateTimeFormatter.ofPattern("dd MMM yyyy")
        .withZone(ZoneId.systemDefault())
        .format(this)
}
