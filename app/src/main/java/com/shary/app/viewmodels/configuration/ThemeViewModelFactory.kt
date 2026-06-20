package com.shary.app.viewmodels.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shary.app.infrastructure.persistance.repositories.ThemeRepositoryImpl

class ThemeViewModelFactory(private val repo: ThemeRepositoryImpl) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return ThemeViewModel(repo) as T
    }
}
