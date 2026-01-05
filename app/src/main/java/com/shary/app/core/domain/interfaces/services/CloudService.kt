package com.shary.app.core.domain.interfaces.services

import com.shary.app.core.domain.interfaces.states.CloudState
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import kotlinx.coroutines.flow.MutableStateFlow

interface CloudService {
    var cloudState: MutableStateFlow<CloudState>

    suspend fun sendPing(): Boolean
    suspend fun isUserRegisteredInCloud(username: String): Boolean
    suspend fun uploadData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
    ): Map<String, StatusDataSentDb>
    suspend fun uploadRequest(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
    ): Map<String, StatusDataSentDb>
    
    suspend fun uploadUser(username: String): String
    suspend fun deleteUser(username: String): Boolean
    suspend fun getPubKey(usernameHash: String): String

    // >>> NUEVO: autenticación anónima Firebase <<<
    /** Inicia sesión anónima si no existe y devuelve uid. Idempotente. */
    suspend fun ensureAnonymousSession(): Result<String>

    /** Fuerza refresco del ID token y lo guarda en Session; devuelve token. */
    suspend fun refreshIdToken(): Result<String>

    /** Cierra sesión Firebase (limpia Session.authToken). */
    suspend fun signOutCloud(): Result<Unit>

    /** Fetch encrypted data from Payload for the current user */
    suspend fun fetchPayloadData(username: String): Result<List<String>>

    /** Fetch encrypted data from Request for the current user */
    suspend fun fetchRequestData(username: String): Result<List<String>>
}
