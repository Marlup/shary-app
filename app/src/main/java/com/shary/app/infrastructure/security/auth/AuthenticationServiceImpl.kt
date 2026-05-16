package com.shary.app.infrastructure.security.auth

import android.content.Context
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.interfaces.states.AuthState
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.infrastructure.security.helper.SecurityUtils
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Encode
import com.shary.app.infrastructure.security.helper.SecurityUtils.deleteCredentialsTimestamp
import com.shary.app.infrastructure.security.helper.SecurityUtils.deleteSignatureFile
import com.shary.app.infrastructure.security.manager.CredentialsWrapKeyException
import com.shary.app.utils.log.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
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
    private val firebaseAuth: FirebaseAuth,
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
     * 1) Derive safe password from email + plain password.
     * 2) Initialize local keys with CryptographyManager.
     * 3) Persist signature file (contains only public keys).
     * 4) Persist encrypted JSON credentials in storage.
     */
    override suspend fun signUp(context: Context, username: String, email: String, password: String) = runCatching {
        val canonicalEmail = normalizeEmail(email)
        val safePassword = computeSafePassword(canonicalEmail, password)
        AppLogger.info("AuthenticationServiceImpl", "event=signup_start email=${AppLogger.emailHint(canonicalEmail)}")
        val isEmailVerified = ensureFirebaseEmailSignupAndVerification(canonicalEmail, password)
        cloud.cloudState.value = cloud.cloudState.value.copy(
            email = canonicalEmail,
            isUserValidated = isEmailVerified
        )

        // Initialize keys locally
        crypto.initializeKeysWithUser(context, canonicalEmail, safePassword)
        crypto.saveSignature(context, username, canonicalEmail, safePassword)

        // --- CLOUD REGISTRATION ---
        val cloudReachable = cloud.sendPing()
        AppLogger.info("AuthenticationServiceImpl", "event=cloud_registration_start")
        if (cloudReachable && isEmailVerified) {
            val registered = cloud.isUserRegisteredInCloud(canonicalEmail)
        AppLogger.debug("AuthenticationServiceImpl", "event=cloud_check_user_registered")
            if (!registered) {
                val token = cloud.uploadUser(canonicalEmail)
                if (token.isNotEmpty()) {
                    AppLogger.info("AuthenticationServiceImpl", "event=cloud_user_uploaded")
                    setAuthToken(token)
                } else {
                    AppLogger.warn("AuthenticationServiceImpl", "event=cloud_upload_failed_using_local")
                }
            } else {
                AppLogger.info("AuthenticationServiceImpl", "event=cloud_user_already_exists")
            }
        } else {
            if (!isEmailVerified) {
                AppLogger.warn("AuthenticationServiceImpl", "event=signup_email_not_verified_cloud_registration_deferred")
            } else {
                AppLogger.warn("AuthenticationServiceImpl", "event=cloud_unreachable_offline_signup")
            }
        }

        // Persist locally
        cacheCredentials(username, canonicalEmail, safePassword,getAuthToken())
        persistCredentials(context)
        preLoadLocalKeys(canonicalEmail, safePassword)
        if (!isEmailVerified) {
            AppLogger.info("AuthenticationServiceImpl", "event=signup_pending_email_verification")
        }
    }

    /**
     * Sign-in flow:
     * 1) Derive safe password.
     * 2) Initialize keys for that user.
     * 3) Load and decrypt JSON credentials from local storage.
     * 4) Verify that provided email + password match stored credentials.
     */
    override suspend fun signIn(context: Context, email: String, password: String) = runCatching {
        val canonicalEmail = normalizeEmail(email)
        val safePassword = computeSafePassword(canonicalEmail, password)
        ensureFirebaseVerifiedSignIn(canonicalEmail, password)
        cloud.cloudState.value = cloud.cloudState.value.copy(
            email = canonicalEmail,
            isUserValidated = true
        )
        crypto.initializeKeysWithUser(context, canonicalEmail, safePassword)
        preLoadLocalKeys(canonicalEmail, safePassword)

        loadCredentials(context, email, safePassword)
        check(isAuthenticated(canonicalEmail, safePassword)) { "Invalid credentials" }
        AppLogger.info("AuthenticationServiceImpl", "event=signin_local_auth_completed")
        AppLogger.info("StartupTrace", "event=local_auth_done")
    }

    override suspend fun logoutForRelogin() {
        _state.value = AuthState()
        cache.clearAllCaches()
        cloud.signOutCloud()
        firebaseAuth.signOut()
    }

    /** Clears in-memory state and deletes the encrypted credentials file. */
    override suspend fun signOut(context: Context) {
        logoutForRelogin()
        store.deleteCredentials(context)
        deleteSignatureFile(context)
        deleteCredentialsTimestamp(context)
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
        AppLogger.debug("AuthenticationServiceImpl", "event=persist_credentials_start")

        val json = JSONObject().apply {
            put("user_email", s.email)
            put("user_username", s.username)
            put("version", 3)
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
        AppLogger.info("AuthenticationServiceImpl", "event=credentials_stored size=${bytes.size}")
    }

    /** Loads credentials JSON from encrypted storage and updates AuthState. */
    private fun loadCredentials(context: Context, email: String, safePassword: String) {
        val encrypted = store.readCredentials(context) ?: error("Credentials file not found")
        val candidateEmails = listOf(normalizeEmail(email), email.trim())
            .filter { it.isNotBlank() }
            .distinct()
        var wrapKeyFailureDetected = false

        val data = candidateEmails.firstNotNullOfOrNull { candidate ->
            runCatching {
                val credentialsKey = crypto.deriveLocalKey(
                    candidate,
                    safePassword.toCharArray(),
                    Purpose.Credentials.code
                )
                crypto.decryptCredentials(candidate, credentialsKey, encrypted)
            }.getOrElse { error ->
                if (error is CredentialsWrapKeyException) {
                    wrapKeyFailureDetected = true
                }
                null
            }
        } ?: run {
            if (wrapKeyFailureDetected) {
                purgeCredentialsForRecovery(context)
                throw SecurityException(
                    "Secure storage was reset on this device. Local credentials were cleared. Please sign in again."
                )
            }
            throw SecurityException("Invalid credentials")
        }
        AppLogger.debug("AuthenticationServiceImpl", "event=load_credentials_start")
        val username = data.optString("user_username");
        val storedEmail = normalizeEmail(data.optString("user_email"));
        cacheCredentials(
            username = data.optString("user_username"),
            email    = storedEmail,
            safePassword = safePassword,
            token    = getAuthToken()
        )
        cache.cacheOwnerUsername(username)
        cache.cacheOwnerEmail(storedEmail)
        AppLogger.info("AuthenticationServiceImpl", "event=credentials_loaded size=${encrypted.size}")
    }

    /** Verifies if email + safePassword match in-memory AuthState. */
    private fun isAuthenticated(email: String, safePassword: String): Boolean {
        val s = _state.value
        return email.isNotBlank() && safePassword.isNotBlank() &&
                normalizeEmail(s.email) == normalizeEmail(email) && s.safePassword == safePassword
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

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private suspend fun ensureFirebaseEmailSignupAndVerification(email: String, password: String): Boolean {
        val normalized = normalizeEmail(email)

        runCatching {
            firebaseAuth.createUserWithEmailAndPassword(normalized, password).await()
            AppLogger.info("AuthenticationServiceImpl", "event=firebase_signup_created")
        }.recoverCatching { error ->
            if (error is FirebaseAuthUserCollisionException) {
                AppLogger.warn("AuthenticationServiceImpl", "event=firebase_signup_user_exists")
                firebaseAuth.signInWithEmailAndPassword(normalized, password).await()
            } else {
                throw error
            }
        }.getOrElse { error ->
            throw mapFirebaseAuthException(error)
        }

        val user = firebaseAuth.currentUser ?: throw IllegalStateException("Firebase user unavailable after signup.")
        user.reload().await()

        if (!user.isEmailVerified) {
            user.sendEmailVerification().await()
            AppLogger.info("AuthenticationServiceImpl", "event=firebase_verification_email_sent")
            return false
        }

        AppLogger.info("AuthenticationServiceImpl", "event=firebase_email_already_verified")
        return true
    }

    private suspend fun ensureFirebaseVerifiedSignIn(email: String, password: String) {
        val normalized = normalizeEmail(email)
        runCatching {
            firebaseAuth.signInWithEmailAndPassword(normalized, password).await()
        }.getOrElse { error ->
            throw mapFirebaseAuthException(error)
        }

        val user = firebaseAuth.currentUser ?: throw IllegalStateException("Firebase user unavailable after sign-in.")
        user.reload().await()
        if (!user.isEmailVerified) {
            user.sendEmailVerification().await()
            throw SecurityException("Email not verified. We sent a new verification email.")
        }
    }

    private fun mapFirebaseAuthException(error: Throwable): Throwable {
        return when (error) {
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> SecurityException("Invalid email or password.")
            else -> error
        }
    }

    private fun purgeCredentialsForRecovery(context: Context) {
        store.deleteCredentials(context)
        deleteSignatureFile(context)
        deleteCredentialsTimestamp(context)
        cache.clearAllCaches()
        _state.value = AuthState()
        AppLogger.warn("AuthenticationServiceImpl", "event=credentials_recovery_purge_completed")
    }
}
