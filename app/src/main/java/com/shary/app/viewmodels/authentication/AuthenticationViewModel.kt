package com.shary.app.viewmodels.authentication

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import com.shary.app.core.domain.interfaces.services.CloudService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Represents which auth flow the user is performing */
enum class AuthenticationMode { LOGIN, SIGNUP }

/** Holds the form fields for login/signup */
data class AuthLogForm(
    val mode: AuthenticationMode = AuthenticationMode.SIGNUP,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = ""
)

/**
 * One-shot events that the UI can observe.
 * These represent important transitions or results,
 * separate from the continuous state (like loading).
 */
sealed interface AuthenticationEvent {
    data object Success : AuthenticationEvent
    data object CloudSignedOut : AuthenticationEvent
    data object UserRegisteredInCloud : AuthenticationEvent
    data object UserNotRegisteredInCloud : AuthenticationEvent
    data class CloudAnonymousReady(val uid: String) : AuthenticationEvent
    data class CloudTokenRefreshed(val token: String) : AuthenticationEvent
    data class Error(val message: String) : AuthenticationEvent
}

/**
 * AuthenticationViewModel
 *
 * Responsibilities:
 * - Manages login and signup UI flows (form state, validation, mode).
 * - Delegates to AuthenticationService for local credential handling.
 * - Delegates to CloudService for anonymous Firebase authentication.
 * - Exposes reactive StateFlows (auth form, loading) and Channels for events.
 *
 * UI should observe:
 * - [authState]: current authentication state (username, email, token, etc.)
 * - [mode]: whether user is in LOGIN or SIGNUP
 * - [logForm]: the form data being edited
 * - [loading]: true while a request is in progress
 * - [events]: one-shot events (success, error, cloud state changes)
 */
