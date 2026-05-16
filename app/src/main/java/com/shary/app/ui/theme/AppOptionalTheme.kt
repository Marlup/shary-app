package com.shary.app.ui.theme

import androidx.compose.runtime.Composable
import com.shary.app.core.domain.types.enums.AppTheme

@Composable
fun AppOptionalTheme(
    theme: AppTheme = AppTheme.Pastel,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    SharyTheme(content = content)
}
