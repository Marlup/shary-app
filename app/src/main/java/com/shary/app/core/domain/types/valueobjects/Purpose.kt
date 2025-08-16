package com.shary.app.core.domain.types.valueobjects

/**
    Field attribute purpose (avoid loosen strings).
 */
sealed class Purpose(val code: String) {
    data object Key : Purpose("key")
    data object Value : Purpose("value")
    data object Alias : Purpose("alias")
    data object Tag : Purpose("tag")
    data object Credentials : Purpose("credentials")
    data class Custom(val name: String) : Purpose(name)
}

