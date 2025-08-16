package com.shary.app.core.domain.interfaces.states


data class AuthState(
    var email: String = "",
    var username: String = "",
    var safePassword: String = "",
    var authToken: String = "",
    var isOnline: Boolean = false
)