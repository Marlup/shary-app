package com.shary.app.viewmodels.field

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.Field
import com.shary.app.repositories.`interface`.FieldRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FieldViewModel(
    private val fieldRepository: FieldRepository
) : ViewModel() {

    private val _fields = MutableStateFlow<List<Field>>(emptyList())
    val fields: StateFlow<List<Field>> get() = _fields

    // Internal: mutable, only for this ViewModel
    private val _selectedKeys = MutableStateFlow<List<String>>(emptyList())

    // External: immutable, for the UI
    val selectedKeys: StateFlow<List<String>> = _selectedKeys

    init {
        loadFields()
    }

    fun toggleFieldSelection(key: String, isSelected: Boolean) {
        _selectedKeys.update { current ->
            if (isSelected) current + key else current - key
        }
    }

    fun clearSelectedKeys() {
        _selectedKeys.value = emptyList()
    }

    private fun loadFields() {
        viewModelScope.launch {
            _fields.value = fieldRepository.getAllFields()
        }
    }

    private suspend fun asyncSaveField(field: Field): Boolean {
        val success = fieldRepository.saveFieldIfNotExists(field)
        loadFields()
        return success
    }

    fun saveField(field: Field): Deferred<Boolean> {
        return viewModelScope.async {
            asyncSaveField(field)
        }
    }

    private suspend fun asyncDeleteField(key: String) {
        fieldRepository.deleteField(key)
        loadFields()
    }

    fun deleteField(key: String){
        viewModelScope.async {
            asyncDeleteField(key)
        }
    }

    private suspend fun asyncUpdateValue(key: String, value: String) {
        fieldRepository.updateValue(key, value)
        loadFields()
    }

    fun updateValue(key: String, value: String): Deferred<Unit> {
        return viewModelScope.async {
            asyncUpdateValue(key, value)
        }
    }

    private suspend fun asyncUpdateAlias(key: String, alias: String) {
        fieldRepository.updateAlias(key, alias)
        loadFields()
    }

    fun updateAlias(key: String, alias: String): Deferred<Unit> {
        return viewModelScope.async {
            asyncUpdateAlias(key, alias)
        }
    }
}
