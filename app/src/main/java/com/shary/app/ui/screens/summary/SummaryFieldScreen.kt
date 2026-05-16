package com.shary.app.ui.screens.summary

import CloudEvent
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.shary.app.ui.components.RecipientBlob
import com.shary.app.ui.components.ReviewKeyValueRow
import com.shary.app.ui.components.ReviewSectionCard
import com.shary.app.ui.components.SendMethodCard
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.ui.screens.home.utils.SendOption
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.utils.cloudUiStatus
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.communication.EmailViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.request.RequestViewModel
import com.shary.app.viewmodels.user.UserViewModel
import com.shary.app.ui.screens.utils.cloudErrorMessage
import com.shary.app.ui.utils.isOnWifiNetwork
import com.shary.app.utils.log.AppLogger
import kotlinx.coroutines.launch

@Composable
fun SummaryFieldScreen(navController: NavHostController) {
    val context = LocalContext.current
    val emailViewModel: EmailViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    val fields = remember { fieldViewModel.getCachedFields() }
    val recipients = remember { userViewModel.getCachedUsers() }
    val recipient = recipients.firstOrNull()
    var selectedMethod: SendOption by remember { mutableStateOf(SendOption.Cloud) }
    val cloudLoading by cloudViewModel.isLoading.collectAsState()
    val cloudState by cloudViewModel.cloudState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val cloudStatus = cloudUiStatus(cloudState)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        emailViewModel.intent.collect { intent ->
            context.startActivity(Intent.createChooser(intent, "Choose Email App"))
        }
    }

    LaunchedEffect(Unit) {
        cloudViewModel.events.collect { event ->
            when (event) {
                is CloudEvent.DataUploaded -> {
                    requestViewModel.markActiveReceivedRequestAsResponded()
                    snackbarHostState.showSnackbar(formatCloudAck(event.result))
                }
                is CloudEvent.Error -> {
                    snackbarHostState.showSnackbar(cloudErrorMessage(event.throwable))
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        emailViewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Violet50)
    ) {
        Column {
            SummaryHero(
                title = "Review & Send",
                subtitle = "${fields.size} fields · ${recipients.size} recipient(s) · encrypted",
                onBack = { navController.popBackStack() }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ReviewSectionCard(sectionLabel = "Recipient") {
                    if (recipient != null) {
                        RecipientBlob(name = recipient.username, email = recipient.email)
                    } else {
                        Text("No recipient selected", color = Violet500)
                    }
                }

                ReviewSectionCard(sectionLabel = "Fields") {
                    if (fields.isEmpty()) {
                        Text("No fields selected", color = Violet500)
                    } else {
                        fields.forEachIndexed { index, field ->
                            ReviewKeyValueRow(
                                key = field.key,
                                value = com.shary.app.core.domain.types.valueobjects.FieldValueContract.parse(field.value).plainData,
                                last = index == fields.lastIndex
                            )
                        }
                    }
                }

                ReviewSectionCard(sectionLabel = "Send method") {
                    SendMethodCard(
                        icon = Icons.Default.Email,
                        title = "Email",
                        description = recipient?.email?.let { "to $it" } ?: "No recipient",
                        isSelected = selectedMethod == SendOption.Email,
                        onClick = { selectedMethod = SendOption.Email }
                    )
                    Spacer(Modifier.height(8.dp))
                    SendMethodCard(
                        icon = Icons.Default.Cloud,
                        title = "Cloud",
                        description = "Secure upload to cloud relay",
                        isSelected = selectedMethod == SendOption.Cloud,
                        onClick = { selectedMethod = SendOption.Cloud },
                        enabled = cloudStatus.ready,
                        onDisabledClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cloud disabled: ${cloudStatus.reason}")
                            }
                        }
                    )
                    if (!cloudStatus.ready) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Cloud disabled: ${cloudStatus.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Violet500
                        )
                    }
                }

                SharyPrimaryButton(
                    text = if (cloudLoading && selectedMethod == SendOption.Cloud) {
                        "Sending..."
                    } else {
                        "Send securely"
                    },
                    onClick = {
                        when (selectedMethod) {
                            SendOption.Email -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Preparing email composer...")
                                }
                                emailViewModel.sendResponse(fields, recipients)
                            }
                            SendOption.Cloud -> {
                                if (settings.wifiOnlyCloudSync && !isOnWifiNetwork(context)) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Wi-Fi only sync is enabled. Connect to Wi-Fi to continue.")
                                    }
                                    return@SharyPrimaryButton
                                }
                                val owner = userViewModel.getOwner()
                                if (owner.email.isBlank()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Owner email unavailable. Sign in again.")
                                    }
                                    return@SharyPrimaryButton
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar("Sending data securely, please wait...")
                                }
                                AppLogger.info("SummaryFieldScreen", "event=send_to_cloud")
                                cloudViewModel.uploadData(
                                    fields = fields,
                                    owner = owner,
                                    recipients = recipients,
                                    expiryDays = settings.defaultCloudExpiryDays
                                )
                            }
                        }
                    },
                    enabled = fields.isNotEmpty() && recipients.isNotEmpty() && !cloudLoading &&
                        (selectedMethod != SendOption.Cloud || cloudStatus.ready),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                )

                Text(
                    text = "End-to-end encrypted · data never stored",
                    style = MaterialTheme.typography.bodySmall,
                    color = Violet500,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

private fun formatCloudAck(result: Map<String, StatusDataSentDb>): String {
    if (result.isEmpty()) return "No backend acknowledgement received."

    val successCount = result.count { (_, status) ->
        status == StatusDataSentDb.SUCCESS || status == StatusDataSentDb.STORED
    }
    val failedCount = result.size - successCount

    return when {
        successCount == result.size -> "Sent successfully to $successCount recipient(s)."
        successCount == 0 -> "Send failed for all recipients."
        else -> "Sent to $successCount recipient(s), $failedCount failed."
    }
}

@Composable
private fun SummaryHero(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Violet600)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .minimumInteractiveComponentSize()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}
