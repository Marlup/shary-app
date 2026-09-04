package com.shary.app.infrastructure.connector.mcp

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val DEFAULT_MCP_PORT = 8765

data class SharyMcpHttpServerState(
    val running: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = DEFAULT_MCP_PORT,
    val endpointPath: String = "/mcp",
    val lastError: String? = null
) {
    val endpoint: String
        get() = "http://$host:$port$endpointPath"
}

@Singleton
class SharyMcpHttpServer @Inject constructor(
    private val mcpServer: SharyMcpServer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(SharyMcpHttpServerState())

    val state: StateFlow<SharyMcpHttpServerState> = _state.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val transport = SharyMcpHttpTransport(mcpServer::handleRawJson)

    @Synchronized
    fun start(port: Int = DEFAULT_MCP_PORT): Result<SharyMcpHttpServerState> {
        if (_state.value.running) {
            return Result.success(_state.value)
        }

        return runCatching {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port))
            serverSocket = socket
            val runningState = SharyMcpHttpServerState(running = true, port = port)
            _state.value = runningState
            serverJob = scope.launch { acceptLoop(socket) }
            runningState
        }.onFailure { error ->
            _state.value = _state.value.copy(running = false, lastError = error.message)
        }
    }

    @Synchronized
    fun stop() {
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        _state.value = _state.value.copy(running = false, lastError = null)
    }

    private suspend fun acceptLoop(socket: ServerSocket) {
        while (scope.coroutineContext.isActive && !socket.isClosed) {
            val client = runCatching { socket.accept() }.getOrNull() ?: continue
            scope.launch { handleClient(client) }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 15_000
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            when (val readResult = runCatching { readHttpRequest(input) }.getOrNull()) {
                is HttpReadResult.Success -> {
                    val response = transport.handle(readResult.request)
                    writeHttpResponse(
                        output = output,
                        statusCode = response.statusCode,
                        contentType = response.contentType,
                        body = response.body,
                        responseHeaders = response.headers
                    )
                }
                is HttpReadResult.Failure -> {
                    writeHttpResponse(
                        output = output,
                        statusCode = readResult.statusCode,
                        contentType = "text/plain; charset=utf-8",
                        body = readResult.message
                    )
                }
                null -> {
                    writeHttpResponse(output, 400, "text/plain; charset=utf-8", "Bad Request")
                }
            }
        }
    }

    private fun readHttpRequest(input: BufferedInputStream): HttpReadResult {
        val headerBytes = mutableListOf<Byte>()
        var previous = 0
        var matched = 0
        val end = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())

        while (headerBytes.size < MAX_HEADER_BYTES) {
            val next = input.read()
            if (next < 0) return HttpReadResult.Failure(400, "Bad Request")
            headerBytes += next.toByte()
            matched = if (next == end[matched].toInt()) matched + 1 else if (next == end[0].toInt()) 1 else 0
            previous = next
            if (matched == end.size) break
        }
        if (previous < 0 || matched != end.size) {
            return HttpReadResult.Failure(431, "Request headers are too large")
        }

        val headerText = headerBytes.toByteArray().toString(StandardCharsets.UTF_8)
        val lines = headerText.split("\r\n").filter { it.isNotEmpty() }
        val requestLine = lines.firstOrNull()?.split(" ")
            ?: return HttpReadResult.Failure(400, "Bad Request")
        if (requestLine.size < 2) return HttpReadResult.Failure(400, "Bad Request")

        val headers = linkedMapOf<String, String>()
        lines.drop(1).forEach { line ->
            val separator = line.indexOf(':')
            if (separator > 0) {
                headers[line.substring(0, separator).trim()] = line.substring(separator + 1).trim()
            }
        }

        val contentLengthHeader = headers.headerValue("Content-Length")
        val contentLength = contentLengthHeader?.toIntOrNull()
            ?: if (contentLengthHeader == null) 0 else {
                return HttpReadResult.Failure(400, "Invalid Content-Length")
            }
        when (validateMcpContentLength(contentLength)) {
            400 -> return HttpReadResult.Failure(400, "Invalid Content-Length")
            413 -> return HttpReadResult.Failure(413, "MCP request body is too large")
            null -> Unit
            else -> return HttpReadResult.Failure(400, "Invalid Content-Length")
        }
        val body = if (contentLength > 0) {
            readExactly(input, contentLength).toString(StandardCharsets.UTF_8)
        } else {
            ""
        }
        if (body.toByteArray(StandardCharsets.UTF_8).size != contentLength) {
            return HttpReadResult.Failure(400, "Incomplete request body")
        }

        return HttpReadResult.Success(
            SharyMcpHttpRequest(
                method = requestLine[0],
                path = requestLine[1].substringBefore("?"),
                headers = headers,
                body = body
            )
        )
    }

    private fun readExactly(input: BufferedInputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(buffer, offset, length - offset)
            if (read < 0) break
            offset += read
        }
        return if (offset == length) buffer else buffer.copyOf(offset)
    }

    private fun writeHttpResponse(
        output: BufferedOutputStream,
        statusCode: Int,
        contentType: String?,
        body: String?,
        responseHeaders: Map<String, String> = emptyMap()
    ) {
        val bodyBytes = body?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0)
        val statusText = when (statusCode) {
            200 -> "OK"
            202 -> "Accepted"
            400 -> "Bad Request"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            406 -> "Not Acceptable"
            413 -> "Content Too Large"
            415 -> "Unsupported Media Type"
            431 -> "Request Header Fields Too Large"
            else -> "HTTP $statusCode"
        }
        val headers = buildString {
            append("HTTP/1.1 $statusCode $statusText\r\n")
            append("Connection: close\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            if (contentType != null && bodyBytes.isNotEmpty()) {
                append("Content-Type: $contentType\r\n")
            }
            responseHeaders.forEach { (name, value) ->
                append("$name: $value\r\n")
            }
            append("\r\n")
        }
        output.write(headers.toByteArray(StandardCharsets.UTF_8))
        output.write(bodyBytes)
        output.flush()
    }

    private sealed interface HttpReadResult {
        data class Success(val request: SharyMcpHttpRequest) : HttpReadResult
        data class Failure(val statusCode: Int, val message: String) : HttpReadResult
    }

    private companion object {
        const val MAX_HEADER_BYTES = 16 * 1024
    }
}
