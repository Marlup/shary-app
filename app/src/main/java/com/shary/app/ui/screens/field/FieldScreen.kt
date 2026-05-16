package com.shary.app.ui.screens.field

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
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
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessageB64
import com.shary.app.ui.components.DockAction
import com.shary.app.ui.components.EmptyState
import com.shary.app.ui.components.FieldCard
import com.shary.app.ui.components.SharyCommandDock
import com.shary.app.ui.components.SharyIconButton
import com.shary.app.ui.components.SharySearchBar
import com.shary.app.ui.components.SharySectionNavigationBar
import com.shary.app.ui.components.SharyTopBar
import com.shary.app.ui.components.SectionTab
import com.shary.app.ui.components.shimmer
import com.shary.app.ui.screens.field.components.AddCopiedFieldDialog
import com.shary.app.ui.screens.field.components.AddFieldDialog
import com.shary.app.ui.screens.field.components.ChangePasswordDialog
import com.shary.app.ui.screens.field.components.CloudInboxReviewDialog
import com.shary.app.ui.screens.field.components.SortMenu
import com.shary.app.ui.screens.field.components.UpdateFieldDialog
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.cloudErrorMessage
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.utils.isOnWifiNetwork
import com.shary.app.utils.BiometricAuthManager
import com.shary.app.utils.log.AppLogger
import com.shary.app.utils.log.StartupTrace
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.user.UserViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FieldsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    val fields by fieldViewModel.fields.collectAsState()
    val users by userViewModel.users.collectAsState()
    val selectedFields by fieldViewModel.selectedFields.collectAsState()
    val filteredFields by fieldViewModel.filteredFields.collectAsState()
    val recoverableKeys by fieldViewModel.recoverableKeys.collectAsState()
    val sortBy by fieldViewModel.sortByParameter.collectAsState()
    val ascendingSortByMap by fieldViewModel.ascendingSortByMap.collectAsState()
    val isLoading by fieldViewModel.isLoading.collectAsState()
    val cloudInboxItems by fieldViewModel.cloudInboxItems.collectAsState()
    val isCloudInboxLoading by fieldViewModel.isCloudInboxLoading.collectAsState()
    val cloudState by cloudViewModel.cloudState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val cloudReady = cloudState.isOnline && cloudState.isUserValidated

    var openAddDialog by remember { mutableStateOf(false) }
    var openChangePasswordDialog by remember { mutableStateOf(false) }
    var openAddFieldCopyDialog by remember { mutableStateOf(false) }
    var openUpdateFieldDialog by remember { mutableStateOf(false) }
    var openCloudInboxDialog by remember { mutableStateOf(false) }
    var autoInboxAttempted by rememberSaveable { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<FieldDomain?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var lastSubmittedKey by remember { mutableStateOf<String?>(null) }
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }
    var filterExpanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    var suppressNextMultiDeletedFeedback by remember { mutableStateOf(false) }
    var hasLoggedFirstBatchRender by rememberSaveable { mutableStateOf(false) }
    var clipboardAutoClearJob by remember { mutableStateOf<Job?>(null) }

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

    val selectedSingle = selectedFields.singleOrNull()
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
                SharyTopBar(
                    title = "",
                    subtitle = if (activeFilter == "All") {
                        "${visibleFields.size} stored"
                    } else {
                        "${visibleFields.size} shown"
                    },
                    actions = {
                        SortMenu(
                            currentSort = sortBy,
                            isAscendingMap = ascendingSortByMap,
                            onSortChange = { s, asc -> fieldViewModel.updateSort(s, asc) }
                        )
                        Box {
                            SharyIconButton(
                                icon = Icons.Default.FilterList,
                                contentDescription = "Filter tags",
                                onClick = { filterExpanded = true }
                            )
                            DropdownMenu(
                                expanded = filterExpanded,
                                onDismissRequest = { filterExpanded = false }
                            ) {
                                searchFilters.forEach { filter ->
                                    val isSelected = filter == activeFilter
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (filter == "All") "All tags" else filter)
                                        },
                                        onClick = {
                                            activeFilter = filter
                                            filterExpanded = false
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
                        // Keep ThemeMenuButton in code for future re-enable, but do not render while disabled.
                        // ThemeMenuButton(enabled = false, onThemeChosen = {})
                        SharyIconButton(
                            icon = Icons.Default.CloudDownload,
                            contentDescription = "Sync from cloud",
                            enabled = cloudReady,
                            onClick = ::openCloudInbox
                        )
                        SharyIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add field",
                            onClick = { openAddDialog = true }
                        )
                        if (selectedSingle != null) {
                            SharyIconButton(
                                icon = Icons.Default.Edit,
                                contentDescription = "Edit selected field",
                                onClick = {
                                    editingField = selectedSingle
                                    openUpdateFieldDialog = true
                                }
                            )
                            SharyIconButton(
                                icon = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate selected field",
                                onClick = {
                                    editingField = selectedSingle
                                    openAddFieldCopyDialog = true
                                }
                            )
                        }
                        Box {
                            SharyIconButton(
                                icon = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                onClick = { moreExpanded = true }
                            )
                            DropdownMenu(
                                expanded = moreExpanded,
                                onDismissRequest = { moreExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Configuration") },
                                    onClick = {
                                        moreExpanded = false
                                        navController.navigate(Screen.Settings.route)
                                    },
                                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Settings, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Change password") },
                                    onClick = {
                                        moreExpanded = false
                                        openChangePasswordDialog = true
                                    },
                                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Lock, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Logout") },
                                    onClick = {
                                        moreExpanded = false
                                        fieldViewModel.clearSelectedFields()
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.Fields.route) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    leadingIcon = {
                                        androidx.compose.material3.Icon(
                                            Icons.AutoMirrored.Filled.Logout,
                                            null
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
                SharySearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search your fields…"
                )
            }
        },
        bottomBar = {
            Column {
                SharyCommandDock(
                    selectedCount = selectedFields.size,
                    onClearSelection = fieldViewModel::clearSelectedFields,
                    primaryAction = DockAction(
                        label = "Review & Send →",
                        enabled = selectedFields.isNotEmpty() && userViewModel.anyCachedUser()
                    ),
                    secondaryActions = emptyList(),
                    destructiveAction = DockAction(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        enabled = selectedFields.isNotEmpty()
                    ) {
                        if (selectedFields.isNotEmpty()) {
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
                    onPrimaryClick = {
                        if (userViewModel.anyCachedUser() && selectedFields.isNotEmpty()) {
                            navController.navigate(Screen.SummaryField.route)
                        } else {
                            snackbarMessage = "Select at least one field and one recipient"
                        }
                    }
                )
                SharySectionNavigationBar(
                    currentTab = SectionTab.FIELDS,
                    onTabSelected = { tab ->
                        when (tab) {
                            SectionTab.FIELDS -> Unit
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
            if (isLoading && visibleFields.isNotEmpty()) {
                Text(
                    text = "Decrypting remaining fields...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
            when {
                isLoading && visibleFields.isEmpty() -> {
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
                            FieldCard(
                                fieldKey = field.key,
                                fieldValue = FieldValueContract.parse(field.value).plainData,
                                tag = field.tag,
                                alias = field.keyAlias.takeIf { it.isNotBlank() }?.let { "alias: $it" },
                                isSelected = selectedFields.any {
                                    it.key.trim().equals(field.key.trim(), ignoreCase = true)
                                },
                                onClick = { fieldViewModel.toggleFieldSelection(field) },
                                onLongPressCopy = { value ->
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            ClipEntry(ClipData.newPlainText("field_value", value))
                                        )
                                    }
                                    clipboardAutoClearJob?.cancel()
                                    if (settings.clipboardAutoClearSeconds > 0) {
                                        clipboardAutoClearJob = scope.launch {
                                            delay(settings.clipboardAutoClearSeconds * 1000L)
                                            clipboard.setClipEntry(
                                                ClipEntry(ClipData.newPlainText("", ""))
                                            )
                                        }
                                    }
                                    snackbarMessage = "Value copied to clipboard"
                                },
                                copyLongPressMillis = settings.copyLongPressMillis.toLong(),
                                modifier = Modifier.padding(vertical = cardVerticalPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    if (openAddDialog) {
        AddFieldDialog(
            onDismiss = { openAddDialog = false },
            onAddField = { newField ->
                lastFlow = AddFlow.ADD
                lastSubmittedKey = newField.key
                fieldViewModel.addField(newField)
            }
        )
    }

    if (openAddFieldCopyDialog && editingField != null) {
        AddCopiedFieldDialog(
            targetField = editingField!!,
            onDismiss = { openAddFieldCopyDialog = false },
            onAddCopiedField = { newCopyDomain ->
                lastFlow = AddFlow.COPY
                lastSubmittedKey = newCopyDomain.key
                fieldViewModel.addField(newCopyDomain)
                openAddFieldCopyDialog = false
            }
        )
    }

    if (openUpdateFieldDialog && editingField != null) {
        UpdateFieldDialog(
            targetField = editingField!!,
            onDismiss = {
                editingField = null
                openUpdateFieldDialog = false
            },
            onUpdateField = { newField ->
                fieldViewModel.updateField(editingField!!, newField.copy(dateAdded = editingField!!.dateAdded))
                editingField = null
                openUpdateFieldDialog = false
            },
            canRecoverPreviousValue = recoverableKeys.contains(
                editingField!!.key.trim().lowercase()
            ),
            onRecoverPreviousValue = {
                fieldViewModel.recoverPreviousValue(editingField!!)
                editingField = null
                openUpdateFieldDialog = false
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
