package com.shary.app.core.domain.types.valueobjects

import kotlinx.serialization.Serializable

@Serializable
data class DeleteUserPayload(
    val user: String,
    val signature: String
)
