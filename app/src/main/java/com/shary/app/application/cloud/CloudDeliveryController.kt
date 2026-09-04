package com.shary.app.application.cloud

import com.shary.app.core.domain.types.enums.StatusDataSentDb
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CloudDeliveryKind {
    DATA,
    REQUEST
}

enum class CloudDeliverySummary {
    SENT,
    PARTIAL,
    FAILED
}

data class CloudRecipientOutcome(
    val address: String,
    val status: StatusDataSentDb
) {
    val acknowledged: Boolean
        get() = status.isAcknowledged()
}

data class CloudDeliveryReceipt(
    val kind: CloudDeliveryKind,
    val transactionId: String?,
    val summary: CloudDeliverySummary,
    val outcomes: List<CloudRecipientOutcome>
) {
    val failedRecipients: List<String>
        get() = outcomes.filterNot(CloudRecipientOutcome::acknowledged).map(CloudRecipientOutcome::address)
}

sealed interface CloudDeliveryState {
    data object Review : CloudDeliveryState

    data class Sending internal constructor(
        internal val attempt: CloudDeliveryAttempt
    ) : CloudDeliveryState {
        val kind: CloudDeliveryKind
            get() = attempt.kind
        val attemptedRecipients: List<String>
            get() = attempt.attemptedRecipients
    }

    data class Receipt(val receipt: CloudDeliveryReceipt) : CloudDeliveryState
}

data class CloudDeliveryAttempt internal constructor(
    internal val operationId: Long,
    val kind: CloudDeliveryKind,
    val transactionId: String?,
    val expectedRecipients: List<String>,
    val attemptedRecipients: List<String>,
    internal val previousOutcomes: List<CloudRecipientOutcome>
)

/**
 * Process-local delivery state for one review route.
 *
 * Claims happen synchronously, before a caller launches asynchronous transport
 * work. This makes rapid initial and retry taps idempotent while retaining a
 * terminal receipt until the caller explicitly resets it.
 */
class CloudDeliveryController {
    private val lock = Any()
    private var nextOperationId = 0L
    private val _state = MutableStateFlow<CloudDeliveryState>(CloudDeliveryState.Review)

    val state: StateFlow<CloudDeliveryState> = _state.asStateFlow()

    fun begin(
        kind: CloudDeliveryKind,
        recipients: List<String>,
        transactionId: String? = null
    ): CloudDeliveryAttempt? = synchronized(lock) {
        if (_state.value != CloudDeliveryState.Review) return@synchronized null

        val expectedRecipients = normalizeRecipients(recipients)
        if (expectedRecipients.isEmpty()) return@synchronized null
        val normalizedTransactionId = transactionId.normalizedTransactionId()
        if (kind == CloudDeliveryKind.REQUEST && normalizedTransactionId == null) {
            return@synchronized null
        }

        createAttempt(
            kind = kind,
            transactionId = normalizedTransactionId,
            expectedRecipients = expectedRecipients,
            attemptedRecipients = expectedRecipients,
            previousOutcomes = emptyList()
        )
    }

    fun retryFailed(
        kind: CloudDeliveryKind,
        transactionId: String? = null
    ): CloudDeliveryAttempt? = synchronized(lock) {
        val receipt = (_state.value as? CloudDeliveryState.Receipt)?.receipt
            ?: return@synchronized null
        if (receipt.kind != kind || receipt.summary == CloudDeliverySummary.SENT) {
            return@synchronized null
        }
        if (receipt.transactionId != transactionId.normalizedTransactionId()) {
            return@synchronized null
        }

        val failedRecipients = receipt.failedRecipients
        if (failedRecipients.isEmpty()) return@synchronized null

        createAttempt(
            kind = receipt.kind,
            transactionId = receipt.transactionId,
            expectedRecipients = receipt.outcomes.map(CloudRecipientOutcome::address),
            attemptedRecipients = failedRecipients,
            previousOutcomes = receipt.outcomes
        )
    }

    fun restore(receipt: CloudDeliveryReceipt): Boolean = synchronized(lock) {
        if (_state.value != CloudDeliveryState.Review || receipt.outcomes.isEmpty()) {
            return@synchronized false
        }
        if (receipt.kind == CloudDeliveryKind.REQUEST && receipt.transactionId.normalizedTransactionId() == null) {
            return@synchronized false
        }

        val normalizedOutcomes = normalizeOutcomeOrder(receipt.outcomes)
        if (normalizedOutcomes.isEmpty()) return@synchronized false
        _state.value = CloudDeliveryState.Receipt(
            receipt.copy(
                transactionId = receipt.transactionId.normalizedTransactionId(),
                summary = summarize(normalizedOutcomes),
                outcomes = normalizedOutcomes
            )
        )
        true
    }

