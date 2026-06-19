package com.shary.app.core.domain.types.valueobjects

data class ConnectorSuccessEnvelope<T>(
    val requestId: String,
    val schemaVersion: String,
    val timestamp: Long,
    val data: T
)

data class ConnectorErrorEnvelope(
    val requestId: String,
    val schemaVersion: String,
    val error: ConnectorErrorPayload
)

data class ConnectorErrorPayload(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val retryable: Boolean
)
