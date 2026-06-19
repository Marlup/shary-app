package com.shary.app.application.connector

import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.ConnectorAuditDecision
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.ConnectorAuditEvent
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class CommitConnectorActionsRequest(
    val clientId: String,
    val pendingId: String,
    val requestId: String,
    val confirmationGranted: Boolean
)

data class CommitConnectorActionsResult(
    val pendingId: String,
    val committed: Boolean,
    val appliedCount: Int,
    val failedCount: Int,
    val itemResults: List<Map<String, String>>
)

class CommitConnectorActionsUseCase @Inject constructor(
    private val connectorRepository: ConnectorRepository,
    private val fieldRepository: FieldRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(request: CommitConnectorActionsRequest): Result<CommitConnectorActionsResult> =
        runCatching {
            require(request.clientId.isNotBlank()) { "clientId cannot be blank" }
            require(request.pendingId.isNotBlank()) { "pendingId cannot be blank" }
            require(request.requestId.isNotBlank()) { "requestId cannot be blank" }
            require(request.confirmationGranted) { "confirmation required" }

            val pending = connectorRepository.pendingActions.first()
                .firstOrNull { it.pendingId == request.pendingId }
                ?: error("Pending action not found")
            require(pending.clientId == request.clientId) { "pending action not owned by client" }

            val results = mutableListOf<Map<String, String>>()
            var applied = 0
            var failed = 0

            pending.proposedActions.forEach { action ->
                val result = runCatching {
                    when (action.type) {
                        ConnectorMutationType.CREATE_FIELD -> {
                            fieldRepository.saveField(
                                FieldDomain.create(
                                    key = action.key.orEmpty(),
                                    value = action.value.orEmpty(),
                                    keyAlias = action.keyAlias.orEmpty(),
                                    tag = parseTag(action.tag)
                                )
                            )
                        }
                        ConnectorMutationType.UPDATE_FIELD -> {
                            val key = action.key.orEmpty()
                            action.value?.let { fieldRepository.updateValue(key, it) }
                            action.keyAlias?.let { fieldRepository.updateAlias(key, it) }
                            action.tag?.let { fieldRepository.updateTag(key, parseTag(it)) }
                        }
                        ConnectorMutationType.CREATE_USER -> {
                            userRepository.upsertUser(
                                UserDomain.create(
                                    username = action.username.orEmpty(),
                                    email = action.email.orEmpty()
                                )
                            )
                        }
                        ConnectorMutationType.UPDATE_USER -> {
                            userRepository.upsertUser(
                                UserDomain.create(
                                    username = action.username.orEmpty(),
                                    email = action.email.orEmpty()
                                )
                            )
                        }
                    }
                }

                if (result.isSuccess) {
                    applied += 1
                    results += mapOf(
                        "type" to action.type.name,
                        "status" to "applied"
                    )
                } else {
                    failed += 1
                    results += mapOf(
                        "type" to action.type.name,
                        "status" to "failed",
                        "reason" to (result.exceptionOrNull()?.message ?: "unknown")
                    )
                }
            }

            connectorRepository.upsertPendingAction(
                pending.copy(
                    status = if (failed == 0) "committed" else "partial_failure",
                    updatedAt = System.currentTimeMillis() / 1000
                )
            )

            connectorRepository.appendAuditEvent(
                ConnectorAuditEvent(
                    eventId = "evt_${UUID.randomUUID()}",
                    timestamp = System.currentTimeMillis() / 1000,
                    clientId = pending.clientId,
                    requestId = request.requestId,
                    operation = "actions.commit",
                    targetType = "mixed",
                    targetIds = listOf(request.pendingId),
                    decision = if (failed == 0) ConnectorAuditDecision.ALLOWED else ConnectorAuditDecision.ERROR,
                    reasonCode = if (failed == 0) "commit_success" else "partial_failure"
                )
            )

            CommitConnectorActionsResult(
                pendingId = request.pendingId,
                committed = failed == 0,
                appliedCount = applied,
                failedCount = failed,
                itemResults = results
            )
        }

    private fun parseTag(raw: String?): Tag {
        if (raw.isNullOrBlank()) return Tag.Unknown
        return Tag.fromString(raw, color = null)
    }
}
