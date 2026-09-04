package com.shary.app.infrastructure.connector.transport

import androidx.compose.ui.graphics.Color
import com.shary.app.application.connector.CommitConnectorActionsRequest
import com.shary.app.application.connector.CommitConnectorActionsUseCase
import com.shary.app.application.connector.ConnectorMutationAction
import com.shary.app.application.connector.ConnectorMutationType
import com.shary.app.application.connector.PreviewConnectorActionsRequest
import com.shary.app.application.connector.PreviewConnectorActionsUseCase
import com.shary.app.application.connector.policy.ConnectorPolicyEvaluator
import com.shary.app.application.connector.policy.SensitiveFieldMcpPolicy
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
import com.shary.app.core.domain.types.valueobjects.FieldValueTypes
import com.shary.app.core.domain.types.valueobjects.SENSITIVE_MASK
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
                    operation = "fields.metadata.read",
                    capability = ConnectorCapability.FIELDS_READ
                ) { _ ->
                    val fields = fieldRepository.getAllFields().first().map { field ->
                        val sensitive = SensitiveFieldMcpPolicy.isSensitive(field)
                        mapOf(
                            "key" to field.key,
                            "key_alias" to if (sensitive) SENSITIVE_MASK else field.keyAlias,
                            "tag" to field.tag.name,
                            "value_type" to FieldValueTypes.wireTypeOf(field.value),
                            "date_added" to field.dateAdded.toString(),
                            "is_mcp_shareable" to (sensitive && field.isMcpShareable)
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
                request.path.startsWith("/connector/v1/fields/") -> {
                withAuthorizedScope(
                    request = request,
                    requestId = requestId,
                    operation = "fields.value.read",
                    capability = ConnectorCapability.FIELDS_VALUE_READ
                ) { _ ->
                    val key = request.path.removePrefix("/connector/v1/fields/").trim()
                    val field = fieldRepository.getAllFields().first()
                        .firstOrNull { it.key.equals(key, ignoreCase = true) }
                        ?: return@withAuthorizedScope errorResponse(
                            requestId, 404, ConnectorErrorCodes.NOT_FOUND, "Field not found"
                        )
                    val policyFailure = runCatching {
                        SensitiveFieldMcpPolicy.requireReadable(field)
                    }.exceptionOrNull()
                    if (policyFailure != null) {
                        return@withAuthorizedScope errorResponse(
                            requestId = requestId,
                            statusCode = 403,
                            code = ConnectorErrorCodes.SENSITIVE_FIELD_NOT_MCP_SHAREABLE,
                            message = SensitiveFieldMcpPolicy.BLOCKED_REASON
                        )
                    }
                    LoopbackResponse(
                        statusCode = 200,
                        body = ConnectorSuccessEnvelope(
                            requestId = requestId,
                            schemaVersion = "1.0",
                            timestamp = now,
                            data = mapOf(
                                "key" to field.key,
                                "value" to field.value,
                                "key_alias" to field.keyAlias,
                                "tag" to field.tag.name,
                                "value_type" to FieldValueTypes.wireTypeOf(field.value),
                                "date_added" to field.dateAdded.toString(),
                                "is_mcp_shareable" to field.isMcpShareable
                            )
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
                    val valueType = request.body["value_type"]?.toString()
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
                    val createPolicyFailure = runCatching {
                        SensitiveFieldMcpPolicy.requireCreatable(valueType)
                    }.exceptionOrNull()
                    if (createPolicyFailure != null) {
                        return@withAuthorizedScope errorResponse(
                            requestId = requestId,
                            statusCode = 403,
                            code = ConnectorErrorCodes.SENSITIVE_FIELD_CREATION_DENIED,
                            message = SensitiveFieldMcpPolicy.CREATE_REASON
                        )
                    }
                    val storedValue = encodeFieldValue(value, valueType).getOrElse { error ->
                        return@withAuthorizedScope errorResponse(
                            requestId = requestId,
                            statusCode = 400,
                            code = ConnectorErrorCodes.VALIDATION_FAILED,
                            message = error.message ?: "Invalid value_type"
                        )
                    }
                    fieldRepository.saveField(
                        FieldDomain.create(
                            key = key,
                            value = storedValue,
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
                    val value = request.body["value"]?.toString()
                    val valueType = request.body["value_type"]?.toString()
                    val existingField = fieldRepository.getAllFields().first()
                        .firstOrNull { it.key.equals(key, ignoreCase = true) }
                        ?: return@withAuthorizedScope errorResponse(
                            requestId, 404, ConnectorErrorCodes.NOT_FOUND, "Field not found"
                        )
                    val updatePolicyFailure = runCatching {
                        SensitiveFieldMcpPolicy.requireUpdatable(existingField, valueType)
                    }.exceptionOrNull()
                    if (updatePolicyFailure != null) {
                        val reason = updatePolicyFailure.message.orEmpty()
                        val code = if (reason == SensitiveFieldMcpPolicy.BLOCKED_REASON) {
                            ConnectorErrorCodes.SENSITIVE_FIELD_NOT_MCP_SHAREABLE
                        } else {
                            ConnectorErrorCodes.SENSITIVE_FIELD_TYPE_CHANGE_DENIED
                        }
                        return@withAuthorizedScope errorResponse(
                            requestId = requestId,
                            statusCode = 403,
                            code = code,
                            message = reason.ifBlank { SensitiveFieldMcpPolicy.TYPE_CHANGE_REASON }
                        )
                    }
                    if (value == null && !valueType.isNullOrBlank()) {
                        return@withAuthorizedScope errorResponse(
                            requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED,
                            "value_type requires value"
                        )
                    }
                    if (value != null) {
                        val storedValue = encodeFieldValue(value, valueType).getOrElse { error ->
                            return@withAuthorizedScope errorResponse(
                                requestId, 400, ConnectorErrorCodes.VALIDATION_FAILED,
                                error.message ?: "Invalid value_type"
                            )
                        }
                        fieldRepository.updateValue(key, storedValue)
                    }
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

        val response = block(auth)
        if (operation != "actions.commit") {
            appendOutcomeAudit(
                auth = auth,
                requestId = requestId,
                operation = operation,
                response = response
            )
        }
        return response
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
                valueType = map["value_type"]?.toString(),
                keyAlias = map["key_alias"]?.toString(),
                tag = map["tag"]?.toString(),
                email = map["email"]?.toString(),
                username = map["username"]?.toString()
            )
        }
    }

    private fun encodeFieldValue(value: String, valueType: String?): Result<String> =
        if (valueType.isNullOrBlank()) Result.success(value)
        else FieldValueTypes.encodeForStorage(value, valueType)

    private fun parseTag(raw: String?): Tag =
        if (raw.isNullOrBlank()) Tag.Unknown
        else Tag.fromString(raw, color = Color.Gray.copy(alpha = 0.7f))

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
            val client = connectorRepository.getClient(clientId)
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
                    ),
                    ownerUid = client?.ownerUid.orEmpty(),
                    vaultId = client?.vaultId.orEmpty(),
                    agentProfileId = client?.agentProfileId.orEmpty(),
                    grantId = client?.grantId.orEmpty()
                )
            )
        }
    }

    private suspend fun appendOutcomeAudit(
        auth: ConnectorAuthContext,
        requestId: String,
        operation: String,
        response: LoopbackResponse
    ) {
        runCatching {
            connectorRepository.appendAuditEvent(
                ConnectorAuditEvent(
                    eventId = "evt_${UUID.randomUUID()}",
                    timestamp = System.currentTimeMillis() / 1000,
                    clientId = auth.session.clientId,
                    requestId = requestId,
                    operation = operation,
                    targetType = "request",
                    targetIds = emptyList(),
                    decision = if (response.statusCode < 400) {
                        ConnectorAuditDecision.ALLOWED
                    } else {
                        ConnectorAuditDecision.ERROR
                    },
                    reasonCode = if (response.statusCode < 400) "operation_success" else "operation_failed",
                    metadata = mapOf("status_code" to response.statusCode.toString()),
                    ownerUid = auth.session.ownerUid,
                    vaultId = auth.session.vaultId,
                    agentProfileId = auth.session.agentProfileId,
                    grantId = auth.session.grantId
                )
            )
        }
    }
}
