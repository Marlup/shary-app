package com.shary.app.application.connector

data class ConnectorMutationAction(
    val type: ConnectorMutationType,
    val key: String? = null,
    val value: String? = null,
    val keyAlias: String? = null,
    val tag: String? = null,
    val email: String? = null,
    val username: String? = null
)

enum class ConnectorMutationType {
    CREATE_FIELD,
    UPDATE_FIELD,
    CREATE_USER,
    UPDATE_USER
}
