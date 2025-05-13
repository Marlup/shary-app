package com.shary.app.ui.screens.home

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shary.app.core.Session
import com.shary.app.services.cloud.CloudService
import com.shary.app.services.email.EmailService
import com.shary.app.services.messaging.TelegramService
import com.shary.app.services.messaging.WhatsAppService
import com.shary.app.ui.screens.fields.utils.SendFieldsDialog
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.home.utils.AppTopBar
import com.shary.app.ui.screens.home.utils.ShareFieldsGenericButton

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    session: Session,
    emailService: EmailService,
    cloudService: CloudService,
    whatsAppService: WhatsAppService,
    telegramService: TelegramService
) {
    val context = LocalContext.current

    // +++++ Selection Summary +++++
    var emailsEmpty by remember { mutableStateOf(true) }
    var fieldsEmpty by remember { mutableStateOf(true) }

    var openSendDialog by remember { mutableStateOf(false) }
    //var sendOption by remember { mutableStateOf("Cloud") }

    // Run at launch
    LaunchedEffect(Unit) {
        emailsEmpty = session.selectedEmails.value.isEmpty()
        fieldsEmpty = session.selectedFields.value.isEmpty()
    }

    Scaffold(
        topBar = { AppTopBar(navController) },
        modifier = Modifier
            .background(color = Color(0xFFF7F3FF)) // Hardcoded
    ) { padding ->
        Column {

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    //.fillMaxWidth()
                    .fillMaxHeight(0.30f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Button(
                        onClick = { navController.navigate(Screen.Fields.route) },
                        modifier = Modifier.size(50.dp, 50.dp)
                    ) {
                        Text("Fields")
                    }
                }
                item {
                    Button(
                        onClick = { navController.navigate(Screen.Users.route) },
                        modifier = Modifier.size(50.dp, 50.dp)
                    ) {
                        Text("Users")
                    }
                }
                item {
                    Button(
                        onClick = { navController.navigate(Screen.Requests.route) },
                        modifier = Modifier.size(50.dp, 50.dp)
                    ) {
                        Text("Requests")
                    }
                }
                item {
                    Button(
                        onClick = { navController.navigate(Screen.FileVisualizer.route) },
                        modifier = Modifier.size(50.dp, 50.dp)
                    ) {
                        Text("File Visualizer")
                    }
                }
            }

            if (openSendDialog) {
                var sendOption = ""
                SendFieldsDialog(
                    selectedOption = sendOption,
                    onDismiss = { openSendDialog = false },
                    onOptionSelected = { sendOption = it },
                    onSend = {
                        when (sendOption) {
                            "Email" -> emailService.sendEmailViaClient(
                                session.selectedFields.value,
                                session.selectedEmails.value
                            )
                            "Cloud" -> {
                                //TODO("Implement cloud sending")
                                Toast.makeText(context, "Currently on Development", Toast.LENGTH_SHORT).show()
                            }
                            "Whatsapp" -> whatsAppService.sendFieldsToWhatsApp(
                                session.selectedFields.value,
                                session.selectedPhoneNumber.value
                                )
                            "Telegram" -> telegramService.sendFieldsToTelegram(
                                session.selectedFields.value,
                                session.selectedPhoneNumber.value
                            )
                        }
                        openSendDialog = false
                    },
                    onLeave = {
                        println("Reseting session selectedEmails and selectedFields")
                        session.selectedEmails.value = emptyList()
                        session.selectedFields.value = emptyList()
                    }
                )
            }

            var textSummaryWarning = ""
            if (!emailsEmpty && !fieldsEmpty) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row {
                        // Super Header
                        Text(
                            text = "Selection Summary",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                    }

                    HorizontalDivider(Modifier.padding(horizontal = 8.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Column: Emails
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Emails",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            HorizontalDivider(Modifier
                                //.padding(horizontal = 16.dp)
                                .fillMaxWidth(0.50f)
                            )

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(session.selectedEmails.value.toList()) { email ->
                                    Text(
                                        text = email,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 0.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Column: Keys
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Keys",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            HorizontalDivider(Modifier
                                //.padding(horizontal = 16.dp)
                                .fillMaxWidth(0.50f)
                            )

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(session.selectedFields.value.toList()) { field ->
                                    Text(
                                        text = field.key,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 0.dp)
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { openSendDialog = true },
                            modifier = Modifier
                                .size(45.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Fields")
                        }
                    }
                }
            } else if (emailsEmpty && fieldsEmpty) {
                textSummaryWarning = "Select at least one field and one user to send the data"
            } else if (emailsEmpty) {
                textSummaryWarning = "Fields selected. Select at least one user to send the data"
                ShareFieldsGenericButton(session.selectedFields.value, Modifier)
            } else {
                textSummaryWarning = "Users selected. Select at least one field to send the data"
            }

            // Warning message for summary
            Column {
                Text(
                    text = textSummaryWarning,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
