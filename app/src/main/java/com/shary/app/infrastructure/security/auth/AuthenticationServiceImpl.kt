package com.shary.app.infrastructure.security.auth

import android.content.Context
import android.util.Log
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.interfaces.states.AuthState
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.infrastructure.security.helper.SecurityUtils
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Encode
import com.shary.app.infrastructure.services.cloud.CloudServiceImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthenticationServiceImpl
 *
 * Responsibilities:
 * - Coordinates sign-up and sign-in flows with CryptographyManager.
 * - Persists user credentials as an encrypted JSON blob using LocalVault.
 * - Keeps current AuthState as a reactive StateFlow for UI observation.
 * - Optionally simulates a backend for challenge/response authentication.
 */
@Singleton
class AuthenticationServiceImpl @Inject constructor(
    private val crypto: CryptographyManager,
    private val store: CredentialsStore,
    private val cloud: CloudService,
    private val cache: CacheService,
) : AuthenticationService {

    private val DELAY_TIME_SECONDS = 40L
    private val _state = MutableStateFlow(AuthState())
    override val state: StateFlow<AuthState> = _state

    // -------------------- State accessors --------------------

    override fun getAuthToken(): String = _state.value.authToken
    override fun setAuthToken(newAuthToken: String) { _state.value.authToken = newAuthToken }

    override fun getIsOnline(): Boolean = state.value.isOnline
    override fun setIsOnline(value: Boolean) { _state.value.isOnline = value }

    override fun getSafePassword(): String = state.value.safePassword
    override fun setSafePassword(value: String) { _state.value.safePassword = value }

    override fun getLocalKeyByPurpose(purpose: Purpose): ByteArray? =
        state.value.localKeys[purpose.code]
    override fun addLocalKeyByPurpose(purpose: Purpose, value: ByteArray) {
        _state.value.localKeys[purpose.code] = value
    }

    override fun isSignatureActive(context: Context) = store.hasSignature(context)
    override fun isCredentialsActive(context: Context) = store.hasCredentials(context)

    // -------------------- Sign-up / Sign-in --------------------

    /**
     * Sign-up flow:
     * 1) Derive safe password from username + plain password.
     * 2) Initialize local keys with CryptographyManager.
     * 3) Persist signature file (contains only public keys).
     * 4) Persist encrypted JSON credentials in storage.
     */
    override suspend fun signUp(context: Context, username: String, email: String, password: String) = runCatching {
        val safePassword = computeSafePassword(email, password)
        Log.d("AuthenticationServiceImpl", "signUp - email: $email")

        // Initialize keys locally
        crypto.initializeKeysWithUser(context, email, safePassword)
        crypto.saveSignature(context, username, email, safePassword)

        // --- CLOUD REGISTRATION ---
        val cloudReachable = cloud.sendPing()
        Log.w("AuthenticationServiceImpl", "Cloud registration launched")
        if (cloudReachable) {
            val registered = cloud.isUserRegisteredInCloud(email)
        Log.w("AuthenticationServiceImpl", "Cloud isUserRegisteredInCloud launched")
            if (!registered) {
                val token = cloud.uploadUser(email)
                if (token.isNotEmpty()) {
                    Log.d("AuthenticationServiceImpl", "Cloud user uploaded, token received.")
                    setAuthToken(token)
                } else {
                    Log.w("AuthenticationServiceImpl", "Cloud upload failed, using local only.")
                }
            } else {
                Log.w("AuthenticationServiceImpl", "User already exists in cloud.")
            }
        } else {
            Log.w("AuthenticationServiceImpl", "Cloud unreachable, continuing offline sign-up.")
        }

        // Persist locally
        cacheCredentials(username, email, safePassword,getAuthToken())
        persistCredentials(context)
        preLoadLocalKeys(email, safePassword)
    }

    /**
     * Sign-in flow:
     * 1) Derive safe password.
     * 2) Initialize keys for that user.
     * 3) Load and decrypt JSON credentials from local storage.
     * 4) Verify that provided email + password match stored credentials.
     */
    override suspend fun signIn(context: Context, email: String, password: String) = runCatching {
        val safePassword = computeSafePassword(email, password)
        crypto.initializeKeysWithUser(context, email, safePassword)
        preLoadLocalKeys(email, safePassword)

        loadCredentials(context, email)
        check(isAuthenticated(email, safePassword)) { "Invalid credentials" }

        // --- CLOUD VERIFICATION ---
        val online = cloud.sendPing()
        Log.w("AuthenticationServiceImpl", "Cloud verification launched")
        if (online) {
            val isRegistered = cloud.isUserRegisteredInCloud(email)
            Log.w("AuthenticationServiceImpl", "Cloud isUserRegisteredInCloud launched")
            if (!isRegistered) {
                Log.w("AuthenticationServiceImpl", "After ping: User not found in cloud, forcing re-upload.")
                cloud.uploadUser(email)
            } else {
                Log.d("AuthenticationServiceImpl", "After ping: User verified in cloud.")
            }

            // Obtain/refresh token for cloud sync
            val refreshed = cloud.refreshIdToken().getOrNull()
            if (!refreshed.isNullOrEmpty()) {
                setAuthToken(refreshed)
            } else {
                Log.w("AuthenticationServiceImpl", "Failed to refresh ID token from cloud.")
            }
        } else {
            Log.w("AuthenticationServiceImpl", "Cloud offline, continuing local sign-in.")
        }
    }

    /** Clears in-memory state and deletes the encrypted credentials file. */
    override suspend fun signOut(context: Context) {
        _state.value = AuthState()
        cloud.signOutCloud()
        store.deleteCredentials(context)
    }

    // -------------------- Helpers --------------------

    /** Safe password = base64( SHA256(password + ":" + code) ) */
    private fun computeSafePassword(salt: String, password: String): String =
        base64Encode(crypto.hashPassword(password, salt))

    private fun cacheCredentials(username: String, email: String, safePassword: String, token: String = "") {
        _state.value = _state.value.copy(
            username = username,
            email = email,
            safePassword = safePassword,
            authToken = token
        )
    }

    /** Persists credentials as an encrypted JSON blob using CryptographyManager. */
    private fun persistCredentials(context: Context) {
        val s = _state.value
        Log.d("AuthenticationServiceImpl", "persistCredentials - username: ${s.username}")

        val json = JSONObject().apply {
            put("user_email", s.email)
            put("user_username", s.username)
            put("user_safe_password", s.safePassword)
            put("user_validation_token", s.authToken)
            put("version", 2)
            put("ts", SecurityUtils.getCurrentUtcTimestamp())
        }
        val bytes = crypto.encryptCredentialsByDerivation(
            s.email,
            s.safePassword,
            Purpose.Credentials.code,
            json
        )
        store.writeCredentials(context, bytes)
        cache.cacheOwnerUsername(s.username)
        cache.cacheOwnerEmail(s.email)
        Log.d("Auth", "Encrypted credentials stored (${bytes.size} bytes).")
    }

    /** Loads credentials JSON from encrypted storage and updates AuthState. */
    private fun loadCredentials(context: Context, email: String) {
        val encrypted = store.readCredentials(context) ?: error("Credentials file not found")
        val data = crypto.decryptCredentials(
            email,
            getLocalKeyByPurpose(Purpose.Credentials)!!,
            encrypted
        )
        Log.d("AuthenticationServiceImpl", "loadCredentials - username: ${data.optString("user_username")}")
        val username = data.optString("user_username");
        val email = data.optString("user_email");
        cacheCredentials(
            username = data.optString("user_username"),
            email    = data.optString("user_email"),
            safePassword     = data.optString("user_safe_password"),
            token    = data.optString("user_validation_token")
        )
        cache.cacheOwnerUsername(username)
        cache.cacheOwnerEmail(email)
        Log.d("AuthenticationServiceImpl", "Encrypted credentials loaded (${encrypted.size} bytes).")
    }

    /** Verifies if email + safePassword match in-memory AuthState. */
    private fun isAuthenticated(email: String, safePassword: String): Boolean {
        val s = _state.value
        return email.isNotBlank() && safePassword.isNotBlank() &&
                s.email == email && s.safePassword == safePassword
    }

    // -------------------- In-memory backend simulation --------------------
    private val users = ConcurrentHashMap<String, Pair<ByteArray, ByteArray>>() // username -> (signPub, kexPub)
    private val challenges = ConcurrentHashMap<String, ByteArray>()

    /** Stores user's public keys (used in fake backend simulation). */
    private suspend fun doRegisterIdentity(username: String, email: String, signPublic: ByteArray, kexPublic: ByteArray): Boolean {
        delay(50)
        users[email] = signPublic to kexPublic
        return true
    }

    /** Verifies challenge/response (for future backend). */
    private suspend fun doVerifyLogin(email: String, challenge: ByteArray, signature: ByteArray): Boolean {
        delay(DELAY_TIME_SECONDS)
        val (signPub, _) = users[email] ?: return false
        val last = challenges[email] ?: return false
        if (!last.contentEquals(challenge)) return false
        return crypto.verifyDetached(challenge, signature, signPub)
    }

    /** Preloads local symmetric keys for all built-in purposes. */
    private fun preLoadLocalKeys(email: String, password: String) {
        Purpose.builtIns.forEach { purpose ->
            addLocalKeyByPurpose(
                purpose,
                crypto.deriveLocalKey(
                    email,
                    password.toCharArray(),
                    purpose.code
                )
            )
        }
    }
}