    fun complete(
        attempt: CloudDeliveryAttempt,
        result: Map<String, StatusDataSentDb>
    ): CloudDeliveryReceipt? = synchronized(lock) {
        val activeAttempt = (_state.value as? CloudDeliveryState.Sending)?.attempt
        if (activeAttempt?.operationId != attempt.operationId) return@synchronized null

        val resultByRecipient = result.entries.associate { (address, status) ->
            address.normalizedAddress() to status
        }
        val outcomesByRecipient = attempt.previousOutcomes.associateByTo(linkedMapOf()) {
            it.address.normalizedAddress()
        }

        attempt.attemptedRecipients.forEach { address ->
            outcomesByRecipient[address.normalizedAddress()] = CloudRecipientOutcome(
                address = expectedDisplayAddress(attempt.expectedRecipients, address),
                status = resultByRecipient[address.normalizedAddress()] ?: StatusDataSentDb.ERROR
            )
        }

        val outcomes = attempt.expectedRecipients.map { address ->
            outcomesByRecipient[address.normalizedAddress()]
                ?: CloudRecipientOutcome(address, StatusDataSentDb.ERROR)
        }
        val summary = summarize(outcomes)
        val receipt = CloudDeliveryReceipt(
            kind = attempt.kind,
            transactionId = attempt.transactionId,
            summary = summary,
            outcomes = outcomes
        )
        _state.value = CloudDeliveryState.Receipt(receipt)
        receipt
    }

    fun fail(attempt: CloudDeliveryAttempt): CloudDeliveryReceipt? = complete(attempt, emptyMap())

    fun reset(): Boolean = synchronized(lock) {
        if (_state.value is CloudDeliveryState.Sending) return@synchronized false
        _state.value = CloudDeliveryState.Review
        true
    }

    private fun createAttempt(
        kind: CloudDeliveryKind,
        transactionId: String?,
        expectedRecipients: List<String>,
        attemptedRecipients: List<String>,
        previousOutcomes: List<CloudRecipientOutcome>
    ): CloudDeliveryAttempt {
        nextOperationId += 1
        val attempt = CloudDeliveryAttempt(
            operationId = nextOperationId,
            kind = kind,
            transactionId = transactionId,
            expectedRecipients = expectedRecipients,
            attemptedRecipients = attemptedRecipients,
            previousOutcomes = previousOutcomes
        )
        _state.value = CloudDeliveryState.Sending(attempt)
        return attempt
    }
}

private fun normalizeOutcomeOrder(outcomes: List<CloudRecipientOutcome>): List<CloudRecipientOutcome> {
    val seen = mutableSetOf<String>()
    return outcomes.mapNotNull { outcome ->
        val address = outcome.address.trim()
        val normalizedAddress = address.normalizedAddress()
        outcome.copy(address = address).takeIf {
            normalizedAddress.isNotEmpty() && seen.add(normalizedAddress)
        }
    }
}

private fun summarize(outcomes: List<CloudRecipientOutcome>): CloudDeliverySummary {
    val acknowledgedCount = outcomes.count(CloudRecipientOutcome::acknowledged)
    return when {
        acknowledgedCount == outcomes.size -> CloudDeliverySummary.SENT
        acknowledgedCount == 0 -> CloudDeliverySummary.FAILED
        else -> CloudDeliverySummary.PARTIAL
    }
}

private fun normalizeRecipients(recipients: List<String>): List<String> {
    val seen = mutableSetOf<String>()
    return recipients.mapNotNull { address ->
        val displayAddress = address.trim()
        val normalizedAddress = displayAddress.normalizedAddress()
        displayAddress.takeIf { normalizedAddress.isNotEmpty() && seen.add(normalizedAddress) }
    }
}

private fun expectedDisplayAddress(expectedRecipients: List<String>, address: String): String {
    val normalizedAddress = address.normalizedAddress()
    return expectedRecipients.firstOrNull { it.normalizedAddress() == normalizedAddress } ?: address.trim()
}

private fun String.normalizedAddress(): String = trim().lowercase(Locale.ROOT)

private fun String?.normalizedTransactionId(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun StatusDataSentDb.isAcknowledged(): Boolean = when (this) {
    StatusDataSentDb.SUCCESS,
    StatusDataSentDb.STORED,
    StatusDataSentDb.EXISTS -> true
    else -> false
}
