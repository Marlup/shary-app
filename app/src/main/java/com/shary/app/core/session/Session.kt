package com.shary.app.core.session

import android.content.Context
import android.util.Log
import com.shary.app.Field
import com.shary.app.services.security.CryptoManager
import com.shary.app.services.security.securityUtils.SecurityUtils.aesDecrypt
import com.shary.app.services.security.securityUtils.SecurityUtils.aesEncrypt
import com.shary.app.services.security.securityUtils.SecurityUtils.base64Encode
import com.shary.app.services.security.securityUtils.SecurityUtils.credentialsFile
import com.shary.app.services.security.securityUtils.SecurityUtils.signatureFile
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject

object Session {

    var sessionEmail: String = ""
    var sessionUsername: String = ""
    private var sessionSafePassword: String = ""
    var sessionAuthToken: String = ""
    var isSessionOnline: Boolean = false

    // Inyectado desde DependencyContainer
    lateinit var cryptoManager: CryptoManager

    private var checkedUsers: List<String>? = null

    var selectedEmails = MutableStateFlow<List<String>>(emptyList())
    var selectedPhoneNumber = MutableStateFlow<String?>("")
    var selectedFields = MutableStateFlow<List<Field>>(emptyList())
    var selectedRequestFields = MutableStateFlow<List<Field>>(emptyList())

    fun isSignatureActive(context: Context): Boolean = signatureFile(context).exists()
    fun isCredentialsActive(context: Context): Boolean = credentialsFile(context).exists()

    private fun isAuthenticated(username: String, safePassword: String): Boolean {
        Log.d("isAuthenticated - username vs", username + " - " + this.sessionUsername)
        Log.d("isAuthenticated - safePassword vs", safePassword + " - " + this.sessionSafePassword)
        if (username.isBlank() || safePassword.isBlank())
            return false
        if (username == this.sessionUsername && safePassword == this.sessionSafePassword) {
            return true
        }
        return false
    }

    fun logup(context: Context, username: String, email: String, password: String): Boolean {
        Log.d("logup - password", password)

        val safePassword = base64Encode(
            cryptoManager.hashPassword(password, username)
        )
        Log.d("logup - password", safePassword)

        // Generar nuevas claves y guardar firma
        cryptoManager.initializeKeysWithUser(context, username, safePassword)
        cryptoManager.saveSignature(context, username, email, safePassword)

        cacheCredentials(username, email, safePassword)
        storeCachedCredentials(context)

        return true
    }

    fun login(context: Context, username: String, password: String): Boolean {
        Log.d("login - password", password)
        val safePassword = base64Encode(
            cryptoManager.hashPassword(password, username)
        )
        Log.d("login - password", safePassword)

        // Regenerar claves deterministas desde semilla persistida
        cryptoManager.initializeKeysWithUser(context, username, safePassword)
        loadCredentials(context, username, safePassword)
        val ok: Boolean = isAuthenticated(username, safePassword)

        if (!ok)
            Log.d("Session - login", "Credentials didn't matched")
        return ok
    }

    fun storeCachedCredentials(context: Context) {
        val encryptionKey = cryptoManager.hashPassword(this.sessionSafePassword, this.sessionUsername)

        val json = JSONObject().apply {
            put("user_email", sessionEmail)
            put("user_username", sessionUsername)
            put("user_safe_password", sessionSafePassword)
            put("user_validation_token", sessionAuthToken)
        }

        val encrypted = aesEncrypt(encryptionKey, json.toString())
        val file = credentialsFile(context)
        if (!file.exists()) {
            file.writeBytes(encrypted)
            Log.d("Session", "Credentials stored successfully.")
        } else {
            Log.w("Session", "Credentials file already exists.")
        }
    }

    private fun loadCredentials(context: Context, username: String, safePassword: String) {
        val file = credentialsFile(context)
        if (!file.exists()) {
            Log.w("Session", "Credentials file not found.")
            return
        }

        val encrypted = file.readBytes()
        val encryptionKey = cryptoManager.hashPassword(safePassword, username)

        try {
            val decryptedJson = aesDecrypt(encryptionKey, encrypted)
            val data = JSONObject(decryptedJson)

            this.sessionEmail = data.optString("user_email")
            this.sessionUsername = data.optString("user_username")
            this.sessionSafePassword = data.optString("user_safe_password")
            this.sessionAuthToken = data.optString("user_validation_token")
        } catch (e: Exception) {
            Log.e("Session", "Error loading credentials: ${e.message}")
        }
    }

    fun deleteCredentials(context: Context) {
        val file = credentialsFile(context)
        if (file.exists()) file.delete()
        else Log.i("Session", "Credentials already deleted.")
    }

    // ---------------- Field Cache ----------------
    fun cacheSelectedFields(fields: List<Field>) {
        Log.d("Session", "Saving selected fields: $fields")
        selectedFields.value = fields
    }

    fun cacheSelectedEmails(emails: List<String>) {
        Log.d("Session", "Saving selected emails: $emails")
        selectedEmails.value = emails
    }

    fun cacheSelectedPhoneNumbers(phoneNumber: String) {
        Log.d("Session", "Saving selected phone number: $phoneNumber")
        selectedPhoneNumber.value = phoneNumber
    }

    fun cacheCredentials(username: String, email: String, safePassword: String) {
        this.sessionUsername = username
        this.sessionEmail = email
        this.sessionSafePassword = safePassword
    }

    fun resetSelectedData() {
        Log.d("Session", "Resetting selected fields/emails")
        selectedFields.value = emptyList()
        selectedEmails.value = emptyList()
    }

    fun getSelectedFields(): List<Field> = selectedFields.value
    fun getSelectedEmails(): List<String> = selectedEmails.value
    fun getSelectedPhoneNumber(): String? = selectedPhoneNumber.value
    fun isAnyFieldSelected(): Boolean = selectedFields.value.isNotEmpty()
    fun isAnyEmailSelected(): Boolean = selectedEmails.value.isNotEmpty()
}
