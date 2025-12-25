package com.shary.app.core.domain.types.valueobjects

import com.shary.app.viewmodels.authentication.AuthenticationMode

/** Holds the form fields for login/signup */
data class AuthLogForm(
    val mode: AuthenticationMode = AuthenticationMode.SIGNUP,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = ""
)