package com.shary.app.core.domain.interfaces.handler

interface PushNotificationHandler {
    fun onNewToken(token: String)
    fun onMessage(data: Map<String, String>)
}
