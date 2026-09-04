package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.types.valueobjects.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setRememberEmailAtLogin(enabled: Boolean)
    suspend fun saveRememberedEmail(email: String)
    suspend fun clearRememberedEmail()
    suspend fun setMcpEnabled(enabled: Boolean)
    suspend fun setAutoOpenCloudInboxOnStart(enabled: Boolean)
    suspend fun setDefaultCloudExpiryDays(days: Int)
    suspend fun setShowFriendlySenderIdentity(enabled: Boolean)
    suspend fun setRequireBiometricForCloudInbox(enabled: Boolean)
    suspend fun setCompactListMode(enabled: Boolean)
    suspend fun setWifiOnlyCloudSync(enabled: Boolean)
    suspend fun setCopyLongPressMillis(milliseconds: Int)
    suspend fun setClipboardAutoClearSeconds(seconds: Int)
}
