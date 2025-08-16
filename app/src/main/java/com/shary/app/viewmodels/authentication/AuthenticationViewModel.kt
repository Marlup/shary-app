package com.shary.app.viewmodels.authentication

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.security.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, SIGNUP }

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
    private val auth: AuthService
) : ViewModel() {

    // Public auth state (email, username, token, isOnline…)
    val authState = auth.state

    // UI form + mode
    private val _mode = MutableStateFlow(AuthMode.LOGIN)
    val mode: StateFlow<AuthMode> = _mode

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
    fun setMode(m: AuthMode) { _mode.value = m }
    fun setUsername(v: String) { _form.value = _form.value.copy(username = v) }
    fun setEmail(v: String) { _form.value = _form.value.copy(email = v) }
    fun setPassword(v: String) { _form.value = _form.value.copy(password = v) }
    fun setConfirm(v: String) { _form.value = _form.value.copy(confirm = v) }
    fun resetForm() { _form.value = AuthForm() }

    // ---- Entry helpers for navigator / splash ----
    fun isCredentialsActive(ctx: Context) = auth.isCredentialsActive(ctx)
    fun isSignatureActive(ctx: Context) = auth.isSignatureActive(ctx)

    // ---- Submit (login or signup) ----
    fun submit(ctx: Context) {
        val f = _form.value
        when (_mode.value) {
            AuthMode.LOGIN -> login(ctx, f.username, f.password)
            AuthMode.SIGNUP -> signup(ctx, f.username, f.email, f.password, f.confirm)
        }
    }

    private fun login(ctx: Context, username: String, password: String) {
        val err = validateLogin(username, password)
        if (err != null) { emitError(err); return }

        viewModelScope.launch {
            _loading.value = true
            val result = auth.signIn(ctx, username, password)
            _loading.value = false
            result.onSuccess {
                _events.trySend(AuthEvent.Success)
            }.onFailure { e ->
                emitError(e.message ?: "Invalid credentials")
            }
        }
    }

    private fun signup(ctx: Context, username: String, email: String, password: String, confirm: String) {
        val err = validateSignup(username, email, password, confirm)
        if (err != null) { emitError(err); return }

        viewModelScope.launch {
            _loading.value = true
            val result = auth.signUp(ctx, username, email, password)
            _loading.value = false
            result.onSuccess {
                _events.trySend(AuthEvent.Success)
            }.onFailure { e ->
                emitError(e.message ?: "Sign up failed")
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
