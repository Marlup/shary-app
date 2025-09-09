package com.shary.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.ui.theme.OptionalThemes.autumnDarkColors
import com.shary.app.ui.theme.OptionalThemes.autumnLightColors
import com.shary.app.ui.theme.OptionalThemes.candyDarkColors
import com.shary.app.ui.theme.OptionalThemes.candyLightColors
import com.shary.app.ui.theme.OptionalThemes.cyberDarkColors
import com.shary.app.ui.theme.OptionalThemes.cyberLightColors
import com.shary.app.ui.theme.OptionalThemes.forestDarkColors
import com.shary.app.ui.theme.OptionalThemes.forestLightColors
import com.shary.app.ui.theme.OptionalThemes.greyDarkColors
import com.shary.app.ui.theme.OptionalThemes.greyLightColors
import com.shary.app.ui.theme.OptionalThemes.oceanDarkColors
import com.shary.app.ui.theme.OptionalThemes.oceanLightColors
import com.shary.app.ui.theme.OptionalThemes.pastelDarkColors
import com.shary.app.ui.theme.OptionalThemes.pastelLightColors
import com.shary.app.ui.theme.OptionalThemes.roughDarkColors
import com.shary.app.ui.theme.OptionalThemes.roughLightColors
import com.shary.app.ui.theme.OptionalThemes.sunsetDarkColors
import com.shary.app.ui.theme.OptionalThemes.sunsetLightColors
import com.shary.app.ui.theme.OptionalThemes.synthDarkColors
import com.shary.app.ui.theme.OptionalThemes.synthLightColors

@Composable
fun AppOptionalTheme(
    theme: AppTheme = AppTheme.Pastel,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    var selectedTheme by rememberSaveable { mutableStateOf(AppTheme.Grey) }

    val colors = when (theme) {
        AppTheme.Pastel -> if (darkTheme) pastelDarkColors else pastelLightColors
        AppTheme.Candy -> if (darkTheme) candyDarkColors else candyLightColors
        AppTheme.Autumn -> if (darkTheme) autumnDarkColors else autumnLightColors
        AppTheme.Forest -> if (darkTheme) forestDarkColors else forestLightColors
        AppTheme.Ocean -> if (darkTheme) oceanDarkColors else oceanLightColors
        AppTheme.Cyberpunk -> if (darkTheme) cyberDarkColors else cyberLightColors
        AppTheme.Synthwave -> if (darkTheme) synthDarkColors else synthLightColors
        AppTheme.Grey -> if (darkTheme) greyDarkColors else greyLightColors
        AppTheme.Sunset -> if (darkTheme) sunsetDarkColors else sunsetLightColors
        AppTheme.Rough -> if (darkTheme) roughDarkColors else roughLightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)