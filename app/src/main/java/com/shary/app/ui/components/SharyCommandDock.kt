package com.shary.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.DestructiveBg
import com.shary.app.ui.theme.DestructiveText
import com.shary.app.ui.theme.SharySpacing
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet600

enum class DockVisualStyle {
    FullWidth,
    IconOnly
}

@Composable
fun SharyCommandDock(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    primaryAction: DockAction,
    secondaryActions: List<DockAction>,
    destructiveAction: DockAction?,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualStyle: DockVisualStyle = DockVisualStyle.FullWidth
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLight)
    ) {
        HorizontalDivider(color = Violet200, thickness = 1.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SharySpacing.dockPaddingH,
                    end = SharySpacing.dockPaddingH,
                    top = 10.dp,
                    bottom = SharySpacing.dockPaddingBottom
                )
        ) {
            AnimatedContent(targetState = selectedCount > 0, label = "dock-actions") { hasSelection ->
                if (hasSelection) {
                    when (visualStyle) {
                        DockVisualStyle.FullWidth -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                destructiveAction?.iconOnly(
                                    containerColor = DestructiveBg,
                                    contentColor = DestructiveText
                                )
                                ClearSelectionButton(onClearSelection = onClearSelection)

                                secondaryActions.forEach { action ->
                                    action.iconOnly()
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    SharyPrimaryButton(
                                        text = primaryAction.label,
                                        icon = primaryAction.icon,
                                        onClick = onPrimaryClick,
                                        enabled = primaryAction.enabled,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        DockVisualStyle.IconOnly -> {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    destructiveAction?.iconOnly(
                                        containerColor = DestructiveBg,
                                        contentColor = DestructiveText
                                    )
                                    secondaryActions.forEach { action ->
                                        action.iconOnly()
                                    }
                                }
                                ClearSelectionButton(
                                    onClearSelection = onClearSelection,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                Row(
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    primaryAction.iconOnly(
                                        onClick = onPrimaryClick,
                                        containerColor = Violet600,
                                        contentColor = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockAction.iconOnly(
    onClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val resolvedIcon = icon ?: return
    SharyIconButton(
        icon = resolvedIcon,
        contentDescription = contentDescription.ifBlank { label.ifBlank { "Dock action" } },
        onClick = onClick ?: this.onClick,
        enabled = enabled,
        buttonSize = 52.dp,
        containerColor = containerColor,
        contentColor = contentColor
    )
}
