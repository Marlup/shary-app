package com.shary.app.application.email

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EmailShareKind {
    REQUEST,
    RESPONSE
}

data class EmailShareLease(
    val attachmentId: String,
    val expiresAtEpochMillis: Long,
    val kind: EmailShareKind
)

sealed interface EmailShareState {
    data object Idle : EmailShareState

    data class Preparing(val kind: EmailShareKind) : EmailShareState

    data class Prepared(val lease: EmailShareLease) : EmailShareState

    data class Launching(val lease: EmailShareLease) : EmailShareState

    data class ShareSheetOpened(val lease: EmailShareLease) : EmailShareState

    data class PreparationFailed(
        val kind: EmailShareKind,
        val message: String
    ) : EmailShareState

    data class LaunchFailed(
        val attachmentId: String,
        val message: String
    ) : EmailShareState
}

/**
 * Owns the process-local email handoff state. A share-sheet handoff is the most
 * conclusive email state available here; this model deliberately has no sent or
 * delivered state because Android's share intent does not provide either receipt.
 */
class EmailShareController {
    private val _state = MutableStateFlow<EmailShareState>(EmailShareState.Idle)
    val state: StateFlow<EmailShareState> = _state.asStateFlow()

    @Synchronized
    fun tryBeginPreparation(kind: EmailShareKind): Boolean {
        return when (_state.value) {
            is EmailShareState.Preparing,
            is EmailShareState.Prepared,
            is EmailShareState.Launching -> false

            else -> {
                _state.value = EmailShareState.Preparing(kind)
                true
            }
        }
    }

    @Synchronized
    fun prepared(lease: EmailShareLease): Boolean {
        val current = _state.value as? EmailShareState.Preparing ?: return false
        if (current.kind != lease.kind) return false
        _state.value = EmailShareState.Prepared(lease)
        return true
    }

    @Synchronized
    fun preparationFailed(kind: EmailShareKind, message: String): Boolean {
        val current = _state.value as? EmailShareState.Preparing ?: return false
        if (current.kind != kind) return false
        _state.value = EmailShareState.PreparationFailed(kind, message)
        return true
    }

    @Synchronized
    fun claimPrepared(attachmentId: String): Boolean {
        val current = _state.value as? EmailShareState.Prepared ?: return false
        if (current.lease.attachmentId != attachmentId) return false
        _state.value = EmailShareState.Launching(current.lease)
        return true
    }

    @Synchronized
    fun shareSheetOpened(attachmentId: String): Boolean {
        val current = _state.value as? EmailShareState.Launching ?: return false
        if (current.lease.attachmentId != attachmentId) return false
        _state.value = EmailShareState.ShareSheetOpened(current.lease)
        return true
    }

    @Synchronized
    fun launchFailed(attachmentId: String, message: String): Boolean {
        val lease = when (val current = _state.value) {
            is EmailShareState.Prepared -> current.lease
            is EmailShareState.Launching -> current.lease
            else -> return false
        }
        if (lease.attachmentId != attachmentId) return false
        _state.value = EmailShareState.LaunchFailed(attachmentId, message)
        return true
    }
}
