package com.shary.app.ui.screens.summary

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.home.utils.SendOption
import com.shary.app.ui.screens.home.utils.SendCommunicationDialog
import com.shary.app.ui.screens.home.utils.SendFieldsGenericButton
import com.shary.app.ui.screens.summary.utils.SummaryTopAppBar
import com.shary.app.ui.screens.utils.GoToScreen
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.communication.EmailViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.user.UserViewModel

@Composable
fun SummaryScreen(navController: NavHostController) {
    val context = LocalContext.current

    // ---------------- ViewModels ----------------
    val emailViewModel: EmailViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    var sendOption by remember { mutableStateOf<SendOption?>(null) }
    var openSendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        emailViewModel.intent.collect { intent ->
            context.startActivity(
                Intent.createChooser(intent, "Choose Email App")
            )
        }
    }

    Scaffold(
        topBar = { SummaryTopAppBar(navController) },
        modifier = Modifier.background(color = Color(0xFFF7F3FF)),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Go back to Fields button
                GoToScreen(
                    navController,
                    Screen.Fields,
                    Icons.Filled.TextFields
                )

                // Go back to Users button
                GoToScreen(
                    navController,
                    Screen.Users,
                    Icons.Filled.Person
                )

                SendFieldsGenericButton(
                    fieldViewModel.getCachedFields(),
                    onClick = { openSendDialog = true }
                )
            }
        }
    ) { padding ->

        if (openSendDialog) {
            SendCommunicationDialog(
                options = SendOption.all,
                onOptionSelected = { sendOption = it },
                onSend = {
                    openSendDialog = false
                    when (sendOption) {

                        SendOption.Email -> {
                            emailViewModel.sendResponse(
                                fieldViewModel.getCachedFields(),
                                userViewModel.getCachedUsers()
                            )
                        }

                        SendOption.Cloud -> {
                            cloudViewModel.uploadData(
                                fieldViewModel.getCachedFields(),
                                userViewModel.getOwnerEmail(),
                                userViewModel.getCachedUsers(),
                                false
                            )
                        }

                        null -> TODO()
                    }
                },
                onDismiss = { openSendDialog = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "You Are Going to Send:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ==== Table Header ====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emails",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "Fields",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
            }

            HorizontalDivider()

            // ==== Table Rows ====
            val fields = fieldViewModel.getCachedFields()
            val emails = userViewModel.getCachedUsers().map { it.email }
            val maxRows = maxOf(emails.size, fields.size)

            LazyColumn {
                items(maxRows) { index ->
                    val email = emails.getOrNull(index) ?: ""
                    val field = fields.getOrNull(index)?.key ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = email,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = field,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}
