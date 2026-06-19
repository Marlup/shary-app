package com.shary.app.core.domain.types.valueobjects

object ConnectorErrorCodes {
    const val UNAUTHORIZED_SCOPE = "unauthorized_scope"
    const val SESSION_EXPIRED = "session_expired"
    const val POLICY_DENIED = "policy_denied"
    const val CONFIRMATION_REQUIRED = "confirmation_required"
    const val RATE_LIMITED = "rate_limited"
    const val VALIDATION_FAILED = "validation_failed"
    const val NOT_FOUND = "not_found"
    const val INTERNAL_ERROR = "internal_error"
}
