package com.shary.app.ui.screens.request

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel // deprecated location of hiltViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.reset
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.request.utils.AddRequestDialog
import com.shary.app.ui.screens.request.utils.SendRequestDialog
import com.shary.app.ui.screens.utils.SpecialComponents.CompactActionButton
import com.shary.app.ui.screens.utils.FieldMatchingDialog
import com.shary.app.ui.screens.utils.LoadingOverlay
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.communication.EmailViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.request.RequestViewModel
import com.shary.app.viewmodels.user.UserViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(navController: NavHostController) {

    // ---------------- ViewModels ----------------
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val emailViewModel: EmailViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }

    // Local working list of requested fields (Domain)
    val listMode by requestViewModel.listMode.collectAsState()
    val receivedRequests by requestViewModel.receivedRequests.collectAsState()
    val sentRequests by requestViewModel.sentRequests.collectAsState()
    val draftFields by requestViewModel.draftFields.collectAsState()
    val isLoading by requestViewModel.isLoading.collectAsState()
    val requestFields = if (listMode == RequestViewModel.RequestListMode.SENT) sentRequests else receivedRequests

    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var openSendDialog by remember { mutableStateOf(false) }
    var openMatchingDialog by remember { mutableStateOf(false) }
    var activeRequest by remember { mutableStateOf<RequestDomain?>(null) }
    val selectedDraftFields = remember { mutableStateListOf<FieldDomain>() }

    // ---- Checked rows (Domain) ----
    val selectedFields by fieldViewModel.selectedFields.collectAsState()
    val storedFields by fieldViewModel.fields.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // Collect RequestViewModel events
    LaunchedEffect(Unit) {
        requestViewModel.events.collect { event ->
            when (event) {
                is com.shary.app.core.domain.interfaces.events.RequestEvent.FetchedFromCloud -> {
                    snackbarMessage = "Fetched request with ${event.matchedCount} fields"
                }
                is com.shary.app.core.domain.interfaces.events.RequestEvent.FetchError -> {
                    snackbarMessage = "Fetch error: ${event.throwable.message}"
                }
            }
        }
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> Unit
                Lifecycle.Event.ON_STOP -> {
                    // Persist a Request built from the current local list
                    if (listMode == RequestViewModel.RequestListMode.RECEIVED && selectedFields.isNotEmpty()) {
                        fieldViewModel.setSelectedFields()
                    }
                }
                else -> {}
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // ---------------- UI ----------------
    LoadingOverlay(isLoading = isLoading) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Requests") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.primaryContainer,
                        titleContentColor = colorScheme.primary
                    ),
                    expandedHeight = 64.dp
                )
            },
            floatingActionButton = {
                val isReceiveMode = listMode == RequestViewModel.RequestListMode.RECEIVED
                val isSendMode = listMode == RequestViewModel.RequestListMode.SENT
                val selectedSendAvailable = selectedDraftFields.isNotEmpty()
                val selectedReceiveAvailable = selectedFields.isNotEmpty()
                val requestReadyToSend = draftFields.isNotEmpty() && userViewModel.anyCachedUser()

                HorizontalDivider(thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ---- Left: Delete ----
                    Box(
                        modifier = Modifier
                            .weight(0.15f),
                        contentAlignment = Alignment.Center
                    ) {
                        CompactActionButton(
                            onClick = {
                                if (isReceiveMode && selectedReceiveAvailable) {
                                    fieldViewModel.deleteFields(selectedFields)
                                    fieldViewModel.clearSelectedFields()
                                    snackbarMessage = "Deleted ${selectedFields.size} fields"
                                }
                                if (isSendMode && selectedSendAvailable) {
                                    val removedCount = selectedDraftFields.size
                                    requestViewModel.removeDraftFields(selectedDraftFields.toList())
                                    selectedDraftFields.clear()
                                    snackbarMessage = "Removed $removedCount fields"
                                }
                            },
                            backgroundColor = colorScheme.error,
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete Fields",
                            enabled = (isReceiveMode && selectedReceiveAvailable) || (isSendMode && selectedSendAvailable)
                        )
                    }

                    // ---- Center: Add + Download + Users ----
                    Row(
                        modifier = Modifier.weight(0.70f),
                        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactActionButton(
                            onClick = { openAddDialog = true },
                            icon = Icons.Default.Add,
                            backgroundColor = colorScheme.primary,
                            contentDescription = "Add Field",
                            enabled = isSendMode
                        )

                        CompactActionButton(
                            onClick = {
                                val currentUser = userViewModel.getOwner()
                                if (currentUser.username.isNotEmpty()) {
                                    requestViewModel.fetchRequestsFromCloud(currentUser.username, currentUser)
                                } else {
                                    snackbarMessage = "User not logged in"
                                }
                            },
                            icon = Icons.Default.CloudDownload,
                            backgroundColor = colorScheme.primary,
                            contentDescription = "Fetch Requests from Cloud",
                            enabled = isReceiveMode
                        )

                        CompactActionButton(
                            onClick = { navController.navigate(Screen.Users.route) },
                            icon = Icons.Default.Person,
                            backgroundColor = colorScheme.primary,
                            contentDescription = "Send to Users",
                            enabled = isReceiveMode || isSendMode
                        )
                    }

                    // ---- Right: Summary ----
                    Box(
                        modifier = Modifier
                            .weight(0.15f),
                        contentAlignment = Alignment.Center
                    ) {
                        CompactActionButton(
                            onClick = {
                                if (isReceiveMode && selectedReceiveAvailable && userViewModel.anyCachedUser()) {
                                    fieldViewModel.setSelectedFields()
                                    navController.navigate(Screen.Summary.route)
                                }
                                if (isSendMode && requestReadyToSend) {
                                    openSendDialog = true
                                }
                            },
                            icon = Icons.Default.AssignmentTurnedIn,
                            backgroundColor = colorScheme.tertiary,
                            contentDescription = "Summary",
                            enabled = (isReceiveMode && selectedReceiveAvailable && userViewModel.anyCachedUser()) || requestReadyToSend
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.90f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    SegmentedButton(
                        selected = listMode == RequestViewModel.RequestListMode.RECEIVED,
                        onClick = { requestViewModel.setListMode(RequestViewModel.RequestListMode.RECEIVED) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Receive")
                    }
                    SegmentedButton(
                        selected = listMode == RequestViewModel.RequestListMode.SENT,
                        onClick = { requestViewModel.setListMode(RequestViewModel.RequestListMode.SENT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Send")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        "Key",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Key Alias",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = Color.Gray)

                when {
                    listMode == RequestViewModel.RequestListMode.SENT && draftFields.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = draftFields,
                                key = { _, field -> "${field.key}-${field.dateAdded}" }
                            ) { index, field ->
                                val isSelected = selectedDraftFields.contains(field)
                                val backgroundColor = when {
                                    isSelected -> colorScheme.secondaryContainer
                                    index % 2 == 0 -> colorScheme.surface
                                    else -> colorScheme.surfaceVariant
                                }

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .alpha(if (isSelected) 1f else 0.9f),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = backgroundColor
                                    ),
                                    onClick = {
                                        if (isSelected) {
                                            selectedDraftFields.remove(field)
                                        } else {
                                            selectedDraftFields.add(field)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            field.key,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            field.keyAlias.orEmpty(),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    listMode == RequestViewModel.RequestListMode.RECEIVED && requestFields.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = requestFields,
                                key = { _, request -> request.dateAdded } // stable key
                            ) { index, request ->
                                val backgroundColor = when {
                                    index % 2 == 0 -> colorScheme.surface
                                    else -> colorScheme.surfaceVariant
                                }

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = backgroundColor
                                    ),
                                    onClick = {
                                        activeRequest = request
                                        openMatchingDialog = true
                                    },
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            "Request from ${request.sender.username}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        request.fields.forEach { field ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    field.key,
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    field.keyAlias.orEmpty(),
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    listMode == RequestViewModel.RequestListMode.SENT -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No fields added yet", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No requests available", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = Color.Gray)
            }

            // Add field (for the in-progress request)
            if (openAddDialog) {
                AddRequestDialog(
                    onDismiss = { openAddDialog = false },
                    onAddRequest = { key, keyAlias ->
                        if (key.isNotBlank()) {
                            val field = FieldDomain(
                                key = key.trim(),
                                keyAlias = keyAlias.trim(),
                                value = "", // requests only need the key; keep value empty
                                tag = Tag.Unknown,
                                dateAdded = Instant.now()
                            )
                            requestViewModel.addDraftField(field)
                            openAddDialog = false
                            snackbarMessage = "Requested key '$key' added"
                        } else {
                            snackbarMessage = "Requested key is required"
                        }
                    }
                )
            }

            if (openMatchingDialog && activeRequest != null) {
                FieldMatchingDialog(
                    storedFields = storedFields,
                    requestKeys = activeRequest!!.fields.map { it.key to it.keyAlias.orEmpty() },
                    onDismiss = {
                        openMatchingDialog = false
                        activeRequest = null
                    },
                    onAccept = { matchedFields ->
                        fieldViewModel.setSelectedFields(matchedFields)
                        snackbarMessage = "Matched ${matchedFields.size} fields"
                        openMatchingDialog = false
                        activeRequest = null
                    },
                    onAddField = { newField ->
                        fieldViewModel.addField(newField)
                    }
                )
            }

            if (openSendDialog) {
                SendRequestDialog(
                    onDismiss = { openSendDialog = false },
                    onSend = {
                        openSendDialog = false
                        cloudViewModel.uploadData(
                            draftFields,
                            userViewModel.getOwner(),
                            userViewModel.getCachedUsers(),
                            true
                        )
                        requestViewModel.clearDraftFields()
                        selectedDraftFields.clear()
                        snackbarMessage = "Request sent"
                    }
                )
            }
        }
    }
}
