// ui/viewmodels/ThemeViewModel.kt
package com.shary.app.viewmodels.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.infrastructure.persistance.repositories.ThemeRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repo: ThemeRepositoryImpl
) : ViewModel() {
    val selectedTheme = repo.selectedTheme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppTheme.Pastel
    )

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch { repo.saveTheme(theme) }
    }
}
