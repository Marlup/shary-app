package com.shary.app.application.security

import android.content.Context
import androidx.datastore.core.DataStore
import com.shary.app.Field
import com.shary.app.FieldList
import com.shary.app.Request
import com.shary.app.RequestList
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.models.stableLegacyRequestId
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.infrastructure.security.helper.SecurityUtils
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangePasswordAndRewrapDataKeyUseCase @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val cryptographyManager: CryptographyManager,
    private val credentialsStore: CredentialsStore,
    private val fieldCodec: FieldCodec,
    private val fieldDataStore: DataStore<FieldList>,
    private val requestDataStore: DataStore<RequestList>
) {

    suspend operator fun invoke(
        context: Context,
        oldPassword: String,
        newPassword: String
    ) {
        val auth = authenticationService.state.value
        val email = auth.email.ifBlank { throw IllegalStateException("No active user session.") }

        val oldKek = deriveSafePassword(email, oldPassword)
        val newKek = deriveSafePassword(email, newPassword)

        check(oldKek == auth.safePassword) { "Old password is incorrect." }
        check(newKek != oldKek) { "New password must be different from old password." }

        val oldCredentialsBlob = credentialsStore.readCredentials(context)
            ?: throw IllegalStateException("Credentials file not found.")
        val oldFieldSnapshot = fieldDataStore.data.first()
        val oldRequestSnapshot = requestDataStore.data.first()

        val currentCredentialsJson = cryptographyManager.decryptCredentialsByDerivation(
            email = email,
            p = oldKek,
            purpose = Purpose.Credentials.code,
            encrypted = oldCredentialsBlob
        )

        val rewrappedSnapshots = rewrapDataKey(
            email = email,
            oldKek = oldKek,
            newKek = newKek,
            fieldSnapshot = oldFieldSnapshot,
            requestSnapshot = oldRequestSnapshot
        )

        val newCredentialsBlob = encryptCredentialsWithNewKek(
            email = email,
            newKek = newKek,
            oldJson = currentCredentialsJson
        )

        try {
            fieldDataStore.updateData { rewrappedSnapshots.fieldList }
            requestDataStore.updateData { rewrappedSnapshots.requestList }
            credentialsStore.writeCredentials(context, newCredentialsBlob)
            authenticationService.setSafePassword(newKek)
        } catch (error: Throwable) {
            runCatching {
                fieldDataStore.updateData { oldFieldSnapshot }
                requestDataStore.updateData { oldRequestSnapshot }
                credentialsStore.writeCredentials(context, oldCredentialsBlob)
                applyLocalKeys(email, oldKek)
                authenticationService.setSafePassword(oldKek)
            }
            throw error
        }
    }

    private fun rewrapDataKey(
        email: String,
        oldKek: String,
        newKek: String,
        fieldSnapshot: FieldList,
        requestSnapshot: RequestList
    ): RewrappedSnapshots {
        applyLocalKeys(email, oldKek)

        val plainFields = fieldSnapshot.fieldsList.map(::decodeField)
        val plainRequests = requestSnapshot.requestsList.map(::decodeRequest)
        val plainReceivedRequests = requestSnapshot.receivedRequestsList.map(::decodeRequest)
        val plainSentRequests = requestSnapshot.sentRequestsList.map(::decodeRequest)

        applyLocalKeys(email, newKek)

        val newFieldList = FieldList.newBuilder()
            .addAllFields(plainFields.map(::encodeField))
            .build()

        val newRequestList = RequestList.newBuilder()
            .addAllRequests(plainRequests.map(::encodeRequest))
            .addAllReceivedRequests(plainReceivedRequests.map(::encodeRequest))
            .addAllSentRequests(plainSentRequests.map(::encodeRequest))
            .build()

        return RewrappedSnapshots(newFieldList, newRequestList)
    }

    private fun decodeField(field: Field): PlainField =
        PlainField(
            key = fieldCodec.decode(field.key, Purpose.Key),
            value = fieldCodec.decode(field.value, Purpose.Value),
            keyAlias = fieldCodec.decode(field.keyAlias, Purpose.Alias),
            tag = fieldCodec.decode(field.tag, Purpose.Tag),
            dateAdded = field.dateAdded,
            isMcpShareable = field.isMcpShareable
        )

    private fun encodeField(field: PlainField): Field =
        Field.newBuilder()
            .setKey(fieldCodec.encode(field.key, Purpose.Key))
            .setValue(fieldCodec.encode(field.value, Purpose.Value))
            .setKeyAlias(fieldCodec.encode(field.keyAlias, Purpose.Alias))
            .setTag(fieldCodec.encode(field.tag, Purpose.Tag))
            .setDateAdded(field.dateAdded)
            .setIsMcpShareable(field.isMcpShareable)
            .build()

    private fun decodeRequest(request: Request): PlainRequest =
        PlainRequest(
            id = request.id.ifBlank {
                stableLegacyRequestId(
                    user = request.user,
                    requesterId = request.requesterId,
                    dateAddedEpochMillis = request.dateAdded,
                    owned = request.owned,
                    recipients = request.recipientsList,
                    fieldIdentityParts = request.fieldsList.map { it.key }
                )
            },
            fields = request.fieldsList.map(::decodeField),
            user = request.user,
            requesterId = request.requesterId,
            recipients = request.recipientsList,
            dateAdded = request.dateAdded,
            owned = request.owned,
            responded = request.responded
        )

    private fun encodeRequest(request: PlainRequest): Request =
        Request.newBuilder()
            .setId(request.id)
            .addAllFields(request.fields.map(::encodeField))
            .setUser(request.user)
            .setRequesterId(request.requesterId)
            .addAllRecipients(request.recipients)
            .setDateAdded(request.dateAdded)
            .setOwned(request.owned)
            .setResponded(request.responded)
            .build()

    private fun encryptCredentialsWithNewKek(
        email: String,
        newKek: String,
        oldJson: JSONObject
    ): ByteArray {
        oldJson.put("ts", SecurityUtils.getCurrentUtcTimestamp())
        oldJson.put("version", 3)
        return cryptographyManager.encryptCredentialsByDerivation(
            email = email,
            p = newKek,
            purpose = Purpose.Credentials.code,
            json = oldJson
        )
    }

    private fun applyLocalKeys(email: String, safePassword: String) {
        Purpose.builtIns.forEach { purpose ->
            val localKey = cryptographyManager.deriveLocalKey(
                email = email,
                password = safePassword.toCharArray(),
                purpose = purpose.code
            )
            authenticationService.addLocalKeyByPurpose(purpose, localKey)
        }
    }

    private fun deriveSafePassword(email: String, password: String): String =
        SecurityUtils.base64Encode(cryptographyManager.hashPassword(password, email))
}

private data class PlainField(
    val key: String,
    val value: String,
    val keyAlias: String,
    val tag: String,
    val dateAdded: Long,
    val isMcpShareable: Boolean
)

private data class PlainRequest(
    val id: String,
    val fields: List<PlainField>,
    val user: String,
    val requesterId: String,
    val recipients: List<String>,
    val dateAdded: Long,
    val owned: Boolean,
    val responded: Boolean
)

private data class RewrappedSnapshots(
    val fieldList: FieldList,
    val requestList: RequestList
)
