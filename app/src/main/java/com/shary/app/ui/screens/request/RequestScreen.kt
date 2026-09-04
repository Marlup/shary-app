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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.application.request.RequestDraftFieldAddOutcome
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.core.domain.interfaces.events.FieldEvent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDeliverySummary
import com.shary.app.core.domain.models.deliverySummaryOrNull
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessageB64
import com.shary.app.ui.components.DockAction
import com.shary.app.ui.components.DockVisualStyle
import com.shary.app.ui.components.EmptyState
import com.shary.app.ui.components.PrimaryTabOverflowMenu
import com.shary.app.ui.components.RequestCard
import com.shary.app.ui.components.RequestStatusTone
import com.shary.app.ui.components.SharyBottomActionShelf
import com.shary.app.ui.components.SharyModalAction
import com.shary.app.ui.components.SharyCommandDock
import com.shary.app.ui.components.SharySectionNavigationBar
import com.shary.app.ui.components.SharySectionToolbar
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.components.SectionTab
import com.shary.app.ui.screens.field.components.ChangePasswordDialog
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.request.utils.AddRequestDialog
import com.shary.app.ui.screens.request.utils.RequestDetailDialog
import com.shary.app.ui.screens.request.utils.RequestInboxReviewDialog
import com.shary.app.ui.screens.utils.cloudErrorMessage
import com.shary.app.ui.screens.utils.FieldMatchingDialog
import com.shary.app.ui.theme.SurfaceMid
import com.shary.app.ui.theme.DestructiveText
import com.shary.app.ui.theme.TagMintText
import com.shary.app.ui.theme.TextMuted
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

