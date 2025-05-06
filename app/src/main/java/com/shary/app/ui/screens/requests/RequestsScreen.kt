package com.shary.app.ui.screens.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.Field
import com.shary.app.services.requestField.RequestFieldService
import com.shary.app.ui.screens.ui_utils.GoBackButton
import com.shary.app.ui.screens.ui_utils.SelectableRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(navController: NavHostController, requestFieldService: RequestFieldService) {

    val snackbarHostState = remember { SnackbarHostState() }
    val requestFields = remember { mutableStateListOf<Field>() }

    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var openSendDialog by remember { mutableStateOf(false) }

    val selectedKeys = remember { mutableStateListOf<String>() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                println("Saving selected emails on stop: $requestFields")
                requestFieldService.cacheRequestFields(requestFields)
            }
        }

        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Requests") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        floatingActionButton = {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {

                // Go back button
                GoBackButton(navController)

                // Add row button
                FloatingActionButton(onClick = { openAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Request")
                }

                // Delete row button
                val isEnabled = selectedKeys.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (isEnabled) {
                            requestFields.removeIf { field ->
                                selectedKeys.contains(field.key)
                            }
                            selectedKeys.clear()
                        }
                    },
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    contentColor = if (isEnabled) Color.White else Color.LightGray,
                    modifier = Modifier.alpha(if (isEnabled) 1f else 0.6f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
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

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Spacer(modifier = Modifier.width(24.dp))
                Text("Key", Modifier.weight(1f))
                Text("Key Alias", Modifier.weight(1f))
            }

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)

            if (requestFields.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(requestFields) { index, field ->
                        SelectableRow(
                            item = field,
                            index = index,
                            isSelected = selectedKeys.contains(field.key),
                            onCheckedChange = { checked ->
                                if (checked) selectedKeys.add(field.key) else
                                    selectedKeys.remove(field.key)
                                              },
                        ) { fieldItem ->
                            Text(fieldItem.key, Modifier.weight(1f))
                            Text(fieldItem.keyAlias, Modifier.weight(1f))
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
                    Text("No requests available", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
            //Spacer(modifier = Modifier.height(32.dp))
        }

        if (openAddDialog) {
            AddRequestDialog(
                onDismiss = { openAddDialog = false },
                onAddRequest = { key, keyAlias ->
                    if (key.isNotBlank()) {
                        val field = Field.newBuilder()
                            .setKey(key)
                            .setKeyAlias(keyAlias)
                            .build()
                        requestFields.add(field)

                        openAddDialog = false
                        snackbarMessage = "Requested key '$key' added"
                    } else {
                        snackbarMessage = "Requested key is required"
                    }
                }
            )
        }

        if (openSendDialog) {
            SendRequestDialog(
                onDismiss = { openSendDialog = false },
                onSend = {
                    openSendDialog = false
                    TODO("Implement email sending")
                }
            )
        }
    }
}

