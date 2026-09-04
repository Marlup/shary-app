package com.shary.app.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shary.app.infrastructure.persistance.credentials.FileCredentialsStore
import com.shary.app.infrastructure.security.helper.SecurityUtils
import com.shary.app.infrastructure.security.helper.SecurityUtils.credentialsFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.signatureFile
import com.shary.app.infrastructure.security.helper.SecurityUtils.writeBytesAtomic
import com.shary.app.infrastructure.security.helper.SecurityUtils.writeTextAtomic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFilePersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun cleanup() {
        credentialsFile(context).takeIf { it.exists() }?.delete()
        signatureFile(context).takeIf { it.exists() }?.delete()
        SecurityUtils.deleteCredentialsTimestamp(context)
    }

    @Test
    fun writeBytesAtomic_persistsCredentialsBlob() {
        val file = credentialsFile(context)
        val payload = "test-credentials".encodeToByteArray()

        writeBytesAtomic(file, payload)

        assertTrue(file.exists())
        assertEquals("test-credentials", file.readText())
    }

    @Test
    fun writeTextAtomic_persistsSignatureJson() {
        val file = signatureFile(context)
        val json = "{\"ok\":true}"

        writeTextAtomic(file, json)

        assertTrue(file.exists())
        assertEquals(json, file.readText())
    }

    @Test
    fun fileCredentialsStore_roundTrip_and_delete() {
        val store = FileCredentialsStore(context)
        val payload = "encrypted-blob".encodeToByteArray()

        store.writeCredentials(context, payload)
        val read = store.readCredentials(context)

        assertNotNull(read)
        assertEquals("encrypted-blob", read!!.decodeToString())

        store.deleteCredentials(context)
        assertFalse(store.hasCredentials(context))
    }
}
