package com.shary.app.ui.screens.request

import android.util.Log
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel // deprecated location of hiltViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.reset
import com.shary.app.core.domain.types.enums.RequestListMode
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.request.utils.AddRequestDialog
import com.shary.app.ui.screens.request.utils.SendRequestDialog
import com.shary.app.ui.screens.utils.SpecialComponents.CompactActionButton
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
    //val emailViewModel: EmailViewModel = hiltViewModel()
    //val cloudViewModel: CloudViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }

    // ---------------- States from RequestViewModel ----------------
    val listMode by requestViewModel.listMode.collectAsState()
    val draftFields by requestViewModel.draftFields.collectAsState()
    val receivedRequests by requestViewModel.receivedRequests.collectAsState()
    val sentRequests by requestViewModel.sentRequests.collectAsState()
    val requestsToShow = if (listMode == RequestListMode.SENT) {
        sentRequests
    } else {
        receivedRequests
    }

    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>("") }


    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Collect RequestViewModel events
    LaunchedEffect(Unit) {
        requestViewModel.events.collect { event ->
            when (event) {
                is RequestEvent.FetchedFromCloud -> {
                    snackbarMessage = "Fetched and matched ${event.matchedCount} fields from request"
                }
                is RequestEvent.FetchError -> {
                    snackbarMessage = "Fetch error: ${event.throwable.message}"
                }
            }
        }
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d("RequestsScreen", "[1] Before updateDraftRequest()")
            when (event) {
                Lifecycle.Event.ON_START -> Unit
                Lifecycle.Event.ON_STOP -> {
                    if (listMode == RequestListMode.SENT) {
                        Log.d("RequestsScreen", "[2] Before updateDraftRequest()")
                        requestViewModel.setDraftFields()
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
            val actionButtonsEnabled = listMode == RequestListMode.SENT
            val actionButtonsAvailable = actionButtonsEnabled && draftFields.isNotEmpty()

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
                            if (actionButtonsAvailable) {
                                requestViewModel.clearDraftFields()
                                requestViewModel.removeDraftFields()
                                snackbarMessage = "Deleted ${draftFields.size} fields"
                            }
                        },
                        backgroundColor = colorScheme.error,
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete Fields",
                        enabled = actionButtonsAvailable
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
                        enabled = actionButtonsEnabled
                    )

                    CompactActionButton(
                        onClick = {
                            val currentUser = userViewModel.getOwner()
                            Log.d("RequestsScreen", "[1] currentUser: $currentUser")
                            if (currentUser.username.isNotEmpty()) {
                                Log.d("RequestsScreen", "[2] currentUser: $currentUser")
                                requestViewModel.fetchRequestsFromCloud(currentUser.username, currentUser)
                            } else {
                                snackbarMessage = "User not logged in"
                            }
                        },
                        icon = Icons.Default.CloudDownload,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "Fetch Requests from Cloud",
                        enabled = actionButtonsEnabled
                    )

                    CompactActionButton(
                        onClick = { navController.navigate(Screen.Users.route) },
                        icon = Icons.Default.Person,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "Send to Users",
                        enabled = actionButtonsEnabled
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
                            Log.d("RequestsScreen", "[5] anyDraftRequestCached: ${requestViewModel.anyDraftFieldCached()}")
                            Log.d("RequestsScreen", "[6] anyDraftRequestCached: ${requestViewModel.anyDraftFieldCached()}")
                            if (draftFields.isNotEmpty()
                                && userViewModel.anyCachedUser()) {
                                requestViewModel.setDraftFields()
                                navController.navigate(Screen.SummaryRequest.route)
                            }
                        },
                        icon = Icons.Default.AssignmentTurnedIn,
                        backgroundColor = colorScheme.tertiary,
                        contentDescription = "Summary: Request",
                        enabled = draftFields.isNotEmpty() && userViewModel.anyCachedUser()
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
                    selected = listMode == RequestListMode.RECEIVED,
                    onClick = { requestViewModel.setListMode(RequestListMode.RECEIVED) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Received")
                }
                SegmentedButton(
                    selected = listMode == RequestListMode.SENT,
                    onClick = { requestViewModel.setListMode(RequestListMode.SENT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Sent")
                }
            }

            // -----------------------------------------------------------
            // --------------- List of Received or Requested Fields ------
            // -----------------------------------------------------------
            /*
            Text(
                if (listMode == RequestListMode.SENT) "Sent Requests" else "Received Requests",
                modifier = Modifier.padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
             */

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Requested Keys",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)

            if (requestsToShow.isNotEmpty()) {
                Log.d("RequestsScreen", "requestsToShow: $requestsToShow")
                Log.d("Number of requests", "${requestsToShow.size}")
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = requestsToShow,
                        key = { _, request -> request.dateAdded } // stable key
                    ) { index, request ->
                        val isSelected =
                            request.fields.isNotEmpty() && request.fields.all { it in draftFields }
                        val actionButtonsEnabled = listMode == RequestListMode.SENT

                        // background colors
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
                            enabled = actionButtonsEnabled,
                            onClick = {
                                // If all fields in the request are already selected, unselect them all.
                                // Otherwise, select all of them.
                                if (actionButtonsEnabled) {
                                    if (isSelected) {
                                        request.fields.forEach {
                                            fieldViewModel.toggleFieldSelection(it) // Will unselect
                                        }
                                    } else {
                                        request.fields.forEach {
                                            if (it !in draftFields) {
                                                fieldViewModel.toggleFieldSelection(it) // Will select only those not yet selected
                                            }
                                        }
                                    }
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    request.fields.joinToString { it.key }.ifBlank { "No keys requested" },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (listMode == RequestListMode.SENT) {
                            "No sent requests available"
                        } else {
                            "No received requests available"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (listMode == RequestListMode.SENT) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxHeight(0.5f),
                    thickness = 1.dp,
                    color = Color.Gray
                )

                // ----------------------------------------------------------------
                // -------------------- List of Drafted Fields --------------------
                // ----------------------------------------------------------------

                Text(
                    "Drafted Fields",
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

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

                if (draftFields.isNotEmpty()) {
                    Log.d("RequestsScreen", "draftFields: $draftFields")
                    Log.d("Number of requests", "${draftFields.size}")
                    LazyColumn(
                        modifier = Modifier
                            //.weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = draftFields,
                            key = { _, field -> field.dateAdded } // stable key
                        ) { _, field ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = colorScheme.surface
                                ),
                                enabled = true,
                                onClick = {},
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
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No drafted fields available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxHeight(1.0f),
                    thickness = 1.dp,
                    color = Color.Gray
                )
            }
        }
    }

    // Add field (for the in-progress request)
    if (openAddDialog) {
        AddRequestDialog(
            onDismiss = { openAddDialog = false },
            onAddRequest = { key, keyAlias ->
                Log.d("[1] RequestsScreen", "onAddRequest: $key, $keyAlias")
                if (key.isNotBlank()) {
                    Log.d("[2] RequestsScreen", "onAddRequest: $key, $keyAlias")
                    requestViewModel.addDraftField(FieldDomain.initialize().copy(
                        key = key.trim(),
                        value = keyAlias.trim()
                    ))
                    openAddDialog = false
                    snackbarMessage = "Requested key '$key' added"
                } else {
                    snackbarMessage = "Requested key is required"
                }
            }
        )
    }
}
