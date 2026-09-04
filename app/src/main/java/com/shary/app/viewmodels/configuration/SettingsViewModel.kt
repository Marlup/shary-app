package com.shary.app.viewmodels.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.repositories.SettingsRepository
import com.shary.app.core.domain.types.valueobjects.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings()
    )

    fun setRememberEmailAtLogin(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRememberEmailAtLogin(enabled) }
    }

    fun rememberEmailIfEnabled(email: String) {
        viewModelScope.launch { settingsRepository.saveRememberedEmail(email) }
    }

    fun clearRememberedEmail() {
        viewModelScope.launch { settingsRepository.clearRememberedEmail() }
    }

    fun setMcpEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMcpEnabled(enabled) }
    }

    fun setAutoOpenCloudInboxOnStart(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoOpenCloudInboxOnStart(enabled) }
    }

    fun setDefaultCloudExpiryDays(days: Int) {
        viewModelScope.launch { settingsRepository.setDefaultCloudExpiryDays(days) }
    }

    fun setShowFriendlySenderIdentity(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowFriendlySenderIdentity(enabled) }
    }

    fun setRequireBiometricForCloudInbox(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRequireBiometricForCloudInbox(enabled) }
    }

    fun setCompactListMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCompactListMode(enabled) }
    }

    fun setWifiOnlyCloudSync(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWifiOnlyCloudSync(enabled) }
    }

    fun setCopyLongPressMillis(milliseconds: Int) {
        viewModelScope.launch { settingsRepository.setCopyLongPressMillis(milliseconds) }
    }

    fun setClipboardAutoClearSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setClipboardAutoClearSeconds(seconds) }
    }
}
