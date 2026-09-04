package com.shary.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shary.app.ui.theme.DestructiveBg
import com.shary.app.ui.theme.DestructiveText
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import kotlin.math.roundToInt

enum class SharyModalComplexity {
    Compact,
    Complex
}

enum class SharyModalPresentation {
    FullScreen,
    BottomSheet,
    Dialog
}

enum class SharyModalNavigation {
    Close,
    Back
}

sealed interface SharyModalOperationState {
    data object Idle : SharyModalOperationState
    data object Validating : SharyModalOperationState
    data object Saving : SharyModalOperationState
    data object Saved : SharyModalOperationState
    data class Error(val message: String) : SharyModalOperationState
}

enum class SharyModalDismissal {
    Dismiss,
    ConfirmDiscard,
    Block
}

data class SharyModalAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

data class SharyModalDestructiveAction(
    val label: String,
    val confirmationTitle: String,
    val confirmationMessage: String,
    val onConfirm: () -> Unit,
    val enabled: Boolean = true
)

object SharyModalPolicy {
    private const val MediumWidthDp = 600
    private const val ExpandedWidthDp = 840

    fun presentation(
        windowWidthDp: Int,
        complexity: SharyModalComplexity
    ): SharyModalPresentation = when {
        complexity == SharyModalComplexity.Complex && windowWidthDp < ExpandedWidthDp ->
            SharyModalPresentation.FullScreen
        complexity == SharyModalComplexity.Compact && windowWidthDp < MediumWidthDp ->
            SharyModalPresentation.BottomSheet
        else -> SharyModalPresentation.Dialog
    }

    fun dismissal(
        operationState: SharyModalOperationState,
        isDirty: Boolean
    ): SharyModalDismissal = when {
        operationState == SharyModalOperationState.Validating ||
            operationState == SharyModalOperationState.Saving -> SharyModalDismissal.Block
        isDirty -> SharyModalDismissal.ConfirmDiscard
        else -> SharyModalDismissal.Dismiss
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharyModalScaffold(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    complexity: SharyModalComplexity = SharyModalComplexity.Compact,
    presentationOverride: SharyModalPresentation? = null,
    navigation: SharyModalNavigation = SharyModalNavigation.Close,
    operationState: SharyModalOperationState = SharyModalOperationState.Idle,
    isDirty: Boolean = false,
    bodyScrollable: Boolean = true,
    primaryAction: SharyModalAction? = null,
    secondaryAction: SharyModalAction? = null,
    destructiveAction: SharyModalDestructiveAction? = null,
    body: @Composable ColumnScope.() -> Unit
) {
    var confirmation by rememberSaveable { mutableStateOf<PendingConfirmation?>(null) }

    fun requestDismiss() {
        when (SharyModalPolicy.dismissal(operationState, isDirty)) {
            SharyModalDismissal.Dismiss -> onDismissRequest()
            SharyModalDismissal.ConfirmDiscard -> confirmation = PendingConfirmation.Discard
            SharyModalDismissal.Block -> Unit
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth.value.roundToInt()
        val presentation = presentationOverride
            ?: SharyModalPolicy.presentation(widthDp, complexity)

        val content: @Composable () -> Unit = {
            SharyModalContent(
                title = title,
                subtitle = subtitle,
                navigation = navigation,
                operationState = operationState,
                modifier = modifier,
                bodyScrollable = bodyScrollable,
                primaryAction = primaryAction,
                secondaryAction = secondaryAction,
                destructiveAction = destructiveAction,
                onNavigate = ::requestDismiss,
                onDestructiveAction = {
                    confirmation = PendingConfirmation.Destructive
                },
                body = body
            )
        }

        when (presentation) {
            SharyModalPresentation.FullScreen -> {
                Dialog(
                    onDismissRequest = ::requestDismiss,
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { paneTitle = title },
                        shape = RectangleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .safeDrawingPadding()
                                .imePadding()
                        ) {
                            content()
                        }
                    }
                }
            }

            SharyModalPresentation.BottomSheet -> {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = ::requestDismiss,
                    sheetState = sheetState,
                    shape = SharyRadius.sheet,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.semantics { paneTitle = title }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 720.dp)
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        content()
                    }
                }
            }

            SharyModalPresentation.Dialog -> {
                Dialog(
                    onDismissRequest = ::requestDismiss,
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.9f)
                                .widthIn(max = 720.dp)
                                .imePadding()
                                .semantics { paneTitle = title },
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }

