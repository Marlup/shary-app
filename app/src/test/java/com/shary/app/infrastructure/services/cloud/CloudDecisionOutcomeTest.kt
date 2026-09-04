package com.shary.app.infrastructure.services.cloud

import com.google.firebase.auth.FirebaseAuth
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudDecisionOutcomeTest {

    private val service = CloudServiceImpl(
        cryptographyManager = mockk<CryptographyManager>(relaxed = true),
        firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    )

    @Test
    fun completeProcessedReceiptIsAccepted() {
        val accepted = service.resolveDecisionOutcome(
            result = CloudServiceImpl.DecisionHttpResult(
                statusCode = 200,
                ack = CloudServiceImpl.DecisionAckEnvelope(
                    status = "already_processed",
                    reasonCode = "decision_accept",
                    decisionId = "decision-id",
                    requestId = "logical-request-id",
                    schemaVersion = 1
                ),
                error = null,
                requestId = "logical-request-id"
            ),
            endpoint = "/request_decision"
        )

        assertTrue(accepted)
    }

    @Test
    fun emptySuccessfulReceiptIsRejected() {
        expectBackendFailure {
            service.resolveDecisionOutcome(
                result = CloudServiceImpl.DecisionHttpResult(
                    statusCode = 200,
                    ack = null,
                    error = null,
                    requestId = "correlation-id"
                ),
                endpoint = "/request_decision"
            )
        }
    }

    @Test
    fun oppositeDecisionConflictIsNeverAcknowledgedAsSuccess() {
        expectBackendFailure {
            service.resolveDecisionOutcome(
                result = CloudServiceImpl.DecisionHttpResult(
                    statusCode = 409,
                    ack = null,
                    error = CloudServiceImpl.CloudErrorEnvelope(
                        errorCode = "decision_conflict",
                        message = "Decision already finalized",
                        requestId = "correlation-id",
                        retryAfterSeconds = null
                    ),
                    requestId = "correlation-id"
                ),
                endpoint = "/request_decision"
            )
        }
    }

    private fun expectBackendFailure(block: () -> Unit) {
        try {
            block()
            fail("Expected CloudBackendApiException")
        } catch (_: CloudBackendApiException) {
            // Expected.
        }
    }
}
