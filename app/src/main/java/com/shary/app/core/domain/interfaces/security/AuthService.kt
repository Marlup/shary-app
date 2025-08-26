package com.shary.app.core.domain.interfaces.security

import android.content.Context
import com.shary.app.core.domain.interfaces.states.AuthState
import com.shary.app.core.domain.types.valueobjects.Purpose
import kotlinx.coroutines.flow.StateFlow


interface AuthService {
    val state: StateFlow<AuthState>

    fun getAuthToken(): String
    fun setAuthToken(newAuthToken: String)

    fun getIsOnline(): Boolean
    fun setIsOnline(value: Boolean)

    fun getSafePassword(): String
    fun setSafePassword(value: String)

    fun getLocalKeyByPurpose(purpose: Purpose): ByteArray?
    fun addLocalKeyByPurpose(purpose: Purpose, value: ByteArray)

    fun isSignatureActive(context: Context): Boolean
    fun isCredentialsActive(context: Context): Boolean

    suspend fun signUp(
        context: Context,
        username: String,
        email: String,
        password: String
    ): Result<Unit>
    suspend fun signIn(context: Context, username: String, password: String): Result<Unit>
    fun signOut(context: Context)
}
