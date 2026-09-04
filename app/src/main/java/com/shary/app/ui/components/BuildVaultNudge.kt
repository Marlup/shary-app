package com.shary.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.TextMuted
import com.shary.app.ui.theme.Violet300
import com.shary.app.ui.theme.Violet800

@Composable
fun BuildVaultNudge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                    )
                )
                drawRoundRect(
                    color = Violet300,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = stroke
                )
            }
            .padding(horizontal = 18.dp, vertical = 22.dp)
            .testTag("build-vault-nudge"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Build out your vault",
            style = MaterialTheme.typography.titleLarge,
            color = Violet800
        )
        Text(
            text = "Add a few more fields — like an ID number or emergency contact — to start sharing selectively.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
