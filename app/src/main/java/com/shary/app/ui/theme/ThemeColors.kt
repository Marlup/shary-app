package com.shary.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.types.enums.AppTheme

/**
 * Additional colors for visual elements and text that extend the Material3 ColorScheme
 */
data class ExtendedColors(
    // Visual Elements
    val accent: Color,
    val border: Color,
    val container: Color,
    val divider: Color,
    val highlight: Color,

    // Text Colors
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textAccent: Color,
    val textLink: Color
)

/**
 * Get extended colors for the current theme and dark mode
 */
@Composable
fun getExtendedColors(
    theme: AppTheme,
    darkTheme: Boolean = isSystemInDarkTheme()
): ExtendedColors {
    return when (theme) {
        AppTheme.Pastel -> if (darkTheme) pastelDarkExtendedColors else pastelLightExtendedColors
        AppTheme.Candy -> if (darkTheme) candyDarkExtendedColors else candyLightExtendedColors
        AppTheme.Autumn -> if (darkTheme) autumnDarkExtendedColors else autumnLightExtendedColors
        AppTheme.Forest -> if (darkTheme) forestDarkExtendedColors else forestLightExtendedColors
        AppTheme.Ocean -> if (darkTheme) oceanDarkExtendedColors else oceanLightExtendedColors
        AppTheme.Cyberpunk -> if (darkTheme) cyberDarkExtendedColors else cyberLightExtendedColors
        AppTheme.Synthwave -> if (darkTheme) synthDarkExtendedColors else synthLightExtendedColors
        AppTheme.Grey -> if (darkTheme) greyDarkExtendedColors else greyLightExtendedColors
        AppTheme.Sunset -> if (darkTheme) sunsetDarkExtendedColors else sunsetLightExtendedColors
        AppTheme.Rough -> if (darkTheme) roughDarkExtendedColors else roughLightExtendedColors
    }
}

// ============================================================
// Extended Colors for each theme
// ============================================================

// ------------ 1. Pastel Breeze ------------
private val pastelLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.pastel_light_accent,
    border = OptionalThemes.pastel_light_border,
    container = OptionalThemes.pastel_light_container,
    divider = OptionalThemes.pastel_light_divider,
    highlight = OptionalThemes.pastel_light_highlight,
    textPrimary = OptionalThemes.pastel_light_textPrimary,
    textSecondary = OptionalThemes.pastel_light_textSecondary,
    textTertiary = OptionalThemes.pastel_light_textTertiary,
    textAccent = OptionalThemes.pastel_light_textAccent,
    textLink = OptionalThemes.pastel_light_textLink
)

private val pastelDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.pastel_dark_accent,
    border = OptionalThemes.pastel_dark_border,
    container = OptionalThemes.pastel_dark_container,
    divider = OptionalThemes.pastel_dark_divider,
    highlight = OptionalThemes.pastel_dark_highlight,
    textPrimary = OptionalThemes.pastel_dark_textPrimary,
    textSecondary = OptionalThemes.pastel_dark_textSecondary,
    textTertiary = OptionalThemes.pastel_dark_textTertiary,
    textAccent = OptionalThemes.pastel_dark_textAccent,
    textLink = OptionalThemes.pastel_dark_textLink
)

// ------------ 2. Sweet Candy ------------
private val candyLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.candy_light_accent,
    border = OptionalThemes.candy_light_border,
    container = OptionalThemes.candy_light_container,
    divider = OptionalThemes.candy_light_divider,
    highlight = OptionalThemes.candy_light_highlight,
    textPrimary = OptionalThemes.candy_light_textPrimary,
    textSecondary = OptionalThemes.candy_light_textSecondary,
    textTertiary = OptionalThemes.candy_light_textTertiary,
    textAccent = OptionalThemes.candy_light_textAccent,
    textLink = OptionalThemes.candy_light_textLink
)

private val candyDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.candy_dark_accent,
    border = OptionalThemes.candy_dark_border,
    container = OptionalThemes.candy_dark_container,
    divider = OptionalThemes.candy_dark_divider,
    highlight = OptionalThemes.candy_dark_highlight,
    textPrimary = OptionalThemes.candy_dark_textPrimary,
    textSecondary = OptionalThemes.candy_dark_textSecondary,
    textTertiary = OptionalThemes.candy_dark_textTertiary,
    textAccent = OptionalThemes.candy_dark_textAccent,
    textLink = OptionalThemes.candy_dark_textLink
)

// ------------ 3. Cozy Autumn ------------
private val autumnLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.autumn_light_accent,
    border = OptionalThemes.autumn_light_border,
    container = OptionalThemes.autumn_light_container,
    divider = OptionalThemes.autumn_light_divider,
    highlight = OptionalThemes.autumn_light_highlight,
    textPrimary = OptionalThemes.autumn_light_textPrimary,
    textSecondary = OptionalThemes.autumn_light_textSecondary,
    textTertiary = OptionalThemes.autumn_light_textTertiary,
    textAccent = OptionalThemes.autumn_light_textAccent,
    textLink = OptionalThemes.autumn_light_textLink
)

