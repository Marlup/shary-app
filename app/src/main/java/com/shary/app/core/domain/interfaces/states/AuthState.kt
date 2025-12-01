package com.shary.app.core.domain.interfaces.states


data class AuthState(
    var email: String = "",
    var username: String = "",
    var safePassword: String = "",
    var authToken: String = "",
    var localKeys: MutableMap<String, ByteArray> = mutableMapOf(),
    var isOnline: Boolean = false
)