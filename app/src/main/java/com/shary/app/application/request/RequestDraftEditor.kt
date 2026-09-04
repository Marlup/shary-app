package com.shary.app.application.request

import com.shary.app.core.domain.models.RequestedFieldDraft

enum class RequestDraftFieldAddOutcome {
    ADDED,
    DUPLICATE,
    INVALID
}

data class RequestDraftFieldAddTransition(
    val requestedFields: List<RequestedFieldDraft>,
    val outcome: RequestDraftFieldAddOutcome
)

fun addRequestedFieldToDraft(
    current: List<RequestedFieldDraft>,
    candidate: RequestedFieldDraft
): RequestDraftFieldAddTransition {
    val normalized = RequestedFieldDraft(
        key = candidate.key.trim(),
        keyAlias = candidate.keyAlias.trim()
    )
    if (normalized.key.isBlank()) {
        return RequestDraftFieldAddTransition(current, RequestDraftFieldAddOutcome.INVALID)
    }
    if (current.any { it.key.trim().equals(normalized.key, ignoreCase = true) }) {
        return RequestDraftFieldAddTransition(current, RequestDraftFieldAddOutcome.DUPLICATE)
    }
    return RequestDraftFieldAddTransition(
        requestedFields = current + normalized,
        outcome = RequestDraftFieldAddOutcome.ADDED
    )
}
