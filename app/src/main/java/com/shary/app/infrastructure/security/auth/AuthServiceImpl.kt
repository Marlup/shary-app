package com.shary.app.infrastructure.security.auth

import android.content.Context
import android.util.Log
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.AuthBackend
import com.shary.app.core.domain.interfaces.security.AuthService
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.states.AuthState
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.infrastructure.security.helper.SecurityUtils
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Encode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthServiceImpl
 *
 * Responsibilities:
 * - Orchestrates sign-up/sign-in using CryptographyManager.
 * - Persists user credentials as an **encrypted JSON blob** (via cryptoManager).
 * - Exposes reactive AuthState for UI.
 *
 * Storage format:
 * - JSON fields:
 *     user_email, user_username, user_safe_password, user_validation_token, version, ts
 * - Entire JSON is encrypted by CryptographyManager.encryptCredentialsJson().
 */
@Singleton
class AuthServiceImpl @Inject constructor(
    private val crypto: CryptographyManager,
    private val store: CredentialsStore,
    private val backend: AuthBackend
) : AuthService {

    private val DELAY_TIME_SECONDS = 40L
    private val _state = MutableStateFlow(AuthState())
    override val state: StateFlow<AuthState> = _state


    // AuthToken
    override fun getAuthToken(): String = _state.value.authToken
    override fun setAuthToken(newAuthToken: String) { _state.value.authToken = newAuthToken }


    // IsOnline
    override fun getIsOnline(): Boolean = state.value.isOnline
    override fun setIsOnline(value: Boolean) {  _state.value.isOnline = value }


    // SafePassword
    override fun getSafePassword(): String = state.value.safePassword
    override fun setSafePassword(value: String) { _state.value.safePassword = value }


    // Local Keys
    override fun getLocalKeyByPurpose(purpose: Purpose): ByteArray? =
        state.value.localKeys[purpose.code]
    override fun addLocalKeyByPurpose(purpose: Purpose, value: ByteArray) {
        _state.value.localKeys[purpose.code] = value
    }


    // SafePassword
    override fun isSignatureActive(context: Context) = store.hasSignature(context)
    override fun isCredentialsActive(context: Context) = store.hasCredentials(context)


    /**
     * Sign-up flow:
     * 1) Initialize keys + save signature file.
     * 2) Register identity in the in-memory "server".
     * 3) Cache and persist encrypted credentials JSON through CryptographyManager.
     */
    override suspend fun signUp(context: Context, username: String, email: String, password: String) = runCatching {
        val safe = computeSafePassword(username, password)
        crypto.initializeKeysWithUser(context, username, safe)
        crypto.saveSignature(context, username, email, safe)

        val signPub = crypto.getSignPublic()
        val kexPub  = crypto.getKexPublic()
        //check(backend.registerIdentity(username, email, signPub, kexPub))

        cacheCredentials(username, email, safe)
        persistCredentials(context)
        preLoadLocalKeys(username, safe)
    }

    /**
     * Sign-in flow:
     * 1) Initialize keys for the user.
     * 2) Decrypt credentials JSON and verify match.
     * 3) Perform challenge/response against in-memory "server".
     */
    override suspend fun signIn(context: Context, username: String, password: String) = runCatching {
        val safe = computeSafePassword(username, password)
        crypto.initializeKeysWithUser(context, username, safe)

        preLoadLocalKeys(username, safe)
        loadCredentials(context, username, safe)
        check(isAuthenticated(username, safe)) { "Invalid credentials" }

        // Start challenge/response:
        //val challenge = backend.requestChallenge(username)
        //val signature = crypto.signDetached(challenge)
        //check(backend.verifyLogin(username, challenge, signature)) { "Challenge verification failed" }

        //_state.value = _state.value.copy(isOnline = true)


    }

    /** Clears in-memory state and deletes the encrypted credentials file. */
    override fun signOut(context: Context) {
        _state.value = AuthState()
        store.deleteCredentials(context)
    }

    // ---------------- Helpers ----------------

    /**
     * Computes the “safePassword” used across the app:
     * base64( hashPassword(plainPassword, username) )
     */
    private fun computeSafePassword(username: String, password: String): String =
        base64Encode(crypto.hashPassword(password, username))

    private fun cacheCredentials(username: String, email: String, safe: String, token: String = "") {
        _state.value = _state.value.copy(
            username = username,
            email = email,
            safePassword = safe,
            authToken = token
        )
    }

    /** Build + encrypt the JSON credentials + store. */
    private fun persistCredentials(context: Context) {
        val s = _state.value

        Log.d("AuthServiceImpl", "persistCredentials - username: ${s.username}")

        val json = JSONObject().apply {
            put("user_email", s.email)
            put("user_username", s.username)
            put("user_safe_password", s.safePassword)
            put("user_validation_token", s.authToken)
            put("version", 2) // bump as needed for future schema evolution
            put("ts", SecurityUtils.getCurrentUtcTimestamp())
        }
        val bytes = crypto.encryptCredentialsByDerivation(
            s.username,
            s.safePassword,
            Purpose.Credentials.code,
            json
        )
        store.writeCredentials(context, bytes)
        Log.d("Auth", "Encrypted credentials stored (${bytes.size} bytes).")
    }

    /** Read, decrypt and load the JSON credentials through CryptographyManager. */
    private fun loadCredentials(context: Context, username: String, safe: String) {
        val encrypted = store.readCredentials(context) ?: error("Credentials file not found")
        val data = crypto.decryptCredentials(
            username,
            getLocalKeyByPurpose(Purpose.Credentials)!!,
            encrypted
        )

        Log.d("AuthServiceImpl", "loadCredentials - username: ${data.optString("user_username")}")

        cacheCredentials(
            username = data.optString("user_username"),
            email    = data.optString("user_email"),
            safe     = data.optString("user_safe_password"),
            token    = data.optString("user_validation_token")
        )
        Log.d("AuthServiceImpl", "Encrypted credentials loaded (${encrypted.size} bytes).")
        Log.d("AuthServiceImpl", "loadCredentialsEncryptedJson (username) - ${data.optString("user_username")}")
        Log.d("AuthServiceImpl", "loadCredentialsEncryptedJson (email) - ${data.optString("user_email")}")
        Log.d("AuthServiceImpl", "loadCredentialsEncryptedJson (password) - ${data.optString("user_safe_password")}")
        Log.d("AuthServiceImpl", "loadCredentialsEncryptedJson (token) - ${data.optString("user_validation_token")}")
    }

    private fun isAuthenticated(username: String, safePassword: String): Boolean {
        val s = _state.value
        Log.d("AuthServiceImpl", "isAuthenticated (usernames) - ${s.username}, $username}")
        Log.d("AuthServiceImpl", "isAuthenticated (safePasswords) - ${s.safePassword}, $safePassword}")
        return username.isNotBlank() && safePassword.isNotBlank() &&
                s.username == username && s.safePassword == safePassword
    }

    // ---------------- In-memory “server” simulation ----------------
    private val rng = SecureRandom()
    private val users = ConcurrentHashMap<String, Pair<ByteArray, ByteArray>>() // user -> (signPub, kexPub)
    private val challenges = ConcurrentHashMap<String, ByteArray>()

    /** Stores the user's public keys. Replace with real backend call in prod. */
    private suspend fun doRegisterIdentity(
        username: String,
        email: String,
        signPublic: ByteArray,
        kexPublic: ByteArray
    ): Boolean {
        delay(50)
        users[username] = signPublic to kexPublic
        return true
    }

    /** Verifies challenge equality and signature using the registered Ed25519 pub key. */
    private suspend fun doVerifyLogin(
        username: String,
        challenge: ByteArray,
        signature: ByteArray
    ): Boolean {
        delay(DELAY_TIME_SECONDS)
        Log.w("AuthServiceImpl", "doVerifyLogin: $challenge, ${challenges[username]}")
        Log.w("AuthServiceImpl", "doVerifyLogin: ${!challenges[username].contentEquals(challenge)}")
        Log.w("AuthServiceImpl", "users: $users")
        Log.w("AuthServiceImpl", "users[username]: ${users[username]}")
        val (signPub, _) = users[username] ?: return false
        val last = challenges[username] ?: return false
        Log.w("AuthServiceImpl", "before last.contentEquals: ${!challenges[username].contentEquals(challenge)}")
        if (!last.contentEquals(challenge)) return false
        Log.w("AuthServiceImpl", "after last.contentEquals: ${!challenges[username].contentEquals(challenge)}")
        return crypto.verifyDetached(challenge, signature, signPub)
    }

    private fun preLoadLocalKeys(username: String, password: String) {
        Purpose.builtIns.forEach { purpose ->
            addLocalKeyByPurpose(
                purpose,
                crypto.deriveLocalKey(
                    username,
                    password.toCharArray(),
                    purpose.code
                )
            )
        }
    }
}
