package com.shary.app.infrastructure.security.local

import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.security.CredentialsProvider


class FieldCodecVault(
    private val vault: LocalVault,
    private val creds: CredentialsProvider
) : FieldCodec {
    private val u get() = creds.username()
    private val p get() = creds.password()

    override fun encode(message: String, purposeString: String): String =
        vault.encryptToString(message, u, p, purposeString, null)

    override fun decode(message: String, purposeString: String): String =
        vault.decryptToString(message, u, p, purposeString, null)
}
