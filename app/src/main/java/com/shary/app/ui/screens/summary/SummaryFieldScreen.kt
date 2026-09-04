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
import androidx.compose.runtime.DisposableEffect
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.application.email.EmailShareState
import com.shary.app.ui.components.ReviewKeyValueRow
import com.shary.app.ui.components.ReviewSectionCard
import com.shary.app.ui.components.SendMethodCard
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.application.cloud.CloudDeliveryState
import com.shary.app.application.cloud.CloudDeliverySummary
import com.shary.app.ui.screens.home.utils.SendOption
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.utils.cloudUiStatus
import com.shary.app.core.domain.interfaces.events.RequestEvent
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.communication.EmailViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.request.RequestViewModel
import com.shary.app.viewmodels.user.UserViewModel
import com.shary.app.ui.screens.utils.cloudErrorMessage
import com.shary.app.ui.utils.isOnWifiNetwork
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.FieldValueTypes
import com.shary.app.utils.BiometricAuthManager
import com.shary.app.utils.log.AppLogger
import kotlinx.coroutines.launch

@Composable
fun SummaryFieldScreen(
    navController: NavHostController,
    responseRequestId: String? = null
) {
    val context = LocalContext.current
    val emailViewModel: EmailViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    val cachedFields = remember { fieldViewModel.getCachedFields() }
    val vaultFields by fieldViewModel.fields.collectAsState()
    val cachedRecipients = remember { userViewModel.getCachedUsers() }
    val requestWorkflow by requestViewModel.requestWorkflow.collectAsState()
    val workflowRestoreComplete by requestViewModel.isWorkflowRestoreComplete.collectAsState()
    val reviewState = remember(
        cachedFields,
        vaultFields,
        cachedRecipients,
        requestWorkflow,
        responseRequestId
    ) {
        resolveSummaryFieldReviewState(
            cachedFields = if (responseRequestId == null) cachedFields else vaultFields,
            generalRecipients = cachedRecipients,
            workflow = requestWorkflow,
            responseRequestId = responseRequestId
        )
    }
    val fields = reviewState.fields
    val recipients = reviewState.recipients
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensitiveFieldCount = remember(fields) {
        fields.count { FieldValueTypes.isSensitive(it.value) }
    }
    val hasSensitiveFields = sensitiveFieldCount > 0
    val privacyCopy = reviewPrivacyCopy(
        method = selectedMethod,
        payloadKind = ReviewPayloadKind.FIELD_VALUES,
        recipientCount = recipients.size,
        sensitiveFieldCount = sensitiveFieldCount,
        cloudExpiryDays = settings.defaultCloudExpiryDays
    )
    var sensitiveAuthorized by remember(fields) {
        mutableStateOf(!hasSensitiveFields)
    }
    var responseStatusUpdateFailed by remember(responseRequestId) { mutableStateOf(false) }

    fun requestSensitiveAccess() {
        if (!hasSensitiveFields) {
            sensitiveAuthorized = true
            return
        }
        val activity = context as? FragmentActivity
        if (activity == null) {
            scope.launch {
                snackbarHostState.showSnackbar("Device authentication is unavailable on this screen")
            }
            return
        }
        val error = BiometricAuthManager(context, activity) {
            sensitiveAuthorized = true
        }.authenticate()
        if (error != null) {
            scope.launch { snackbarHostState.showSnackbar(error) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && hasSensitiveFields) {
                sensitiveAuthorized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasSensitiveFields) {
        if (hasSensitiveFields && !sensitiveAuthorized) requestSensitiveAccess()
    }

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

    LaunchedEffect(responseRequestId) {
        responseRequestId?.let(cloudViewModel::restoreResponseDelivery)
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

    LaunchedEffect(cloudReceipt, responseRequestId, reviewState.responseCompleted) {
        if (
            cloudReceipt?.summary == CloudDeliverySummary.SENT &&
            responseRequestId != null &&
            !reviewState.responseCompleted
        ) {
            responseStatusUpdateFailed = false
            requestViewModel.markActiveReceivedRequestAsResponded(responseRequestId)
        }
    }

    LaunchedEffect(Unit) {
        emailViewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        requestViewModel.events.collect { event ->
            if (event is RequestEvent.ResponseStatusUpdateFailed) {
                responseStatusUpdateFailed = true
                snackbarHostState.showSnackbar(
                    "Data was sent, but request history could not be updated: " +
                        (event.throwable.message ?: "unknown error")
                )
            }
        }
    }

    LaunchedEffect(reviewState.responseStateMissing, workflowRestoreComplete) {
        if (reviewState.responseStateMissing && workflowRestoreComplete) {
            snackbarHostState.showSnackbar(
                "This response draft is no longer available. Return to Requests and select it again."
            )
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
                subtitle = "${fields.size} fields · ${recipients.size} recipients",
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
                        Text("No fields selected", color = Violet500)
                    } else {
                        fields.forEachIndexed { index, field ->
                            ReviewKeyValueRow(
                                key = field.key,
                                value = if (FieldValueTypes.isSensitive(field.value)) {
                                    FieldValueTypes.displayData(
                                        storedValue = field.value,
                                        revealSensitive = sensitiveAuthorized
                                    )
                                } else {
                                    FieldValueContract.parse(field.value).plainData
                                },
                                last = index == fields.lastIndex
                            )
                        }
                        if (hasSensitiveFields && !sensitiveAuthorized) {
                            Spacer(Modifier.height(8.dp))
                            SharyPrimaryButton(
                                text = "Unlock sensitive fields",
                                onClick = ::requestSensitiveAccess,
                                modifier = Modifier.fillMaxWidth()
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
                    privacyCopy.sensitiveWarning?.let { warning ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                cloudReceipt?.let { CloudDeliveryReceiptSection(it) }
                if (selectedMethod == SendOption.Email) {
                    EmailShareReceiptSection(emailShareState)
                }

                SharyPrimaryButton(
                    text = when {
                        reviewState.responseCompleted &&
                            cloudReceipt?.summary != CloudDeliverySummary.SENT -> "Response sent"
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
                        cloudReceipt?.summary == CloudDeliverySummary.SENT &&
                            responseRequestId != null &&
                            !reviewState.responseCompleted &&
                            responseStatusUpdateFailed -> "Retry request status update"
                        cloudReceipt?.summary == CloudDeliverySummary.SENT &&
                            responseRequestId != null &&
                            !reviewState.responseCompleted -> "Updating request..."
                        cloudReceipt?.summary == CloudDeliverySummary.SENT -> "Done"
                        cloudReceipt != null -> "Retry failed (${cloudReceipt.failedRecipients.size})"
                        else -> privacyCopy.primaryAction
                    },
                    onClick = {
                        if (hasSensitiveFields && !sensitiveAuthorized) {
                            requestSensitiveAccess()
                            return@SharyPrimaryButton
                        }
                        when (selectedMethod) {
                            SendOption.Email -> {
                                emailViewModel.sendResponse(fields, recipients)
                            }
                            SendOption.Cloud -> {
                                if (cloudReceipt?.summary == CloudDeliverySummary.SENT) {
                                    if (responseRequestId != null && !reviewState.responseCompleted) {
                                        responseStatusUpdateFailed = false
                                        requestViewModel.markActiveReceivedRequestAsResponded(responseRequestId)
                                        return@SharyPrimaryButton
                                    }
                                    if (responseRequestId == null) {
                                        fieldViewModel.clearSelectedFields()
                                        userViewModel.clearSelectedUsers()
                                    } else {
                                        requestViewModel.clearActiveReceivedRequest()
                                    }
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
                                AppLogger.info("SummaryFieldScreen", "event=send_to_cloud")
                                if (cloudReceipt == null) {
                                    cloudViewModel.uploadData(
                                        fields = fields,
                                        owner = owner,
                                        recipients = recipients,
                                        expiryDays = settings.defaultCloudExpiryDays,
                                        responseRequestId = responseRequestId
                                    )
                                } else {
                                    cloudViewModel.retryFailedData(
                                        fields = fields,
                                        owner = owner,
                                        recipients = recipients,
                                        expiryDays = settings.defaultCloudExpiryDays,
                                        responseRequestId = responseRequestId
                                    )
                                }
                            }
                        }
                    },
                    enabled = fields.isNotEmpty() && recipients.isNotEmpty() && !cloudBusy && !emailBusy &&
                        (
                            !reviewState.responseCompleted ||
                                cloudReceipt?.summary == CloudDeliverySummary.SENT
                            ) &&
                        (!hasSensitiveFields || sensitiveAuthorized) &&
                        (selectedMethod != SendOption.Cloud || cloudStatus.ready) &&
                        !(
                            selectedMethod == SendOption.Cloud &&
                                cloudReceipt?.summary == CloudDeliverySummary.SENT &&
                                responseRequestId != null &&
                                !reviewState.responseCompleted &&
                                !responseStatusUpdateFailed
                            ),
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
