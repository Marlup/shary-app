package com.shary.app.core.domain.security

import com.shary.app.core.domain.types.valueobjects.Sealed


interface Box {

    fun seal(
        plain: ByteArray,
        myPrivate: ByteArray,
        peerPublic: ByteArray,
        aad: ByteArray? = null
    ): Sealed
    fun open(
        sealed: Sealed,
        myPrivate: ByteArray,
        peerPublic: ByteArray,
        aad: ByteArray? = null
    ): ByteArray
}
