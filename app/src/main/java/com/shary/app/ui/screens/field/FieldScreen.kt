package com.shary.app.ui.screens.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.fragment.app.FragmentActivity
import com.shary.app.core.domain.interfaces.events.FieldEvent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.SearchFieldBy
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.FieldValueTypes
import com.shary.app.core.domain.types.valueobjects.SENSITIVE_MASK
import com.shary.app.core.domain.types.valueobjects.TaskValueContract
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessageB64
import com.shary.app.ui.components.DockAction
import com.shary.app.ui.components.BuildVaultNudge
import com.shary.app.ui.components.EmptyState
import com.shary.app.ui.components.FieldCard
import com.shary.app.ui.components.FieldsToolbar
import com.shary.app.ui.components.PrimaryTabOverflowMenu
import com.shary.app.ui.components.DockVisualStyle
import com.shary.app.ui.components.SharyCommandDock
import com.shary.app.ui.components.SharyBottomActionShelf
import com.shary.app.ui.components.SharySectionNavigationBar
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.components.SectionTab
import com.shary.app.ui.components.shimmer
import com.shary.app.ui.screens.field.components.AddCopiedFieldDialog
import com.shary.app.ui.screens.field.components.AddFieldDialog
import com.shary.app.ui.screens.field.components.ChangePasswordDialog
import com.shary.app.ui.screens.field.components.CloudInboxReviewDialog
import com.shary.app.ui.screens.field.components.EditFieldTagDialog
import com.shary.app.ui.screens.field.components.EditFieldValueDialog
import com.shary.app.ui.screens.field.components.FieldDetailDialog
import com.shary.app.ui.screens.field.components.UpdateFieldDialog
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.SendFlowDestination
import com.shary.app.ui.screens.fieldSendFlowCta
import com.shary.app.ui.theme.DestructiveText
import com.shary.app.ui.theme.TagMintText
import com.shary.app.ui.screens.utils.cloudErrorMessage
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet800
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.utils.CopyFeedback
import com.shary.app.ui.utils.cloudUiStatus
import com.shary.app.ui.utils.isOnWifiNetwork
import com.shary.app.utils.BiometricAuthManager
import com.shary.app.utils.log.AppLogger
import com.shary.app.utils.log.StartupTrace
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.clipboard.ClipboardViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.field.FieldEditorOperation
import com.shary.app.viewmodels.field.FieldEditorOperationState
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.user.UserViewModel
import kotlinx.coroutines.launch

