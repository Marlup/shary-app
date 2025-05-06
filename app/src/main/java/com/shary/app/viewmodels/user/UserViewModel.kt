package com.shary.app.viewmodels.user

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.User
import com.shary.app.repositories.`interface`.UserRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> get() = _users

    // Interno: mutable, solo para este ViewModel
    private val _selectedEmails = MutableStateFlow<List<String>>(emptyList())

    // Externo: inmutable para la UI
    val selectedEmails: StateFlow<List<String>> = _selectedEmails

    fun toggleUserSelection(email: String, isSelected: Boolean) {
        _selectedEmails.update { current ->
            if (isSelected) current + email else current - email
        }
    }

    fun clearSelectedEmails() {
        _selectedEmails.value = emptyList()
    }

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _users.value = userRepository.getAllUsers()
        }
    }

    private suspend fun asyncSaveUser(user: User): Boolean {
        val success = userRepository.saveUserIfNotExists(user)
        loadUsers() // Recargar la lista
        return success
    }

    fun saveUser(user: User): Deferred<Boolean> {
        return viewModelScope.async {
            asyncSaveUser(user)
        }
    }

    private suspend fun asyncDeleteUser(email: String): Boolean {
        val success = userRepository.deleteUser(email)
        loadUsers() // Recargar la lista
        return success
    }

    fun deleteUser(email: String): Deferred<Boolean> {
        return viewModelScope.async {
            asyncDeleteUser(email)
        }
    }
}