@Suppress("ASSIGNED_VALUE_IS_NEVER_READ", "UNUSED_VALUE")
@Composable
fun RequestsScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val fields by fieldViewModel.fields.collectAsState()
    val users by userViewModel.users.collectAsState()
    val requestWorkflow by requestViewModel.requestWorkflow.collectAsState()
    val listMode by requestViewModel.listMode.collectAsState()
    val draftFields by requestViewModel.draftFields.collectAsState()
    val receivedRequests by requestViewModel.receivedRequests.collectAsState()
    val sentRequests by requestViewModel.sentRequests.collectAsState()
    val requestInboxItems by requestViewModel.requestInboxItems.collectAsState()
    val isRequestInboxLoading by requestViewModel.isCloudInboxLoading.collectAsState()
    val requestInboxDecisionState by requestViewModel.requestInboxDecisionState.collectAsState()
    val cloudState by cloudViewModel.cloudState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val cloudReady = cloudState.isOnline && cloudState.isUserValidated

    val requestsToShow = if (listMode == RequestListMode.SENT) sentRequests else receivedRequests
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddRequestSheet by remember { mutableStateOf(false) }
    var showMatchDialog by remember { mutableStateOf(false) }
    var openRequestInboxDialog by remember { mutableStateOf(false) }
    var openChangePasswordDialog by remember { mutableStateOf(false) }
    var autoInboxAttempted by rememberSaveable { mutableStateOf(false) }
    var detailRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    val cardVerticalPadding = if (settings.compactListMode) 2.dp else 4.dp
    val visibleRequests = remember(requestsToShow, searchQuery) {
        filterRequestsByQuery(requestsToShow, searchQuery)
    }

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
                is RequestEvent.ResponseStatusUpdateFailed -> {
                    snackbarMessage = event.throwable.message
                        ?: "Unable to update the received request status"
                }
                is RequestEvent.CloudInboxLoaded -> {
                    snackbarMessage = "Pending cloud requests: ${event.count}. Review one by one."
                }
                RequestEvent.CloudInboxEmpty -> {
                    snackbarMessage = "No pending request items"
                }
                is RequestEvent.CloudInboxRejected -> {
                    snackbarMessage = if (event.backendAcknowledged) {
                        "Cloud acknowledged Reject. No local request was imported."
                    } else {
                        "No local request was imported. Cloud acknowledgement unavailable."
                    }
                }
                is RequestEvent.CloudInboxAccepted -> {
                    snackbarMessage = if (event.backendAcknowledged) {
                        "Local received request available with ${event.importedKeyCount} keys; " +
                            "cloud acknowledged Accept."
                    } else {
                        "Local received request available with ${event.importedKeyCount} keys; " +
                            "cloud acknowledgement unavailable."
                    }
                }
            }
        }
    }

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

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                autoInboxAttempted = false
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
                SharySectionToolbar(
                    query = searchQuery,
                    onQueryChange = { updatedQuery ->
                        if (updatedQuery != searchQuery) {
                            detailRequestId = null
                            if (listMode == RequestListMode.RECEIVED) {
                                requestViewModel.clearActiveReceivedRequest()
                            }
                        }
                        searchQuery = updatedQuery
                    }
                ) {
                    PrimaryTabOverflowMenu(
                        onOpenSettings = { navController.navigate(Screen.Settings.route) },
                        onChangePassword = { openChangePasswordDialog = true },
                        onLogout = {
                            fieldViewModel.clearSelectedFields()
                            onLogout()
                        }
                    )
                }
                AnimatedContent(targetState = listMode, label = "requests-mode-switcher") { mode ->
                    RequestsModeSwitcher(
                        listMode = mode,
                        receivedCount = receivedRequests.size,
                        onModeChange = { updatedMode ->
                            detailRequestId = null
                            requestViewModel.setListMode(updatedMode)
                        }
                    )
                }
            }
        },
        bottomBar = {
            Column {
                if (draftFields.isNotEmpty()) {
                    SharyCommandDock(
                        selectedCount = draftFields.size,
                        onClearSelection = {
                            if (listMode == RequestListMode.SENT) {
                                requestViewModel.clearDraftFields()
                            } else {
                                requestViewModel.clearActiveReceivedRequest()
                            }
                        },
                        primaryAction = DockAction(
                            label = if (listMode == RequestListMode.RECEIVED) "Match fields" else "Add requested key",
                            icon = if (listMode == RequestListMode.RECEIVED) Icons.Default.Compare else Icons.Default.Add,
                            contentDescription = if (listMode == RequestListMode.RECEIVED) {
                                "Match selected request fields"
                            } else {
                                "Add requested key"
                            }
                        ),
                        secondaryActions = emptyList(),
                        destructiveAction = if (listMode == RequestListMode.SENT) {
                            DockAction(
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                contentDescription = "Delete selected requested keys"
                            ) {
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
                        } else {
                            null
                        },
                        visualStyle = DockVisualStyle.IconOnly,
                        onPrimaryClick = {
                            if (listMode == RequestListMode.RECEIVED) {
                                showMatchDialog = true
                            } else {
                                showAddRequestSheet = true
                            }
                        }
                    )
                } else {
                    val cloudContainerColor = if (cloudReady) TagMintText else DestructiveText
                    SharyBottomActionShelf {
                        SharySoftButton(
                            text = "Cloud inbox",
                            icon = Icons.Default.CloudDownload,
                            contentDescription = if (cloudReady) "Open request cloud inbox" else "Cloud unavailable",
                            enabled = cloudReady,
                            onDisabledClick = { snackbarMessage = "Cloud unavailable or not verified" },
                            modifier = Modifier.weight(1f),
                            minHeight = 52.dp,
                            containerColor = cloudContainerColor,
                            contentColor = Color.White,
                            onClick = ::openRequestInbox
                        )
                        SharySoftButton(
                            text = if (listMode == RequestListMode.SENT) "Add requested key" else "Match fields",
                            icon = if (listMode == RequestListMode.SENT) Icons.Default.Add else Icons.Default.Compare,
                            contentDescription = if (listMode == RequestListMode.SENT) {
                                "Add request"
                            } else {
                                "Select a received request to match"
                            },
                            modifier = Modifier.weight(1f),
                            minHeight = 52.dp,
                            containerColor = Violet600,
                            contentColor = Color.White,
                            onClick = {
                                if (listMode == RequestListMode.SENT) {
                                    showAddRequestSheet = true
                                } else {
                                    snackbarMessage = "Select a received request first"
                                }
                            }
                        )
                    }
                }
                SharySectionNavigationBar(
                    currentTab = SectionTab.REQUESTS,
                    onTabSelected = { tab ->
                        when (tab) {
                            SectionTab.FIELDS -> navController.navigate(Screen.Fields.route) {
                                launchSingleTop = true
                            }
                            SectionTab.CONNECT -> navController.navigate(Screen.Connectors.route) {
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
            if (visibleRequests.isEmpty()) {
                val (title, body, action) = if (searchQuery.isNotBlank()) {
                    Triple(
                        "No matching requests",
                        "Try a different sender, recipient, or field key",
                        "Clear search"
                    )
                } else if (listMode == RequestListMode.SENT) {
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
                        if (searchQuery.isNotBlank()) {
                            searchQuery = ""
                        } else if (listMode == RequestListMode.SENT) {
                            showAddRequestSheet = true
                        } else {
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
                    items(
                        items = visibleRequests,
                        key = { request -> "${listMode.name}:${request.id}" }
                    ) { request ->
                        val isActive = requestWorkflow.activeReceivedRequest?.id == request.id &&
                            listMode == RequestListMode.RECEIVED
                        val deliverySummary = request.deliverySummaryOrNull()
                        RequestCard(
                            senderName = request.user.ifBlank { "Unknown sender" },
                            timestamp = request.dateAdded.formatRelativeLabel(),
                            keys = request.fields.map { it.key },
                            fieldCount = request.fields.size,
                            statusLabel = if (listMode == RequestListMode.RECEIVED) {
                                if (request.responded) "Responded" else "Pending"
                            } else {
                                when (deliverySummary) {
                                    RequestDeliverySummary.SENT -> "Sent"
                                    RequestDeliverySummary.PARTIAL -> "Partial"
                                    RequestDeliverySummary.FAILED -> "Failed"
                                    null -> "Sent"
                                }
                            },
                            statusTone = if (listMode == RequestListMode.RECEIVED) {
                                if (request.responded) RequestStatusTone.RECEIVED else RequestStatusTone.PENDING
                            } else {
                                when (deliverySummary) {
                                    RequestDeliverySummary.SENT -> RequestStatusTone.SENT
                                    RequestDeliverySummary.PARTIAL -> RequestStatusTone.PARTIAL
                                    RequestDeliverySummary.FAILED -> RequestStatusTone.FAILED
                                    null -> RequestStatusTone.SENT
                                }
                            },
                            isActive = isActive,
                            onCardTap = {
                                detailRequestId = request.id
                            },
                            onToggleSelection = {
                                if (listMode == RequestListMode.RECEIVED) {
                                    if (requestWorkflow.activeReceivedRequest?.id == request.id) {
                                        requestViewModel.clearActiveReceivedRequest()
                                    } else if (!requestViewModel.setActiveReceivedRequest(request)) {
                                        snackbarMessage =
                                            "This request is completed or has no verified requester identity"
                                    }
                                }
                            },
                            onMatchClick = if (listMode == RequestListMode.RECEIVED) {
                                {
                                    if (isActive) {
                                        showMatchDialog = true
                                    } else {
                                        snackbarMessage = "Select this request before matching fields"
                                    }
                                }
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

    val detailRequest = findRequestById(requestsToShow, detailRequestId)
    if (detailRequest != null) {
        val responseDraft = requestWorkflow.responseDraft
            ?.takeIf { draft -> draft.requestId == detailRequest.id }
        val detailMatchedFieldCount = responseDraft?.matchedFieldKeys?.size
        val isDetailActive = requestWorkflow.activeReceivedRequest?.id == detailRequest.id
        RequestDetailDialog(
            request = detailRequest,
            mode = listMode,
            onDismiss = { detailRequestId = null },
            matchedFieldCount = detailMatchedFieldCount,
            primaryAction = if (
                listMode == RequestListMode.RECEIVED && !detailRequest.responded
            ) {
                SharyModalAction(
                    label = if ((detailMatchedFieldCount ?: 0) > 0) {
                        "Continue matching"
                    } else {
                        "Match fields"
                    },
                    onClick = {
                        val canMatch = isDetailActive ||
                            requestViewModel.setActiveReceivedRequest(detailRequest)
                        if (canMatch) {
                            detailRequestId = null
                            showMatchDialog = true
                        } else {
                            snackbarMessage =
                                "This request has no verified requester identity"
                        }
                    }
                )
            } else {
                null
            }
        )
    }

    if (showAddRequestSheet && listMode == RequestListMode.SENT) {
        AddRequestDialog(
            onDismiss = { showAddRequestSheet = false },
            onAddRequest = { key, keyAlias ->
                requestViewModel.addDraftField(
                    FieldDomain.initialize().copy(
                        key = key,
                        value = "",
                        keyAlias = keyAlias
                    )
                ).also { outcome ->
                    if (outcome == RequestDraftFieldAddOutcome.ADDED) {
                        snackbarMessage = "Requested key '$key' added"
                    }
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
                scope.launch {
                    val responseRequestId = requestViewModel.prepareResponse(selected)
                    if (responseRequestId != null) {
                        showMatchDialog = false
                        navController.navigate(Screen.SummaryField.responseRoute(responseRequestId))
                    } else {
                        snackbarMessage = "Unable to persist the response review draft"
                    }
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
            decisionState = requestInboxDecisionState,
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
            onReceiptConsumed = { hasMoreItems ->
                requestViewModel.consumeRequestInboxDecisionReceipt()
                if (!hasMoreItems) {
                    openRequestInboxDialog = false
                }
            },
            onCancel = { openRequestInboxDialog = false }
        )
    }

    if (openChangePasswordDialog) {
        val isPasswordLoading by fieldViewModel.isLoading.collectAsState()
        ChangePasswordDialog(
            isLoading = isPasswordLoading,
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
            color = if (selected) androidx.compose.ui.graphics.Color.White else TextMuted
        )
    }
}

private fun java.time.Instant.formatRelativeLabel(): String {
    return DateTimeFormatter.ofPattern("dd MMM yyyy")
        .withZone(ZoneId.systemDefault())
        .format(this)
}
