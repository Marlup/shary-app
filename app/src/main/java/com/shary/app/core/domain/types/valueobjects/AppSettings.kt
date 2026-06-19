package com.shary.app.core.domain.types.valueobjects

data class AppSettings(
    val rememberEmailAtLogin: Boolean = true,
    val rememberedEmail: String = "",
    val autoOpenCloudInboxOnStart: Boolean = false,
    val defaultCloudExpiryDays: Int = 7,
    val showFriendlySenderIdentity: Boolean = true,
    val requireBiometricForCloudInbox: Boolean = false,
    val compactListMode: Boolean = false,
    val wifiOnlyCloudSync: Boolean = false,
    val copyLongPressMillis: Int = 600,
    val clipboardAutoClearSeconds: Int = 0
)
