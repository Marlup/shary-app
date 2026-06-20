package com.shary.app.infrastructure.connector.mcp

data class McpConnectionContext(
    val headers: Map<String, String> = emptyMap()
)

data class McpTransportResponse(
    val statusCode: Int,
    val contentType: String? = null,
    val body: String? = null,
    val headers: Map<String, String> = emptyMap()
)

internal data class McpJsonRpcRequest(
    val id: Any?,
    val method: String,
    val params: Map<String, Any?>,
    val isNotification: Boolean
)

internal object McpErrorCodes {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
}
