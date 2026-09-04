package com.shary.app.application.connector.policy

import com.shary.app.core.domain.types.enums.ConnectorCapability
import com.shary.app.core.domain.types.enums.ConnectorPolicyMode
import com.shary.app.core.domain.types.valueobjects.ConnectorErrorCodes
import com.shary.app.core.domain.types.valueobjects.ConnectorPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectorPolicyEvaluatorTest {
    private val evaluator = ConnectorPolicyEvaluator()

    @Test
    fun evaluate_denies_when_capability_missing() {
        val policy = ConnectorPolicy(
            clientId = "c1",
            mode = ConnectorPolicyMode.STRICT,
            askEveryTime = true,
            allowedCapabilities = setOf(ConnectorCapability.FIELDS_READ),
            updatedAt = 1L
        )

        val decision = evaluator.evaluate(
            policy = policy,
            capability = ConnectorCapability.USERS_READ,
            confirmationGranted = true
        )

        assertFalse(decision.allowed)
        assertEquals(ConnectorErrorCodes.UNAUTHORIZED_SCOPE, decision.reasonCode)
    }

    @Test
    fun evaluate_strict_write_requires_confirmation() {
        val policy = ConnectorPolicy(
            clientId = "c1",
            mode = ConnectorPolicyMode.STRICT,
            askEveryTime = true,
            allowedCapabilities = setOf(ConnectorCapability.FIELDS_WRITE),
            updatedAt = 1L
        )

        val denied = evaluator.evaluate(
            policy = policy,
            capability = ConnectorCapability.FIELDS_WRITE,
            confirmationGranted = false
        )
        val allowed = evaluator.evaluate(
            policy = policy,
            capability = ConnectorCapability.FIELDS_WRITE,
            confirmationGranted = true
        )

        assertFalse(denied.allowed)
        assertEquals(ConnectorErrorCodes.CONFIRMATION_REQUIRED, denied.reasonCode)
        assertTrue(allowed.allowed)
    }
}
