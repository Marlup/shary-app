package com.shary.app.infrastructure.services.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class VaultBackupFileService @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun write(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: throw IOException("Could not open the selected file for writing.")
    }

    suspend fun read(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use(::readLimited)
            ?: throw IOException("Could not open the selected file for reading.")
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_BACKUP_BYTES) {
                throw IOException("Vault backup exceeds the 64 MB limit.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        const val MAX_BACKUP_BYTES = 64 * 1024 * 1024
    }
}
