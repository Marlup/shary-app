package com.shary.app.infrastructure.connector.mcp

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatibility facade for the local HTTP server and existing callers.
 *
 * New transports should depend on [SharyMcpDispatcher] directly.
 */
@Singleton
class SharyMcpServer @Inject constructor(
    private val dispatcher: SharyMcpDispatcher
) {
    suspend fun handleRawJson(
        rawBody: String,
        headers: Map<String, String> = emptyMap()
    ): McpTransportResponse = dispatcher.dispatch(rawBody, headers)
}
