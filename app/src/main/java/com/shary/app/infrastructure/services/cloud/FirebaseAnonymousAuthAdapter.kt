package com.shary.app.infrastructure.services.cloud

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

internal class FirebaseAnonymousAuthAdapter(
    private val auth: FirebaseAuth
) {
    /** Idempotente: si hay user, devuelve uid actual. Si no, hace sign-in anónimo. */
    suspend fun ensureSession(): String {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        ?: error("Anonymous sign-in returned null user")
        Log.d("FirebaseAnonymousAuthAdapter", "ensureSession() - user, uid: ${user.uid}")

        // Guardamos token en Session para tus headers (true => forceRefresh)
        val token = user.getIdToken(true).await().token ?: ""
        Log.d("FirebaseAnonymousAuthAdapter", "ensureSession() - token $token")
        return user.uid
    }

    /** Refresca ID token y lo guarda en Session. */
    suspend fun refreshToken(): String {
        val user = auth.currentUser ?: error("No anonymous session")
        val token = user.getIdToken(true).await().token ?: ""
        return token
    }

    fun signOut() {
        auth.signOut()
    }
}
