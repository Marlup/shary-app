package com.shary.app.infrastructure.connector.transport

import com.shary.app.application.connector.CommitConnectorActionsRequest
import com.shary.app.application.connector.CommitConnectorActionsUseCase
import com.shary.app.application.connector.ConnectorMutationAction
import com.shary.app.application.connector.ConnectorMutationType
import com.shary.app.application.connector.PreviewConnectorActionsRequest
import com.shary.app.application.connector.PreviewConnectorActionsUseCase
import com.shary.app.application.connector.policy.ConnectorPolicyEvaluator
import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.ConnectorAuditDecision
import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import com.shary.app.core.domain.types.valueobjects.ConnectorErrorCodes
import com.shary.app.core.domain.types.valueobjects.ConnectorErrorEnvelope
import com.shary.app.core.domain.types.valueobjects.ConnectorErrorPayload
import com.shary.app.core.domain.types.valueobjects.ConnectorSuccessEnvelope
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class LoopbackConnectorRouter @Inject constructor(
    private val authService: ConnectorAuthService,
    private val policyEvaluator: ConnectorPolicyEvaluator,
    private val connectorRepository: ConnectorRepository,
    private val fieldRepository: FieldRepository,
    private val userRepository: UserRepository,
    private val previewConnectorActionsUseCase: PreviewConnectorActionsUseCase,
    private val commitConnectorActionsUseCase: CommitConnectorActionsUseCase
) {
    private val commitIdempotencyResponses = ConcurrentHashMap<String, LoopbackResponse>()

    suspend fun handle(request: LoopbackRequest): LoopbackResponse {
        val requestId = request.headers["X-Request-Id"].orEmpty().ifBlank { "missing-request-id" }
        val now = System.currentTimeMillis() / 1000

        return when {
            request.method.equals("GET", ignoreCase = true) &&
                request.path == "/connector/v1/fields" -> {
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "fields.read",
                    capability = ConnectorCapability.FIELDS_READ
                ) { _ ->
                    val fields = fieldRepository.getAllFields().first().map { field ->
                        mapOf(
                            "key" to field.key,
                            "value" to field.value,
                            "key_alias" to field.keyAlias,
                            "tag" to field.tag.name,
                            "date_added" to field.dateAdded.toString()
                        )
                    }
                    LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf("items" to fields)
                        )
                    )
                }
            }

            request.method.equals("GET", ignoreCase = true) &&
                request.path == "/connector/v1/users" -> {
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "users.read",
                    capability = ConnectorCapability.USERS_READ
                ) { _ ->
                    val users = userRepository.getAllUsers().map { user ->
                        mapOf(
                            "username" to user.username,
                            "email" to user.email,
                            "date_added" to user.dateAdded.toString()
                        )
                    }
                    LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf("items" to users)
                        )
                    )
                }
            }

            request.method.equals("POST", ignoreCase = true) &&
                request.path == "/connector/v1/fields" -> {
                if (request.idempotencyKey() == null) {
                    return errorResponse(
                        requestId = requestId,
                        statusCode = 400,
                        code = ConnectorErrorCodes.VALIDATION_FAILED,
                        message = "Idempotency-Key header is required for mutating operations"
                    )
                }
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "fields.write.create",
                    capability = ConnectorCapability.FIELDS_WRITE,
                    confirmationGranted = request.confirmationGranted()
                ) { _ ->
                    val key = request.body["key"]?.toString().orEmpty()
                    val value = request.body["value"]?.toString().orEmpty()
                    val keyAlias = request.body["key_alias"]?.toString().orEmpty()
                    val tag = parseTag(request.body["tag"]?.toString())

                    if (key.isBlank() || value.isBlank()) {
                        return@withAuthorizedScope errorResponse(
                            requestId = requestId,
                            statusCode = 400,
                            code = ConnectorErrorCodes.VALIDATION_FAILED,
                            message = "key and value are required"
                        )
                    }
                    fieldRepository.saveField(
                        FieldDomain.create(
                            key = key,
                            value = value,
                            keyAlias = keyAlias,
                            tag = tag
                        )
                    )
                    LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf("created" to true, "key" to key)
                        )
                    )
                }
            }

            request.method.equals("PATCH", ignoreCase = true) &&
                request.path.startsWith("/connector/v1/fields/") -> {
                if (request.idempotencyKey() == null) {
                    return errorResponse(
                        requestId = requestId,
                        statusCode = 400,
                        code = ConnectorErrorCodes.VALIDATION_FAILED,
                        message = "Idempotency-Key header is required for mutating operations"
                    )
                }
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "fields.write.update",
                    capability = ConnectorCapability.FIELDS_WRITE,
                    confirmationGranted = request.confirmationGranted()
                ) { _ ->
                    val key = request.path.removePrefix("/connector/v1/fields/").trim()
                    if (key.isBlank()) {
                        return@withAuthorizedScope errorResponse(
                            requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED, "field key required"
                        )
                    }
                    request.body["value"]?.toString()?.let { fieldRepository.updateValue(key, it) }
                    request.body["key_alias"]?.toString()?.let { fieldRepository.updateAlias(key, it) }
                    request.body["tag"]?.toString()?.let { fieldRepository.updateTag(key, parseTag(it)) }
                    LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf("updated" to true, "key" to key)
                        )
                    )
                }
            }

            request.method.equals("POST", ignoreCase = true) &&
                request.path == "/connector/v1/users" -> {
                if (request.idempotencyKey() == null) {
                    return errorResponse(
                        requestId = requestId,
                        statusCode = 400,
                        code = ConnectorErrorCodes.VALIDATION_FAILED,
                        message = "Idempotency-Key header is required for mutating operations"
                    )
                }
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "users.write.create",
                    capability = ConnectorCapability.USERS_WRITE,
                    confirmationGranted = request.confirmationGranted()
                ) { _ ->
                    val username = request.body["username"]?.toString().orEmpty()
                    val email = request.body["email"]?.toString().orEmpty()
                    if (username.isBlank() || email.isBlank()) {
                        return@withAuthorizedScope errorResponse(
                            requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED, "username and email are required"
                        )
                    }
                    userRepository.upsertUser(UserDomain.create(username = username, email = email))
                    LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf("created" to true, "email" to email)
                        )
                    )
                }
            }

            request.method.equals("PATCH", ignoreCase = true) &&
                request.path.startsWith("/connector/v1/users/") -> {
                if (request.idempotencyKey() == null) {
                    return errorResponse(
                        requestId = requestId,
                        statusCode = 400,
                        code = ConnectorErrorCodes.VALIDATION_FAILED,
                        message = "Idempotency-Key header is required for mutating operations"
                    )
                }
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "users.write.update",
                    capability = ConnectorCapability.USERS_WRITE,
                    confirmationGranted = request.confirmationGranted()
                ) { _ ->
                    val emailPath = request.path.removePrefix("/connector/v1/users/").trim()
                    val username = request.body["username"]?.toString().orEmpty()
                    if (emailPath.isBlank() || username.isBlank()) {
                        return@withAuthorizedScope errorResponse(
                            requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED, "email path and username are required"
                        )
                    }
                    userRepository.upsertUser(UserDomain.create(username = username, email = emailPath))
                    LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf("updated" to true, "email" to emailPath)
                        )
                    )
                }
            }

            request.method.equals("POST", ignoreCase = true) &&
                request.path == "/connector/v1/actions/preview" -> {
                if (request.idempotencyKey() == null) {
                    return errorResponse(
                        requestId = requestId,
                        statusCode = 400,
                        code = ConnectorErrorCodes.VALIDATION_FAILED,
                        message = "Idempotency-Key header is required for mutating operations"
                    )
                }
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "actions.preview",
                    capability = ConnectorCapability.ACTIONS_PREVIEW,
                    confirmationGranted = true
                ) { auth ->
                    val parsedActions = parseActions(request.body["actions"])
                    if (parsedActions.isEmpty()) {
                        return@withAuthorizedScope errorResponse(
                            requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED, "actions are required"
                        )
                    }
                    val result = previewConnectorActionsUseCase(
                        PreviewConnectorActionsRequest(
                            clientId = auth.session.clientId,
                            requestId = requestId,
                            actions = parsedActions
                        )
                    ).getOrElse {
                        return@withAuthorizedScope errorResponse(
                            requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED, it.message ?: "preview failed"
                        )
                    }
                    LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf("pending_id" to result.pendingId, "summary" to result.summary)
                        )
                    )
                }
            }

            request.method.equals("POST", ignoreCase = true) &&
                request.path == "/connector/v1/actions/commit" -> {
                val idempotencyKey = request.idempotencyKey()
                    ?: return errorResponse(
                        requestId = requestId,
                        statusCode = 400,
                        code = ConnectorErrorCodes.VALIDATION_FAILED,
                        message = "Idempotency-Key header is required for mutating operations"
                    )
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "actions.commit",
                    capability = ConnectorCapability.ACTIONS_COMMIT,
                    confirmationGranted = request.confirmationGranted()
                ) { auth ->
                    val commitDedupKey = "${auth.session.clientId}|$requestId|$idempotencyKey|actions.commit"
                    commitIdempotencyResponses[commitDedupKey]?.let { cached ->
                        return@withAuthorizedScope cached
                    }
                    val pendingId = request.body["pending_id"]?.toString().orEmpty()
                    if (pendingId.isBlank()) {
                        return@withAuthorizedScope errorResponse(
                            requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED, "pending_id is required"
                        )
                    }
                    val commit = commitConnectorActionsUseCase(
                        CommitConnectorActionsRequest(
                            clientId = auth.session.clientId,
                            pendingId = pendingId,
                            requestId = requestId,
                            confirmationGranted = request.confirmationGranted()
                        )
                    ).getOrElse {
                        val failure = errorResponse(
                            requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED, it.message ?: "commit failed"
                        )
                        commitIdempotencyResponses[commitDedupKey] = failure
                        return@withAuthorizedScope failure
                    }
                    val response = LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf(
                                "pending_id" to commit.pendingId,
                                "committed" to commit.committed,
                                "applied_count" to commit.appliedCount,
                                "failed_count" to commit.failedCount,
                                "items" to commit.itemResults
                            )
                        )
                    )
                    commitIdempotencyResponses[commitDedupKey] = response
                    response
                }
            }

            else -> errorResponse(requestId, 404, ConnectorErrorCodes.NOT_FOUND, "Endpoint not found")
        }
    }

    private suspend fun withAuthorizedScope(
        request: LoopbackRequest,
        requestId: String,
        operation: String,
        capability: ConnectorCapability,
        confirmationGranted: Boolean = true,
        block: suspend (ConnectorAuthContext) -> LoopbackResponse
    ): LoopbackResponse {
        val auth = when (val authResult = authService.authenticateBearer(request.headers)) {
            is ConnectorAuthResult.Authorized -> authResult.context
            is ConnectorAuthResult.Denied -> {
                authResult.clientId?.let {
                    appendDeniedAudit(
                        clientId = it,
                        requestId = requestId,
                        operation = operation,
                        reasonCode = authResult.code
                    )
                }
                return errorResponse(
                    requestId = requestId,
                    statusCode = 401,
                    code = authResult.code,
                    message = "Authentication failed"
                )
            }
        }

        val policyDecision = policyEvaluator.evaluate(
            policy = auth.policy,
            capability = capability,
            confirmationGranted = confirmationGranted
        )
        if (!policyDecision.allowed) {
            appendDeniedAudit(
                clientId = auth.session.clientId,
                requestId = requestId,
                operation = operation,
                reasonCode = policyDecision.reasonCode ?: ConnectorErrorCodes.POLICY_DENIED
            )
            return errorResponse(
                requestId = requestId,
                statusCode = 403,
                code = policyDecision.reasonCode ?: ConnectorErrorCodes.POLICY_DENIED,
                message = "Scope denied by connector policy"
            )
        }

        return block(auth)
    }

    private fun errorResponse(
        requestId: String,
        statusCode: Int,
        code: String,
        message: String
    ): LoopbackResponse = LoopbackResponse(
        statusCode = statusCode,
        body = ConnectorErrorEnvelope(
            requestId = requestId,
            schemaVersion = "1.0",
            error = ConnectorErrorPayload(
                code = code,
                message = message,
                retryable = false
            )
        )
    )

    private fun parseActions(raw: Any?): List<ConnectorMutationAction> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val type = map["type"]?.toString()?.let {
                runCatching { ConnectorMutationType.valueOf(it) }.getOrNull()
            } ?: return@mapNotNull null
            ConnectorMutationAction(
                type = type,
                key = map["key"]?.toString(),
                value = map["value"]?.toString(),
                keyAlias = map["key_alias"]?.toString(),
                tag = map["tag"]?.toString(),
                email = map["email"]?.toString(),
                username = map["username"]?.toString()
            )
        }
    }

    private fun parseTag(raw: String?): Tag =
        if (raw.isNullOrBlank()) Tag.Unknown
        else Tag.fromString(raw, color = null)

    private fun LoopbackRequest.idempotencyKey(): String? =
        headers.entries.firstOrNull { it.key.equals("Idempotency-Key", ignoreCase = true) }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    private fun LoopbackRequest.confirmationGranted(): Boolean =
        headers.entries.firstOrNull { it.key.equals("X-Confirm", ignoreCase = true) }
            ?.value
            ?.equals("true", ignoreCase = true) == true

    private suspend fun appendDeniedAudit(
        clientId: String,
        requestId: String,
        operation: String,
        reasonCode: String
    ) {
        runCatching {
            connectorRepository.appendAuditEvent(
                ConnectorAuditEvent(
                    eventId = "evt_${UUID.randomUUID()}",
                    timestamp = System.currentTimeMillis() / 1000,
                    clientId = clientId,
                    requestId = requestId,
                    operation = operation,
                    targetType = "request",
                    targetIds = emptyList(),
                    decision = ConnectorAuditDecision.DENIED,
                    reasonCode = reasonCode,
                    metadata = mapOf(
                        "path" to operation
                    )
                )
            )
        }
    }
}
