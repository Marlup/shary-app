package com.shary.app.infrastructure.services.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudUtilsTest {

    @Test
    fun `authBearerHeader includes authorization and request id`() {
        val headers = Utils.authBearerHeader("token-123", "req-abc")

        assertEquals("Bearer token-123", headers["Authorization"])
        assertEquals("req-abc", headers[Constants.HEADER_X_REQUEST_ID])
    }

    @Test
    fun `authBearerHeader keeps request id when token missing`() {
        val headers = Utils.authBearerHeader(token = null, requestId = "req-only")

        assertFalse(headers.containsKey("Authorization"))
        assertEquals("req-only", headers[Constants.HEADER_X_REQUEST_ID])
    }

    @Test
    fun `authBearerHeader empty when both values missing`() {
        val headers = Utils.authBearerHeader(token = null, requestId = null)

        assertTrue(headers.isEmpty())
    }
}
