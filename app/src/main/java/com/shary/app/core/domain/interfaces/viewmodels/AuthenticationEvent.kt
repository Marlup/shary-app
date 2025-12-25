package com.shary.app.core.domain.interfaces.viewmodels

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