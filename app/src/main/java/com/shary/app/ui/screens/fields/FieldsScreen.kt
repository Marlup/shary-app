package com.shary.app.ui.screens.fields

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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.shary.app.Field
import com.shary.app.core.Session
import com.shary.app.services.field.FieldService
import com.shary.app.ui.screens.fields.utils.AddFieldDialog
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.ui.screens.utils.ItemRow
import com.shary.app.ui.screens.utils.RowSearcher
import com.shary.app.ui.screens.utils.SelectableRow
import com.shary.app.utils.DateUtils
import com.shary.app.viewmodels.ViewModelFactory
import com.shary.app.viewmodels.field.FieldViewModel
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen(
    navController: NavHostController,
    session: Session,
    fieldViewModelFactory: ViewModelFactory<FieldViewModel>,
    fieldService: FieldService
) {

    // ---- Create the ViewModel ----
    val viewModel: FieldViewModel = viewModel(factory = fieldViewModelFactory)

    // ---- Table and DB rows ----
    val fieldList by viewModel.fields.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Add dialog ----
    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // ---- Editing/Updating field ----
    var editingField by remember { mutableStateOf<Field?>(null) }
    var editedValue by remember { mutableStateOf("") }
    var editedAlias by remember { mutableStateOf("") }

    // ---- Checked rows ----
    //val selectedKeys = remember { mutableStateListOf<String>() }
    val selectedKeys by viewModel.selectedKeys.collectAsState()

    // ---- Search Fields ----
    var showSearcher by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var searchByKey by remember { mutableStateOf(true) }


    val filteredFields = fieldList.filter { field ->
        if (searchByKey)
            field.key.contains(searchText, ignoreCase = true)
        else
            field.keyAlias.contains(searchText, ignoreCase = true)
    }
        .toMutableList()

    LaunchedEffect(snackbarMessage, editingField) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
        editedValue = editingField?.value ?: ""
        editedAlias = editingField?.keyAlias ?: ""
    }

    fun clearStates() {
        editingField = null
        editedValue = ""
        editedAlias = ""
        searchText = ""
        filteredFields.clear()
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val currentFilteredFields = fieldList.filter {
                    if (searchByKey) it.key.contains(searchText, ignoreCase = true)
                    else it.keyAlias.contains(searchText, ignoreCase = true)
                }.filter { it.key in selectedKeys }

                session.cacheSelectedFields(currentFilteredFields)
                // Drop remembered screen states
                clearStates()
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
                title = { Text("Fields") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                expandedHeight = 30.dp
            )
                 },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
            ) {

                // Go back button
                GoBackButton(navController)

                // Add row button
                FloatingActionButton(onClick = { openAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Field")
                }

                // Delete row button
                val isEnabled = selectedKeys.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (isEnabled) {
                            // Copy of selectedUsers
                            selectedKeys.toList().forEach { key ->
                                viewModel.viewModelScope.launch {
                                    viewModel.deleteField(key)
                                }
                                viewModel.clearSelectedKeys()
                            }
                            snackbarMessage = "Deleted fields"
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
                .padding(8.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
            horizontalAlignment =  Alignment.Start, //Alignment.CenterHorizontally
        ) {
            // Right: Search input
            RowSearcher(
                searchText,
                onSearchTextChange = { searchText = it },
                searchByFirstColumn = searchByKey,
                onSearchByChange = { searchByKey = it },
                Pair("key", "alias")
            )

            if (filteredFields.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredFields) { index, field ->
                        SelectableRow(
                            item = field,
                            index = index,
                            isSelected = selectedKeys.contains(field.key),
                            onCheckedChange = { checked ->
                                viewModel.toggleFieldSelection(field.key, checked)
                            },
                        ) { _ ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Column with key + value
                                ItemRow(
                                    item = fieldService.fieldToTriple(field)
                                ) { item ->
                                    editingField = fieldService.valuesToField(
                                        item.first,
                                        item.second,
                                        item.third
                                    )
                                    editedValue = field.value
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No fields available", style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
        }
    }

    if (openAddDialog) {
        AddFieldDialog(
            onDismiss = { openAddDialog = false },
            onAddField = { key, keyAlias, value ->
                if (key.isNotBlank() && value.isNotBlank()) {
                    val field = Field
                        .newBuilder()
                        .setKey(key)
                        .setKeyAlias(keyAlias)
                        .setValue(value)
                        .setDateAdded(Instant.now().toEpochMilli())
                        .build()

                    viewModel.viewModelScope.launch {
                        val success = viewModel.saveField(field).await()
                        // Close dialog if successful field added
                        openAddDialog = !success
                        val alterMessage = if (success) "added" else "already exists"
                        snackbarMessage = "Field '$key' $alterMessage"
                    }
                } else {
                    snackbarMessage = "Key and value are required"
                }
            }
        )
    }

    editingField?.let { field ->
        AlertDialog(
            onDismissRequest = { editingField = null },
            title = { Text("Update ${field.key}") },
            text = {
                Column {

                    val formattedDate = DateUtils.formatTimeMillis(field.dateAdded)
                    Text("Added at: $formattedDate")

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        label = { Text("New Value") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedAlias,
                        onValueChange = { editedAlias = it },
                        label = { Text("Alias") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.viewModelScope.launch {
                        if (field.value != editedValue)
                            viewModel.updateValue(field.key, editedValue)
                        if (field.keyAlias != editedAlias)
                            viewModel.updateAlias(field.key, editedAlias)
                    }
                    editingField = null
                    snackbarMessage = "Field '${field.key}' updated"
                }) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = { editingField = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}