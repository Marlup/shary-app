package com.shary.app.core.domain.interfaces.states

import com.shary.app.infrastructure.security.helper.SecurityUtils
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Encode
import org.bouncycastle.util.encoders.Base64


data class Identity(
    private val signPublic: ByteArray,
    private val kexPublic: ByteArray,
    private val signSeed: ByteArray,   // Private keys are created on-demand from the seed
    private val kexSeed: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Identity

        if (!signPublic.contentEquals(other.signPublic)) return false
        if (!kexPublic.contentEquals(other.kexPublic)) return false
        if (!signSeed.contentEquals(other.signSeed)) return false
        if (!kexSeed.contentEquals(other.kexSeed)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signPublic.contentHashCode()
        result = 31 * result + kexPublic.contentHashCode()
        result = 31 * result + signSeed.contentHashCode()
        result = 31 * result + kexSeed.contentHashCode()
        return result
    }

    // ==================== Getters ====================

    // Both Public Keys
    fun getSignPublic(): ByteArray = signPublic.copyOf()
    fun getKexPublic(): ByteArray = kexPublic.copyOf()
    fun getSignPublicString(): String = SecurityUtils.base64Encode(signPublic.copyOf())
    fun getKexPublicKeyString(): String = SecurityUtils.base64Encode(kexPublic.copyOf())

    // Both Seeds
    fun getSignSeed(): ByteArray = signSeed.copyOf()
    fun getKexSeed(): ByteArray = kexSeed.copyOf()
    fun getSignSeedString(): String = SecurityUtils.base64Encode(signSeed.copyOf())
    fun getKexSeedString(): String = SecurityUtils.base64Encode(kexSeed.copyOf())
}