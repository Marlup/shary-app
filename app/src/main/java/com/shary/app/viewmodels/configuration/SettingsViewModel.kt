package com.shary.app.viewmodels.configuration

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.application.backup.VaultBackupManager
import com.shary.app.core.domain.interfaces.repositories.SettingsRepository
import com.shary.app.core.domain.types.valueobjects.AppSettings
import com.shary.app.core.domain.types.valueobjects.VaultBackupPreview
import com.shary.app.infrastructure.security.backup.VaultBackupFormatException
import com.shary.app.infrastructure.security.backup.VaultBackupWrongPassphraseException
import com.shary.app.infrastructure.services.backup.VaultBackupFileService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VaultTransferMode {
    EXPORT,
    IMPORT,
    IMPORT_PREVIEW
}

data class VaultTransferUiState(
    val mode: VaultTransferMode? = null,
    val passphrase: String = "",
    val confirmPassphrase: String = "",
    val preview: VaultBackupPreview? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val vaultBackupManager: VaultBackupManager,
    private val vaultBackupFileService: VaultBackupFileService
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings()
    )

    private val _vaultTransfer = MutableStateFlow(VaultTransferUiState())
    val vaultTransfer: StateFlow<VaultTransferUiState> = _vaultTransfer.asStateFlow()

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

    fun beginExport() {
        _vaultTransfer.value = VaultTransferUiState(mode = VaultTransferMode.EXPORT)
    }

    fun beginImport() {
        _vaultTransfer.value = VaultTransferUiState(mode = VaultTransferMode.IMPORT)
    }

    fun updatePassphrase(value: String) {
        _vaultTransfer.value = _vaultTransfer.value.copy(
            passphrase = value,
            error = null
        )
    }

    fun updateConfirmPassphrase(value: String) {
        _vaultTransfer.value = _vaultTransfer.value.copy(
            confirmPassphrase = value,
            error = null
        )
    }

    fun validateExportPassphrase(): Boolean {
        val state = _vaultTransfer.value
        val error = when {
            state.passphrase.length < MIN_PASSPHRASE_LENGTH ->
                "Use a passphrase of at least $MIN_PASSPHRASE_LENGTH characters."
            state.passphrase != state.confirmPassphrase ->
                "The passphrases do not match."
            else -> null
        }
        if (error != null) {
            _vaultTransfer.value = state.copy(error = error)
            return false
        }
        _vaultTransfer.value = state.copy(error = null)
        return true
    }

    fun validateImportPassphrase(): Boolean {
        val state = _vaultTransfer.value
        if (state.passphrase.isBlank()) {
            _vaultTransfer.value = state.copy(error = "Enter the backup passphrase.")
            return false
        }
        _vaultTransfer.value = state.copy(error = null)
        return true
    }

    fun exportTo(uri: Uri) {
        val state = _vaultTransfer.value
        if (state.isBusy || state.mode != VaultTransferMode.EXPORT) return
        if (!validateExportPassphrase()) return

        val passphrase = _vaultTransfer.value.passphrase.toCharArray()
        _vaultTransfer.value = _vaultTransfer.value.copy(isBusy = true, error = null)
        viewModelScope.launch {
            try {
                val bytes = vaultBackupManager.export(passphrase)
                vaultBackupFileService.write(uri, bytes)
                _vaultTransfer.value = VaultTransferUiState(
                    message = "Encrypted vault exported."
                )
            } catch (error: Exception) {
                _vaultTransfer.value = _vaultTransfer.value.copy(
                    isBusy = false,
                    error = vaultErrorMessage(error)
                )
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun inspectImport(uri: Uri) {
        val state = _vaultTransfer.value
        if (state.isBusy || state.mode != VaultTransferMode.IMPORT) return
        if (!validateImportPassphrase()) return

        val passphrase = _vaultTransfer.value.passphrase.toCharArray()
        _vaultTransfer.value = _vaultTransfer.value.copy(isBusy = true, error = null)
        viewModelScope.launch {
            try {
                val bytes = vaultBackupFileService.read(uri)
                val preview = vaultBackupManager.inspect(bytes, passphrase)
                _vaultTransfer.value = VaultTransferUiState(
                    mode = VaultTransferMode.IMPORT_PREVIEW,
                    preview = preview
                )
            } catch (error: Exception) {
                _vaultTransfer.value = _vaultTransfer.value.copy(
                    isBusy = false,
                    error = vaultErrorMessage(error)
                )
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun confirmImport() {
        val preview = _vaultTransfer.value.preview ?: return
        if (_vaultTransfer.value.isBusy) return

        _vaultTransfer.value = _vaultTransfer.value.copy(isBusy = true, error = null)
        viewModelScope.launch {
            try {
                val result = vaultBackupManager.apply(preview)
                val warningSuffix = if (result.warnings.isEmpty()) {
                    ""
                } else {
                    " ${result.warnings.size} warning(s)."
                }
                _vaultTransfer.value = VaultTransferUiState(
                    message = "Imported ${result.fieldsCreated} field(s), " +
                        "${result.contactsCreated} contact(s), " +
                        "${result.requestsCreated} request(s)." + warningSuffix
                )
            } catch (error: Exception) {
                _vaultTransfer.value = _vaultTransfer.value.copy(
                    isBusy = false,
                    error = vaultErrorMessage(error)
                )
            }
        }
    }

    fun cancelTransfer() {
        if (_vaultTransfer.value.isBusy) return
        _vaultTransfer.value = VaultTransferUiState()
    }

    private fun vaultErrorMessage(error: Throwable): String = when (error) {
        is VaultBackupWrongPassphraseException ->
            "Wrong passphrase or corrupted vault backup."
        is VaultBackupFormatException ->
            error.message ?: "Unsupported or damaged vault backup."
        is IOException ->
            error.message ?: "Could not read or write the selected file."
        else ->
            error.message ?: "Vault transfer failed."
    }

    private companion object {
        const val MIN_PASSPHRASE_LENGTH = 12
    }
}
