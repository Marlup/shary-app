package com.shary.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SelectionCardBg
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceWhite
import com.shary.app.ui.theme.TagMintBg
import com.shary.app.ui.theme.TagMintText
import com.shary.app.ui.theme.TagPeachBg
import com.shary.app.ui.theme.TagPeachText
import com.shary.app.ui.theme.TagSkyBg
import com.shary.app.ui.theme.TagSkyText
import com.shary.app.ui.theme.TagVioletBg
import com.shary.app.ui.theme.TagVioletText
import com.shary.app.ui.theme.TextMuted
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet700
import com.shary.app.ui.theme.Violet900

enum class RequestStatusTone {
    RECEIVED,
    PENDING,
    SENT,
    PARTIAL,
    FAILED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestCard(
    senderName: String,
    timestamp: String,
    keys: List<String>,
    fieldCount: Int,
    statusLabel: String = "PENDING",
    statusTone: RequestStatusTone = RequestStatusTone.PENDING,
    isActive: Boolean,
    onCardTap: () -> Unit,
    onToggleSelection: () -> Unit,
    onMatchClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val borderColor = animateColorAsState(
        targetValue = if (isActive) Violet600 else Violet200,
        animationSpec = tween(180),
        label = "request-border"
    )
    val containerColor = animateColorAsState(
        targetValue = if (isActive) SelectionCardBg else SurfaceWhite,
        animationSpec = tween(180),
        label = "request-bg"
    )

    Card(
        onClick = onCardTap,
        modifier = modifier.fillMaxWidth(),
        shape = SharyRadius.card,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor.value),
        border = BorderStroke(1.5.dp, borderColor.value)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Violet900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$timestamp · $fieldCount field${if (fieldCount == 1) "" else "s"} requested",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                RequestStatusChip(label = statusLabel, tone = statusTone)
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                keys.forEach { key ->
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier
                            .background(com.shary.app.ui.theme.SurfaceMid, SharyRadius.chip)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            if (isActive && onMatchClick != null) {
                Text(
                    text = "Match to my fields →",
                    style = MaterialTheme.typography.labelMedium,
                    color = Violet700,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onMatchClick)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                RequestSelectionRing(
                    isSelected = isActive,
                    onToggleSelection = onToggleSelection
                )
            }
        }
    }
}

@Composable
private fun RequestSelectionRing(
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(
                value = isSelected,
                role = Role.Checkbox,
                onValueChange = { onToggleSelection() }
            )
            .semantics {
                toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
                stateDescription = if (isSelected) "Selected" else "Not selected"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (isSelected) Violet600 else Color.Transparent, CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) Violet600 else Violet200,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun RequestStatusChip(label: String, tone: RequestStatusTone) {
    val (bg, fg) = when (tone) {
        RequestStatusTone.RECEIVED -> TagSkyBg to TagSkyText
        RequestStatusTone.PENDING -> TagVioletBg to TagVioletText
        RequestStatusTone.SENT -> TagMintBg to TagMintText
        RequestStatusTone.PARTIAL -> TagPeachBg to TagPeachText
        RequestStatusTone.FAILED -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    }

    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .background(bg, SharyRadius.chip)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}
