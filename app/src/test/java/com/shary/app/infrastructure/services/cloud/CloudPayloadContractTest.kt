package com.shary.app.infrastructure.services.cloud

import com.shary.app.core.domain.types.enums.CloudPayloadDecision
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CloudPayloadContractTest {

    @Test
    fun canonicalDigestMatchesFrozenBackendVector() {
        val digest = CloudPayloadContract.canonicalDigest(
            senderHash = "sender-hash",
            recipientHash = "recipient-hash",
            encryptedData = "encrypted-payload",
            creationAt = 1_700_000_000L,
            expiresAt = 1_700_604_800L
        )

        assertEquals(
            "UHLyIxj+tX0iP5D0CktR6Rh37thLojP2FPVo4vioBsM=",
            Base64.getEncoder().encodeToString(digest)
        )
    }

    @Test
    fun decisionBodyUsesBackendRequestIdField() {
        val body = Json.parseToJsonElement(
            CloudPayloadContract.requestDecisionBody(
                recipientHash = "recipient-hash",
                requestId = "verification-hash",
                decision = CloudPayloadDecision.ACCEPT.name.lowercase(),
                notifySender = false,
                schemaVersion = 1
            )
        ).jsonObject

        assertEquals("verification-hash", body.getValue("request_id").jsonPrimitive.content)
        assertFalse(body.containsKey("verification"))
    }

    @Test
    fun inboxDecoderAcceptsServerMetadataFields() {
        val payload = CloudPayloadContract.decodeInboxPayload(
            Json.parseToJsonElement(
                """
                {
                  "user":"sender",
                  "recipient":"recipient",
                  "creation_at":1,
                  "expires_at":2,
                  "schema_version":1,
                  "data":"payload",
                  "verification":"verification",
                  "signature":"signature",
                  "doc_id":"server-document",
                  "request_id":"logical-request"
                }
                """.trimIndent()
            )
        )

        assertEquals("verification", payload.verification)
    }
}