    when (confirmation) {
        PendingConfirmation.Discard -> {
            AlertDialog(
                onDismissRequest = { confirmation = null },
                title = { Text("Discard changes?") },
                text = { Text("Your unsaved changes will be lost.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmation = null
                            onDismissRequest()
                        }
                    ) {
                        Text("Discard")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmation = null }) {
                        Text("Keep editing")
                    }
                }
            )
        }

        PendingConfirmation.Destructive -> {
            destructiveAction?.let { action ->
                AlertDialog(
                    onDismissRequest = { confirmation = null },
                    title = { Text(action.confirmationTitle) },
                    text = { Text(action.confirmationMessage) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmation = null
                                action.onConfirm()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Confirm ${action.label}")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmation = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        null -> Unit
    }
}

@Composable
private fun SharyModalContent(
    title: String,
    subtitle: String?,
    navigation: SharyModalNavigation,
    operationState: SharyModalOperationState,
    modifier: Modifier,
    bodyScrollable: Boolean,
    primaryAction: SharyModalAction?,
    secondaryAction: SharyModalAction?,
    destructiveAction: SharyModalDestructiveAction?,
    onNavigate: () -> Unit,
    onDestructiveAction: () -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = title }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigate,
                enabled = operationState != SharyModalOperationState.Validating &&
                    operationState != SharyModalOperationState.Saving
            ) {
                Icon(
                    imageVector = when (navigation) {
                        SharyModalNavigation.Close -> Icons.Default.Close
                        SharyModalNavigation.Back -> Icons.AutoMirrored.Filled.ArrowBack
                    },
                    contentDescription = when (navigation) {
                        SharyModalNavigation.Close -> "Close"
                        SharyModalNavigation.Back -> "Back"
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        OperationStatus(operationState)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        val bodyModifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .then(
                if (bodyScrollable) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)

        Column(
            modifier = bodyModifier,
            content = body
        )

        if (primaryAction != null || secondaryAction != null || destructiveAction != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SharyModalFooter(
                operationState = operationState,
                primaryAction = primaryAction,
                secondaryAction = secondaryAction,
                destructiveAction = destructiveAction,
                onDestructiveAction = onDestructiveAction
            )
        }
    }
}

@Composable
private fun OperationStatus(operationState: SharyModalOperationState) {
    when (operationState) {
        SharyModalOperationState.Idle -> Unit
        SharyModalOperationState.Validating -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Validating…",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        SharyModalOperationState.Saving -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Saving…",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        SharyModalOperationState.Saved -> {
            Text(
                text = "Saved",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        is SharyModalOperationState.Error -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = operationState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SharyModalFooter(
    operationState: SharyModalOperationState,
    primaryAction: SharyModalAction?,
    secondaryAction: SharyModalAction?,
    destructiveAction: SharyModalDestructiveAction?,
    onDestructiveAction: () -> Unit
) {
    val isBusy = operationState == SharyModalOperationState.Validating ||
        operationState == SharyModalOperationState.Saving
    val useVerticalActions = LocalDensity.current.fontScale >= 1.3f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        destructiveAction?.let { action ->
            FilledTonalButton(
                onClick = onDestructiveAction,
                enabled = action.enabled && !isBusy,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DestructiveBg,
                    contentColor = DestructiveText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(action.label, maxLines = 2)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (useVerticalActions) {
            secondaryAction?.let { action ->
                SharyModalSecondaryAction(action, isBusy, Modifier.fillMaxWidth())
            }
            primaryAction?.let { action ->
                SharyModalPrimaryAction(action, isBusy, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (secondaryAction == null && primaryAction == null) {
                    Spacer(Modifier.height(0.dp))
                }
                secondaryAction?.let { action ->
                    SharyModalSecondaryAction(action, isBusy, Modifier.weight(1f))
                }
                primaryAction?.let { action ->
                    SharyModalPrimaryAction(action, isBusy, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SharyModalPrimaryAction(
    action: SharyModalAction,
    isBusy: Boolean,
    modifier: Modifier
) {
    Button(
        onClick = action.onClick,
        enabled = action.enabled && !isBusy,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(action.label, maxLines = 2)
    }
}

@Composable
private fun SharyModalSecondaryAction(
    action: SharyModalAction,
    isBusy: Boolean,
    modifier: Modifier
) {
    FilledTonalButton(
        onClick = action.onClick,
        enabled = action.enabled && !isBusy,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(action.label, maxLines = 2)
    }
}

private enum class PendingConfirmation {
    Discard,
    Destructive
}
