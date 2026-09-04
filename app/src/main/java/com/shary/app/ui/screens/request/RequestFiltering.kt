package com.shary.app.ui.screens.request

import com.shary.app.core.domain.models.RequestDomain

internal fun filterRequestsByQuery(
    requests: List<RequestDomain>,
    query: String
): List<RequestDomain> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return requests

    return requests.filter { request ->
        request.user.contains(normalizedQuery, ignoreCase = true) ||
            request.recipients.any { it.contains(normalizedQuery, ignoreCase = true) } ||
            request.fields.any { it.key.contains(normalizedQuery, ignoreCase = true) }
    }
}

internal fun findRequestById(
    requests: List<RequestDomain>,
    requestId: String?
): RequestDomain? {
    val stableId = requestId?.takeIf { it.isNotBlank() } ?: return null
    return requests.firstOrNull { it.id == stableId }
}
