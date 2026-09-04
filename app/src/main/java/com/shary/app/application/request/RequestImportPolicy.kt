package com.shary.app.application.request

import java.util.Locale

/**
 * Verifies that the address embedded in decrypted request JSON belongs to the
 * authenticated sender from the cloud envelope.
 */
fun requireAuthenticatedRequester(
    requesterAddress: String,
    authenticatedRequesterId: String,
    addressHasher: (String) -> String
): String {
    val normalizedAddress = requesterAddress.trim().lowercase(Locale.ROOT)
    val normalizedRequesterId = authenticatedRequesterId.trim()
    require(normalizedAddress.isNotEmpty()) { "Request has no requester address" }
    require(normalizedRequesterId.isNotEmpty()) { "Request has no authenticated requester" }
    if (addressHasher(normalizedAddress) != normalizedRequesterId) {
        throw SecurityException("Request sender identity does not match its requester address")
    }
    return normalizedRequesterId
}

fun normalizeRequestedFieldKeys(keys: List<String>): List<String> = keys
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinctBy { it.lowercase(Locale.ROOT) }
