package com.shary.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Field (
    val key: String,
    val value: String,
    val dateAdded: String = "now" // Default for now
)