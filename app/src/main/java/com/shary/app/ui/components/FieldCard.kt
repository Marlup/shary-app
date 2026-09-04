package com.shary.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.ui.theme.SelectionCardBg
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SharySpacing
import com.shary.app.ui.theme.SharyTextStyles
import com.shary.app.ui.theme.SurfaceWhite
import com.shary.app.ui.theme.TextMuted
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet700
import com.shary.app.ui.theme.Violet800
import com.shary.app.ui.theme.Violet900
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FieldCard(
    fieldKey: String,
    fieldValue: String,
    tag: Tag?,
    typedValueLabel: String? = null,
    alias: String? = null,
    isSelected: Boolean,
    onCardTap: () -> Unit,
    onToggleSelection: () -> Unit,
    onLongPressCopy: (String) -> Unit = {},
    copyLongPressMillis: Long = 600L,
    modifier: Modifier = Modifier
) {
    val borderColor = animateColorAsState(
        targetValue = if (isSelected) Violet600 else Violet200,
        animationSpec = tween(180),
        label = "field-border"
    )
    val containerColor = animateColorAsState(
        targetValue = if (isSelected) SelectionCardBg else SurfaceWhite,
        animationSpec = tween(180),
        label = "field-bg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(fieldKey, fieldValue, copyLongPressMillis) {
                detectTapGestures(
                    onPress = {
                        coroutineScope {
                            val start = SystemClock.elapsedRealtime()
                            var copyTriggered = false
                            val copyJob = launch {
                                delay(copyLongPressMillis)
                                copyTriggered = true
                                onLongPressCopy(fieldValue)
                            }
                            val released = tryAwaitRelease()
                            val heldFor = SystemClock.elapsedRealtime() - start
                            copyJob.cancel()
                            if (released && !copyTriggered && heldFor < copyLongPressMillis) {
                                onCardTap()
                            }
                        }
                    }
                )
            },
        shape = SharyRadius.card,
        colors = CardDefaults.cardColors(containerColor = containerColor.value),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.5.dp, borderColor.value)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = SharySpacing.cardPaddingH,
                vertical = SharySpacing.cardPaddingV
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = fieldKey,
                    style = MaterialTheme.typography.titleLarge,
                    color = Violet900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(8.dp))
                TagChip(tag = tag)
                /*
                if (!typedValueLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.size(6.dp))
                    TypedValueChip(label = typedValueLabel)
                }
                 */
                Spacer(Modifier.size(8.dp))
                SelectionRing(
                    isSelected = isSelected,
                    onToggleSelection = onToggleSelection
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fieldValue,
                    style = SharyTextStyles.fieldValueMedium.copy(color = Violet800),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .background(com.shary.app.ui.theme.SurfaceMid, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
                /*
                Spacer(Modifier.size(8.dp))
                SelectionRing(
                    isSelected = isSelected,
                    onToggleSelection = onToggleSelection
                )
                 */
            }

            if (!alias.isNullOrBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    thickness = 1.dp,
                    color = Violet200
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Violet800, fontWeight = FontWeight.Medium)) {
                            append("alias")
                        }
                        append(" · ")
                        append(alias)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SelectionRing(
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
        contentAlignment = Alignment.TopCenter
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
private fun TypedValueChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Violet700,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .width(64.dp)
            .height(24.dp)
            .background(Violet200, SharyRadius.chip)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
