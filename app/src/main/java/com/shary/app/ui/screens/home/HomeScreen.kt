package com.shary.app.ui.screens.home

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shary.app.core.Session
import com.shary.app.services.bluetooth.BluetoothService
import com.shary.app.services.cloud.CloudService
import com.shary.app.services.email.EmailService
import com.shary.app.services.messaging.TelegramService
import com.shary.app.services.messaging.WhatsAppService
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.home.utils.AppTopBar
import com.shary.app.ui.screens.home.utils.BluetoothDeviceSelectorDialog
import com.shary.app.ui.screens.home.utils.SendOption
import com.shary.app.ui.screens.home.utils.SendServiceDialog
import com.shary.app.ui.screens.home.utils.ShareFieldsGenericButton
import com.shary.app.utils.FormattingUtils.makeKeyValueTextFromFields

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    session: Session,
    emailService: EmailService,
    cloudService: CloudService,
    whatsAppService: WhatsAppService,
    telegramService: TelegramService,
    bluetoothService: BluetoothService
) {
    val context = LocalContext.current

    // +++++ Selection Summary +++++
    var sendOption by remember { mutableStateOf<SendOption?>(null) }
    var showSendDialog by remember { mutableStateOf(false) }

    // ----- Bluetooth dialog -----
    var openBluetoothDialog by remember { mutableStateOf(false) }

    fun handleSendOption(option: SendOption) {
        when (option) {
            SendOption.Email -> emailService.sendEmailViaClient(
                session.getSelectedFields(),
                session.getSelectedEmails()
            )
            SendOption.Cloud -> Toast.makeText(context, "Currently on Development", Toast.LENGTH_SHORT).show()
            SendOption.Whatsapp -> whatsAppService.sendFieldsToWhatsApp(
                session.getSelectedFields(),
                session.getSelectedPhoneNumber()
            )
            SendOption.Telegram -> telegramService.sendFieldsToTelegram(
                session.getSelectedFields(),
                session.getSelectedPhoneNumber()
            )
            SendOption.Bluetooth -> {
                showSendDialog = false
                openBluetoothDialog = true
                return
            }
        }
        showSendDialog = false
    }

    fun resetSelectedData() {
        session.resetSelectedData()
    }

    // Run at launch
    LaunchedEffect(Unit) {
        //resetSelectedData()
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

            if (showSendDialog) {
                SendServiceDialog(
                    options = SendOption.all,
                    onOptionSelected = { sendOption = it },
                    onSendConfirmed = {
                        showSendDialog = false
                        // Ejecuta aquí la lógica de envío
                        sendOption?.let { handleSendOption(it) }
                    },
                    onCancel = { showSendDialog = false }
                )
            }

            if (openBluetoothDialog) {
                BluetoothDeviceSelectorDialog(
                    bluetoothService = bluetoothService,
                    dataToSend = makeKeyValueTextFromFields(session.getSelectedFields()),
                    onDismiss = { openBluetoothDialog = false },
                    onFinished = {
                        openBluetoothDialog = false
                        resetSelectedData()
                    }
                )
            }

            var textSummaryWarning = ""
            if (session.isAnyFieldSelected() && session.isAnyEmailSelected()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row {
                        Column(
                        ) {
                            ShareFieldsGenericButton(
                                session.getSelectedFields(),
                                onClick = { showSendDialog = true },
                                Modifier.align(Alignment.CenterHorizontally)
                            )
                            // Super Header
                            Text(
                                text = "Sending Summary",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        /*
                        FloatingActionButton(
                            onClick = { showSendDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                //.align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Pre-send data",
                            )
                        }
                         */
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
                                items(session.getSelectedEmails()) { email ->
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
                                items(session.getSelectedFields()) { field ->
                                    Text(
                                        text = field.key,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 0.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (session.isAnyFieldSelected()) {
                textSummaryWarning = "Fields selected. Select at least one user to send the data"
            } else if (session.isAnyEmailSelected()) {
                textSummaryWarning = "Users selected. Select at least one field to send the data"
            } else {
                textSummaryWarning = "Select at least one field and one user to send the data"
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
