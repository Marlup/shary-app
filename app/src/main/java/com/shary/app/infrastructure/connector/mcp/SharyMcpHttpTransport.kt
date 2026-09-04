package com.shary.app.infrastructure.connector.mcp

internal const val SHARY_MCP_PROTOCOL_VERSION = "2025-06-18"
internal const val SHARY_MCP_ENDPOINT_PATH = "/mcp"
internal const val SHARY_MCP_MAX_BODY_BYTES = 1024 * 1024

internal data class SharyMcpHttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String
)

internal class SharyMcpHttpTransport(
    private val handleMcp: suspend (String, Map<String, String>) -> McpTransportResponse
) {
    suspend fun handle(request: SharyMcpHttpRequest): McpTransportResponse {
        if (request.path != SHARY_MCP_ENDPOINT_PATH) {
            return textResponse(404, "Not Found")
        }
        if (!isOriginAllowed(request.headers.headerValue("Origin"))) {
            return textResponse(403, "Forbidden origin")
        }

        return when (request.method.uppercase()) {
            "POST" -> handlePost(request)
            "GET" -> textResponse(
                statusCode = 405,
                body = "SSE stream is not supported by this Shary MCP server",
                headers = mapOf("Allow" to "POST")
            )
            "DELETE" -> textResponse(
                statusCode = 405,
                body = "This stateless Shary MCP server has no HTTP session to delete",
                headers = mapOf("Allow" to "POST")
            )
            else -> textResponse(
                statusCode = 405,
                body = "Method Not Allowed",
                headers = mapOf("Allow" to "POST")
            )
        }
    }

    private suspend fun handlePost(request: SharyMcpHttpRequest): McpTransportResponse {
        val contentType = request.headers.headerValue("Content-Type")
        if (contentType == null || !contentType.substringBefore(';').trim().equals("application/json", true)) {
            return textResponse(415, "Content-Type must be application/json")
        }

        val requestedProtocol = request.headers.headerValue("MCP-Protocol-Version")
        if (requestedProtocol != null && requestedProtocol != SHARY_MCP_PROTOCOL_VERSION) {
            return textResponse(
                statusCode = 400,
                body = "Unsupported MCP-Protocol-Version: $requestedProtocol"
            )
        }

        val accept = request.headers.headerValue("Accept")
        if (accept != null && !accept.accepts("application/json")) {
            return textResponse(406, "Accept must allow application/json")
        }

        val response = handleMcp(request.body, request.headers)
        return response.copy(
            headers = response.headers + mapOf(
                "Cache-Control" to "no-store",
                "MCP-Protocol-Version" to SHARY_MCP_PROTOCOL_VERSION
            )
        )
    }

    private fun String.accepts(mediaType: String): Boolean =
        split(',').any { candidate ->
            val normalized = candidate.substringBefore(';').trim()
            normalized == "*/*" || normalized.equals(mediaType, ignoreCase = true)
        }

    private fun isOriginAllowed(origin: String?): Boolean {
        if (origin.isNullOrBlank()) return true
        return origin == "http://localhost" ||
            origin == "https://localhost" ||
            origin.startsWith("http://localhost:") ||
            origin.startsWith("https://localhost:") ||
            origin == "http://127.0.0.1" ||
            origin == "https://127.0.0.1" ||
            origin.startsWith("http://127.0.0.1:") ||
            origin.startsWith("https://127.0.0.1:")
    }

    private fun textResponse(
        statusCode: Int,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): McpTransportResponse = McpTransportResponse(
        statusCode = statusCode,
        contentType = "text/plain; charset=utf-8",
        body = body,
        headers = headers + mapOf("Cache-Control" to "no-store")
    )
}

internal fun Map<String, String>.headerValue(name: String): String? =
    entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

internal fun validateMcpContentLength(contentLength: Int): Int? = when {
    contentLength < 0 -> 400
    contentLength > SHARY_MCP_MAX_BODY_BYTES -> 413
    else -> null
}
