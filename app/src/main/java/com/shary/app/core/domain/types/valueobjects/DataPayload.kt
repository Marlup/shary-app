package com.shary.app.core.domain.types.valueobjects

import com.shary.app.infrastructure.services.cloud.Constants.CLOUD_SCHEMA_VERSION
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataPayload(
    val user: String,
    val recipient: String,
    @SerialName("creation_at")
    val creationAt: Long,
    @SerialName("expires_at")
    val expiresAt: Long,
    @SerialName("schema_version")
    val schemaVersion: Int = CLOUD_SCHEMA_VERSION,
    val data: String,
    val verification: String,
    val signature: String
)
