package com.shary.app.ui.screens.fileVisualizer

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shary.app.core.Session
import com.shary.app.services.file.FileService
import com.shary.app.ui.screens.ui_utils.GoBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileVisualizerScreen(
    navController: NavHostController,
    session: Session,
    fileService: FileService
) {
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf("") }
    var jsonFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var tableData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val files = fileService.getJsonFiles()
        if (files != jsonFiles) {
            jsonFiles = files
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("File Visualizer") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {
                GoBackButton(navController)

                FloatingActionButton(onClick = { dropdownExpanded = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Open Dropdown")
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

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                jsonFiles.forEach { file ->
                    DropdownMenuItem(
                        text = { Text(file) },
                        onClick = {
                            dropdownExpanded = false
                            if (file == selectedFile) {
                                Toast.makeText(context, "Same file selected. No changes made.", Toast.LENGTH_SHORT).show()
                            } else {
                                try {
                                    selectedFile = file
                                    Log.d("FileVisualizerScreer", selectedFile)
                                    val raw = fileService.loadFileOfFields(file)
                                    tableData = raw["fields"] ?: emptyMap()
                                } catch (e: Exception) {
                                    Log.e("FileVisualizer", "Error loading JSON", e)
                                    Toast.makeText(context, "Error loading file.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Key", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Value", Modifier.weight(1f), fontWeight = FontWeight.Bold)
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
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(key, Modifier.weight(1f))
                        Text(value, Modifier.weight(1f))
                    }
                    HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                }
            }
        }
    }
}
