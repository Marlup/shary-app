package com.shary.app.infrastructure.connector.mcp

import com.shary.app.infrastructure.connector.transport.LoopbackRequest
import com.shary.app.infrastructure.connector.transport.LoopbackResponse
import com.shary.app.infrastructure.connector.transport.LoopbackServer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharyMcpDispatcher @Inject constructor(
    private val loopbackServer: LoopbackServer,
    private val accessController: McpAccessController
) {
    suspend fun dispatch(
        rawBody: String,
        headers: Map<String, String> = emptyMap()
    ): McpTransportResponse {
        val parsed = runCatching { McpJsonCodec.parseObject(rawBody) }
            .getOrElse {
                return jsonResponse(
                    statusCode = 400,
                    payload = jsonRpcError(null, McpErrorCodes.PARSE_ERROR, "Invalid JSON-RPC body")
                )
            }

        val method = parsed["method"]?.toString()
        val idPresent = parsed.containsKey("id")
        if (method == null) {
            return if (parsed.containsKey("result") || parsed.containsKey("error")) {
                McpTransportResponse(statusCode = 202)
            } else {
                jsonResponse(
                    statusCode = 400,
                    payload = jsonRpcError(null, McpErrorCodes.INVALID_REQUEST, "method is required")
                )
            }
        }

        val request = McpJsonRpcRequest(
            id = parsed["id"],
            method = method,
            params = parsed["params"]?.let { McpJsonCodec.mapAt(it, "params") } ?: emptyMap(),
            isNotification = !idPresent
        )

        if (request.isNotification) {
            return McpTransportResponse(statusCode = 202)
        }

        val response = runCatching {
            handleRequest(request, McpConnectionContext(headers))
        }.getOrElse { error ->
            jsonRpcError(
                id = request.id,
                code = McpErrorCodes.INTERNAL_ERROR,
                message = error.message ?: "MCP server error"
            )
        }
        return jsonResponse(statusCode = 200, payload = response)
    }

    private suspend fun handleRequest(
        request: McpJsonRpcRequest,
        context: McpConnectionContext
    ): Map<String, Any?> = when (request.method) {
        "initialize" -> jsonRpcResult(
            request.id,
            linkedMapOf(
                "protocolVersion" to SHARY_MCP_PROTOCOL_VERSION,
                "capabilities" to mapOf(
                    "tools" to mapOf("listChanged" to false)
                ),
                "serverInfo" to mapOf(
                    "name" to "shary-mcp",
                    "title" to "Shary MCP Server",
                    "version" to "0.1.0"
                ),
                "instructions" to "Use this server only with a paired Shary connector. Send Authorization: Bearer <token> on MCP HTTP requests. Mutating tools require idempotency_key and may require confirm=true."
            )
        )

        "ping" -> jsonRpcResult(request.id, emptyMap<String, Any?>())

        "tools/list" -> jsonRpcResult(
            request.id,
            mapOf("tools" to toolDefinitions())
        )

        "tools/call" -> when (val decision = accessController.evaluate(context.headers)) {
            McpAccessDecision.Allowed -> handleToolCall(request, context)
            is McpAccessDecision.Denied -> jsonRpcResult(
                request.id,
                toolErrorResult(
                    code = decision.code,
                    message = decision.message,
                    metadata = decision.metadata
                )
            )
        }

        else -> jsonRpcError(
            id = request.id,
            code = McpErrorCodes.METHOD_NOT_FOUND,
            message = "Unsupported MCP method: ${request.method}"
        )
    }

    private suspend fun handleToolCall(
        request: McpJsonRpcRequest,
        context: McpConnectionContext
    ): Map<String, Any?> {
        val name = request.params["name"]?.toString()
            ?: return jsonRpcError(request.id, McpErrorCodes.INVALID_PARAMS, "tools/call name is required")
        val arguments = request.params["arguments"]?.let { McpJsonCodec.mapAt(it, "arguments") } ?: emptyMap()

        val loopbackRequest = when (name) {
            "shary.fields.list" -> loopbackRequest(
                context = context,
                requestId = request.requestId(),
                method = "GET",
                path = "/connector/v1/fields",
                arguments = arguments
            )

            "shary.fields.create" -> loopbackRequest(
                context = context,
                requestId = request.requestId(),
                method = "POST",
                path = "/connector/v1/fields",
                arguments = arguments,
                body = arguments.withoutTransportKeys()
            )

            "shary.fields.update" -> {
                val key = arguments.requiredString("key")
                    ?: return jsonRpcError(request.id, McpErrorCodes.INVALID_PARAMS, "key is required")
                loopbackRequest(
                    context = context,
                    requestId = request.requestId(),
                    method = "PATCH",
                    path = "/connector/v1/fields/$key",
                    arguments = arguments,
                    body = arguments.withoutTransportKeys().filterKeys { it != "key" }
                )
            }

            "shary.users.list" -> loopbackRequest(
                context = context,
                requestId = request.requestId(),
                method = "GET",
                path = "/connector/v1/users",
                arguments = arguments
            )

            "shary.users.create" -> loopbackRequest(
                context = context,
                requestId = request.requestId(),
                method = "POST",
                path = "/connector/v1/users",
                arguments = arguments,
                body = arguments.withoutTransportKeys()
            )

            "shary.users.update" -> {
                val email = arguments.requiredString("email")
                    ?: return jsonRpcError(request.id, McpErrorCodes.INVALID_PARAMS, "email is required")
                loopbackRequest(
                    context = context,
                    requestId = request.requestId(),
                    method = "PATCH",
                    path = "/connector/v1/users/$email",
                    arguments = arguments,
                    body = arguments.withoutTransportKeys().filterKeys { it != "email" }
                )
            }

            "shary.actions.preview" -> loopbackRequest(
                context = context,
                requestId = request.requestId(),
                method = "POST",
                path = "/connector/v1/actions/preview",
                arguments = arguments + ("confirm" to true),
                body = mapOf("actions" to (arguments["actions"] ?: emptyList<Any?>()))
            )

            "shary.actions.commit" -> loopbackRequest(
                context = context,
                requestId = request.requestId(),
                method = "POST",
                path = "/connector/v1/actions/commit",
                arguments = arguments,
                body = mapOf("pending_id" to arguments["pending_id"])
            )

            else -> return jsonRpcError(
                id = request.id,
                code = McpErrorCodes.INVALID_PARAMS,
                message = "Unknown Shary tool: $name"
            )
        }

        val loopbackResponse = loopbackServer.handle(loopbackRequest)
        return jsonRpcResult(request.id, loopbackResponse.toToolResult())
    }

    private fun loopbackRequest(
        context: McpConnectionContext,
        requestId: String,
        method: String,
        path: String,
        arguments: Map<String, Any?>,
        body: Map<String, Any?> = emptyMap()
    ): LoopbackRequest {
        val headers = context.headers.toMutableMap()
        headers["X-Request-Id"] = requestId
        arguments["idempotency_key"]?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let {
            headers["Idempotency-Key"] = it
        }
        if (arguments["confirm"] == true || arguments["confirm"]?.toString()?.equals("true", true) == true) {
            headers["X-Confirm"] = "true"
        }
        return LoopbackRequest(
            method = method,
            path = path,
            headers = headers,
            body = body
        )
    }

    private fun LoopbackResponse.toToolResult(): Map<String, Any?> {
        val structured = McpJsonCodec.normalize(body)
        return linkedMapOf(
            "content" to listOf(
                mapOf(
                    "type" to "text",
                    "text" to McpJsonCodec.stringify(structured)
                )
            ),
            "structuredContent" to structured,
            "isError" to (statusCode >= 400)
        )
    }

    private fun toolErrorResult(
        code: String,
        message: String,
        metadata: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> {
        val structured = linkedMapOf<String, Any?>(
            "error" to linkedMapOf<String, Any?>(
                "code" to code,
                "message" to message
            ).apply { putAll(metadata) }
        )
        return linkedMapOf(
            "content" to listOf(
                mapOf(
                    "type" to "text",
                    "text" to McpJsonCodec.stringify(structured)
                )
            ),
            "structuredContent" to structured,
            "isError" to true
        )
    }

    private fun toolDefinitions(): List<Map<String, Any?>> = listOf(
        tool(
            name = "shary.fields.list",
            title = "List Shary Fields",
            description = "List fields from the paired Shary vault using the fields.read capability.",
            properties = emptyMap(),
            required = emptyList()
        ),
        tool(
            name = "shary.fields.create",
            title = "Create Shary Field",
            description = "Create a Shary field using fields.write. Requires idempotency_key and, in strict policy mode, confirm=true.",
            properties = mapOf(
                "key" to stringSchema("Field key name."),
                "value" to stringSchema("Field value."),
                "key_alias" to stringSchema("Optional display alias."),
                "tag" to stringSchema("Optional Shary tag name."),
                "idempotency_key" to stringSchema("Client-generated unique key for this mutation."),
                "confirm" to booleanSchema("Set true when the user has confirmed this mutation.")
            ),
            required = listOf("key", "value", "idempotency_key")
        ),
        tool(
            name = "shary.fields.update",
            title = "Update Shary Field",
            description = "Update a Shary field value, alias, or tag using fields.write.",
            properties = mapOf(
                "key" to stringSchema("Existing field key name."),
                "value" to stringSchema("Replacement value."),
                "key_alias" to stringSchema("Replacement display alias."),
                "tag" to stringSchema("Replacement Shary tag name."),
                "idempotency_key" to stringSchema("Client-generated unique key for this mutation."),
                "confirm" to booleanSchema("Set true when the user has confirmed this mutation.")
            ),
            required = listOf("key", "idempotency_key")
        ),
        tool(
            name = "shary.users.list",
            title = "List Shary Users",
            description = "List known Shary users using the users.read capability.",
            properties = emptyMap(),
            required = emptyList()
        ),
        tool(
            name = "shary.users.create",
            title = "Create Shary User",
            description = "Create a Shary user using users.write.",
            properties = mapOf(
                "username" to stringSchema("User display name."),
                "email" to stringSchema("User email address."),
                "idempotency_key" to stringSchema("Client-generated unique key for this mutation."),
                "confirm" to booleanSchema("Set true when the user has confirmed this mutation.")
            ),
            required = listOf("username", "email", "idempotency_key")
        ),
        tool(
            name = "shary.users.update",
            title = "Update Shary User",
            description = "Update a Shary user display name using users.write.",
            properties = mapOf(
                "email" to stringSchema("Existing user email address."),
                "username" to stringSchema("Replacement user display name."),
                "idempotency_key" to stringSchema("Client-generated unique key for this mutation."),
                "confirm" to booleanSchema("Set true when the user has confirmed this mutation.")
            ),
            required = listOf("email", "username", "idempotency_key")
        ),
        tool(
            name = "shary.actions.preview",
            title = "Preview Shary Actions",
            description = "Preview a batch of Shary mutations before commit using actions.preview.",
            properties = mapOf(
                "actions" to mapOf(
                    "type" to "array",
                    "description" to "Mutation action list.",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "type" to mapOf(
                                "type" to "string",
                                "enum" to listOf("CREATE_FIELD", "UPDATE_FIELD", "CREATE_USER", "UPDATE_USER")
                            ),
                            "key" to stringSchema("Field key."),
                            "value" to stringSchema("Field value."),
                            "key_alias" to stringSchema("Field alias."),
                            "tag" to stringSchema("Field tag."),
                            "email" to stringSchema("User email."),
                            "username" to stringSchema("User display name.")
                        ),
                        "required" to listOf("type")
                    )
                ),
                "idempotency_key" to stringSchema("Client-generated unique key for this preview.")
            ),
            required = listOf("actions", "idempotency_key")
        ),
        tool(
            name = "shary.actions.commit",
            title = "Commit Shary Actions",
            description = "Commit a previously previewed Shary action batch using actions.commit.",
            properties = mapOf(
                "pending_id" to stringSchema("Pending action id returned by shary.actions.preview."),
                "idempotency_key" to stringSchema("Client-generated unique key for this commit."),
                "confirm" to booleanSchema("Set true when the user has confirmed this mutation.")
            ),
            required = listOf("pending_id", "idempotency_key")
        )
    )

    private fun tool(
        name: String,
        title: String,
        description: String,
        properties: Map<String, Any?>,
        required: List<String>
    ): Map<String, Any?> = linkedMapOf(
        "name" to name,
        "title" to title,
        "description" to description,
        "inputSchema" to mapOf(
            "type" to "object",
            "properties" to properties,
            "required" to required,
            "additionalProperties" to false
        )
    )

    private fun stringSchema(description: String): Map<String, String> =
        mapOf("type" to "string", "description" to description)

    private fun booleanSchema(description: String): Map<String, String> =
        mapOf("type" to "boolean", "description" to description)

    private fun Map<String, Any?>.withoutTransportKeys(): Map<String, Any?> =
        filterKeys { it != "idempotency_key" && it != "confirm" }

    private fun Map<String, Any?>.requiredString(key: String): String? =
        this[key]?.toString()?.trim()?.takeIf { it.isNotBlank() }

    private fun McpJsonRpcRequest.requestId(): String =
        id?.toString()?.takeIf { it.isNotBlank() } ?: "mcp_${UUID.randomUUID()}"

    private fun jsonRpcResult(id: Any?, result: Map<String, Any?>): Map<String, Any?> =
        linkedMapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to result
        )

    private fun jsonRpcError(
        id: Any?,
        code: Int,
        message: String,
        data: Any? = null
    ): Map<String, Any?> {
        val error = linkedMapOf<String, Any?>(
            "code" to code,
            "message" to message
        )
        if (data != null) {
            error["data"] = data
        }
        return linkedMapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "error" to error
        )
    }

    private fun jsonResponse(statusCode: Int, payload: Map<String, Any?>): McpTransportResponse =
        McpTransportResponse(
            statusCode = statusCode,
            contentType = "application/json; charset=utf-8",
            body = McpJsonCodec.stringify(payload)
        )

}