@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val authService: AuthenticationService,
    private val cloudService: CloudService
) : ViewModel() {

    // Public reactive state for UI binding
    val authState = authService.state
    val cloudState = cloudService.cloudState

    // Current mode (LOGIN or SIGNUP)
    private val _currentMode = MutableStateFlow(AuthenticationMode.LOGIN)
    val mode: StateFlow<AuthenticationMode> = _currentMode

    // Current form values
    private var _logForm = MutableStateFlow(AuthLogForm())
    val logForm: StateFlow<AuthLogForm> = _logForm

    // Indicates whether a network or crypto task is in progress
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // One-shot events: UI should collect this flow for reactions
    private val _events = Channel<AuthenticationEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Toggles for password/confirm visibility in UI
    var passwordVisible by mutableStateOf(false)
    var confirmVisible by mutableStateOf(false)

    // -------------------- Form/UI Helpers --------------------

    fun togglePasswordVisibility() { passwordVisible = !passwordVisible }
    fun toggleConfirmVisibility() { confirmVisible = !confirmVisible }

    fun resetForm() { _logForm.value = AuthLogForm() }

    fun setMode(value: AuthenticationMode) {
        _logForm.update { it.copy(mode = value) }
    }
    fun updateUsername(value: String) {
        _logForm.update { it.copy(username = value) }
    }
    fun updateEmail(value: String) {
        _logForm.update { it.copy(email = value) }
    }
    fun updatePassword(value: String) {
        _logForm.update { it.copy(password = value) }
    }
    fun updatePasswordConfirm(value: String) {
        _logForm.update { it.copy(passwordConfirm = value) }
    }
    fun clearLogFormStates() {
        _logForm.value = AuthLogForm(mode = _logForm.value.mode)
    }

    // -------------------- Entry helpers for splash/navigation --------------------

    /** Checks if encrypted credentials exist locally */
    fun isCredentialsActive(context: Context) = authService.isCredentialsActive(context)

    /** Checks if signature file exists locally */
    fun isSignatureActive(context: Context) = authService.isSignatureActive(context)

    // -------------------- Core actions: login/signup --------------------

    /**
     * Validates form and triggers login or signup.
     * Emits Success or Error events depending on outcome.
     */
    fun submit(context: Context) {
        val f = _logForm.value
        Log.w("AuthenticationViewModel", "submit: $f")

        // Validate form based on current mode
        val validationError = when (_logForm.value.mode) {
            AuthenticationMode.LOGIN -> validateLogin(f.username, f.password)
            AuthenticationMode.SIGNUP -> validateSignup(f.username, f.email, f.password, f.passwordConfirm)
        }

        if (validationError != null) {
            emitError(validationError)
            return
        }

        // Launch login/signup in background
        viewModelScope.launch {

            Log.i("AuthenticationViewModel", "submit: $f")
            _loading.value = true
            val result = withContext(Dispatchers.IO) {
                when (_logForm.value.mode) {
                    AuthenticationMode.LOGIN ->
                        authService.signIn(context, f.username, f.password)
                    AuthenticationMode.SIGNUP -> {
                        // Pre sign out to safety avoid preloaded user uid and tokens.
                        authService.signOut(context)
                        authService.signUp(context, f.username, f.email, f.password)
                    }
                }
            }
            _loading.value = false

            result.onSuccess {
                _events.trySend(AuthenticationEvent.Success)
            }.onFailure { e ->
                val msg = when (_logForm.value.mode) {
                    AuthenticationMode.LOGIN -> e.message ?: "Invalid credentials"
                    AuthenticationMode.SIGNUP -> e.message ?: "Sign up failed"
                }
                emitError(msg)
            }
        }
    }

    // -------------------- Cloud auth integration --------------------

    /**
     * Ensures an anonymous Firebase session exists.
     * Should be called after login/signup or during splash.
     */
    fun connectCloudAnonymously() {
        viewModelScope.launch {
            _loading.value = true
            val res = cloudService.ensureAnonymousSession()
            _loading.value = false

            res.onSuccess { uid ->
                _events.trySend(AuthenticationEvent.CloudAnonymousReady(uid))
            }.onFailure { e ->
                _events.trySend(AuthenticationEvent.Error(e.message ?: "Cloud anonymous auth failed"))
            }
        }
    }

    /**
     * Refreshes Firebase ID token and emits updated token.
     * Useful before hitting protected endpoints.
     */
    fun refreshCloudToken() {
        viewModelScope.launch {
            _loading.value = true
            val res = cloudService.refreshIdToken()
            _loading.value = false

            res.onSuccess { token ->
                _events.trySend(AuthenticationEvent.CloudTokenRefreshed(token))
            }.onFailure { e ->
                _events.trySend(AuthenticationEvent.Error(e.message ?: "Token refresh failed"))
            }
        }
    }

    /**
     * Signs out from Firebase anonymous session.
     * Does NOT clear local credentials.
     */
    fun signOutCloud() {
        viewModelScope.launch {
            _loading.value = true
            val res = cloudService.signOutCloud()
            _loading.value = false

            res.onSuccess {
                _events.trySend(AuthenticationEvent.CloudSignedOut)
            }.onFailure { e ->
                _events.trySend(AuthenticationEvent.Error(e.message ?: "Cloud sign-out failed"))
            }
        }
    }

    fun onLoginSuccess(username: String) {
        viewModelScope.launch {
            val registered = runCatching { cloudService.isUserRegisteredInCloud(username) }
                .getOrDefault(false)

            if (registered) {
                Log.d("AuthenticationViewModel", "User already registered in Cloud")
                _events.trySend(AuthenticationEvent.UserRegisteredInCloud)
            } else {
                Log.d("AuthenticationViewModel", "User is not registered in Cloud")
                _events.trySend(AuthenticationEvent.UserNotRegisteredInCloud)
            }
        }
    }

    fun getToken(): String? = cloudState.value.token

    // -------------------- Helpers --------------------

    private fun emitError(msg: String) {
        _events.trySend(AuthenticationEvent.Error(msg))
    }

    private fun validateLogin(username: String, password: String): String? =
        when {
            username.isBlank() -> "Username required"
            password.isBlank() -> "Password required"
            else -> null
        }

    private fun validateSignup(username: String, email: String, password: String, confirm: String): String? {
        Log.i("AuthenticationViewModel", "validateSignup: $username, $email, $password, $confirm")
        return when {
            username.isBlank() -> "Username required"
            email.isBlank() -> "Email required"
            !email.contains("@") -> "Invalid email"
            password.length < 8 -> "Password must be at least 8 chars"
            password != confirm -> "Passwords don’t match"
            else -> null
        }
    }
}
