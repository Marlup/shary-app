package com.shary.app.infrastructure.persistance.credentials

import android.content.Context
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.infrastructure.security.helper.SecurityUtils.credentialsFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.credentialsLockFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.legacyCredentialsFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.markCredentialsLocked as markLockedFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.clearCredentialsLocked as clearLockedFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.isCredentialsLocked as isLockedFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.signatureFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.writeBytesAtomic
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FileCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context
) : CredentialsStore {
    override fun hasSignature(context: Context) = signatureFile(context).exists() || hasCredentials(context)
    override fun hasCredentials(context: Context) =
        credentialsFile(context).exists() || legacyCredentialsFile(context).exists()

    override fun hasUsableCredentials(context: Context, crypto: CryptographyManager): Boolean {
        val blob = readCredentials(context) ?: return false
        val usable = crypto.isCredentialsBlobUsable(blob)
        if (usable) {
            clearLockedFile(context)
        } else {
            markLockedFile(context)
        }
        return usable
    }

    override fun isCredentialsLocked(context: Context): Boolean = isLockedFile(context)

    override fun markCredentialsLocked(context: Context) {
        markLockedFile(context)
    }

    override fun clearCredentialsLocked(context: Context) {
        clearLockedFile(context)
    }

    override fun readCredentials(context: Context): ByteArray? =
        credentialsFile(context).takeIf { it.exists() }?.readBytes()
            ?: legacyCredentialsFile(context).takeIf { it.exists() }?.readBytes()

    override fun writeCredentials(context: Context, bytes: ByteArray) {
        val f = credentialsFile(context)
        if (!f.exists()) f.parentFile?.mkdirs()
        writeBytesAtomic(f, bytes)
        legacyCredentialsFile(context).takeIf { it.exists() }?.delete()
    }

    override fun deleteCredentials(context: Context) {
        credentialsFile(context).takeIf { it.exists() }?.delete()
        legacyCredentialsFile(context).takeIf { it.exists() }?.delete()
        credentialsLockFile(context).takeIf { it.exists() }?.delete()
    }
}
