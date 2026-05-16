package com.shary.app.core.domain.interfaces.services

import com.shary.app.core.constants.CloudInboxPolicy
import com.shary.app.core.domain.interfaces.states.CloudState
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.CloudPayloadDecision
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.core.domain.types.valueobjects.DataPayload
import com.shary.app.core.domain.types.valueobjects.RotateIdentityV2Payload
import kotlinx.coroutines.flow.MutableStateFlow

interface CloudService {
    var cloudState: MutableStateFlow<CloudState>

    suspend fun sendPing(): Boolean
    suspend fun isUserRegisteredInCloud(email: String): Boolean
    suspend fun uploadData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int = CloudInboxPolicy.DEFAULT_EXPIRY_DAYS
    ): Map<String, StatusDataSentDb>
    suspend fun uploadRequest(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int = CloudInboxPolicy.DEFAULT_EXPIRY_DAYS
    ): Map<String, StatusDataSentDb>
    
    suspend fun uploadUser(email: String): String
    suspend fun rotateUserIdentity(payload: RotateIdentityV2Payload): Boolean
    suspend fun deleteUser(email: String): Boolean
    suspend fun getPubKey(emailHash: String): String

    // >>> NUEVO: autenticación anónima Firebase <<<
    /** Inicia sesión anónima si no existe y devuelve uid. Idempotente. */
    suspend fun ensureAnonymousSession(): Result<String>

    /** Fuerza refresco del ID token y lo guarda en Session; devuelve token. */
    suspend fun refreshIdToken(): Result<String>

    /** Cierra sesión Firebase (limpia Session.authToken). */
    suspend fun signOutCloud(): Result<Unit>

    /** Fetch encrypted data from Payload for the current user */
    suspend fun fetchPayloadDataFromEmail(email: String): Result<List<String>>

    /** Fetch encrypted data from Request for the current user */
    suspend fun fetchRequestDataFromEmail(email: String): Result<List<String>>

    /** Fetch pending encrypted payload metadata/content without importing into local storage. */
    suspend fun fetchPayloadInboxFromEmail(email: String): Result<List<DataPayload>>

    /** Fetch pending encrypted request metadata/content without importing into local storage. */
    suspend fun fetchRequestInboxFromEmail(email: String): Result<List<DataPayload>>

    /** Decrypt a specific payload document for the current user. */
    suspend fun decryptPayloadFromInbox(email: String, payload: DataPayload): Result<String>

    /** Decrypt a specific request document for the current user. */
    suspend fun decryptRequestFromInbox(email: String, payload: DataPayload): Result<String>

    /**
     * Sends recipient decision (accept/reject) for one payload.
     * Backend may remove pending copy for this recipient when acknowledged.
     */
    suspend fun sendPayloadDecision(
        email: String,
        payload: DataPayload,
        decision: CloudPayloadDecision,
        notifySender: Boolean = false
    ): Result<Boolean>

    /**
     * Sends recipient decision (accept/reject) for one request payload.
     * Backend may remove pending copy for this recipient when acknowledged.
     */
    suspend fun sendRequestDecision(
        email: String,
        payload: DataPayload,
        decision: CloudPayloadDecision,
        notifySender: Boolean = false
    ): Result<Boolean>
}
