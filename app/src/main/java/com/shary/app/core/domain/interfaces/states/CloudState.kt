package com.shary.app.core.domain.interfaces.states

data class CloudState (
    var email: String = "",
    var username: String = "",
    var token: String? = "",
    var fcmToken: String? = "",
    var localKeys: MutableMap<String, ByteArray> = mutableMapOf(),
    var isOnline: Boolean = false
)
