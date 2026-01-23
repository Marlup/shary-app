package com.shary.app.infrastructure.security.derivation

import com.shary.app.core.domain.security.Kdf
import com.shary.app.infrastructure.security.derivation.hkdf.HkdfSha256


class KeyDerivation(private val kdf: Kdf, val hkdf: HkdfSha256) {
    fun masterSeed(email: String, password: CharArray, appId: String): ByteArray =
        kdf.derive(password, ("$appId:$email").encodeToByteArray(), 32)

    fun idSignSeed(master: ByteArray): ByteArray =
        hkdf.expand(master, info = "shary:id:sign".encodeToByteArray(), len = 32)

    fun idKexSeed(master: ByteArray): ByteArray =
        hkdf.expand(master, info = "shary:id:kex".encodeToByteArray(), len = 32)

    fun sessionSeed(master: ByteArray, nonce: ByteArray): ByteArray =
        hkdf.expand(master, info = ("shary:session:".encodeToByteArray() + nonce), len = 32)
}
