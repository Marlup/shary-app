package com.shary.app.infrastructure.persistance.credentials

import android.content.Context
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.infrastructure.security.helper.SecurityUtils.credentialsFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.signatureFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FileCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context
) : CredentialsStore {
    override fun hasSignature(context: Context) = signatureFile(context).exists()
    override fun hasCredentials(context: Context) = credentialsFile(context).exists()

    override fun readCredentials(context: Context): ByteArray? =
        credentialsFile(context).let { if (it.exists()) it.readBytes() else null }

    override fun writeCredentials(context: Context, bytes: ByteArray) {
        val f = credentialsFile(context)
        if (!f.exists()) f.parentFile?.mkdirs()
        f.writeBytes(bytes) // overwrite-on-write
    }

    override fun deleteCredentials(context: Context) {
        credentialsFile(context).takeIf { it.exists() }?.delete()
    }
}
