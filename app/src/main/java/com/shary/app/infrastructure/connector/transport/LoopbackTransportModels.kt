package com.shary.app.infrastructure.connector.transport

data class LoopbackRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val body: Map<String, Any?> = emptyMap()
)

data class LoopbackResponse(
    val statusCode: Int,
    val body: Any
)
