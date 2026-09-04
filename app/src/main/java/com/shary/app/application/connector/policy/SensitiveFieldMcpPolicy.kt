package com.shary.app.application.connector.policy

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.valueobjects.FieldValueType
import com.shary.app.core.domain.types.valueobjects.FieldValueTypes

object SensitiveFieldMcpPolicy {
    const val BLOCKED_REASON = "sensitive_field_not_mcp_shareable"
    const val TYPE_CHANGE_REASON = "sensitive_field_type_change_denied"
    const val CREATE_REASON = "sensitive_field_creation_denied"

    fun isSensitive(field: FieldDomain): Boolean =
        FieldValueTypes.isSensitive(field.value)

    fun requireReadable(field: FieldDomain) {
        require(!isSensitive(field) || field.isMcpShareable) { BLOCKED_REASON }
    }

    fun requireCreatable(requestedType: String?) {
        require(FieldValueType.fromWireName(requestedType) != FieldValueType.SENSITIVE) {
            CREATE_REASON
        }
    }

    fun requireUpdatable(field: FieldDomain, requestedType: String?) {
        val requested = FieldValueType.fromWireName(requestedType)
        val currentlySensitive = isSensitive(field)
        require(requested != FieldValueType.SENSITIVE || currentlySensitive) {
            TYPE_CHANGE_REASON
        }
        require(!currentlySensitive || requested == null || requested == FieldValueType.SENSITIVE) {
            TYPE_CHANGE_REASON
        }
        require(!currentlySensitive || field.isMcpShareable) { BLOCKED_REASON }
    }
}
