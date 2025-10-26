package com.shary.app.viewmodels.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shary.app.infrastructure.repositories.ThemeRepository

class ThemeViewModelFactory(private val repo: ThemeRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ThemeViewModel(repo) as T
    }
}
