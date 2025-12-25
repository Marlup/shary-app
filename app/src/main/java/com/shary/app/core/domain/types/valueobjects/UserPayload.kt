package com.shary.app.core.domain.types.valueobjects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPayload(
    val user: String,
    @SerialName("creation_at")
    val creationAt: Long,
    @SerialName("expires_at")
    val expiresAt: Long,
    val pubkey: String,
    val verification: String,
    val signature: String,
    val fcmToken: String? = null
)