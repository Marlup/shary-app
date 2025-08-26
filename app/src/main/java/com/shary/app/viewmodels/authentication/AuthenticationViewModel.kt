package com.shary.app.viewmodels.authentication

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.security.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AuthenticationMode { LOGIN, SIGNUP }

data class AuthForm(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = ""
)

sealed interface AuthEvent {
    data object Success : AuthEvent
    data class Error(val message: String) : AuthEvent
}

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    // Public auth state (email, username, token, isOnline…)
    val authState = authService.state

    // UI form + mode
    private val _mode = MutableStateFlow(AuthenticationMode.LOGIN)
    val mode: StateFlow<AuthenticationMode> = _mode

    private val _form = MutableStateFlow(AuthForm())
    val form: StateFlow<AuthForm> = _form

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // One-shot events
    private val _events = Channel<AuthEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    var passwordVisible by mutableStateOf(false)
    var confirmVisible by mutableStateOf(false)

    fun togglePasswordVisibility() { passwordVisible = !passwordVisible }
    fun toggleConfirmVisibility() { confirmVisible = !confirmVisible }


    // ---- Mode & form updates ----
    fun setMode(m: AuthenticationMode) { _mode.value = m }
    fun setUsername(v: String) { _form.value = _form.value.copy(username = v) }
    fun setEmail(v: String) { _form.value = _form.value.copy(email = v) }
    fun setPassword(v: String) { _form.value = _form.value.copy(password = v) }
    fun setConfirm(v: String) { _form.value = _form.value.copy(confirm = v) }
    fun resetForm() { _form.value = AuthForm() }

    // ---- Entry helpers for navigator / splash ----
    fun isCredentialsActive(context: Context) = authService.isCredentialsActive(context)
    fun isSignatureActive(context: Context) = authService.isSignatureActive(context)

    // ---- Submit (login or signup) ----
    fun submit(context: Context) {
        val f = _form.value
        Log.w("AuthenticationViewModel", "submit: $f")

        // basic validation before launching coroutine
        val validationError = when (_mode.value) {
            AuthenticationMode.LOGIN -> validateLogin(f.username, f.password)
            AuthenticationMode.SIGNUP -> validateSignup(f.username, f.email, f.password, f.confirm)
        }

        if (validationError != null) {
            emitError(validationError)
            return
        }

        // launch actual login/signup work in background
        viewModelScope.launch {
            _loading.value = true
            val result = withContext(Dispatchers.IO) {
                when (_mode.value) {
                    AuthenticationMode.LOGIN ->
                        authService.signIn(context, f.username, f.password)
                    AuthenticationMode.SIGNUP ->
                        authService.signUp(context, f.username, f.email, f.password)
                }
            }
            _loading.value = false

            result.onSuccess {
                _events.trySend(AuthEvent.Success)
            }.onFailure { e ->
                val msg = when (_mode.value) {
                    AuthenticationMode.LOGIN -> e.message ?: "Invalid credentials"
                    AuthenticationMode.SIGNUP -> e.message ?: "Sign up failed"
                }
                emitError(msg)
            }
        }
    }

    private fun emitError(msg: String) {
        _events.trySend(AuthEvent.Error(msg))
    }

    // ---- Validation (tweak as needed) ----
    private fun validateLogin(username: String, password: String): String? =
        when {
            username.isBlank() -> "Username required"
            password.isBlank() -> "Password required"
            else -> null
        }

    private fun validateSignup(username: String, email: String, password: String, confirm: String): String? =
        when {
            username.isBlank() -> "Username required"
            email.isBlank() -> "Email required"
            !email.contains("@") -> "Invalid email"
            password.length < 8 -> "Password must be at least 8 chars"
            password != confirm -> "Passwords don’t match"
            else -> null
        }
}
