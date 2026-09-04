package com.shary.app.infrastructure.persistance.repositories

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shary.app.core.constants.CloudInboxPolicy
import com.shary.app.core.domain.interfaces.repositories.SettingsRepository
import com.shary.app.core.domain.types.valueobjects.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("app_settings_prefs")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepository {
    private companion object {
        const val MIN_COPY_LONG_PRESS_MS = 300
        const val MAX_COPY_LONG_PRESS_MS = 1500
        const val DEFAULT_COPY_LONG_PRESS_MS = 600
        const val MIN_CLIPBOARD_AUTO_CLEAR_SECONDS = 0
        const val MAX_CLIPBOARD_AUTO_CLEAR_SECONDS = 120
        const val DEFAULT_CLIPBOARD_AUTO_CLEAR_SECONDS = 0
    }

    private object Keys {
        val REMEMBER_EMAIL_AT_LOGIN = booleanPreferencesKey("remember_email_at_login")
        val REMEMBERED_EMAIL = stringPreferencesKey("remembered_email")
        val MCP_ENABLED = booleanPreferencesKey("mcp_enabled")
        val AUTO_OPEN_CLOUD_INBOX = booleanPreferencesKey("auto_open_cloud_inbox_on_start")
        val DEFAULT_CLOUD_EXPIRY_DAYS = intPreferencesKey("default_cloud_expiry_days")
        val SHOW_FRIENDLY_SENDER_IDENTITY = booleanPreferencesKey("show_friendly_sender_identity")
        val REQUIRE_BIOMETRIC_FOR_CLOUD_INBOX = booleanPreferencesKey("require_biometric_for_cloud_inbox")
        val COMPACT_LIST_MODE = booleanPreferencesKey("compact_list_mode")
        val WIFI_ONLY_CLOUD_SYNC = booleanPreferencesKey("wifi_only_cloud_sync")
        val COPY_LONG_PRESS_MILLIS = intPreferencesKey("copy_long_press_millis")
        val CLIPBOARD_AUTO_CLEAR_SECONDS = intPreferencesKey("clipboard_auto_clear_seconds")
    }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val rememberEmail = prefs[Keys.REMEMBER_EMAIL_AT_LOGIN] ?: true
        AppSettings(
            rememberEmailAtLogin = rememberEmail,
            rememberedEmail = if (rememberEmail) {
                prefs[Keys.REMEMBERED_EMAIL].orEmpty().trim()
            } else {
                ""
            },
            mcpEnabled = prefs[Keys.MCP_ENABLED] ?: false,
            autoOpenCloudInboxOnStart = prefs[Keys.AUTO_OPEN_CLOUD_INBOX] ?: false,
            defaultCloudExpiryDays = (prefs[Keys.DEFAULT_CLOUD_EXPIRY_DAYS]
                ?: CloudInboxPolicy.DEFAULT_EXPIRY_DAYS)
                .coerceIn(CloudInboxPolicy.DEFAULT_EXPIRY_DAYS, CloudInboxPolicy.MAX_EXPIRY_DAYS),
            showFriendlySenderIdentity = prefs[Keys.SHOW_FRIENDLY_SENDER_IDENTITY] ?: true,
            requireBiometricForCloudInbox = prefs[Keys.REQUIRE_BIOMETRIC_FOR_CLOUD_INBOX] ?: false,
            compactListMode = prefs[Keys.COMPACT_LIST_MODE] ?: false,
            wifiOnlyCloudSync = prefs[Keys.WIFI_ONLY_CLOUD_SYNC] ?: false,
            copyLongPressMillis = (prefs[Keys.COPY_LONG_PRESS_MILLIS] ?: DEFAULT_COPY_LONG_PRESS_MS)
                .coerceIn(MIN_COPY_LONG_PRESS_MS, MAX_COPY_LONG_PRESS_MS),
            clipboardAutoClearSeconds = (prefs[Keys.CLIPBOARD_AUTO_CLEAR_SECONDS]
                ?: DEFAULT_CLIPBOARD_AUTO_CLEAR_SECONDS)
                .coerceIn(MIN_CLIPBOARD_AUTO_CLEAR_SECONDS, MAX_CLIPBOARD_AUTO_CLEAR_SECONDS)
        )
    }

    override suspend fun setRememberEmailAtLogin(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REMEMBER_EMAIL_AT_LOGIN] = enabled
            if (!enabled) prefs[Keys.REMEMBERED_EMAIL] = ""
        }
    }

    override suspend fun saveRememberedEmail(email: String) {
        context.settingsDataStore.edit { prefs ->
            val rememberEnabled = prefs[Keys.REMEMBER_EMAIL_AT_LOGIN] ?: true
            prefs[Keys.REMEMBERED_EMAIL] = if (rememberEnabled) email.trim() else ""
        }
    }

    override suspend fun clearRememberedEmail() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REMEMBERED_EMAIL] = ""
        }
    }

    override suspend fun setMcpEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.MCP_ENABLED] = enabled
        }
    }

    override suspend fun setAutoOpenCloudInboxOnStart(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUTO_OPEN_CLOUD_INBOX] = enabled
        }
    }

    override suspend fun setDefaultCloudExpiryDays(days: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_CLOUD_EXPIRY_DAYS] = days.coerceIn(
                CloudInboxPolicy.DEFAULT_EXPIRY_DAYS,
                CloudInboxPolicy.MAX_EXPIRY_DAYS
            )
        }
    }

    override suspend fun setShowFriendlySenderIdentity(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SHOW_FRIENDLY_SENDER_IDENTITY] = enabled
        }
    }

    override suspend fun setRequireBiometricForCloudInbox(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REQUIRE_BIOMETRIC_FOR_CLOUD_INBOX] = enabled
        }
    }

    override suspend fun setCompactListMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.COMPACT_LIST_MODE] = enabled
        }
    }

    override suspend fun setWifiOnlyCloudSync(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY_CLOUD_SYNC] = enabled
        }
    }

    override suspend fun setCopyLongPressMillis(milliseconds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.COPY_LONG_PRESS_MILLIS] =
                milliseconds.coerceIn(MIN_COPY_LONG_PRESS_MS, MAX_COPY_LONG_PRESS_MS)
        }
    }

    override suspend fun setClipboardAutoClearSeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CLIPBOARD_AUTO_CLEAR_SECONDS] = seconds
                .coerceIn(MIN_CLIPBOARD_AUTO_CLEAR_SECONDS, MAX_CLIPBOARD_AUTO_CLEAR_SECONDS)
        }
    }
}
