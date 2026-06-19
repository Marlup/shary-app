package com.shary.app.application.connector

import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import java.util.UUID
import javax.inject.Inject

data class PreviewConnectorActionsRequest(
    val clientId: String,
    val requestId: String,
    val actions: List<ConnectorMutationAction>
)

data class PreviewConnectorActionsResult(
    val pendingId: String,
    val summary: Map<String, String>
)

class PreviewConnectorActionsUseCase @Inject constructor(
    private val connectorRepository: ConnectorRepository
) {
    suspend operator fun invoke(request: PreviewConnectorActionsRequest): Result<PreviewConnectorActionsResult> =
        runCatching {
            require(request.clientId.isNotBlank()) { "clientId cannot be blank" }
            require(request.requestId.isNotBlank()) { "requestId cannot be blank" }
            require(request.actions.isNotEmpty()) { "actions cannot be empty" }

            request.actions.forEach { validateAction(it) }

            val summary = mapOf(
                "total_actions" to request.actions.size.toString(),
                "create_field" to request.actions.count { it.type == ConnectorMutationType.CREATE_FIELD }.toString(),
                "update_field" to request.actions.count { it.type == ConnectorMutationType.UPDATE_FIELD }.toString(),
                "create_user" to request.actions.count { it.type == ConnectorMutationType.CREATE_USER }.toString(),
                "update_user" to request.actions.count { it.type == ConnectorMutationType.UPDATE_USER }.toString()
            )

            val pendingId = "pa_${UUID.randomUUID()}"
            connectorRepository.upsertPendingAction(
                ConnectorPendingAction(
                    pendingId = pendingId,
                    clientId = request.clientId,
                    requestId = request.requestId,
                    proposedActions = request.actions,
                    previewSummary = summary,
                    status = "previewed",
                    updatedAt = System.currentTimeMillis() / 1000
                )
            )

            PreviewConnectorActionsResult(
                pendingId = pendingId,
                summary = summary
            )
        }

    private fun validateAction(action: ConnectorMutationAction) {
        when (action.type) {
            ConnectorMutationType.CREATE_FIELD -> {
                require(!action.key.isNullOrBlank()) { "field key required" }
                require(!action.value.isNullOrBlank()) { "field value required" }
            }
            ConnectorMutationType.UPDATE_FIELD -> {
                require(!action.key.isNullOrBlank()) { "field key required" }
                require(
                    !action.value.isNullOrBlank() ||
                        !action.keyAlias.isNullOrBlank() ||
                        !action.tag.isNullOrBlank()
                ) { "at least one update value required" }
            }
            ConnectorMutationType.CREATE_USER,
            ConnectorMutationType.UPDATE_USER -> {
                require(!action.email.isNullOrBlank()) { "user email required" }
                require(!action.username.isNullOrBlank()) { "username required" }
            }
        }
    }
}
