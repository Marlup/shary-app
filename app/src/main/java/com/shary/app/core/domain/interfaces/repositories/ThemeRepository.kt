package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.AppTheme
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val selectedTheme: Flow<AppTheme>
    suspend fun saveTheme(theme: AppTheme)
}