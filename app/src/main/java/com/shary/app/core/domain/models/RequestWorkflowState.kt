package com.shary.app.core.domain.models

import com.shary.app.core.domain.types.enums.RequestListMode

data class ResponseDraft(
    val requestId: String,
    val requesterId: String,
    val requesterAddress: String,
    val matchedFieldKeys: List<String> = emptyList(),
    val completed: Boolean = false
)

data class RequestedFieldDraft(
    val key: String,
    val keyAlias: String = ""
)

data class OutgoingRequestDraft(
    val requestedFields: List<RequestedFieldDraft> = emptyList(),
    val requestId: String = "",
    val recipientAddresses: List<String> = emptyList()
)

data class RequestWorkflowState(
    val mode: RequestListMode = RequestListMode.RECEIVED,
    val activeReceivedRequest: RequestDomain? = null,
    val responseDraft: ResponseDraft? = null,
    val outgoingRequestDraft: OutgoingRequestDraft = OutgoingRequestDraft()
)

fun RequestWorkflowState.hasPersistableDraftIdentity(): Boolean =
    outgoingRequestDraft.requestId.isNotBlank() || responseDraft?.requestId?.isNotBlank() == true
