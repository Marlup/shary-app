package com.shary.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User (
    val username: String,
    val email: String,
    val dateAdded: String = "now" // Default for now
)