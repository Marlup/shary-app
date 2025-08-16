package com.shary.app.core.domain.interfaces.security

import android.content.Context
import com.shary.app.core.domain.interfaces.states.AuthState
import kotlinx.coroutines.flow.StateFlow


interface AuthService {
    val state: StateFlow<AuthState>

    fun getAuthToken(): String
    fun setAuthToken(newAuthToken: String)

    fun getIsOnline(): Boolean
    fun setIsOnline(value: Boolean)

    fun getSafePassword(): String
    fun setSafePassword(value: String)

    fun isSignatureActive(context: Context): Boolean
    fun isCredentialsActive(context: Context): Boolean

    suspend fun requestChallenge(username: String): ByteArray
    suspend fun registerIdentity(
        username: String,
        email: String,
        signPublic: ByteArray,
        kexPublic: ByteArray
    ): Boolean

    suspend fun verifyLogin(
        username: String,
        challenge: ByteArray,
        signature: ByteArray
    ): Boolean
    suspend fun signUp(
        context: Context,
        username: String,
        email: String,
        password: String
    ): Result<Unit>
    suspend fun signIn(context: Context, username: String, password: String): Result<Unit>
    fun signOut(context: Context)
}
