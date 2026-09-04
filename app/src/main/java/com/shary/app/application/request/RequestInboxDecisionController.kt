package com.shary.app.application.request

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RequestInboxDecision {
    ACCEPT,
    REJECT
}

data class RequestInboxDecisionReceipt(
    val verification: String,
    val decision: RequestInboxDecision,
    val localRequestAvailable: Boolean,
    val requestedKeyCount: Int,
    val backendAcknowledged: Boolean
)

sealed interface RequestInboxDecisionState {
    data object Idle : RequestInboxDecisionState

    data class Processing(
        val verification: String,
        val decision: RequestInboxDecision
    ) : RequestInboxDecisionState

    data class Failed(
        val verification: String,
        val decision: RequestInboxDecision,
        val error: Throwable,
        val localRequestAvailable: Boolean,
        val requestedKeyCount: Int
    ) : RequestInboxDecisionState

    data class Succeeded(
        val receipt: RequestInboxDecisionReceipt
    ) : RequestInboxDecisionState
}

/**
 * Owns the single in-flight request-inbox decision for a ViewModel instance.
 *
 * [tryStart] performs the claim before a coroutine is launched. A failed attempt can only be
 * retried with the same verification and decision because a lost response may have finalized the
 * operation remotely.
 */
class RequestInboxDecisionController {
    private val _state = MutableStateFlow<RequestInboxDecisionState>(
        RequestInboxDecisionState.Idle
    )
    val state: StateFlow<RequestInboxDecisionState> = _state.asStateFlow()

    fun tryStart(verification: String, decision: RequestInboxDecision): Boolean {
        val normalizedVerification = verification.trim()
        if (normalizedVerification.isEmpty()) return false

        while (true) {
            val current = _state.value
            val next = when (current) {
                RequestInboxDecisionState.Idle -> RequestInboxDecisionState.Processing(
                    verification = normalizedVerification,
                    decision = decision
                )

                is RequestInboxDecisionState.Failed -> {
                    if (
                        current.verification != normalizedVerification ||
                        current.decision != decision
                    ) {
                        return false
                    }
                    RequestInboxDecisionState.Processing(
                        verification = normalizedVerification,
                        decision = decision
                    )
                }

                is RequestInboxDecisionState.Processing,
                is RequestInboxDecisionState.Succeeded -> return false
            }

            if (_state.compareAndSet(current, next)) return true
        }
    }

    fun fail(
        verification: String,
        decision: RequestInboxDecision,
        error: Throwable,
        localRequestAvailable: Boolean = false,
        requestedKeyCount: Int = 0
    ): Boolean {
        val normalizedVerification = verification.trim()
        while (true) {
            val current = _state.value as? RequestInboxDecisionState.Processing ?: return false
            if (
                current.verification != normalizedVerification ||
                current.decision != decision
            ) {
                return false
            }
            val next = RequestInboxDecisionState.Failed(
                verification = normalizedVerification,
                decision = decision,
                error = error,
                localRequestAvailable = localRequestAvailable,
                requestedKeyCount = requestedKeyCount.coerceAtLeast(0)
            )
            if (_state.compareAndSet(current, next)) return true
        }
    }

    fun succeed(receipt: RequestInboxDecisionReceipt): Boolean {
        val normalizedReceipt = receipt.copy(
            verification = receipt.verification.trim(),
            requestedKeyCount = receipt.requestedKeyCount.coerceAtLeast(0)
        )
        while (true) {
            val current = _state.value as? RequestInboxDecisionState.Processing ?: return false
            if (
                current.verification != normalizedReceipt.verification ||
                current.decision != normalizedReceipt.decision
            ) {
                return false
            }
            if (
                _state.compareAndSet(
                    current,
                    RequestInboxDecisionState.Succeeded(normalizedReceipt)
                )
            ) {
                return true
            }
        }
    }

    fun consumeReceipt(): Boolean {
        while (true) {
            val current = _state.value as? RequestInboxDecisionState.Succeeded ?: return false
            if (_state.compareAndSet(current, RequestInboxDecisionState.Idle)) return true
        }
    }
}
