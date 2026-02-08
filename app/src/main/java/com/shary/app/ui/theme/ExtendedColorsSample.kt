package com.shary.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.AppTheme

/**
 * Sample composable demonstrating the usage of extended colors
 * This can be used as a reference for applying the new color palette
 */
@Composable
fun ExtendedColorsSample(theme: AppTheme) {
    val extendedColors = getExtendedColors(theme = theme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Extended Colors Demo - ${theme.name}",
            style = MaterialTheme.typography.headlineMedium,
            color = extendedColors.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Visual Elements Section
        Text(
            text = "Visual Elements",
            style = MaterialTheme.typography.titleLarge,
            color = extendedColors.textPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Accent Color Example
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = extendedColors.accent,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Accent Color - Used for highlights and call-to-action elements",
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Border Example
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(
                    width = 2.dp,
                    color = extendedColors.border,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Border Color - Used for borders and outlines",
                color = extendedColors.textPrimary
            )
        }

        // Container Example
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = extendedColors.container,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Container Color - Used for cards and containers",
                color = extendedColors.textPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Divider Example
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Section 1",
                color = extendedColors.textPrimary,
                modifier = Modifier.padding(8.dp)
            )
            HorizontalDivider(
                thickness = 2.dp,
                color = extendedColors.divider
            )
            Text(
                text = "Section 2",
                color = extendedColors.textPrimary,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Highlight Example
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(
                    color = extendedColors.highlight,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Highlight Color - Used for selected items and emphasis",
                color = extendedColors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Text Colors Section
        Text(
            text = "Text Colors",
            style = MaterialTheme.typography.titleLarge,
            color = extendedColors.textPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Primary Text - Main headings and important content",
                color = extendedColors.textPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Secondary Text - Supporting information and descriptions",
                color = extendedColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Tertiary Text - Subtle details and metadata",
                color = extendedColors.textTertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Accent Text - Emphasized keywords and important labels",
                color = extendedColors.textAccent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Link Text - Clickable links and interactive elements",
                color = extendedColors.textLink,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
