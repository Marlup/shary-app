package com.shary.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SharySpacing
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet200

@Composable
fun SharyCommandDock(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    primaryAction: DockAction,
    secondaryActions: List<DockAction>,
    destructiveAction: DockAction?,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier
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
            AnimatedContent(targetState = selectedCount > 0, label = "dock-actions") {
                if (selectedCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        destructiveAction?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                SharyDestructiveButton(
                                    text = it.label,
                                    icon = it.icon,
                                    onClick = it.onClick,
                                    enabled = it.enabled,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        secondaryActions.forEach { action ->
                            Box(modifier = Modifier.weight(1f)) {
                                SharySoftButton(
                                    text = action.label,
                                    icon = action.icon,
                                    onClick = action.onClick,
                                    enabled = action.enabled,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            SelectionPill(
                                selectedCount = selectedCount,
                                onClearSelection = onClearSelection,
                                modifier = Modifier.fillMaxWidth()
                            )
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
            }
        }
    }
}