private val autumnDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.autumn_dark_accent,
    border = OptionalThemes.autumn_dark_border,
    container = OptionalThemes.autumn_dark_container,
    divider = OptionalThemes.autumn_dark_divider,
    highlight = OptionalThemes.autumn_dark_highlight,
    textPrimary = OptionalThemes.autumn_dark_textPrimary,
    textSecondary = OptionalThemes.autumn_dark_textSecondary,
    textTertiary = OptionalThemes.autumn_dark_textTertiary,
    textAccent = OptionalThemes.autumn_dark_textAccent,
    textLink = OptionalThemes.autumn_dark_textLink
)

// ------------ 4. Nature Forest ------------
private val forestLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.forest_light_accent,
    border = OptionalThemes.forest_light_border,
    container = OptionalThemes.forest_light_container,
    divider = OptionalThemes.forest_light_divider,
    highlight = OptionalThemes.forest_light_highlight,
    textPrimary = OptionalThemes.forest_light_textPrimary,
    textSecondary = OptionalThemes.forest_light_textSecondary,
    textTertiary = OptionalThemes.forest_light_textTertiary,
    textAccent = OptionalThemes.forest_light_textAccent,
    textLink = OptionalThemes.forest_light_textLink
)

private val forestDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.forest_dark_accent,
    border = OptionalThemes.forest_dark_border,
    container = OptionalThemes.forest_dark_container,
    divider = OptionalThemes.forest_dark_divider,
    highlight = OptionalThemes.forest_dark_highlight,
    textPrimary = OptionalThemes.forest_dark_textPrimary,
    textSecondary = OptionalThemes.forest_dark_textSecondary,
    textTertiary = OptionalThemes.forest_dark_textTertiary,
    textAccent = OptionalThemes.forest_dark_textAccent,
    textLink = OptionalThemes.forest_dark_textLink
)

// ------------ 5. Ocean Breeze ------------
private val oceanLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.ocean_light_accent,
    border = OptionalThemes.ocean_light_border,
    container = OptionalThemes.ocean_light_container,
    divider = OptionalThemes.ocean_light_divider,
    highlight = OptionalThemes.ocean_light_highlight,
    textPrimary = OptionalThemes.ocean_light_textPrimary,
    textSecondary = OptionalThemes.ocean_light_textSecondary,
    textTertiary = OptionalThemes.ocean_light_textTertiary,
    textAccent = OptionalThemes.ocean_light_textAccent,
    textLink = OptionalThemes.ocean_light_textLink
)

private val oceanDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.ocean_dark_accent,
    border = OptionalThemes.ocean_dark_border,
    container = OptionalThemes.ocean_dark_container,
    divider = OptionalThemes.ocean_dark_divider,
    highlight = OptionalThemes.ocean_dark_highlight,
    textPrimary = OptionalThemes.ocean_dark_textPrimary,
    textSecondary = OptionalThemes.ocean_dark_textSecondary,
    textTertiary = OptionalThemes.ocean_dark_textTertiary,
    textAccent = OptionalThemes.ocean_dark_textAccent,
    textLink = OptionalThemes.ocean_dark_textLink
)

// ------------ 6. Cyberpunk Neon ------------
private val cyberLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.cyber_light_accent,
    border = OptionalThemes.cyber_light_border,
    container = OptionalThemes.cyber_light_container,
    divider = OptionalThemes.cyber_light_divider,
    highlight = OptionalThemes.cyber_light_highlight,
    textPrimary = OptionalThemes.cyber_light_textPrimary,
    textSecondary = OptionalThemes.cyber_light_textSecondary,
    textTertiary = OptionalThemes.cyber_light_textTertiary,
    textAccent = OptionalThemes.cyber_light_textAccent,
    textLink = OptionalThemes.cyber_light_textLink
)

private val cyberDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.cyber_dark_accent,
    border = OptionalThemes.cyber_dark_border,
    container = OptionalThemes.cyber_dark_container,
    divider = OptionalThemes.cyber_dark_divider,
    highlight = OptionalThemes.cyber_dark_highlight,
    textPrimary = OptionalThemes.cyber_dark_textPrimary,
    textSecondary = OptionalThemes.cyber_dark_textSecondary,
    textTertiary = OptionalThemes.cyber_dark_textTertiary,
    textAccent = OptionalThemes.cyber_dark_textAccent,
    textLink = OptionalThemes.cyber_dark_textLink
)

