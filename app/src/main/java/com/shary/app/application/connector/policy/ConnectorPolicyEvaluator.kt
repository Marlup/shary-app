package com.shary.app.application.connector.policy

import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.ConnectorPolicyMode
import com.shary.app.core.domain.types.valueobjects.ConnectorErrorCodes
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import javax.inject.Inject

data class ConnectorPolicyDecision(
    val allowed: Boolean,
    val reasonCode: String? = null
)

class ConnectorPolicyEvaluator @Inject constructor() {
    fun evaluate(
        policy: ConnectorPolicy,
        capability: ConnectorCapability,
        confirmationGranted: Boolean = false
    ): ConnectorPolicyDecision {
        if (capability !in policy.allowedCapabilities) {
            return ConnectorPolicyDecision(
                allowed = false,
                reasonCode = ConnectorErrorCodes.UNAUTHORIZED_SCOPE
            )
        }

        if (policy.askEveryTime && !confirmationGranted && requiresConfirmation(capability, policy.mode)) {
            return ConnectorPolicyDecision(
                allowed = false,
                reasonCode = ConnectorErrorCodes.CONFIRMATION_REQUIRED
            )
        }

        if (!confirmationGranted && requiresConfirmation(capability, policy.mode)) {
            return ConnectorPolicyDecision(
                allowed = false,
                reasonCode = ConnectorErrorCodes.CONFIRMATION_REQUIRED
            )
        }

        return ConnectorPolicyDecision(allowed = true)
    }

    private fun requiresConfirmation(
        capability: ConnectorCapability,
        mode: ConnectorPolicyMode
    ): Boolean {
        val isWrite = capability in setOf(
            ConnectorCapability.FIELDS_WRITE,
            ConnectorCapability.USERS_WRITE,
            ConnectorCapability.ACTIONS_COMMIT
        )

        return when (mode) {
            ConnectorPolicyMode.STRICT -> isWrite
            ConnectorPolicyMode.BALANCED -> capability == ConnectorCapability.ACTIONS_COMMIT
            ConnectorPolicyMode.AUTOMATION -> false
        }
    }
}
