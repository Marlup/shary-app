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
import com.shary.app.core.dependencyContainer.DependencyContainer
import com.shary.app.services.field.FieldService
import com.shary.app.ui.screens.ui_utils.FilterBox
import com.shary.app.ui.screens.ui_utils.GoBackButton
import com.shary.app.ui.screens.ui_utils.SelectableRow
import com.shary.app.utils.DateUtils
import com.shary.app.viewmodels.ViewModelFactory
import com.shary.app.viewmodels.field.FieldViewModel
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen(navController: NavHostController, fieldService: FieldService) {

    // Create the ViewModel
    val viewModel: FieldViewModel = viewModel(
        factory = ViewModelFactory {
            FieldViewModel(
                DependencyContainer.get("field_repository")
            )
        }
    )

    // +++++ Table and DB rows +++++
    val fieldList by viewModel.fields.collectAsState()

    // +++++ Host +++++
    val snackbarHostState = remember { SnackbarHostState() }

    // +++++ Add dialog +++++
    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // +++++ Checked rows +++++
    val selectedKeys = remember { mutableStateListOf<String>() }
    val selectedFields = remember { mutableStateListOf<Field>() }

    // +++++ Search Fields +++++
    var searchText by remember { mutableStateOf("") }
    var searchByKey by remember { mutableStateOf(true) } // true = key, false = keyAlias
    val filteredFields: List<Field> = fieldList.filter { field ->
        if (searchByKey) {
            field.key.contains(searchText, ignoreCase = true)
        } else {
            field.keyAlias.contains(searchText, ignoreCase = true)
        }
    }

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
                val currentFilteredFields = fieldList.filter {
                    if (searchByKey) {
                        it.key.contains(searchText, ignoreCase = true)
                    } else {
                        it.keyAlias.contains(searchText, ignoreCase = true)
                    }
                }.filter { it.key in selectedKeys }

                println("Saving selected keys on stop: $selectedKeys")
                println("Saving selected fields on stop: $currentFilteredFields")
                fieldService.cacheSelectedFields(currentFilteredFields)
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
                    Icon(Icons.Default.Add, contentDescription = "Add Field")
                }

                // Delete row button
                val isEnabled = selectedKeys.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (isEnabled) {
                            // Copy of selectedKeys
                            selectedKeys.toList().forEach { key ->
                                viewModel.viewModelScope.launch {
                                    val success = viewModel.deleteField(key)
                                }
                                selectedKeys.clear()
                            }
                            snackbarMessage = "Deleted selected fields"
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

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically)
            {
                // 🔍 Search and Toggle Filter
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text(if (searchByKey) "Search by Key" else "Search by Key Alias") },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    singleLine = true
                )

                FilterBox(
                    title = "Key",
                    isSelected = searchByKey,
                    onClick = { searchByKey = true },
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilterBox(
                    title = "Alias",
                    isSelected = !searchByKey,
                    onClick = { searchByKey = false }
                )
            }
            Spacer(modifier = Modifier.width(64.dp))

            // 📋 Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {

                Text("Key", Modifier.weight(1f))
                Text("Value", Modifier.weight(1f))
                Text("Key Alias", Modifier.weight(1f))
                Text("Added Date", Modifier.weight(1f))

            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)

            // Filter fields dynamically
            //val filteredFields = fieldList.filter { it.key.contains(searchText, ignoreCase = true) }
            if (filteredFields.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //itemsIndexed(fieldList) { index, field ->
                    itemsIndexed(filteredFields) { index, field ->
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
                            Text(fieldItem.value, Modifier.weight(1f))
                            Text(fieldItem.keyAlias, Modifier.weight(1f))
                            val formattedDate = DateUtils.formatTimeMillis(fieldItem.dateAdded )
                            Text(formattedDate, Modifier.weight(1f))
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
                    Text("No fields available", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
            //Spacer(modifier = Modifier.height(32.dp))
        }

        if (openAddDialog) {
            AddFieldDialog(
                onDismiss = { openAddDialog = false },
                onAddField = { key, value, keyAlias ->
                    if (key.isNotBlank() && value.isNotBlank()) {
                        val field = Field.newBuilder()
                            .setKey(key)
                            .setValue(value)
                            .setKeyAlias(keyAlias)
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
                        snackbarMessage = "Both Key and Value are required"
                    }
                }
            )
        }
    }
}
