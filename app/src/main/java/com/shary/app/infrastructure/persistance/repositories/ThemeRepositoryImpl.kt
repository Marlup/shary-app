// infrastructure/repositories/ThemeRepositoryImpl.kt
package com.shary.app.infrastructure.persistance.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shary.app.core.domain.interfaces.repositories.ThemeRepository
import com.shary.app.core.domain.types.enums.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("theme_prefs")

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    companion object {
        private val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme")
    }

    override val selectedTheme: Flow<AppTheme> =
        context.dataStore.data.map { prefs ->
            val name = prefs[SELECTED_THEME_KEY]
            runCatching {
                AppTheme.valueOf(name ?: AppTheme.Pastel.name)
            }.getOrDefault(AppTheme.Pastel)
        }

    override suspend fun saveTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_THEME_KEY] = theme.name
        }
    }
}
