package com.shary.app.viewmodels.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.application.email.EmailShareController
import com.shary.app.application.email.EmailShareKind
import com.shary.app.application.email.EmailShareLease
import com.shary.app.application.email.EmailShareState
import com.shary.app.core.domain.interfaces.services.EmailService
import com.shary.app.core.domain.interfaces.services.PreparedEmailShare
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.utils.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailViewModel @Inject constructor(
    private val emailService: EmailService
) : ViewModel() {
    private val controller = EmailShareController()
    val shareState: StateFlow<EmailShareState> = controller.state

    @Volatile
    private var pendingShare: PreparedEmailShare? = null

    private val _events = MutableSharedFlow<String>() // success/error messages
    val events: SharedFlow<String> = _events

    fun sendRequest(fields: List<FieldDomain>, emails: List<UserDomain>) {
        prepareShare(EmailShareKind.REQUEST) {
            emailService.prepareRequestShare(fields, emails)
        }
    }

    fun sendResponse(fields: List<FieldDomain>, emails: List<UserDomain>) {
        prepareShare(EmailShareKind.RESPONSE) {
            emailService.prepareResponseShare(fields, emails)
        }
    }

    /**
     * Claims a prepared share exactly once and moves state to Launching before
     * the caller invokes Android's chooser. This prevents recomposition from
     * opening the same chooser repeatedly.
     */
    @Synchronized
    fun claimPreparedShare(attachmentId: String): PreparedEmailShare? {
        val prepared = pendingShare?.takeIf { it.attachmentId == attachmentId } ?: return null
        if (!controller.claimPrepared(attachmentId)) return null
        pendingShare = null
        return prepared
    }

    fun onShareSheetOpened(attachmentId: String) {
        viewModelScope.launch {
            if (emailService.retainPreparedShare(attachmentId)) {
                controller.shareSheetOpened(attachmentId)
            } else {
                val message = "The email attachment is no longer available. Prepare it again."
                controller.launchFailed(attachmentId, message)
                _events.emit(message)
            }
        }
    }

    fun onChooserLaunchFailed(attachmentId: String, throwable: Throwable) {
        viewModelScope.launch {
            emailService.discardPreparedShare(attachmentId)
            AppLogger.error("EmailViewModel", "event=email_share_sheet_launch_failed", throwable)
            val message = "Could not open a compatible sharing app."
            controller.launchFailed(attachmentId, message)
            _events.emit(message)
        }
    }

    private fun prepareShare(
        kind: EmailShareKind,
        prepare: suspend () -> Result<PreparedEmailShare>
    ) {
        if (!controller.tryBeginPreparation(kind)) return

        viewModelScope.launch {
            runCatching { prepare().getOrThrow() }
                .onSuccess { prepared ->
                    val lease = EmailShareLease(
                        attachmentId = prepared.attachmentId,
                        expiresAtEpochMillis = prepared.expiresAtEpochMillis,
                        kind = kind
                    )
                    pendingShare = prepared
                    if (!controller.prepared(lease)) {
                        pendingShare = null
                        emailService.discardPreparedShare(prepared.attachmentId)
                    }
                }
                .onFailure {
                    val message = "Could not prepare the email attachment."
                    controller.preparationFailed(kind, message)
                    _events.emit(message)
                }
        }
    }
}
