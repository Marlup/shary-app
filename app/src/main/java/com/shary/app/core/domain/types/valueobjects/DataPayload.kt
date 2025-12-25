package com.shary.app.core.domain.types.valueobjects

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
    val data: String,
    val verification: String,
    val signature: String
)