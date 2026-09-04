package com.shary.app.ui.screens.summary

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestWorkflowState
import com.shary.app.core.domain.models.UserDomain
import java.util.Locale

internal data class SummaryFieldReviewState(
    val fields: List<FieldDomain>,
    val recipients: List<UserDomain>,
    val responseStateMissing: Boolean,
    val responseCompleted: Boolean
)

internal fun resolveSummaryFieldReviewState(
    cachedFields: List<FieldDomain>,
    generalRecipients: List<UserDomain>,
    workflow: RequestWorkflowState,
    responseRequestId: String?
): SummaryFieldReviewState {
    val routeRequestId = responseRequestId?.trim().orEmpty()
    if (routeRequestId.isEmpty()) {
        return SummaryFieldReviewState(
            fields = cachedFields,
            recipients = generalRecipients,
            responseStateMissing = false,
            responseCompleted = false
        )
    }

    val response = workflow.responseDraft
    val activeRequest = workflow.activeReceivedRequest
    if (
        response == null ||
        activeRequest == null ||
        response.requestId != routeRequestId ||
        activeRequest.id != routeRequestId ||
        response.requesterId.isBlank() ||
        response.requesterAddress.isBlank() ||
        response.matchedFieldKeys.isEmpty()
    ) {
        return missingResponseState()
    }

    val fieldsByKey = cachedFields.associateBy { it.key.normalizedKey() }
    val expectedKeys = response.matchedFieldKeys
        .map(String::normalizedKey)
        .filter(String::isNotBlank)
        .distinct()
    val resolvedFields = expectedKeys.mapNotNull(fieldsByKey::get)
    if (expectedKeys.isEmpty() || resolvedFields.size != expectedKeys.size) {
        return missingResponseState()
    }

    return SummaryFieldReviewState(
        fields = resolvedFields,
        recipients = listOf(
            UserDomain(
                username = response.requesterAddress,
                email = response.requesterAddress
            )
        ),
        responseStateMissing = false,
        responseCompleted = response.completed
    )
}

private fun missingResponseState() = SummaryFieldReviewState(
    fields = emptyList(),
    recipients = emptyList(),
    responseStateMissing = true,
    responseCompleted = false
)

private fun String.normalizedKey(): String = trim().lowercase(Locale.ROOT)
