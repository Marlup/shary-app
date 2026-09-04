package com.shary.app.application.request

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.OutgoingRequestDraft
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestedFieldDraft
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.ResponseDraft
import com.shary.app.core.domain.types.enums.RequestListMode

sealed interface RequestWorkflowAction {
    data class SelectReceivedRequest(val request: RequestDomain) : RequestWorkflowAction

    data class SetResponseMatches(
        val requestId: String,
        val matchedFieldKeys: List<String>
    ) : RequestWorkflowAction

    data class SetOutgoingFields(
        val fields: List<FieldDomain>,
        val candidateRequestId: String
    ) : RequestWorkflowAction

    data class SetOutgoingRecipients(
        val recipientAddresses: List<String>,
        val candidateRequestId: String
    ) : RequestWorkflowAction

    data class ChangeMode(val mode: RequestListMode) : RequestWorkflowAction

    data class CompleteResponse(val requestId: String) : RequestWorkflowAction

    data object ClearReceivedWorkflow : RequestWorkflowAction
    data object ClearOutgoingDraft : RequestWorkflowAction
    data object ClearSession : RequestWorkflowAction
}

fun reduceRequestWorkflow(
    current: RequestWorkflowState,
    action: RequestWorkflowAction
): RequestWorkflowState = when (action) {
    is RequestWorkflowAction.SelectReceivedRequest -> {
        val requestId = action.request.id.trim()
        val requesterId = action.request.requesterId.trim()
        val requesterAddress = action.request.user.trim().lowercase()
        if (
            action.request.responded ||
            requestId.isBlank() ||
            requesterId.isBlank() ||
            requesterAddress.isBlank()
        ) {
            current.copy(
                activeReceivedRequest = null,
                responseDraft = null
            )
        } else {
            current.copy(
                mode = RequestListMode.RECEIVED,
                activeReceivedRequest = action.request,
                responseDraft = ResponseDraft(
                    requestId = requestId,
                    requesterId = requesterId,
                    requesterAddress = requesterAddress
                )
            )
        }
    }

    is RequestWorkflowAction.SetResponseMatches -> {
        val response = current.responseDraft
        if (response == null || response.requestId != action.requestId.trim()) {
            current
        } else {
            current.copy(
                responseDraft = response.copy(
                    matchedFieldKeys = action.matchedFieldKeys
                        .map { it.trim().lowercase() }
                        .filter { it.isNotBlank() }
                        .distinct()
                )
            )
        }
    }

    is RequestWorkflowAction.SetOutgoingFields -> {
        val requestedFields = action.fields
            .map { field ->
                RequestedFieldDraft(
                    key = field.key.trim(),
                    keyAlias = field.keyAlias.trim()
                )
            }
            .filter { it.key.isNotBlank() }
            .distinctBy { it.key.lowercase() }
        val currentDraft = current.outgoingRequestDraft
        val requestId = when {
            requestedFields.isEmpty() -> ""
            requestedFields == currentDraft.requestedFields && currentDraft.requestId.isNotBlank() ->
                currentDraft.requestId
            else -> action.candidateRequestId.trim()
        }
        current.copy(
            outgoingRequestDraft = OutgoingRequestDraft(
                requestedFields = requestedFields,
                requestId = requestId,
                recipientAddresses = currentDraft.recipientAddresses
            )
        )
    }

    is RequestWorkflowAction.SetOutgoingRecipients -> {
        val recipients = action.recipientAddresses
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
        val currentDraft = current.outgoingRequestDraft
        val requestId = when {
            currentDraft.requestedFields.isEmpty() || recipients.isEmpty() -> currentDraft.requestId
            recipients == currentDraft.recipientAddresses && currentDraft.requestId.isNotBlank() ->
                currentDraft.requestId
            else -> action.candidateRequestId.trim()
        }
        current.copy(
            outgoingRequestDraft = currentDraft.copy(
                requestId = requestId,
                recipientAddresses = recipients
            )
        )
    }

    is RequestWorkflowAction.ChangeMode -> when (action.mode) {
        RequestListMode.RECEIVED -> current.copy(mode = action.mode)
        RequestListMode.SENT -> current.copy(
            mode = action.mode,
            activeReceivedRequest = null,
            responseDraft = null
        )
    }

    is RequestWorkflowAction.CompleteResponse -> {
        val requestId = action.requestId.trim()
        val active = current.activeReceivedRequest
        val response = current.responseDraft
        if (active?.id != requestId || response?.requestId != requestId) {
            current
        } else {
            current.copy(
                activeReceivedRequest = active.copy(responded = true),
                responseDraft = response.copy(completed = true)
            )
        }
    }

    RequestWorkflowAction.ClearReceivedWorkflow -> current.copy(
        activeReceivedRequest = null,
        responseDraft = null
    )

    RequestWorkflowAction.ClearOutgoingDraft -> current.copy(
        outgoingRequestDraft = OutgoingRequestDraft()
    )

    RequestWorkflowAction.ClearSession -> RequestWorkflowState()
}
