package com.shary.app.viewmodels.field

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.Field
import com.shary.app.repositories.`interface`.FieldRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FieldViewModel(
    private val fieldRepository: FieldRepository
) : ViewModel() {

    private val _fields = MutableStateFlow<List<Field>>(emptyList())
    val fields: StateFlow<List<Field>> get() = _fields

    init {
        loadFields()
    }

    private fun loadFields() {
        viewModelScope.launch {
            _fields.value = fieldRepository.getAllFields()
        }
    }

    private suspend fun asyncSaveField(field: Field): Boolean {
        val success = fieldRepository.saveFieldIfNotExists(field)
        loadFields() // Recargar la lista
        return success
    }

    fun saveField(field: Field): Deferred<Boolean> {
        return viewModelScope.async {
            asyncSaveField(field)
        }
    }

    private suspend fun asyncDeleteField(key: String): Boolean {
        val success = fieldRepository.deleteField(key)
        loadFields() // Recargar la lista
        return success
    }

    fun deleteField(key: String): Deferred<Boolean> {
        return viewModelScope.async {
            asyncDeleteField(key)
        }
    }
}
