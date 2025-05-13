package com.shary.app.ui.screens.fileVisualizer

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.shary.app.Field
import com.shary.app.core.Session
import com.shary.app.services.file.FileService
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.ui.screens.utils.MatchingDialog
import com.shary.app.viewmodels.ViewModelFactory
import com.shary.app.viewmodels.field.FieldViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileVisualizerScreen(
    navController: NavHostController,
    session: Session,
    fileService: FileService,
    fieldViewModelFactory: ViewModelFactory<FieldViewModel>,
) {

    val viewModel: FieldViewModel = viewModel(factory = fieldViewModelFactory)

    val context = LocalContext.current

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var isMatchingEnabled by remember { mutableStateOf(false) }
    var canMatchFields by remember { mutableStateOf(false) }
    var tableData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var dataFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var requestFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // ---- Add dialog ----
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    fun loadTableFromZip(file: File) {
        selectedFile = file
        val raw = fileService.getFieldsFromZip(file)
        tableData = raw["fields"] ?: emptyMap()
    }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}

                val copiedFile = fileService.copyZipToPrivateStorage(it)

                if (copiedFile != null && fileService.validateZipStructure(copiedFile)) {
                    Log.d("FileVisualizerScreen", "Picked file: ${copiedFile.name}")

                    when (fileService.getModeFromZip(copiedFile)) {
                        "data" -> {
                            if (!dataFiles.contains(copiedFile)) dataFiles = dataFiles + copiedFile
                            loadTableFromZip(copiedFile)
                            canMatchFields = false
                        }
                        "request" -> {
                            if (!requestFiles.contains(copiedFile)) requestFiles = requestFiles + copiedFile
                            loadTableFromZip(copiedFile)
                            canMatchFields = true
                        }
                    }
                } else {
                    Log.e("FileVisualizerScreen", "Invalid or failed to copy: $uri")
                }
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("File Visualizer") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                expandedHeight = 30.dp
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                GoBackButton(navController)

                FloatingActionButton(onClick = {
                    zipPickerLauncher.launch(arrayOf("application/zip"))
                    if (selectedFile != null)
                        Toast.makeText(context, "${selectedFile?.name} loaded", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Pick ZIP File")
                }

                FloatingActionButton(
                    containerColor = if (canMatchFields) MaterialTheme.colorScheme.primary else Color.Gray,
                    contentColor = if (canMatchFields) Color.White else Color.LightGray,
                    modifier = Modifier.alpha(if (canMatchFields) 1f else 0.6f),
                    onClick = { isMatchingEnabled = canMatchFields }
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Enable Matching")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)) {
                Text(
                    "Key",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (canMatchFields) "" else "Value",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tableData.entries.toList()) { (key, value) ->
                    Spacer(modifier = Modifier.width(8.dp))

                    if (canMatchFields) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)) {
                            Text(
                                key,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                value,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)) {
                            Text(key, Modifier.weight(1f))
                            Text(value, Modifier.weight(1f))
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                }
            }

            if (isMatchingEnabled) {
                MatchingDialog(
                    storedFields = viewModel.fields.collectAsState().value,
                    requestKeys = tableData.toList(),
                    onDismiss = {
                        isMatchingEnabled = false
                        //canMatchFields = false
                    },
                    onAccept = { selectedFields ->
                        session.cacheSelectedFields(selectedFields)
                        canMatchFields = true
                    },
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
                                val alterMessage = if (success) "added" else "already exists"
                                snackbarMessage = "Field '$key' $alterMessage"
                            }
                        } else {
                            snackbarMessage = "Key and value are required"
                        }
                    }
                )
            }
        }
    }
}
