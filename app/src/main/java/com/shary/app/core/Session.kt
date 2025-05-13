package com.shary.app.core

import android.content.Context
import android.util.Log
import com.shary.app.Field
import com.shary.app.core.constants.Constants.PATH_AUTHENTICATION
import com.shary.app.core.constants.Constants.PATH_AUTH_SIGNATURE
import com.shary.app.security.CryptographyManager
import com.shary.app.security.CryptographyManager.generateKeysFromSecrets
import com.shary.app.security.securityUtils.SecurityUtils.aesDecrypt
import com.shary.app.security.securityUtils.SecurityUtils.aesEncrypt
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import org.json.JSONObject

object Session {

    var email: String = ""
    var username: String = ""
    private var safePassword: String = ""
    var authToken: String = ""
    var isOnline: Boolean = false

    var cryptographyManager: CryptographyManager? = null
    private var checkedUsers: List<String>? = null

    var selectedEmails = MutableStateFlow<List<String>>(emptyList())
    var selectedPhoneNumber = MutableStateFlow<String?>("")
    var selectedFields = MutableStateFlow<List<Field>>(emptyList())
    var selectedRequestFields = MutableStateFlow<List<Field>>(emptyList())

    fun canActivateSendingSummary(): Boolean{
        return selectedEmails.value.isNotEmpty() &&
                selectedFields.value.isNotEmpty()
    }

    fun initialize(cryptographyManager: CryptographyManager) {
        this.cryptographyManager = cryptographyManager
    }

    fun generateKeys(password: String, username: String) {
        if (password.isBlank() or username.isBlank())
            generateKeysFromSecrets(password, username)
    }

    fun isAuthenticated(): Boolean = email.isNotBlank() && safePassword.isNotBlank()

    fun cacheCredentials(email: String, username: String, password: String) {
        this.email = email
        this.username = username
        this.safePassword = password
        Log.d("Session", "User credentials cached.")
    }

    fun storeCachedCredentials(context: Context) {
        cryptographyManager?.saveSignature(context, email, username, safePassword)

        email.let { e ->
            username.let { u ->
                safePassword.let { p ->
                    authToken.let { v ->
                        storeCredentials(context, e, u, p, v)
                    }
                }
            }
        }
    }

    private fun credentialsFile(context: Context): File {
        val credentialsDir = File(context.filesDir, PATH_AUTHENTICATION)
        if (!credentialsDir.exists()) {
            credentialsDir.mkdirs()
        }
        return File(credentialsDir, ".credentials")
    }
    
    private fun authSignatureFile(context: Context): File {
        val authSignatureDir = File(context.filesDir, PATH_AUTH_SIGNATURE)
        if (!authSignatureDir.exists()) {
            authSignatureDir.mkdirs()
        }
        return File(authSignatureDir, "auth_signature.json")
    }

    private fun existsUser(context: Context): Boolean = credentialsFile(context).exists()

    private fun storeCredentials(
        context: Context,
         email: String,
         username: String,
         password: String,
         token: String
         ) {
        val encryptionKey = cryptographyManager?.hashPassword(password, username) ?: return
        val safePasswordBytes = encryptionKey.joinToString(separator = "") {
            String.format("%02x", it)
        }

        if (!existsUser(context)) {
            val json = JSONObject().apply {
                put("user_email", email)
                put("user_username", username)
                put("user_safe_password", safePasswordBytes)
                put("user_validation_token", token)
            }
            val encrypted = aesEncrypt(encryptionKey, json.toString())

            credentialsFile(context).writeBytes(encrypted)
            if (existsUser(context))
                Log.d("Session", "Credentials stored successfully.")
        } else {
            Log.w("Session", "Cannot store credentials. File already exists.")
        }
    }

    private fun loadCredentials(context: Context, username: String, password: String) {
        if (!existsUser(context)) {
            Log.w("Session", "Credentials file not found.")
            return
        }
        val encrypted = credentialsFile(context).readBytes()
        val encryptionKey = CryptographyManager.hashPassword(password, username)

        try {
            val decryptedJson = aesDecrypt(encryptionKey, encrypted)
            val data = JSONObject(decryptedJson)

            this.email = data.optString("user_email")
            this.username = data.optString("user_username")
            this.safePassword = data.optString("user_safe_password")
            this.authToken = data.optString("user_validation_token")
        } catch (e: Exception) {
            Log.e("Session", "Error loading credentials: ${e.message}")
        }
    }

    fun deleteCredentials(context: Context) {
        if (credentialsFile(context).exists()) {
            credentialsFile(context).delete()
        } else {
            Log.i("Session", "Credentials file already deleted.")
        }
    }

    fun getCheckedUsers(): List<String> = checkedUsers ?: emptyList()

    fun setCheckedUsers(users: List<String>?) {
        if (users != null) checkedUsers = users
    }

    fun tryLogin(context: Context, uiUsername: String, uiPassword: String): Boolean {
        // Make password string
        val uiSafePassword = cryptographyManager?.hashPassword(uiPassword, uiUsername) ?: return false
        val uiSafePasswordString = uiSafePassword.joinToString(separator = "") {
            String.format("%02x", it)
        }

        // Load Credentials
        loadCredentials(context, uiUsername, uiPassword)

        Log.d("Session", "Username: $username | $uiUsername")
        Log.d("Session", "Password: $safePassword | $uiSafePassword")
        return uiUsername == username && uiSafePasswordString == safePassword
    }

    fun isSignatureActive(context: Context): Boolean = authSignatureFile(context).exists()
    fun isCredentialsActive(context: Context): Boolean = credentialsFile(context).exists()

    // Cache selected fields
    fun cacheSelectedFields(fields: List<Field>) {
        println("Saving selected keys on stop: $fields")
        selectedFields.value = fields
    }

    fun cacheSelectedEmails(emails: List<String>) {
        println("Saving selected emails on stop: $emails")
        selectedEmails.value = emails
    }

    fun cacheSelectedPhoneNumbers(phoneNumber: String) {
        println("Saving selected phone Numbers on stop: $phoneNumber")
        selectedPhoneNumber.value = phoneNumber
    }
}
