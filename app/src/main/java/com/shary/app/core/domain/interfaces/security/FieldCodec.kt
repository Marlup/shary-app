package com.shary.app.core.domain.interfaces.security

import com.shary.app.core.domain.types.valueobjects.Purpose


// :domain
interface FieldCodec {
    fun encode(message: String, purposeString: String): String
    fun decode(message: String, purposeString: String): String
}
