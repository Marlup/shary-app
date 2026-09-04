package com.shary.app.core.domain.types.valueobjects

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json

class DataPayloadSerializationTest {

    @Test
    fun `data payload emits schema version`() {
        val payload = DataPayload(
            user = "sender",
            recipient = "recipient",
            creationAt = 1L,
            expiresAt = 2L,
            data = "payload",
            verification = "verification",
            signature = "signature"
        )

        val serialized = Json { encodeDefaults = true }
            .encodeToString(DataPayload.serializer(), payload)

        assertTrue(serialized.contains("\"schema_version\":1"))
    }
}
