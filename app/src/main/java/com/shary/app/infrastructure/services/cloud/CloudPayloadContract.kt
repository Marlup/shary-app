package com.shary.app.infrastructure.services.cloud

import com.shary.app.core.domain.types.valueobjects.DataPayload
import java.security.MessageDigest
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

/** Frozen backend canonicalization shared by payload and request uploads. */
internal object CloudPayloadContract {

    private val inboxJson = Json { ignoreUnknownKeys = true }

    fun canonicalDigest(
        senderHash: String,
        recipientHash: String,
        encryptedData: String,
        creationAt: Long,
        expiresAt: Long
    ): ByteArray {
        val canonical = listOf(
            senderHash,
            recipientHash,
            encryptedData,
            creationAt.toString(),
            expiresAt.toString()
        ).joinToString(".")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
    }

    fun verificationBase64(digest: ByteArray): String =
        Base64.getEncoder().encodeToString(digest)

    fun decodeInboxPayload(element: JsonElement): DataPayload =
        inboxJson.decodeFromJsonElement(DataPayload.serializer(), element)

    fun requestDecisionBody(
        recipientHash: String,
        requestId: String,
        decision: String,
        notifySender: Boolean,
        schemaVersion: Int
    ): String = buildJsonObject {
        put("recipient", JsonPrimitive(recipientHash))
        put("request_id", JsonPrimitive(requestId))
        put("decision", JsonPrimitive(decision))
        put("notify_sender", JsonPrimitive(notifySender))
        put("schema_version", JsonPrimitive(schemaVersion))
    }.toString()
}
