package com.shary.app.core.domain.interfaces.services

import com.shary.app.core.domain.interfaces.states.CloudState
import com.shary.app.core.domain.models.UserDomain
import kotlinx.coroutines.flow.MutableStateFlow

interface CloudService {
    var cloudState: MutableStateFlow<CloudState>

    suspend fun sendPing(): Boolean
    suspend fun isUserRegisteredInCloud(username: String): Boolean
    suspend fun uploadUser(username: String): String
    suspend fun deleteUser(username: String): Boolean
    suspend fun uploadData(
        fields: List<com.shary.app.core.domain.models.FieldDomain>,
        owner: UserDomain,
        consumers: List<UserDomain>,
        isRequest: Boolean
    ): Map<String, com.shary.app.core.domain.types.enums.StatusDataSentDb>
    suspend fun getPubKey(usernameHash: String): String

    // >>> NUEVO: autenticación anónima Firebase <<<
    /** Inicia sesión anónima si no existe y devuelve uid. Idempotente. */
    suspend fun ensureAnonymousSession(): Result<String>

    /** Fuerza refresco del ID token y lo guarda en Session; devuelve token. */
    suspend fun refreshIdToken(): Result<String>

    /** Cierra sesión Firebase (limpia Session.authToken). */
    suspend fun signOutCloud(): Result<Unit>

    /** Fetch encrypted data from Firebase for the current user */
    suspend fun fetchData(username: String): Result<String>
}
