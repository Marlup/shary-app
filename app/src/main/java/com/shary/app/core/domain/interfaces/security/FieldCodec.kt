package com.shary.app.core.domain.interfaces.security

import com.shary.app.core.domain.types.valueobjects.Purpose


// :domain
interface FieldCodec {
    fun encode(message: String, purpose: Purpose): String
    fun decode(message: String, purpose: Purpose): String
}
