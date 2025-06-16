/*
package com.shary.app.services.security

import java.math.BigInteger
import java.security.MessageDigest

class `DeterministicRNG.disabled`(private val seed: ByteArray) {
    private var counter: Int = 0

    private fun getBytes(n: Int): ByteArray {
        val output = mutableListOf<Byte>()
        while (output.size < n) {
            val counterBytes = counter.toBigEndianBytes()
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(seed)
            digest.update(counterBytes)
            output.addAll(digest.digest().toList())
            counter++
        }
        return output.take(n).toByteArray()
    }

    private fun getInt(bits: Int): BigInteger {
        val nbytes = (bits + 7) / 8
        return BigInteger(1, getBytes(nbytes))
    }

    fun getPrime(bits: Int): BigInteger {
        while (true) {
            val candidate = getInt(bits).setBit(0) // Force odd number
            if (candidate.isProbablePrime(40)) {
                return candidate
            }
        }
    }

    private fun Int.toBigEndianBytes(): ByteArray {
        return byteArrayOf(
            ((this shr 24) and 0xFF).toByte(),
            ((this shr 16) and 0xFF).toByte(),
            ((this shr 8) and 0xFF).toByte(),
            (this and 0xFF).toByte()
        )
    }
}
*/