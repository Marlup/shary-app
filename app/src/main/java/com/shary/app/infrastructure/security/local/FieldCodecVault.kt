package com.shary.app.infrastructure.security.local

import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.core.session.Session
import javax.inject.Inject


class FieldCodecVault @Inject constructor(
    private val vault: LocalVault,
    private val session: Session
) : FieldCodec {
    //private val u get() = session.getOwnerUsername()
    //private val p get() = session.getOwnerSafePassword().toCharArray()

    override fun encode(message: String, purpose: Purpose): String =
        vault.encryptToString(message, getLocalKeyByPurpose(purpose), null)

    override fun decode(message: String, purpose: Purpose): String =
        vault.decryptToString(message, getLocalKeyByPurpose(purpose), null)

    private fun getLocalKeyByPurpose(purpose: Purpose): ByteArray =
        session.getLocalKeyByPurpose(purpose) ?: throw Exception("No local key for purpose: $purpose")
}
