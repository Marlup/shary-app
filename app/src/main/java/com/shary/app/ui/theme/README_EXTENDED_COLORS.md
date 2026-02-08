# Extended Colors for AppThemes

This document explains the extended color system added to the Shary app, which provides 5 visual element colors and 5 text colors for each of the 10 available themes.

## Overview

The extended color system complements Material3's ColorScheme by providing additional themed colors for:
- **Visual Elements**: accent, border, container, divider, highlight
- **Text Colors**: textPrimary, textSecondary, textTertiary, textAccent, textLink

Each of the 10 AppThemes (Pastel, Candy, Autumn, Forest, Ocean, Cyberpunk, Synthwave, Grey, Sunset, Rough) has both light and dark variants of these colors.

## Color Definitions

### Visual Element Colors

1. **accent**: Used for highlights and call-to-action elements
2. **border**: Used for borders and outlines around components
3. **container**: Used for background of cards and containers
4. **divider**: Used for separating sections and content
5. **highlight**: Used for selected items and emphasis

### Text Colors

1. **textPrimary**: Main headings and important content
2. **textSecondary**: Supporting information and descriptions
3. **textTertiary**: Subtle details and metadata
4. **textAccent**: Emphasized keywords and important labels
5. **textLink**: Clickable links and interactive text elements

## Files Modified

### Core Files
- `OptionalThemes.kt`: Added 10 new color values per theme (5 visual + 5 text) for both light and dark modes
- `ThemeColors.kt`: New file containing `ExtendedColors` data class and `getExtendedColors()` function
- `ExtendedColorsSample.kt`: Sample composable demonstrating usage of all extended colors

### UI Components Updated
- `ItemRow.kt`: Updated to use extended colors for containers, borders, text, icons, and dividers
- `SpecialComponents.kt`: Updated CompactActionButton to support extended colors

## Usage Guide

### Basic Usage

To use extended colors in your composable, follow these steps:

```kotlin
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.ui.theme.getExtendedColors

@Composable
fun MyComposable(theme: AppTheme = AppTheme.Pastel) {
    val extendedColors = getExtendedColors(theme = theme)

    // Use visual element colors
    Box(
        modifier = Modifier
            .background(color = extendedColors.container)
            .border(width = 2.dp, color = extendedColors.border)
    ) {
        // Use text colors
        Text(
            text = "Primary text",
            color = extendedColors.textPrimary
        )
        Text(
            text = "Secondary text",
            color = extendedColors.textSecondary
        )
    }
}
```

### Example: Custom Card Component

```kotlin
@Composable
fun ThemedCard(
    title: String,
    subtitle: String,
    theme: AppTheme = AppTheme.Pastel
) {
    val extendedColors = getExtendedColors(theme = theme)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = extendedColors.border,
                shape = MaterialTheme.shapes.medium
            ),
        color = extendedColors.container,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = extendedColors.textPrimary,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(
                color = extendedColors.divider,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                color = extendedColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

### Example: Highlighted Button

```kotlin
@Composable
fun HighlightedButton(
    text: String,
    onClick: () -> Unit,
    theme: AppTheme = AppTheme.Pastel
) {
    val extendedColors = getExtendedColors(theme = theme)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = extendedColors.accent
        ),
        modifier = Modifier.border(
            width = 2.dp,
            color = extendedColors.border,
            shape = MaterialTheme.shapes.small
        )
    ) {
        Text(
            text = text,
            color = Color.White
        )
    }
}
```

### Example: Link Text

```kotlin
@Composable
fun LinkText(
    text: String,
    onClick: () -> Unit,
    theme: AppTheme = AppTheme.Pastel
) {
    val extendedColors = getExtendedColors(theme = theme)

    Text(
        text = text,
        color = extendedColors.textLink,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

### Example: Section Divider

```kotlin
@Composable
fun SectionDivider(theme: AppTheme = AppTheme.Pastel) {
    val extendedColors = getExtendedColors(theme = theme)

    HorizontalDivider(
        thickness = 2.dp,
        color = extendedColors.divider,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
```

## Color Palette Per Theme

### Pastel Theme
- **Visual**: Soft yellows, light grays, lilac containers, subtle dividers, gentle highlights
- **Text**: Dark to light grays, purple accents, blue links

### Candy Theme
- **Visual**: Purple candy, pink borders, sky blue containers, pink dividers, peach highlights
- **Text**: Deep to light pinks, purple text, bright pink accents, sky links

### Autumn Theme
- **Visual**: Deep oranges, warm gray borders, orange containers, brown dividers, light orange highlights
- **Text**: Dark to light browns, orange accents, amber links

### Forest Theme
- **Visual**: Light greens, pale green borders, green containers, lime dividers, bright green highlights
- **Text**: Dark to light greens, olive accents, lime green links

### Ocean Theme
- **Visual**: Cyan accents, sky blue borders, light blue containers, blue dividers, bright blue highlights
- **Text**: Deep to light blues, teal accents, cyan links

### Cyberpunk Theme
- **Visual**: Neon green accents, purple borders, yellow containers, purple dividers, pink highlights
- **Text**: Deep to light purples, purple accents, bright purple links

### Synthwave Theme
- **Visual**: Purple accents, pink borders, blue containers, pink dividers, blue highlights
- **Text**: Deep pink to light blues, pink accents, bright blue links

### Grey Theme
- **Visual**: Blue grays, light gray borders, gray containers, medium gray dividers, light blue gray highlights
- **Text**: Dark to light grays, blue gray accents, gray blue links

### Sunset Theme
- **Visual**: Deep oranges, peach borders, light orange containers, orange dividers, bright orange highlights
- **Text**: Deep orange to light peach, orange accents, golden links

### Rough Theme
- **Visual**: Blue grays, light gray borders, gray containers, steel dividers, dark steel highlights
- **Text**: Dark to light steels, steel accents, blue steel links

## Best Practices

1. **Consistency**: Always use extended colors for their intended purpose (e.g., use `textLink` for all links)

2. **Accessibility**: The colors are designed with accessibility in mind, maintaining good contrast ratios

3. **Theme Awareness**: Pass the current theme to components that use extended colors

4. **Fallbacks**: When using Material3 components, combine extended colors with standard MaterialTheme colors

5. **Testing**: Test your UI with different themes to ensure colors work well across all variants

## Integration with Existing Code

The extended color system is designed to work alongside Material3's ColorScheme. You can mix both:

```kotlin
@Composable
fun MixedColorExample(theme: AppTheme = AppTheme.Pastel) {
    val extendedColors = getExtendedColors(theme = theme)

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background) // Material3
            .border(1.dp, extendedColors.border) // Extended
    ) {
        Text(
            text = "Title",
            color = MaterialTheme.colorScheme.primary // Material3
        )
        Text(
            text = "Subtitle",
            color = extendedColors.textSecondary // Extended
        )
    }
}
```

## Sample Demo

To see all extended colors in action, you can use the `ExtendedColorsSample` composable:

```kotlin
import com.shary.app.ui.theme.ExtendedColorsSample

@Composable
fun PreviewExtendedColors() {
    AppOptionalTheme(theme = AppTheme.Cyberpunk) {
        ExtendedColorsSample(theme = AppTheme.Cyberpunk)
    }
}
```

## Summary

The extended color system provides a comprehensive palette that enhances the visual consistency and expressiveness of the app's UI while maintaining full compatibility with Material3 design principles. Each theme now has 20 additional color values (10 for light mode, 10 for dark mode) that can be used to create rich, themed interfaces.
