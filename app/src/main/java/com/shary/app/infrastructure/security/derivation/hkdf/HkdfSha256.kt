package com.shary.app.infrastructure.security.derivation.hkdf

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class HkdfSha256 {
    private fun prk(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(if (salt.isEmpty()) ByteArray(32) else salt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }
    fun expand(ikm: ByteArray, info: ByteArray, len: Int, salt: ByteArray = ByteArray(0)): ByteArray {
        val prk = prk(salt, ikm)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        var t = ByteArray(0)
        val out = ByteArray(len)
        var pos = 0
        var ctr = 1
        while (pos < len) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(ctr.toByte())
            t = mac.doFinal()
            val copy = minOf(t.size, len - pos)
            System.arraycopy(t, 0, out, pos, copy)
            pos += copy
            ctr++
        }
        return out
    }
}
