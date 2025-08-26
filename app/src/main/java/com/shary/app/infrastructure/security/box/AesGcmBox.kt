package com.shary.app.infrastructure.security.box

import com.shary.app.core.domain.security.Box
import com.shary.app.infrastructure.security.cipher.AesGcmCipher
import com.shary.app.infrastructure.security.hkdf.HkdfSha256
import com.shary.app.infrastructure.security.kex.X25519KeyPair
import javax.inject.Inject


class AesGcmBox @Inject constructor(
    private val hkdf: HkdfSha256,
    private val cipher: AesGcmCipher
) : Box {

    override fun seal(plain: ByteArray, myPrivate: ByteArray, peerPublic: ByteArray, aad: ByteArray?): Box.Sealed {
        val my = X25519KeyPair.fromSeed(myPrivate)          // determinista desde seed
        val shared = my.ecdh(peerPublic)
        val aesKey = hkdf.expand(shared, info = "shary:box".encodeToByteArray(), len = 32)
        val (iv, body, tag) = cipher.encrypt(aesKey, plain, aad)
        return Box.Sealed(ephPublicKey = my.publicEncoded(), iv = iv, ciphertext = body, tag = tag)
    }

    override fun open(sealed: Box.Sealed, myPrivate: ByteArray, peerPublic: ByteArray, aad: ByteArray?): ByteArray {
        val me = X25519KeyPair.fromSeed(myPrivate)
        val shared = me.ecdh(peerPublic)
        val aesKey = hkdf.expand(shared, info = "shary:box".encodeToByteArray(), len = 32)
        return cipher.decrypt(aesKey, sealed.iv, sealed.ciphertext, sealed.tag, aad)
    }
}
