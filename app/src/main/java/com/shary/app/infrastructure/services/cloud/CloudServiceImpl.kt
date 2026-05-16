package com.shary.app.infrastructure.services.cloud

import com.shary.app.core.constants.CloudInboxPolicy
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.CloudPayloadDecision
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Decode
import com.shary.app.infrastructure.security.helper.SecurityUtils.base64Encode
import com.shary.app.infrastructure.security.helper.SecurityUtils.getCurrentUtcTimestamp
import com.shary.app.infrastructure.security.helper.SecurityUtils.getTimestampAfterExpiry
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessage
import com.shary.app.infrastructure.security.helper.SecurityUtils.hashMessageB64
import com.shary.app.infrastructure.services.cloud.Utils.authBearerHeader
import com.shary.app.infrastructure.services.cloud.Utils.buildPostRequest
import com.shary.app.infrastructure.services.cloud.Utils.evaluateStatusCode
import com.google.firebase.auth.FirebaseAuth
import com.shary.app.core.domain.interfaces.states.CloudState
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.valueobjects.DataPayload
import com.shary.app.core.domain.types.valueobjects.DeleteIdentityV2Payload
import com.shary.app.core.domain.types.valueobjects.IdentityProofPayload
import com.shary.app.core.domain.types.valueobjects.RegisterIdentityV2Payload
import com.shary.app.core.domain.types.valueobjects.RotateIdentityV2Payload
import com.shary.app.infrastructure.services.cloud.Constants.CLOUD_SCHEMA_VERSION
import com.shary.app.infrastructure.services.cloud.Constants.ENFORCE_VERIFIED_EMAIL_FOR_IDENTITY
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_FETCH_PAYLOAD
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_FETCH_REQUEST
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_PAYLOAD_DECISION
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_PING
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_REQUEST_DECISION
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_V2_DELETE_IDENTITY
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_V2_GET_PUB_KEY
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_V2_REGISTER_IDENTITY
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_V2_ROTATE_IDENTITY
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_UPLOAD_PAYLOAD
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_ENDPOINT_UPLOAD_REQUEST
import com.shary.app.infrastructure.services.cloud.Constants.FIREBASE_MAIN_ENTRYPOINT
import com.shary.app.infrastructure.services.cloud.Constants.HEADER_X_REQUEST_ID
import com.shary.app.utils.Functions.buildJsonStringFromFields
import com.shary.app.utils.Functions.makeJsonStringFromRequestKeys
import com.shary.app.utils.log.AppLogger
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID


/**
 * CloudServiceImpl
 *
 * Concrete implementation of CloudService.
 * - Handles communication with Firebase-backed endpoints (ping, upload, delete, etc.).
 * - Injects FirebaseAuth to establish and maintain anonymous sessions.
 * - Always attaches a valid ID token (Authorization: Bearer <token>) when required.
 * - Encrypts payloads end-to-end (before leaving the device) using CryptographyManager.
 */
