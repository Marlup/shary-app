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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SelectionCardBg
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SharySpacing
import com.shary.app.ui.theme.SurfaceWhite
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet900
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserCard(
    name: String,
    email: String,
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
        label = "user-border"
    )
    val containerColor = animateColorAsState(
        targetValue = if (isSelected) SelectionCardBg else SurfaceWhite,
        animationSpec = tween(180),
        label = "user-bg"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(email, copyLongPressMillis) {
                detectTapGestures(
                    onPress = {
                        coroutineScope {
                            val start = SystemClock.elapsedRealtime()
                            var copyTriggered = false
                            val copyJob = launch {
                                delay(copyLongPressMillis)
                                copyTriggered = true
                                onLongPressCopy(email)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SharySpacing.cardPaddingH,
                    vertical = SharySpacing.cardPaddingV
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(SharyRadius.avatar)
                    .background(Violet600),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials(name),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Violet900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Violet500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .pointerInput(isSelected) {
                        detectTapGestures(onTap = { onToggleSelection() })
                    }
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
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}
