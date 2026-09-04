package com.shary.app.application.connector

import com.shary.app.core.domain.interfaces.repositories.ConnectorRepository
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.application.connector.policy.SensitiveFieldMcpPolicy
import com.shary.app.core.domain.types.valueobjects.ConnectorPendingAction
import com.shary.app.core.domain.types.valueobjects.FieldValueType
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

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
    private val connectorRepository: ConnectorRepository,
    private val fieldRepository: FieldRepository
) {
    suspend operator fun invoke(request: PreviewConnectorActionsRequest): Result<PreviewConnectorActionsResult> =
        runCatching {
            require(request.clientId.isNotBlank()) { "clientId cannot be blank" }
            require(request.requestId.isNotBlank()) { "requestId cannot be blank" }
            require(request.actions.isNotEmpty()) { "actions cannot be empty" }

            val fieldsByKey = fieldRepository.getAllFields().first()
                .associateBy { it.key.trim().lowercase() }
            request.actions.forEach { validateAction(it, fieldsByKey) }
            val client = connectorRepository.getClient(request.clientId)
                ?: error("Connector client not found")
            require(
                client.ownerUid.isNotBlank() &&
                    client.vaultId.isNotBlank() &&
                    client.agentProfileId.isNotBlank() &&
                    client.grantId.isNotBlank()
            ) { "Connector client is not owner-bound" }

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
                    updatedAt = System.currentTimeMillis() / 1000,
                    ownerUid = client.ownerUid,
                    vaultId = client.vaultId,
                    agentProfileId = client.agentProfileId,
                    grantId = client.grantId
                )
            )

            PreviewConnectorActionsResult(
                pendingId = pendingId,
                summary = summary
            )
        }

    private fun validateAction(
        action: ConnectorMutationAction,
        fieldsByKey: Map<String, FieldDomain>
    ) {
        when (action.type) {
            ConnectorMutationType.CREATE_FIELD -> {
                require(!action.key.isNullOrBlank()) { "field key required" }
                require(!action.value.isNullOrBlank()) { "field value required" }
                validateFieldValueType(action)
                SensitiveFieldMcpPolicy.requireCreatable(action.valueType)
            }
            ConnectorMutationType.UPDATE_FIELD -> {
                require(!action.key.isNullOrBlank()) { "field key required" }
                val existing = fieldsByKey[action.key.trim().lowercase()]
                    ?: error("Field not found")
                require(
                    !action.value.isNullOrBlank() ||
                        !action.keyAlias.isNullOrBlank() ||
                        !action.tag.isNullOrBlank()
                ) { "at least one update value required" }
                require(action.valueType.isNullOrBlank() || action.value != null) {
                    "value_type requires field value"
                }
                validateFieldValueType(action)
                SensitiveFieldMcpPolicy.requireUpdatable(existing, action.valueType)
            }
            ConnectorMutationType.CREATE_USER,
            ConnectorMutationType.UPDATE_USER -> {
                require(!action.email.isNullOrBlank()) { "user email required" }
                require(!action.username.isNullOrBlank()) { "username required" }
            }
        }
    }

    private fun validateFieldValueType(action: ConnectorMutationAction) {
        if (action.valueType.isNullOrBlank()) return
        require(FieldValueType.fromWireName(action.valueType) != null) {
            "unsupported value_type='${action.valueType}'"
        }
    }
}