class CloudServiceImpl @Inject constructor(
    private val cryptographyManager: CryptographyManager,
    private val firebaseAuth: FirebaseAuth
) : CloudService {

    private val authAdapter = FirebaseAnonymousAuthAdapter(firebaseAuth)
    override var cloudState = MutableStateFlow(CloudState())
    private val json = Json
    private val jsonWithDefaults = Json { encodeDefaults = true }

    private data class DecisionAckEnvelope(
        val status: String?,
        val reasonCode: String?,
        val decisionId: String?,
        val requestId: String?,
        val schemaVersion: Int?
    )

    private data class CloudErrorEnvelope(
        val errorCode: String?,
        val message: String?,
        val requestId: String?,
        val retryAfterSeconds: Long?
    )

    private data class DecisionHttpResult(
        val statusCode: Int,
        val ack: DecisionAckEnvelope?,
        val error: CloudErrorEnvelope?,
        val requestId: String?
    ) {
        val isSuccessful: Boolean
            get() = statusCode in 200..299
    }

    private fun newRequestId(): String = UUID.randomUUID().toString()

    private fun authHeaders(requestId: String): Map<String, String> =
        authBearerHeader(cloudState.value.token, requestId)

    // -------------------- AUTH --------------------

    /**
     * Ensures that the client has a valid ID token before making requests.
     * If the Session has no token (or expired), refreshes it via FirebaseAuth.
     */
    private suspend fun ensureAuth(): Unit = withContext(Dispatchers.IO) {
        if (cloudState.value.token.isNullOrEmpty()) {
            val session = authAdapter.ensureSession()
            cloudState.value = cloudState.value.copy(
                //username = session.uid,
                token = session.idToken
            )
            setIsOnlineInCLoud(true)
        } else setIsOnlineInCLoud(true)
    }

    override suspend fun ensureAnonymousSession(): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val session = authAdapter.ensureSession()
            cloudState.value = cloudState.value.copy(
                username = session.uid,
                token = session.idToken
            )
            session.uid
        }
    }

    override suspend fun refreshIdToken(): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val token = authAdapter.refreshToken()
            cloudState.value = cloudState.value.copy(token = token)
            token
        }
    }

    override suspend fun signOutCloud(): Result<Unit> = runCatching {
        authAdapter.signOut()
        cloudState.value = cloudState.value.copy(
            username = "",
            token = "",
            isOnline = false,
            isUserValidated = false
        )
    }

    // -------------------- EXISTING FUNCTIONALITY --------------------

    /**
     * Sends a "ping" to the cloud service to check reachability.
     * Updates Session.isOnline based on response.
     */
    override suspend fun sendPing(): Boolean = withContext(Dispatchers.IO) {
        val url: String = FIREBASE_MAIN_ENTRYPOINT + FIREBASE_ENDPOINT_PING

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            OkHttpClient().newCall(request).execute().use { response ->
                cloudState.value.isOnline = response.isSuccessful
                response.isSuccessful
            }
        } catch (e: IOException) {
            cloudState.value.isOnline = false
            false
        }
    }

    /**
     * Checks if a user is registered by verifying if their public key exists.
     */
    override suspend fun isUserRegisteredInCloud(email: String): Boolean = withContext(Dispatchers.IO) {
        ensureAuth()
        val safeUser = safeGetUser(email)
        return@withContext emailHashCandidates(safeUser).any { hash -> getPubKey(hash).isNotEmpty() }
    }

    /**
     * Uploads a new user identity (with keys) to the backend.
     */
    override suspend fun uploadUser(email: String): String = withContext(Dispatchers.IO) {
        ensureAuth()
        val safeUser = safeGetUser(email)
        val canonicalEmail = normalizeUserEmail(safeUser)
        ensureVerifiedIdentityForEmail(canonicalEmail)
        doRegisterIdentityV2(canonicalEmail)
    }

    /**
     * Deletes a user by sending signature + hash.
     */
    override suspend fun deleteUser(email: String): Boolean = withContext(Dispatchers.IO) {
        ensureAuth()
        val safeEmail = safeGetUser(email)
        val canonicalEmail = normalizeUserEmail(safeEmail)
        ensureVerifiedIdentityForEmail(canonicalEmail)
        doDeleteIdentityV2(canonicalEmail)
    }

    override suspend fun rotateUserIdentity(payload: RotateIdentityV2Payload): Boolean = withContext(Dispatchers.IO) {
        ensureAuth()
        val canonicalEmail = normalizeUserEmail(payload.email)
        ensureVerifiedIdentityForEmail(canonicalEmail)
        if (!isCloudReachable()) throw IOException("Cloud service is not reachable")

        val adjustedPayload = payload.copy(email = canonicalEmail)
        val url = FIREBASE_MAIN_ENTRYPOINT + FIREBASE_ENDPOINT_V2_ROTATE_IDENTITY
        val requestId = newRequestId()
        val request = buildPostRequest(
            url,
            Json.encodeToString(RotateIdentityV2Payload.serializer(), adjustedPayload),
            authHeaders(requestId)
        )

        return@withContext try {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IdentityBackendRejectedException("Identity rotation rejected (HTTP ${response.code}).")
                }
                true
            }
        } catch (e: IOException) {
            AppLogger.error("CloudServiceImpl", "event=rotate_identity_v2_exception", e)
            throw e
        }
    }

    /**
     * Uploads encrypted data fields to one or more recipients.
     */
    override suspend fun uploadData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int,
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) return@withContext emptyMap()
        if (isEmptyArguments(fields, recipients)) return@withContext emptyMap()
        ensureVerifiedIdentityForEmail(normalizeUserEmail(owner.email))

        doUploadData(fields, owner, recipients, expiryDays)
    }

    private suspend fun doUploadData(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int
    ) =
        withContext(Dispatchers.IO) {
            val dataJson = buildJsonStringFromFields(fields)
            val safeOwnerEmail = safeGetUser(owner.email)
            val recipientsEmail = recipients.map { it.email }

            buildLoad(dataJson, safeOwnerEmail, recipientsEmail, isRequest=false, expiryDays = expiryDays)
        }

    /**
     * Uploads encrypted request to one or more recipients.
     */
    override suspend fun uploadRequest(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int,
    ): Map<String, StatusDataSentDb> = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) return@withContext emptyMap()
        if (isEmptyArguments(fields, recipients)) return@withContext emptyMap()
        ensureVerifiedIdentityForEmail(normalizeUserEmail(owner.email))

        doUploadRequest(fields, owner, recipients, expiryDays)
    }

    private suspend fun doUploadRequest(
        fields: List<FieldDomain>,
        owner: UserDomain,
        recipients: List<UserDomain>,
        expiryDays: Int
    ) =
        withContext(Dispatchers.IO) {
            val safeOwnerEmail = safeGetUser(owner.email)
            val dataRequestJson = makeJsonStringFromRequestKeys(fields, safeOwnerEmail)
            val recipientsEmail = recipients.map { it.email }

            buildLoad(dataRequestJson, safeOwnerEmail, recipientsEmail, isRequest=true, expiryDays = expiryDays)
        }

    /**
     * Core logic for encrypting data or request-data for each recipientEmail and sending via backend.
     */
    private suspend fun buildLoad(
        data: String,
        ownerEmail: String,
        recipientsEmail: List<String>,
        isRequest: Boolean,
        expiryDays: Int
    ) =
        withContext(Dispatchers.IO) {
            ensureAuth()
            val safeOwnerEmail = safeGetUser(ownerEmail)
            val safeOwnerHash = resolveKnownUserHashOrFallback(safeOwnerEmail)
            val results = mutableMapOf<String, StatusDataSentDb>()
            val safeExpiryDays = expiryDays.coerceIn(
                CloudInboxPolicy.DEFAULT_EXPIRY_DAYS,
                CloudInboxPolicy.MAX_EXPIRY_DAYS
            )
            val expirySeconds = safeExpiryDays * 24L * 60L * 60L

            val client = OkHttpClient()
            recipientsEmail.forEach { recipientEmail ->
                val recipientResolved = resolveRecipientHashAndPubKey(recipientEmail)
                if (recipientResolved == null) {
                    results[recipientEmail] = StatusDataSentDb.ERROR
                    return@forEach
                }
                val (recipientEmailHash, recipientPubKeyB64) = recipientResolved
                val recipientPubKey = base64Decode(recipientPubKeyB64)
                val aad = "shary:$safeOwnerHash:$recipientEmailHash".toByteArray(Charsets.UTF_8)

                val encryptedData = cryptographyManager.encryptWithPeerPublic(
                    data.toByteArray(),
                    recipientPubKey,
                    aad
                )
                val payloadData = base64Encode(encryptedData)
                val (signature, verification) =
                    makeCredentials(listOf(safeOwnerHash, recipientEmailHash, hashMessageB64(data)))

                val payload = DataPayload(
                    user = safeOwnerHash,
                    recipient = recipientEmailHash,
                    creationAt = getCurrentUtcTimestamp(),
                    expiresAt = getTimestampAfterExpiry(extraTime = expirySeconds),
                    data = payloadData,
                    verification = verification,
                    signature = signature
                )

                val url = FIREBASE_MAIN_ENTRYPOINT + when {
                    isRequest -> FIREBASE_ENDPOINT_UPLOAD_REQUEST
                    else -> FIREBASE_ENDPOINT_UPLOAD_PAYLOAD
                }

                val requestId = newRequestId()
                val request = buildPostRequest(
                    url,
                    jsonWithDefaults.encodeToString(DataPayload.serializer(), payload),
                    authHeaders(requestId)
                )
                try {
                    client.newCall(request).execute().use { response ->
                        results[recipientEmail] = evaluateStatusCode(response.code)
                    }
                } catch (e: IOException) {
                    AppLogger.error("CloudServiceImpl", "event=upload_data_exception", e)
                    results[recipientEmail] = StatusDataSentDb.ERROR
                }
            }
            results
        }

    /**
     * Retrieves a public key for a given user hash.
     */
    override suspend fun getPubKey(emailHash: String): String = withContext(Dispatchers.IO) {
        AppLogger.debug("CloudServiceImpl", "event=get_pubkey_start userHash=${AppLogger.redacted(emailHash)}")
        ensureAuth()

        val encodedUser = URLEncoder.encode(emailHash, Charsets.UTF_8.name())
        val urlV2 = "$FIREBASE_MAIN_ENTRYPOINT$FIREBASE_ENDPOINT_V2_GET_PUB_KEY?user_hash=$encodedUser"
        val requestId = newRequestId()

        val requestV2 = Request.Builder()
            .url(urlV2)
            .headers(authHeaders(requestId).toHeaders())
            .build()

        AppLogger.debug("CloudServiceImpl", "event=get_pubkey_request token=${AppLogger.redacted(cloudState.value.token)}")
        return@withContext try {
            val client = OkHttpClient()
            client.newCall(requestV2).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string()
                    val json = Json.parseToJsonElement(body).jsonObject
                    val kex = json["pub_kex_b64"]?.jsonPrimitive?.content.orEmpty()
                    if (kex.isNotBlank()) return@use kex
                }
                ""
            }
        } catch (e: IOException) {
            AppLogger.error("CloudServiceImpl", "event=get_pubkey_exception", e)
            ""
        }
    }

    /**
     * Creates signature + verification pair for a canonical list of fields.
     */
    private fun makeCredentials(elements: List<String>): Pair<String, String> {
        val canonical = elements.joinToString(".")
        val rawHash = hashMessage(canonical)
        val verificationB64 = base64Encode(rawHash)
        val signatureB64 = base64Encode(cryptographyManager.signDetached(rawHash))
        return signatureB64 to verificationB64
    }

    /** Check if backend is reachable (ping + cached state). */
    private suspend fun isCloudReachable(): Boolean =
        cloudState.value.isOnline || sendPing()

    private fun isEmptyArguments(fields: List<FieldDomain>, recipients: List<UserDomain>) =
        fields.isEmpty() || recipients.isEmpty()

    /** Returns provided email or fallback to Session.ownerEmail. */
    private fun safeGetUser(email: String?): String =
        if (email.isNullOrEmpty()) cloudState.value.email.trim() else email.trim()

    suspend fun getAuthToken(): String? {
        val username = FirebaseAuth.getInstance().currentUser ?: return null
        return username.getIdToken(true).await().token
    }

    /**
     * Fetches encrypted data from Firebase for the current user.
     * Decrypts the data and returns it as a JSON string.
     * Note: Firebase returns the MOST RECENT payload if multiple exist.
     */
    override suspend fun fetchPayloadDataFromEmail(email: String): Result<List<String>> {
        return fetchDecryptedData(email, isRequest = false)
    }
    override suspend fun fetchRequestDataFromEmail(email: String): Result<List<String>> {
        return fetchDecryptedData(email, isRequest = true)
    }

    override suspend fun fetchPayloadInboxFromEmail(email: String): Result<List<DataPayload>> = runCatching {
        fetchEncryptedPayloads(email, isRequest = false).map { it.payload }
    }

    override suspend fun fetchRequestInboxFromEmail(email: String): Result<List<DataPayload>> = runCatching {
        fetchEncryptedPayloads(email, isRequest = true).map { it.payload }
    }

    override suspend fun decryptPayloadFromInbox(email: String, payload: DataPayload): Result<String> = runCatching {
        val recipientHash = resolveRecipientHashForPayload(email, payload)
        decryptPayloadWithRecipientHash(payload, recipientHash)
    }

    override suspend fun decryptRequestFromInbox(email: String, payload: DataPayload): Result<String> = runCatching {
        val recipientHash = resolveRecipientHashForPayload(email, payload)
        decryptPayloadWithRecipientHash(payload, recipientHash)
    }

    override suspend fun sendPayloadDecision(
        email: String,
        payload: DataPayload,
        decision: CloudPayloadDecision,
        notifySender: Boolean
    ): Result<Boolean> = runCatching {
        val requestId = newRequestId()
        val result = sendDecision(
            email = email,
            payload = payload,
            decision = decision,
            notifySender = notifySender,
            endpoint = FIREBASE_ENDPOINT_PAYLOAD_DECISION,
            requestId = requestId
        )
        resolveDecisionOutcome(result, FIREBASE_ENDPOINT_PAYLOAD_DECISION)
    }

    override suspend fun sendRequestDecision(
        email: String,
        payload: DataPayload,
        decision: CloudPayloadDecision,
        notifySender: Boolean
    ): Result<Boolean> = runCatching {
        val requestId = newRequestId()
        val requestDecisionResult = sendDecision(
            email = email,
            payload = payload,
            decision = decision,
            notifySender = notifySender,
            endpoint = FIREBASE_ENDPOINT_REQUEST_DECISION,
            requestId = requestId
        )
        when {
            requestDecisionResult.statusCode == 404 || requestDecisionResult.statusCode == 405 -> {
                AppLogger.warn(
                    "CloudServiceImpl",
                    "event=request_decision_fallback legacy_endpoint=${FIREBASE_ENDPOINT_PAYLOAD_DECISION} requestId=${AppLogger.redacted(requestId)}"
                )
                val fallbackResult = sendDecision(
                    email = email,
                    payload = payload,
                    decision = decision,
                    notifySender = notifySender,
                    endpoint = FIREBASE_ENDPOINT_PAYLOAD_DECISION,
                    requestId = requestId
                )
                resolveDecisionOutcome(fallbackResult, FIREBASE_ENDPOINT_PAYLOAD_DECISION)
            }
            else -> resolveDecisionOutcome(requestDecisionResult, FIREBASE_ENDPOINT_REQUEST_DECISION)
        }
    }

    private data class RecipientPayloadEnvelope(
        val recipientHash: String,
        val payload: DataPayload
    )

    private suspend fun fetchDecryptedData(email: String, isRequest: Boolean): Result<List<String>> = runCatching {
        fetchEncryptedPayloads(email, isRequest).map { envelope ->
            decryptPayloadWithRecipientHash(envelope.payload, envelope.recipientHash)
        }
    }

    private suspend fun fetchEncryptedPayloads(email: String, isRequest: Boolean): List<RecipientPayloadEnvelope> =
        withContext(Dispatchers.IO) {
            // Ensure we have a fresh auth token
            ensureAuth()

            // Try to refresh the token to ensure it's valid
            try {
                refreshIdToken()
            } catch (e: Exception) {
                AppLogger.warn("CloudServiceImpl", "event=refresh_token_failed", e)
            }

            if (!isCloudReachable()) {
                throw IOException("Cloud service is not reachable")
            }

            val safeEmail = safeGetUser(email)
            val recipientHashes = emailHashCandidates(safeEmail)
            val endpoint = if (isRequest) FIREBASE_ENDPOINT_FETCH_REQUEST else FIREBASE_ENDPOINT_FETCH_PAYLOAD
            val client = OkHttpClient()

            var lastFetchError: Throwable? = null
            for (recipientHash in recipientHashes) {
                val payloadArray = runCatching {
                    fetchPayloadArrayForRecipientHash(client, endpoint, recipientHash)
                }.getOrElse { error ->
                    lastFetchError = error
                    AppLogger.warn("CloudServiceImpl", "event=fetch_retry_alternative_hash", error)
                    null
                } ?: continue

                if (payloadArray.isEmpty()) continue

                return@withContext payloadArray.mapNotNull { payloadElement ->
                    runCatching {
                        val payload = Json.decodeFromJsonElement(DataPayload.serializer(), payloadElement)
                        RecipientPayloadEnvelope(
                            recipientHash = recipientHash,
                            payload = payload
                        )
                    }.getOrElse { parseError ->
                        AppLogger.warn("CloudServiceImpl", "event=payload_parse_failed", parseError)
                        null
                    }
                }
            }

            lastFetchError?.let { throw it }
            emptyList()
        }

    private suspend fun resolveRecipientHashForPayload(email: String, payload: DataPayload): String {
        val payloadRecipient = payload.recipient.trim()
        if (payloadRecipient.isNotEmpty()) return payloadRecipient
        return emailHashCandidates(safeGetUser(email)).firstOrNull().orEmpty()
    }

    private suspend fun sendDecision(
        email: String,
        payload: DataPayload,
        decision: CloudPayloadDecision,
        notifySender: Boolean,
        endpoint: String,
        requestId: String
    ): DecisionHttpResult = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) {
            throw IOException("Cloud service is not reachable")
        }

        val recipientHash = resolveRecipientHashForPayload(email, payload)
        val decisionBody = buildJsonObject {
            put("recipient", JsonPrimitive(recipientHash))
            put("verification", JsonPrimitive(payload.verification))
            put("decision", JsonPrimitive(decision.name.lowercase()))
            put("notify_sender", JsonPrimitive(notifySender))
            put("schema_version", JsonPrimitive(CLOUD_SCHEMA_VERSION))
        }.toString()
        val url = FIREBASE_MAIN_ENTRYPOINT + endpoint
        val request = buildPostRequest(
            url,
            decisionBody,
            authHeaders(requestId)
        )
        return@withContext try {
            OkHttpClient().newCall(request).execute().use { response ->
                parseDecisionHttpResponse(response)
            }
        } catch (e: IOException) {
            AppLogger.warn("CloudServiceImpl", "event=payload_decision_failed", e)
            throw e
        }
    }

    private fun parseDecisionHttpResponse(response: Response): DecisionHttpResult {
        val bodyString = response.body?.string().orEmpty()
        val (ack, parsedError) = parseDecisionResponseBody(bodyString)
        val retryAfterHeader = response.header("Retry-After")?.trim()?.toLongOrNull()
        val mergedError = when {
            parsedError != null && parsedError.retryAfterSeconds == null && retryAfterHeader != null ->
                parsedError.copy(retryAfterSeconds = retryAfterHeader)
            parsedError != null -> parsedError
            !response.isSuccessful && retryAfterHeader != null ->
                CloudErrorEnvelope(
                    errorCode = null,
                    message = null,
                    requestId = null,
                    retryAfterSeconds = retryAfterHeader
                )
            else -> null
        }

        val resolvedRequestId = extractRequestId(response, ack?.requestId ?: mergedError?.requestId)
        val errorWithRequestId = if (mergedError != null && mergedError.requestId.isNullOrBlank()) {
            mergedError.copy(requestId = resolvedRequestId)
        } else {
            mergedError
        }

        return DecisionHttpResult(
            statusCode = response.code,
            ack = ack,
            error = errorWithRequestId,
            requestId = resolvedRequestId
        )
    }

    private fun parseDecisionResponseBody(bodyString: String): Pair<DecisionAckEnvelope?, CloudErrorEnvelope?> {
        if (bodyString.isBlank()) return null to null
        val root = runCatching { json.parseToJsonElement(bodyString).jsonObject }.getOrNull()
            ?: return null to null

        val ack = DecisionAckEnvelope(
            status = root.stringField("status"),
            reasonCode = root.stringField("reason_code"),
            decisionId = root.stringField("decision_id"),
            requestId = root.stringField("request_id"),
            schemaVersion = root.intField("schema_version")
        ).takeIf { envelope ->
            envelope.status != null ||
                envelope.reasonCode != null ||
                envelope.decisionId != null ||
                envelope.requestId != null ||
                envelope.schemaVersion != null
        }

        val nestedError = root["error"]?.let { runCatching { it.jsonObject }.getOrNull() }
        val errorSource = nestedError ?: root
        val error = CloudErrorEnvelope(
            errorCode = errorSource.stringField("error_code"),
            message = errorSource.stringField("message"),
            requestId = errorSource.stringField("request_id") ?: root.stringField("request_id"),
            retryAfterSeconds = errorSource.longField("retry_after_seconds")
        ).takeIf { envelope ->
            envelope.errorCode != null ||
                envelope.message != null ||
                envelope.requestId != null ||
                envelope.retryAfterSeconds != null
        }

        return ack to error
    }

    private fun resolveDecisionOutcome(result: DecisionHttpResult, endpoint: String): Boolean {
        if (result.isSuccessful) {
            validateAckEnvelope(result.ack, result.requestId)
            return true
        }

        val errorCode = result.error?.errorCode?.trim().orEmpty()
        if (result.statusCode == 409 && errorCode.equals("decision_conflict", ignoreCase = true)) {
            AppLogger.info(
                "CloudServiceImpl",
                "event=decision_conflict_final endpoint=$endpoint requestId=${AppLogger.redacted(result.requestId)}"
            )
            return true
        }

        throw mapDecisionFailureToException(result, endpoint)
    }

    private fun validateAckEnvelope(ack: DecisionAckEnvelope?, fallbackRequestId: String?) {
        if (ack == null) {
            AppLogger.warn(
                "CloudServiceImpl",
                "event=decision_ack_missing requestId=${AppLogger.redacted(fallbackRequestId)}"
            )
            return
        }
        val ackSchemaVersion = ack.schemaVersion
        if (ackSchemaVersion != null && ackSchemaVersion != CLOUD_SCHEMA_VERSION) {
            throw CloudUnsupportedSchemaVersionException(
                message = "Unsupported decision ACK schema_version=$ackSchemaVersion.",
                requestId = ack.requestId ?: fallbackRequestId
            )
        }
    }

    private fun mapDecisionFailureToException(
        result: DecisionHttpResult,
        endpoint: String
    ): Throwable {
        val errorCode = result.error?.errorCode?.trim().orEmpty()
        val requestId = result.error?.requestId ?: result.requestId
        val retryAfter = result.error?.retryAfterSeconds
        if (result.statusCode == 429 || errorCode.equals("rate_limited", ignoreCase = true)) {
            return CloudRateLimitedException(
                retryAfterSeconds = retryAfter,
                requestId = requestId,
                message = "Cloud endpoint $endpoint is rate-limited."
            )
        }
        if (errorCode.equals("unsupported_schema_version", ignoreCase = true)) {
            return CloudUnsupportedSchemaVersionException(
                message = "Backend rejected schema version for endpoint $endpoint.",
                requestId = requestId
            )
        }
        val baseMessage = result.error?.message?.takeIf { it.isNotBlank() }
            ?: "Cloud endpoint $endpoint failed with HTTP ${result.statusCode}."
        return CloudBackendApiException(
            statusCode = result.statusCode,
            errorCode = errorCode.ifBlank { null },
            requestId = requestId,
            message = baseMessage
        )
    }

    private fun extractRequestId(response: Response, bodyRequestId: String?): String? {
        return bodyRequestId
            ?: response.header(HEADER_X_REQUEST_ID)
            ?: response.header("x-request-id")
            ?: response.header("x-correlation-id")
            ?: response.header("traceparent")
    }

    private fun JsonObject.stringField(name: String): String? {
        return this[name]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.intField(name: String): Int? {
        return this[name]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
    }

    private fun JsonObject.longField(name: String): Long? {
        return this[name]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
    }

    private suspend fun decryptPayloadWithRecipientHash(
        payload: DataPayload,
        recipientHash: String
    ): String = withContext(Dispatchers.IO) {
        val encryptedDataB64 = payload.data
        val senderEmailHash = payload.user
        if (encryptedDataB64.isBlank()) throw IllegalStateException("No data field in payload")
        if (senderEmailHash.isBlank()) throw IllegalStateException("No user field in payload")
        if (recipientHash.isBlank()) throw IllegalStateException("No recipient hash available")

        AppLogger.debug("CloudServiceImpl", "event=decrypt_payload senderHash=${AppLogger.redacted(senderEmailHash)}")

        val senderPubKeyB64 = getPubKey(senderEmailHash)
        if (senderPubKeyB64.isEmpty()) {
            throw IllegalStateException("Cannot retrieve sender's public key")
        }

        val senderPubKey = base64Decode(senderPubKeyB64)
        val encryptedData = base64Decode(encryptedDataB64)
        val aad = "shary:$senderEmailHash:$recipientHash".toByteArray(Charsets.UTF_8)
        val decryptedData = cryptographyManager.decryptFromPeerPublic(
            encryptedData,
            senderPubKey,
            aad
        )

        val decryptedDataString = decryptedData.toString(Charsets.UTF_8)
        AppLogger.info("CloudServiceImpl", "event=decrypt_payload_success")
        decryptedDataString
    }

    private suspend fun doDeleteIdentityV2(email: String): Boolean = withContext(Dispatchers.IO) {
        ensureAuth()
        if (!isCloudReachable()) throw IOException("Cloud service is not reachable")

        val canonicalEmail = normalizeUserEmail(email)
        val userHash = hashMessageB64(canonicalEmail)
        val clientTs = getCurrentUtcTimestamp()
        val verification = hashMessage("delete.$userHash.$clientTs")
        val signatureB64 = base64Encode(cryptographyManager.signDetached(verification))

        val payload = DeleteIdentityV2Payload(
            email = canonicalEmail,
            client_ts = clientTs,
            signature_b64 = signatureB64
        )

        val url = Constants.FIREBASE_MAIN_ENTRYPOINT + FIREBASE_ENDPOINT_V2_DELETE_IDENTITY
        val requestId = newRequestId()
        val request = buildPostRequest(
            url,
            Json.encodeToString(DeleteIdentityV2Payload.serializer(), payload),
            authHeaders(requestId)
        )
        return@withContext try {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IdentityBackendRejectedException("Identity delete rejected (HTTP ${response.code}).")
                }
                true
            }
        } catch (e: IOException) {
            AppLogger.error("CloudServiceImpl", "event=delete_identity_v2_exception", e)
            throw e
        }
    }

    private suspend fun doRegisterIdentityV2(email: String): String = withContext(Dispatchers.IO) {
        if (!isCloudReachable()) throw IOException("Cloud service is not reachable")
        ensureAuth()

        val canonicalEmail = normalizeUserEmail(email)
        val userHash = hashMessageB64(canonicalEmail)
        val pubKexB64 = base64Encode(cryptographyManager.getKexPublic())
        val pubSignB64 = base64Encode(cryptographyManager.getSignPublic())
        val clientTs = getCurrentUtcTimestamp()

        val verification = hashMessageB64(
            listOf("register", userHash, pubKexB64, pubSignB64, clientTs.toString()).joinToString(".")
        )
        val signature = base64Encode(cryptographyManager.signDetached(base64Decode(verification)))

        val payload = RegisterIdentityV2Payload(
            email = canonicalEmail,
            pub_kex_b64 = pubKexB64,
            pub_sign_b64 = pubSignB64,
            client_ts = clientTs,
            proof = IdentityProofPayload(
                verification_b64 = verification,
                signature_b64 = signature
            )
        )

        val url = FIREBASE_MAIN_ENTRYPOINT + FIREBASE_ENDPOINT_V2_REGISTER_IDENTITY
        val body = Json.encodeToString(RegisterIdentityV2Payload.serializer(), payload)
        val client = OkHttpClient()
        val requestId = newRequestId()

        for (attempt in 0..1) {
            val request = buildPostRequest(
                url,
                body,
                authHeaders(requestId)
            )
            try {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body.string()
                    if (response.isSuccessful) {
                        val jsonBody = Json.parseToJsonElement(bodyString).jsonObject
                        return@withContext jsonBody["token"]?.jsonPrimitive?.content.orEmpty()
                            .ifBlank { cloudState.value.token.orEmpty() }
                    }

                    val canRetryAuthOnce = attempt == 0 && (response.code == 401 || response.code == 403)
                    if (canRetryAuthOnce) {
                        AppLogger.warn(
                            "CloudServiceImpl",
                            "event=register_identity_v2_retry_on_auth_failure code=${response.code}"
                        )
                        val refreshedToken = refreshIdToken().getOrNull()
                        if (refreshedToken.isNullOrBlank()) {
                            throw SecurityException("Unable to refresh Firebase ID token for identity retry.")
                        }
                        continue
                    }

                    val requestId = response.header("x-request-id")
                        ?: response.header("x-correlation-id")
                        ?: response.header("traceparent")
                    val errorSnippet = AppLogger.redacted(bodyString, head = 64)
                    AppLogger.warn(
                        "CloudServiceImpl",
                        "event=register_identity_v2_http_failed code=${response.code} requestId=${AppLogger.redacted(requestId)} body=${errorSnippet}"
                    )
                    throw IdentityBackendRejectedException("Identity registration rejected (HTTP ${response.code}).")
                }
            } catch (e: Exception) {
                AppLogger.error("CloudServiceImpl", "event=register_identity_v2_exception", e)
                throw e
            }
        }

        throw SecurityException("Identity registration did not return a response.")
    }

    private suspend fun fetchPayloadArrayForRecipientHash(
        client: OkHttpClient,
        endpoint: String,
        recipientHash: String
    ) = withContext(Dispatchers.IO) {
        val encodedRecipient = URLEncoder.encode(recipientHash, Charsets.UTF_8.name())
        val url = "$FIREBASE_MAIN_ENTRYPOINT$endpoint?user=$encodedRecipient"

        val request = Request.Builder()
            .url(url)
            .get()
            .headers(authHeaders(newRequestId()).toHeaders())
            .build()

        AppLogger.debug("CloudServiceImpl", "event=fetch_data_start recipientHash=${AppLogger.redacted(recipientHash)}")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body.string()
                AppLogger.error("CloudServiceImpl", "event=fetch_data_failed code=${response.code}")
                throw IOException("Failed to fetch data: ${response.code} - $errorBody")
            }

            val bodyString = response.body.string()
            AppLogger.debug("CloudServiceImpl", "event=fetch_data_response code=${response.code}")
            val jsonBody = Json.parseToJsonElement(bodyString).jsonObject
            jsonBody["payload"]?.jsonArray
                ?: throw IllegalStateException("No payload array in response")
        }
    }

    private suspend fun resolveRecipientHashAndPubKey(email: String): Pair<String, String>? {
        emailHashCandidates(email).forEach { hash ->
            val pubKey = getPubKey(hash)
            if (pubKey.isNotBlank()) return hash to pubKey
        }
        return null
    }

    private suspend fun resolveKnownUserHashOrFallback(email: String): String {
        emailHashCandidates(email).forEach { hash ->
            if (getPubKey(hash).isNotBlank()) return hash
        }
        return hashMessageB64(normalizeUserEmail(email))
    }

    private fun emailHashCandidates(email: String): List<String> {
        val normalized = normalizeUserEmail(email)
        return if (normalized.isBlank()) emptyList() else listOf(hashMessageB64(normalized))
    }

    private suspend fun ensureVerifiedIdentityForEmail(expectedEmail: String) {
        if (!ENFORCE_VERIFIED_EMAIL_FOR_IDENTITY) return

        val user = firebaseAuth.currentUser
            ?: throw VerifiedEmailRequiredException()
        user.reload().await()

        val refreshedUser = firebaseAuth.currentUser
            ?: throw VerifiedEmailRequiredException()

        val expectedCanonical = normalizeUserEmail(expectedEmail)
        val tokenCanonical = normalizeUserEmail(refreshedUser.email.orEmpty())
        val tokenEmailVerified = refreshedUser.isEmailVerified

        if (!tokenEmailVerified || tokenCanonical.isBlank()) {
            throw VerifiedEmailRequiredException()
        }
        if (tokenCanonical != expectedCanonical) {
            throw IdentityEmailMismatchException()
        }

        val refreshedToken = refreshedUser.getIdToken(true).await().token
            ?: throw SecurityException("Unable to refresh Firebase ID token for verified identity.")
        cloudState.value = cloudState.value.copy(
            username = refreshedUser.uid,
            token = refreshedToken,
            isUserValidated = true,
            email = expectedCanonical
        )
    }

    private fun normalizeUserEmail(email: String): String = email.trim().lowercase()

    private fun setIsOnlineInCLoud(v: Boolean) { cloudState.value.isOnline = v }
}
