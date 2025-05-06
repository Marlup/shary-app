package com.shary.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory<T : ViewModel>(
    private val creator: () -> T
) : ViewModelProvider.Factory {

    override fun <U : ViewModel> create(modelClass: Class<U>): U {
        val viewModel = creator.invoke()
        if (modelClass.isAssignableFrom(viewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return viewModel as U
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
