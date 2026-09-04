package com.shary.app.infrastructure.connector.mcp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharyMcpHttpTransportTest {

    @Test
    fun post_accepts_case_insensitive_local_headers_and_adds_transport_headers() = runBlocking {
        var delegated = false
        val transport = SharyMcpHttpTransport { _, headers ->
            delegated = true
            assertEquals("Bearer test", headers.headerValue("Authorization"))
            McpTransportResponse(200, "application/json", """{"ok":true}""")
        }

        val response = transport.handle(
            request(
                headers = mapOf(
                    "content-type" to "application/json; charset=utf-8",
                    "accept" to "application/json, text/event-stream",
                    "origin" to "http://127.0.0.1:8765",
                    "authorization" to "Bearer test",
                    "mcp-protocol-version" to SHARY_MCP_PROTOCOL_VERSION
                )
            )
        )

        assertTrue(delegated)
        assertEquals(200, response.statusCode)
        assertEquals("no-store", response.headers["Cache-Control"])
        assertEquals(SHARY_MCP_PROTOCOL_VERSION, response.headers["MCP-Protocol-Version"])
    }

    @Test
    fun post_rejects_non_local_origin_before_delegating() = runBlocking {
        var delegated = false
        val transport = SharyMcpHttpTransport { _, _ ->
            delegated = true
            McpTransportResponse(200)
        }

        val response = transport.handle(
            request(headers = baseHeaders() + ("Origin" to "https://example.com"))
        )

        assertFalse(delegated)
        assertEquals(403, response.statusCode)
    }

    @Test
    fun post_rejects_unsupported_protocol_version() = runBlocking {
        val transport = SharyMcpHttpTransport { _, _ -> McpTransportResponse(200) }

        val response = transport.handle(
            request(headers = baseHeaders() + ("MCP-Protocol-Version" to "2024-01-01"))
        )

        assertEquals(400, response.statusCode)
        assertTrue(response.body.orEmpty().contains("Unsupported MCP-Protocol-Version"))
    }

    @Test
    fun post_requires_json_content_type_and_acceptable_json_response() = runBlocking {
        val transport = SharyMcpHttpTransport { _, _ -> McpTransportResponse(200) }

        val missingContentType = transport.handle(request(headers = emptyMap()))
        val unacceptableResponse = transport.handle(
            request(headers = baseHeaders() + ("Accept" to "text/event-stream"))
        )

        assertEquals(415, missingContentType.statusCode)
        assertEquals(406, unacceptableResponse.statusCode)
    }

    @Test
    fun get_and_delete_are_explicitly_unsupported_for_stateless_server() = runBlocking {
        val transport = SharyMcpHttpTransport { _, _ -> McpTransportResponse(200) }

        val getResponse = transport.handle(request(method = "GET", headers = emptyMap()))
        val deleteResponse = transport.handle(request(method = "DELETE", headers = emptyMap()))

        assertEquals(405, getResponse.statusCode)
        assertEquals(405, deleteResponse.statusCode)
        assertEquals("POST", getResponse.headers["Allow"])
        assertEquals("POST", deleteResponse.headers["Allow"])
    }

    @Test
    fun content_length_validation_rejects_negative_and_oversized_requests() {
        assertEquals(400, validateMcpContentLength(-1))
        assertEquals(413, validateMcpContentLength(SHARY_MCP_MAX_BODY_BYTES + 1))
        assertEquals(null, validateMcpContentLength(SHARY_MCP_MAX_BODY_BYTES))
    }

    private fun request(
        method: String = "POST",
        headers: Map<String, String> = baseHeaders()
    ) = SharyMcpHttpRequest(
        method = method,
        path = SHARY_MCP_ENDPOINT_PATH,
        headers = headers,
        body = """{"jsonrpc":"2.0","id":1,"method":"ping"}"""
    )

    private fun baseHeaders(): Map<String, String> = mapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json"
    )
}
