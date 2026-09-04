package com.shary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.core.domain.types.enums.safeTagString
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.Violet900

@Composable
fun TagChip(
    tag: Tag?,
    modifier: Modifier = Modifier
) {
    val bg = tag.safeColor()
    val fg = if (bg.luminance() < 0.45f) Color.White else Violet900

    Text(
        text = tag.safeTagString().uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .width(82.dp)
            .height(24.dp)
            .background(bg, SharyRadius.chip)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}
