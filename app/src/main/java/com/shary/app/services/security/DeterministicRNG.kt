package com.shary.app.services.security

import java.math.BigInteger
import java.security.MessageDigest

class DeterministicRNG(seed: ByteArray) {
    private var state: ByteArray = sha256(seed)

    fun nextBytes(n: Int): ByteArray {
        val output = ByteArray(n)
        var offset = 0
        while (offset < n) {
            state = sha256(state)
            val chunkSize = minOf(n - offset, state.size)
            System.arraycopy(state, 0, output, offset, chunkSize)
            offset += chunkSize
        }
        return output
    }

    fun nextPrime(bits: Int): BigInteger {
        var candidate: BigInteger
        do {
            val bytes = nextBytes((bits + 7) / 8)
            candidate = BigInteger(1, bytes).setBit(bits - 1) // asegurar tamaño
        } while (!candidate.isProbablePrime(100))
        return candidate
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }
}
