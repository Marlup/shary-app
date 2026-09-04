package com.shary.app.ui.screens.summary

import CloudEvent
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.shary.app.application.email.EmailShareState
import com.shary.app.ui.components.ReviewKeyValueRow
import com.shary.app.ui.components.ReviewSectionCard
import com.shary.app.ui.components.SendMethodCard
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.application.cloud.CloudDeliveryState
import com.shary.app.application.cloud.CloudDeliverySummary
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.ui.screens.home.utils.SendOption
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.utils.cloudUiStatus
import com.shary.app.utils.log.AppLogger
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.communication.EmailViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.request.RequestViewModel
import com.shary.app.ui.screens.utils.cloudErrorMessage
import com.shary.app.ui.utils.isOnWifiNetwork
import com.shary.app.viewmodels.user.UserViewModel
import kotlinx.coroutines.launch

@Composable
fun SummaryRequestScreen(
    navController: NavHostController,
    draftRequestId: String? = null
) {
    val context = LocalContext.current
    val emailViewModel: EmailViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    val requestWorkflow by requestViewModel.requestWorkflow.collectAsState()
    val outgoingDraft = requestWorkflow.outgoingRequestDraft
    val requestId = draftRequestId?.trim()?.takeIf(String::isNotBlank)
        ?: outgoingDraft.requestId
    val draftMatchesRoute = requestId.isNotBlank() && outgoingDraft.requestId == requestId
    val fields = remember(outgoingDraft, requestId) {
        if (draftMatchesRoute) requestViewModel.getDraftFields() else emptyList()
    }
    val availableUsers by userViewModel.users.collectAsState()
    val recipients = remember(outgoingDraft, availableUsers, requestId) {
        if (!draftMatchesRoute) {
            emptyList()
        } else {
            outgoingDraft.recipientAddresses.map { address ->
                availableUsers.firstOrNull {
                    it.email.trim().equals(address, ignoreCase = true)
                } ?: UserDomain(email = address)
            }
        }
    }
    var selectedMethod: SendOption by remember { mutableStateOf(SendOption.Cloud) }
    val emailShareState by emailViewModel.shareState.collectAsState()
    val emailBusy = emailShareState is EmailShareState.Preparing ||
        emailShareState is EmailShareState.Prepared ||
        emailShareState is EmailShareState.Launching
    val cloudDeliveryState by cloudViewModel.deliveryState.collectAsState()
    val cloudLoading by cloudViewModel.isLoading.collectAsState()
    val cloudSending = cloudDeliveryState is CloudDeliveryState.Sending
    val cloudBusy = cloudSending || cloudLoading
    val cloudReceipt = (cloudDeliveryState as? CloudDeliveryState.Receipt)?.receipt
    val cloudState by cloudViewModel.cloudState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val cloudStatus = cloudUiStatus(cloudState)
    val privacyCopy = reviewPrivacyCopy(
        method = selectedMethod,
        payloadKind = ReviewPayloadKind.REQUESTED_KEYS,
        recipientCount = recipients.size,
        sensitiveFieldCount = 0,
        cloudExpiryDays = settings.defaultCloudExpiryDays
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(emailShareState) {
        val preparedState = emailShareState as? EmailShareState.Prepared ?: return@LaunchedEffect
        val attachmentId = preparedState.lease.attachmentId
        val prepared = emailViewModel.claimPreparedShare(attachmentId) ?: return@LaunchedEffect
        try {
            context.startActivity(Intent.createChooser(prepared.intent, "Choose sharing app"))
            emailViewModel.onShareSheetOpened(attachmentId)
        } catch (error: Throwable) {
            emailViewModel.onChooserLaunchFailed(attachmentId, error)
        }
    }

    LaunchedEffect(requestId) {
        if (requestId.isNotBlank()) cloudViewModel.restoreRequestDelivery(requestId)
    }

    LaunchedEffect(Unit) {
        cloudViewModel.events.collect { event ->
            when (event) {
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
            SummaryRequestHero(
                title = "Review & Send",
                subtitle = "${fields.size} requested keys · ${recipients.size} recipients",
                onBack = { navController.popBackStack() }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SelectedRecipientsSection(recipients)

                ReviewSectionCard(sectionLabel = "Fields") {
                    if (fields.isEmpty()) {
                        Text("No requested keys", color = Violet500)
                    } else {
                        fields.forEachIndexed { index, field ->
                            ReviewKeyValueRow(
                                key = field.key,
                                value = field.value.ifBlank { field.keyAlias.ifBlank { "-" } },
                                last = index == fields.lastIndex
                            )
                        }
                    }
                }

                ReviewSectionCard(sectionLabel = "Send method") {
                    SendMethodCard(
                        icon = Icons.Default.Email,
                        title = "Email",
                        description = "Readable JSON attachment",
                        isSelected = selectedMethod == SendOption.Email,
                        onClick = { selectedMethod = SendOption.Email },
                        enabled = !cloudBusy && cloudReceipt == null && !emailBusy
                    )
                    Spacer(Modifier.height(8.dp))
                    SendMethodCard(
                        icon = Icons.Default.Cloud,
                        title = "Cloud",
                        description = "Encrypted on device · expires in ${settings.defaultCloudExpiryDays} days",
                        isSelected = selectedMethod == SendOption.Cloud,
                        onClick = { selectedMethod = SendOption.Cloud },
                        enabled = cloudStatus.ready && !cloudBusy && cloudReceipt == null && !emailBusy,
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = privacyCopy.disclosure,
                        style = MaterialTheme.typography.bodySmall,
                        color = Violet500
                    )
                }

                cloudReceipt?.let { CloudDeliveryReceiptSection(it) }
                if (selectedMethod == SendOption.Email) {
                    EmailShareReceiptSection(emailShareState)
                }

                SharyPrimaryButton(
                    text = when {
                        selectedMethod == SendOption.Email -> when (emailShareState) {
                            is EmailShareState.Preparing -> "Preparing attachment..."
                            is EmailShareState.Prepared,
                            is EmailShareState.Launching -> "Opening share sheet..."
                            is EmailShareState.ShareSheetOpened -> "Open another sharing app"
                            is EmailShareState.PreparationFailed,
                            is EmailShareState.LaunchFailed -> "Try sharing again"
                            EmailShareState.Idle -> privacyCopy.primaryAction
                        }
                        cloudSending -> "Sending..."
                        cloudLoading && cloudReceipt?.summary == CloudDeliverySummary.SENT ->
                            "Saving receipt..."
                        cloudReceipt?.summary == CloudDeliverySummary.SENT -> "Done"
                        cloudReceipt != null -> "Retry failed (${cloudReceipt.failedRecipients.size})"
                        else -> privacyCopy.primaryAction
                    },
                    onClick = {
                        when (selectedMethod) {
                            SendOption.Email -> {
                                emailViewModel.sendRequest(fields, recipients)
                            }
                            SendOption.Cloud -> {
                                if (cloudReceipt?.summary == CloudDeliverySummary.SENT) {
                                    requestViewModel.removeDraftFields()
                                    userViewModel.clearSelectedUsers()
                                    cloudViewModel.resetDeliveryState()
                                    navController.popBackStack()
                                    return@SharyPrimaryButton
                                }
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
                                AppLogger.info("SummaryRequestScreen", "event=send_request_to_cloud")
                                if (cloudReceipt == null) {
                                    cloudViewModel.uploadRequest(
                                        fields = fields,
                                        owner = owner,
                                        recipients = recipients,
                                        requestId = requestId,
                                        expiryDays = settings.defaultCloudExpiryDays
                                    )
                                } else {
                                    cloudViewModel.retryFailedRequest(
                                        fields = fields,
                                        owner = owner,
                                        recipients = recipients,
                                        requestId = requestId,
                                        expiryDays = settings.defaultCloudExpiryDays
                                    )
                                }
                            }
                        }
                    },
                    enabled = fields.isNotEmpty() && requestId.isNotBlank() &&
                        recipients.isNotEmpty() && !cloudBusy && !emailBusy &&
                        (selectedMethod != SendOption.Cloud || cloudStatus.ready),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                )

                Text(
                    text = privacyCopy.methodDescription,
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

@Composable
private fun SummaryRequestHero(
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
            Spacer(Modifier.width(8.dp))
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
