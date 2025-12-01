package com.shary.app.infrastructure.security.derivation.kdf

import com.shary.app.core.domain.security.Kdf
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


class Pbkdf2Kdf : Kdf {
    override fun derive(password: CharArray, salt: ByteArray, lengthBytes: Int, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, lengthBytes * 8)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}
