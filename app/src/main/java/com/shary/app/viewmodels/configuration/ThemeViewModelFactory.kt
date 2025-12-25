package com.shary.app.viewmodels.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shary.app.infrastructure.persistance.repositories.ThemeRepositoryImpl

class ThemeViewModelFactory(private val repo: ThemeRepositoryImpl) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ThemeViewModel(repo) as T
    }
}