// ------------ 7. Retro Synthwave ------------
private val synthLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.synth_light_accent,
    border = OptionalThemes.synth_light_border,
    container = OptionalThemes.synth_light_container,
    divider = OptionalThemes.synth_light_divider,
    highlight = OptionalThemes.synth_light_highlight,
    textPrimary = OptionalThemes.synth_light_textPrimary,
    textSecondary = OptionalThemes.synth_light_textSecondary,
    textTertiary = OptionalThemes.synth_light_textTertiary,
    textAccent = OptionalThemes.synth_light_textAccent,
    textLink = OptionalThemes.synth_light_textLink
)

private val synthDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.synth_dark_accent,
    border = OptionalThemes.synth_dark_border,
    container = OptionalThemes.synth_dark_container,
    divider = OptionalThemes.synth_dark_divider,
    highlight = OptionalThemes.synth_dark_highlight,
    textPrimary = OptionalThemes.synth_dark_textPrimary,
    textSecondary = OptionalThemes.synth_dark_textSecondary,
    textTertiary = OptionalThemes.synth_dark_textTertiary,
    textAccent = OptionalThemes.synth_dark_textAccent,
    textLink = OptionalThemes.synth_dark_textLink
)

// ------------ 8. Minimal Grey ------------
private val greyLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.grey_light_accent,
    border = OptionalThemes.grey_light_border,
    container = OptionalThemes.grey_light_container,
    divider = OptionalThemes.grey_light_divider,
    highlight = OptionalThemes.grey_light_highlight,
    textPrimary = OptionalThemes.grey_light_textPrimary,
    textSecondary = OptionalThemes.grey_light_textSecondary,
    textTertiary = OptionalThemes.grey_light_textTertiary,
    textAccent = OptionalThemes.grey_light_textAccent,
    textLink = OptionalThemes.grey_light_textLink
)

private val greyDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.grey_dark_accent,
    border = OptionalThemes.grey_dark_border,
    container = OptionalThemes.grey_dark_container,
    divider = OptionalThemes.grey_dark_divider,
    highlight = OptionalThemes.grey_dark_highlight,
    textPrimary = OptionalThemes.grey_dark_textPrimary,
    textSecondary = OptionalThemes.grey_dark_textSecondary,
    textTertiary = OptionalThemes.grey_dark_textTertiary,
    textAccent = OptionalThemes.grey_dark_textAccent,
    textLink = OptionalThemes.grey_dark_textLink
)

// ------------ 9. Sunset Glow ------------
private val sunsetLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.sunset_light_accent,
    border = OptionalThemes.sunset_light_border,
    container = OptionalThemes.sunset_light_container,
    divider = OptionalThemes.sunset_light_divider,
    highlight = OptionalThemes.sunset_light_highlight,
    textPrimary = OptionalThemes.sunset_light_textPrimary,
    textSecondary = OptionalThemes.sunset_light_textSecondary,
    textTertiary = OptionalThemes.sunset_light_textTertiary,
    textAccent = OptionalThemes.sunset_light_textAccent,
    textLink = OptionalThemes.sunset_light_textLink
)

private val sunsetDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.sunset_dark_accent,
    border = OptionalThemes.sunset_dark_border,
    container = OptionalThemes.sunset_dark_container,
    divider = OptionalThemes.sunset_dark_divider,
    highlight = OptionalThemes.sunset_dark_highlight,
    textPrimary = OptionalThemes.sunset_dark_textPrimary,
    textSecondary = OptionalThemes.sunset_dark_textSecondary,
    textTertiary = OptionalThemes.sunset_dark_textTertiary,
    textAccent = OptionalThemes.sunset_dark_textAccent,
    textLink = OptionalThemes.sunset_dark_textLink
)

// ------------ 10. Rough Industrial ------------
private val roughLightExtendedColors = ExtendedColors(
    accent = OptionalThemes.rough_light_accent,
    border = OptionalThemes.rough_light_border,
    container = OptionalThemes.rough_light_container,
    divider = OptionalThemes.rough_light_divider,
    highlight = OptionalThemes.rough_light_highlight,
    textPrimary = OptionalThemes.rough_light_textPrimary,
    textSecondary = OptionalThemes.rough_light_textSecondary,
    textTertiary = OptionalThemes.rough_light_textTertiary,
    textAccent = OptionalThemes.rough_light_textAccent,
    textLink = OptionalThemes.rough_light_textLink
)

private val roughDarkExtendedColors = ExtendedColors(
    accent = OptionalThemes.rough_dark_accent,
    border = OptionalThemes.rough_dark_border,
    container = OptionalThemes.rough_dark_container,
    divider = OptionalThemes.rough_dark_divider,
    highlight = OptionalThemes.rough_dark_highlight,
    textPrimary = OptionalThemes.rough_dark_textPrimary,
    textSecondary = OptionalThemes.rough_dark_textSecondary,
    textTertiary = OptionalThemes.rough_dark_textTertiary,
    textAccent = OptionalThemes.rough_dark_textAccent,
    textLink = OptionalThemes.rough_dark_textLink
)
