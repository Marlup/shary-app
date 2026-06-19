package com.shary.app.ui.screens.utils

import com.shary.app.infrastructure.services.cloud.CloudBackendApiException
import com.shary.app.infrastructure.services.cloud.CloudRateLimitedException
import com.shary.app.infrastructure.services.cloud.CloudUnsupportedSchemaVersionException
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudErrorMessageTest {

    @Test
    fun `rate limited error shows retry seconds`() {
        val message = cloudErrorMessage(CloudRateLimitedException(retryAfterSeconds = 19))

        assertEquals("Cloud is rate-limited. Try again in 19s.", message)
    }

    @Test
    fun `unsupported schema message is explicit`() {
        val message = cloudErrorMessage(CloudUnsupportedSchemaVersionException())

        assertEquals("Cloud contract mismatch detected. Update the app before retrying.", message)
    }

    @Test
    fun `backend api error prefers error code when present`() {
        val message = cloudErrorMessage(
            CloudBackendApiException(
                statusCode = 400,
                errorCode = "unsupported_schema_version",
                requestId = "req-1",
                message = "backend rejected schema"
            )
        )

        assertEquals("Cloud contract mismatch detected. Update the app before retrying.", message)
    }

    @Test
    fun `decision conflict explains inbox refresh`() {
        val message = cloudErrorMessage(
            CloudBackendApiException(
                statusCode = 409,
                errorCode = "decision_conflict",
                requestId = "req-1",
                message = "already decided"
            )
        )

        assertEquals("This cloud item was already decided. Refresh the inbox before retrying.", message)
    }

    @Test
    fun `auth coded failure asks for sign in retry`() {
        val message = cloudErrorMessage(
            CloudBackendApiException(
                statusCode = 403,
                errorCode = "stale_token",
                requestId = "req-1",
                message = "token stale"
            )
        )

        assertEquals("Cloud session expired or was rejected. Sign in again, then retry.", message)
    }
}
