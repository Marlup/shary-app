package com.shary.app.viewmodels.communication

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.services.EmailService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// application
@HiltViewModel
class EmailViewModel @Inject constructor(
    private val emailService: EmailService
) : ViewModel() {

    private val _intent = MutableSharedFlow<Intent>()
    val intent: SharedFlow<Intent> = _intent

    //private val _sending = MutableStateFlow(false)
    //val sending: StateFlow<Boolean> = _sending

    private val _events = MutableSharedFlow<String>() // success/error messages
    val events: SharedFlow<String> = _events

    // viewmodels/EmailViewModel.kt
    fun sendRequest(fields: List<FieldDomain>, emails: List<UserDomain>) {
        viewModelScope.launch {
            emailService.sendRequestFile(fields, emails)
                .onSuccess { _intent.emit(it) }
                .onFailure { e -> _events.emit("Error: ${e.message}") }
        }
    }

    fun sendResponse(fields: List<FieldDomain>, emails: List<UserDomain>) {
        viewModelScope.launch {
            emailService.sendResponseFile(fields, emails)
                .onSuccess { _intent.emit(it) }
                .onFailure { e -> _events.emit("Error: ${e.message}") }
        }
    }
}
