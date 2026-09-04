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
    const val SENSITIVE_FIELD_NOT_MCP_SHAREABLE = "sensitive_field_not_mcp_shareable"
    const val SENSITIVE_FIELD_TYPE_CHANGE_DENIED = "sensitive_field_type_change_denied"
    const val SENSITIVE_FIELD_CREATION_DENIED = "sensitive_field_creation_denied"
}