private fun typedValueItems(field: FieldDomain): List<String> {
    val parsed = FieldValueContract.parse(field.value)
    val spec = parsed.envelope?.spec ?: return emptyList()
    return when (spec.kind.trim().lowercase()) {
        "list" -> {
            val separator = spec.options["separator"].orEmpty().ifBlank { "," }
            parsed.plainData
                .split(separator)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
        "tasks" -> TaskValueContract.decode(parsed.plainData)
            .getOrDefault(emptyList())
            .map { "${it.title} - ${it.status.wireName}" }
        else -> emptyList()
    }
}

@Suppress("ASSIGNED_VALUE_IS_NEVER_READ", "UNUSED_VALUE")
@Composable
fun FieldsScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val clipboardViewModel: ClipboardViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    val fields by fieldViewModel.fields.collectAsState()
    val users by userViewModel.users.collectAsState()
    val selectedFields by fieldViewModel.selectedFields.collectAsState()
    val filteredFields by fieldViewModel.filteredFields.collectAsState()
    val recoverableKeys by fieldViewModel.recoverableKeys.collectAsState()
    val sortBy by fieldViewModel.sortByParameter.collectAsState()
    val ascendingSortByMap by fieldViewModel.ascendingSortByMap.collectAsState()
    val isLoading by fieldViewModel.isLoading.collectAsState()
    val editorOperationState by fieldViewModel.editorOperationState.collectAsState()
    val sessionDataState by fieldViewModel.sessionDataState.collectAsState()
    val fieldsLoading = sessionDataState.started && !sessionDataState.fieldsComplete
    val cloudInboxItems by fieldViewModel.cloudInboxItems.collectAsState()
    val isCloudInboxLoading by fieldViewModel.isCloudInboxLoading.collectAsState()
    val cloudState by cloudViewModel.cloudState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val cloudStatus = cloudUiStatus(cloudState)
    val cloudReady = cloudStatus.ready

    var openAddDialog by remember { mutableStateOf(false) }
    var openChangePasswordDialog by remember { mutableStateOf(false) }
    var openAddFieldCopyDialog by remember { mutableStateOf(false) }
    var openUpdateFieldDialog by remember { mutableStateOf(false) }
    var openCloudInboxDialog by remember { mutableStateOf(false) }
    var openEditValueDialog by remember { mutableStateOf(false) }
    var openEditTagDialog by remember { mutableStateOf(false) }
    var openFieldStatsDialog by remember { mutableStateOf(false) }
    var autoInboxAttempted by rememberSaveable { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<FieldDomain?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var lastSubmittedKey by remember { mutableStateOf<String?>(null) }
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }
    var sortExpanded by remember { mutableStateOf(false) }
    var suppressNextMultiDeletedFeedback by remember { mutableStateOf(false) }
    var hasLoggedFirstBatchRender by rememberSaveable { mutableStateOf(false) }
    var detailField by remember { mutableStateOf<FieldDomain?>(null) }
    var quickActionField by remember { mutableStateOf<FieldDomain?>(null) }
    val editorIsSaving = editorOperationState is FieldEditorOperationState.Saving
    val editorErrorMessage = (editorOperationState as? FieldEditorOperationState.Failed)?.message

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    LaunchedEffect(Unit) {
        fieldViewModel.events.collect { ev ->
            when (ev) {
                is FieldEvent.Saved -> {
                    if (lastFlow == AddFlow.COPY) {
                        openAddFieldCopyDialog = false
                        snackbarMessage = "Copied field '${lastSubmittedKey.orEmpty()}' added"
                    } else {
                        openAddDialog = false
                        snackbarMessage = "Field '${lastSubmittedKey.orEmpty()}' added"
                    }
                    lastFlow = AddFlow.NONE
                }
                is FieldEvent.AlreadyExists -> {
                    snackbarMessage =
                        if (lastFlow == AddFlow.COPY) {
                            "Copied field '${lastSubmittedKey.orEmpty()}' already exists"
                        } else {
                            "Field '${lastSubmittedKey.orEmpty()}' already exists"
                        }
                    lastFlow = AddFlow.NONE
                }
                is FieldEvent.MultiDeleted -> {
                    if (suppressNextMultiDeletedFeedback) {
                        suppressNextMultiDeletedFeedback = false
                    } else {
                        snackbarMessage = "Deleted ${ev.keys.size} fields"
                    }
                }
                is FieldEvent.ValueUpdated -> snackbarMessage = "Value updated for '${ev.key}'"
                is FieldEvent.AliasUpdated -> snackbarMessage = "Alias updated for '${ev.key}'"
                is FieldEvent.TagUpdated -> snackbarMessage = "Tag updated for '${ev.key}'"
                is FieldEvent.ValueRecovered -> snackbarMessage = "Previous value recovered for '${ev.key}'"
                is FieldEvent.FetchError -> snackbarMessage =
                    cloudErrorMessage(ev.throwable)
                is FieldEvent.CloudInboxLoaded -> {
                    snackbarMessage = "Pending cloud items: ${ev.count}. Review one by one."
                }
                FieldEvent.CloudInboxEmpty -> snackbarMessage = "No pending cloud items"
                is FieldEvent.CloudInboxRejected -> {
                    snackbarMessage = if (ev.backendAcknowledged) {
                        "Item rejected and removed from cloud inbox"
                    } else {
                        "Item rejected locally. Backend acknowledgement unavailable."
                    }
                }
                is FieldEvent.CloudInboxAccepted -> {
                    snackbarMessage = if (ev.backendAcknowledged) {
                        "Item accepted and acknowledged"
                    } else {
                        "Item accepted. Backend acknowledgement unavailable."
                    }
                }
                is FieldEvent.NoNewFields -> snackbarMessage = "No fields loaded"
                is FieldEvent.FetchedFromCloud -> {
                    val loaded = ev.loadedKeys
                    val preDownloaded = ev.preDownloadedKeys
                    snackbarMessage = buildString {
                        if (ev.count > 0) {
                            append("Loaded ${ev.count} fields")
                        } else {
                            append("No fields loaded")
                        }
                        if (loaded.isNotEmpty()) {
                            append(". Loaded: ")
                            append(loaded.joinToString(", "))
                        }
                        if (preDownloaded.isNotEmpty()) {
                            append(". Pre-downloaded: ")
                            append(preDownloaded.joinToString(", "))
                        }
                    }
                }
                FieldEvent.PasswordChanged -> {
                    openChangePasswordDialog = false
                    fieldViewModel.clearSelectedFields()
                    navController.navigate(Screen.Login.routeWithPasswordChanged(true)) {
                        popUpTo(Screen.Fields.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is FieldEvent.Error -> snackbarMessage = ev.throwable.message ?: "An error occurred"
                else -> Unit
            }
        }
    }

    LaunchedEffect(editorOperationState) {
        val terminal = editorOperationState as? FieldEditorOperationState.Saved
            ?: return@LaunchedEffect
        when (terminal.operation) {
            FieldEditorOperation.ADD -> {
                openAddDialog = false
                openAddFieldCopyDialog = false
                editingField = null
            }
            FieldEditorOperation.UPDATE,
            FieldEditorOperation.RECOVER -> {
                openUpdateFieldDialog = false
                editingField = null
            }
        }
        fieldViewModel.resetEditorOperationState()
    }

    LaunchedEffect(searchQuery) {
        fieldViewModel.updateSearch(searchQuery, SearchFieldBy.KEY)
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> Unit
                Lifecycle.Event.ON_STOP -> {
                    fieldViewModel.setSelectedFields(selectedFields)
                    searchQuery = ""
                    autoInboxAttempted = false
                    detailField = null
                    quickActionField = null
                    openEditValueDialog = false
                    openEditTagDialog = false
                    openAddFieldCopyDialog = false
                }
                else -> Unit
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val dynamicTagFilters = remember(fields) {
        fields
            .map { it.tag.toTagString().trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val searchFilters = remember(dynamicTagFilters) {
        listOf("All") + dynamicTagFilters
    }

    LaunchedEffect(searchFilters) {
        if (activeFilter !in searchFilters) {
            activeFilter = "All"
        }
    }

    val visibleFields = remember(filteredFields, activeFilter) {
        filteredFields.filter { field ->
            activeFilter == "All" ||
                    field.tag.toTagString().equals(activeFilter, ignoreCase = true)
        }
    }
    val fieldTagCounts = remember(fields) { buildFieldTagCounts(fields) }

    fun typedValueLabel(field: FieldDomain): String? {
        val kind = FieldValueContract.parse(field.value).envelope?.spec?.kind?.trim()?.lowercase()
        return when (kind) {
            "json" -> "JSON"
            "yaml", "yml" -> "YAML"
            "list" -> "List"
            "questionnaire" -> "Questionnaire"
            "file" -> "File"
            "tasks" -> "Tasks"
            "sensitive" -> "Sensitive"
            "link_url", "url", "link" -> "Link"
            else -> null
        }
    }

    LaunchedEffect(visibleFields.size) {
        if (!hasLoggedFirstBatchRender && visibleFields.isNotEmpty()) {
            hasLoggedFirstBatchRender = true
            val elapsed = StartupTrace.elapsedSinceLoginStartMsOrNull()
            if (elapsed != null) {
                AppLogger.info(
                    "StartupTrace",
                    "event=fields_first_batch_rendered count=${visibleFields.size} elapsed_ms=$elapsed"
                )
            } else {
                AppLogger.info(
                    "StartupTrace",
                    "event=fields_first_batch_rendered count=${visibleFields.size}"
                )
            }
        }
    }

    val cardVerticalPadding = if (settings.compactListMode) 2.dp else 4.dp
    val skeletonHeight = if (settings.compactListMode) 80.dp else 96.dp

    fun openCloudInbox() {
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
            openCloudInboxDialog = true
            fieldViewModel.loadCloudInbox(ownerEmail)
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

    fun withSensitiveAccess(field: FieldDomain, onAuthorized: () -> Unit) {
        if (!FieldValueTypes.isSensitive(field.value)) {
            onAuthorized()
            return
        }
        val hostActivity = context as? FragmentActivity
        if (hostActivity == null) {
            snackbarMessage = "Device authentication is unavailable on this screen"
            return
        }
        val authError = BiometricAuthManager(
            context = context,
            activity = hostActivity,
            onAuthSuccess = onAuthorized
        ).authenticate()
        if (authError != null) snackbarMessage = authError
    }

    fun copyFieldValue(field: FieldDomain) {
        val plainValue = FieldValueContract.parse(field.value).plainData
        clipboardViewModel.copyText(
            label = "field_value",
            text = plainValue,
            sensitive = FieldValueTypes.isSensitive(field.value),
            clearAfterSeconds = settings.clipboardAutoClearSeconds
        )
        CopyFeedback.playSuccessTone()
        snackbarMessage = "Value copied to clipboard"
    }

    LaunchedEffect(settings.autoOpenCloudInboxOnStart, cloudReady) {
        if (settings.autoOpenCloudInboxOnStart && cloudReady && !autoInboxAttempted) {
            autoInboxAttempted = true
            openCloudInbox()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Violet50),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                FieldsToolbar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    filters = searchFilters,
                    activeFilter = activeFilter,
                    onFilterChange = { activeFilter = it },
                    sortAction = {
                        Box {
                        SharySoftButton(
                            text = sortBy.label,
                            icon = Icons.Default.SwapVert,
                            modifier = Modifier
                                .width(54.dp)
                                .border(1.5.dp, Violet600, SharyRadius.input),
                            minHeight = 40.dp,
                            iconSize = 13.dp,
                            iconSpacing = 2.dp,
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                            containerColor = Color.White,
                            contentColor = Violet800,
                            onClick = { sortExpanded = true }
                        )
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            com.shary.app.core.domain.types.enums.SortByParameter.entries.forEach { option ->
                                val isAscending = ascendingSortByMap[option] ?: true
                                val isSelected = option == sortBy
                                DropdownMenuItem(
                                    text = { Text("${option.label} ${if (isAscending) "↑" else "↓"}") },
                                    onClick = {
                                        val nextAscending =
                                            if (isSelected) !isAscending else isAscending
                                        fieldViewModel.updateSort(option, nextAscending)
                                        sortExpanded = false
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                        }
                    },
                    overflowAction = {
                    PrimaryTabOverflowMenu(
                        onOpenSettings = { navController.navigate(Screen.Settings.route) },
                        onChangePassword = { openChangePasswordDialog = true },
                        onLogout = {
                            fieldViewModel.clearSelectedFields()
                            onLogout()
                        },
                        extraItems = { dismissMenu ->
                            DropdownMenuItem(
                                text = { Text("Fields info") },
                                onClick = {
                                    dismissMenu()
                                    openFieldStatsDialog = true
                                },
                                leadingIcon = {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    )
                    }
                )
            }
        },
        bottomBar = {
            Column {
                val hasSelection = selectedFields.isNotEmpty()
                val sendFlowCta = fieldSendFlowCta(
                    hasSelectedFields = hasSelection,
                    hasSelectedUsers = userViewModel.anyCachedUser()
                )
                sendFlowCta?.let { currentSendFlowCta ->
                    SharyCommandDock(
                        selectedCount = selectedFields.size,
                        onClearSelection = fieldViewModel::clearSelectedFields,
                        primaryAction = DockAction(
                            label = currentSendFlowCta.label,
                            icon = when (currentSendFlowCta.destination) {
                                SendFlowDestination.USERS -> Icons.Default.Person
                                else -> Icons.AutoMirrored.Filled.ArrowForward
                            },
                            contentDescription = currentSendFlowCta.contentDescription
                        ),
                        secondaryActions = emptyList(),
                        destructiveAction = DockAction(
                            label = "",
                            icon = Icons.Default.Delete,
                            enabled = hasSelection,
                            contentDescription = "Delete selected fields"
                        ) {
                            if (hasSelection) {
                                val deletedSnapshot = selectedFields.toList()
                                suppressNextMultiDeletedFeedback = true
                                fieldViewModel.deleteFields(deletedSnapshot)
                                fieldViewModel.clearSelectedFields()
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Deleted ${deletedSnapshot.size} fields",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        fieldViewModel.restoreDeletedFields(deletedSnapshot)
                                    }
                                }
                            }
                        },
                        visualStyle = if (currentSendFlowCta.destination == SendFlowDestination.USERS) {
                            DockVisualStyle.FullWidth
                        } else {
                            DockVisualStyle.IconOnly
                        },
                        onPrimaryClick = {
                            fieldViewModel.setSelectedFields(selectedFields)
                            when (currentSendFlowCta.destination) {
                                SendFlowDestination.USERS -> navController.navigate(Screen.Users.route)
                                SendFlowDestination.FIELD_REVIEW -> navController.navigate(Screen.SummaryField.route)
                                else -> Unit
                            }
                        }
                    )
                }
                if (!hasSelection) {
                    val cloudContainerColor = if (cloudReady) TagMintText else DestructiveText
                    SharyBottomActionShelf {
                        SharySoftButton(
                            text = "Cloud inbox",
                            icon = Icons.Default.CloudDownload,
                            contentDescription = if (cloudReady) "Open cloud inbox" else "Cloud unavailable",
                            enabled = cloudReady,
                            onDisabledClick = { snackbarMessage = cloudStatus.detail },
                            modifier = Modifier.weight(1f),
                            minHeight = 52.dp,
                            containerColor = cloudContainerColor,
                            contentColor = Color.White,
                            onClick = ::openCloudInbox
                        )
                        SharySoftButton(
                            text = "Add field",
                            icon = Icons.Default.Add,
                            contentDescription = "Add a new field",
                            modifier = Modifier.weight(1f),
                            minHeight = 52.dp,
                            containerColor = Violet600,
                            contentColor = Color.White,
                            onClick = { openAddDialog = true }
                        )
                    }
                }
                SharySectionNavigationBar(
                    currentTab = SectionTab.FIELDS,
                    onTabSelected = { tab ->
                        when (tab) {
                            SectionTab.FIELDS -> Unit
                            SectionTab.CONNECT -> navController.navigate(Screen.Connectors.route) {
                                launchSingleTop = true
                            }
                            SectionTab.USERS -> navController.navigate(Screen.Users.route) {
                                launchSingleTop = true
                            }
                            SectionTab.REQUESTS -> navController.navigate(Screen.Requests.route) {
                                launchSingleTop = true
                            }
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
        ) {
            if (fieldsLoading && visibleFields.isNotEmpty()) {
                Text(
                    text = "First fields are ready. Decrypting remaining fields in the background...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }
            when {
                fieldsLoading && visibleFields.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                    ) {
                        items(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = cardVerticalPadding)
                                    .height(skeletonHeight)
                                    .clip(RoundedCornerShape(18.dp))
                                    .shimmer()
                            )
                        }
                    }
                }
                visibleFields.isEmpty() -> {
                    EmptyState(
                        title = "No fields yet",
                        body = "Store your first piece of personal data",
                        primaryAction = "Create your first field",
                        onPrimaryAction = { openAddDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                    ) {
                        items(
                            items = visibleFields,
                            key = { it.key }
                        ) { field ->
                            val isSensitive = FieldValueTypes.isSensitive(field.value)
                            FieldCard(
                                fieldKey = field.key,
                                fieldValue = FieldValueTypes.displayData(field.value),
                                tag = field.tag,
                                typedValueLabel = typedValueLabel(field),
                                alias = if (isSensitive) {
                                    SENSITIVE_MASK
                                } else {
                                    field.keyAlias.takeIf { it.isNotBlank() }
                                },
                                isSelected = selectedFields.any {
                                    it.key.trim().equals(field.key.trim(), ignoreCase = true)
                                },
                                onCardTap = {
                                    withSensitiveAccess(field) { detailField = field }
                                },
                                onToggleSelection = { fieldViewModel.toggleFieldSelection(field) },
                                onLongPressCopy = {
                                    withSensitiveAccess(field) {
                                        copyFieldValue(field)
                                    }
                                },
                                copyLongPressMillis = settings.copyLongPressMillis.toLong(),
                                modifier = Modifier.padding(vertical = cardVerticalPadding)
                            )
                        }
                        if (shouldShowBuildVaultNudge(fields.size, searchQuery, activeFilter)) {
                            item(key = "build-vault-nudge") {
                                BuildVaultNudge(
                                    onClick = { openAddDialog = true },
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detailField?.let { field ->
        FieldDetailDialog(
            keyName = field.key,
            value = FieldValueContract.parse(field.value).plainData,
            valueItems = typedValueItems(field),
            alias = field.keyAlias,
            tag = field.tag,
            typedValueLabel = typedValueLabel(field),
            dateAdded = field.dateAdded,
            onCopyValue = { copyFieldValue(field) },
            onDuplicateField = {
                editingField = field
                openAddFieldCopyDialog = true
            },
            onEdit = {
                editingField = field
                detailField = null
                openUpdateFieldDialog = true
            },
            onEditTag = {
                quickActionField = field
                detailField = null
                openEditTagDialog = true
            },
            onDeleteField = {
                val deletedSnapshot = listOf(field)
                suppressNextMultiDeletedFeedback = true
                fieldViewModel.deleteFields(deletedSnapshot)
                fieldViewModel.clearSelectedFields()
                detailField = null
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted 1 field",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        fieldViewModel.restoreDeletedFields(deletedSnapshot)
                    }
                }
            },
            showMcpShareable = FieldValueTypes.isSensitive(field.value),
            isMcpShareable = field.isMcpShareable,
            onMcpShareableChange = { shareable ->
                fieldViewModel.setMcpShareable(field, shareable)
                detailField = field.copy(isMcpShareable = shareable)
            },
            onDismiss = { detailField = null }
        )
    }

    if (openAddDialog) {
        AddFieldDialog(
            onDismiss = {
                openAddDialog = false
                fieldViewModel.resetEditorOperationState()
            },
            onAddField = { newField ->
                lastFlow = AddFlow.ADD
                lastSubmittedKey = newField.key
                fieldViewModel.submitEditorAddField(newField)
            },
            isSaving = editorIsSaving,
            errorMessage = editorErrorMessage
        )
    }

    if (openFieldStatsDialog) {
        AlertDialog(
            onDismissRequest = { openFieldStatsDialog = false },
            title = { Text("Fields info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total stored: ${fields.size}")
                    if (visibleFields.size != fields.size || activeFilter != "All" || searchQuery.isNotBlank()) {
                        Text("Currently shown: ${visibleFields.size}")
                    }
                    if (fieldTagCounts.isEmpty()) {
                        Text("No tag usage available yet.")
                    } else {
                        Text("By tag")
                        fieldTagCounts.forEach { stat ->
                            Text("${stat.tagName}: ${stat.count}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openFieldStatsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (openAddFieldCopyDialog && editingField != null) {
        AddCopiedFieldDialog(
            targetField = editingField!!,
            onDismiss = {
                openAddFieldCopyDialog = false
                editingField = null
                fieldViewModel.resetEditorOperationState()
            },
            onAddCopiedField = { newCopyDomain ->
                lastFlow = AddFlow.COPY
                lastSubmittedKey = newCopyDomain.key
                fieldViewModel.submitEditorAddField(newCopyDomain)
            },
            isSaving = editorIsSaving,
            errorMessage = editorErrorMessage
        )
    }

    if (openUpdateFieldDialog && editingField != null) {
        UpdateFieldDialog(
            targetField = editingField!!,
            onDismiss = {
                editingField = null
                openUpdateFieldDialog = false
                fieldViewModel.resetEditorOperationState()
            },
            onUpdateField = { newField ->
                fieldViewModel.submitEditorUpdateField(
                    editingField!!,
                    newField.copy(dateAdded = editingField!!.dateAdded)
                )
            },
            canRecoverPreviousValue = recoverableKeys.contains(
                editingField!!.key.trim().lowercase()
            ),
            onRecoverPreviousValue = {
                fieldViewModel.recoverPreviousValue(editingField!!)
            },
            isSaving = editorIsSaving,
            errorMessage = editorErrorMessage
        )
    }

    val editValueTarget = quickActionField
    if (openEditValueDialog && editValueTarget != null) {
        EditFieldValueDialog(
            initialValue = FieldValueContract.parse(editValueTarget.value).plainData,
            onDismiss = {
                openEditValueDialog = false
                quickActionField = null
            },
            onConfirm = { newValue ->
                fieldViewModel.updateField(
                    editValueTarget,
                    editValueTarget.copy(value = newValue)
                )
                openEditValueDialog = false
                quickActionField = null
            }
        )
    }

    val editTagTarget = quickActionField
    if (openEditTagDialog && editTagTarget != null) {
        EditFieldTagDialog(
            initialTag = editTagTarget.tag,
            onDismiss = {
                openEditTagDialog = false
                quickActionField = null
            },
            onConfirm = { newTag ->
                fieldViewModel.updateField(
                    editTagTarget,
                    editTagTarget.copy(tag = newTag)
                )
                openEditTagDialog = false
                quickActionField = null
            }
        )
    }

    if (openChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = isLoading,
            onDismiss = { openChangePasswordDialog = false },
            onAccept = { oldPassword, newPassword, repeatNewPassword ->
                fieldViewModel.changePasswordAndRewrapDataKey(
                    context = context,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    repeatNewPassword = repeatNewPassword
                )
            }
        )
    }

    if (openCloudInboxDialog) {
        val knownSendersByHash = remember(users) {
            users.associateBy { user -> hashMessageB64(user.email.trim().lowercase()) }
        }
        CloudInboxReviewDialog(
            isLoading = isCloudInboxLoading,
            pendingItems = cloudInboxItems,
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
                    fieldViewModel.acceptCloudInboxItem(ownerEmail, item)
                } else {
                    snackbarMessage = "User not logged in"
                }
            },
            onReject = { item ->
                val ownerEmail = userViewModel.getOwner().email
                if (ownerEmail.isNotBlank()) {
                    fieldViewModel.rejectCloudInboxItem(ownerEmail, item)
                } else {
                    snackbarMessage = "User not logged in"
                }
            },
            onCancel = { openCloudInboxDialog = false }
        )
    }

}
