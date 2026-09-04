package com.shary.app.infrastructure.mappers

import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.RequestDeliveryChannel
import com.shary.app.core.domain.models.RequestDeliveryOutcome
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.models.stableRequestId
import com.shary.app.core.domain.models.stableLegacyRequestId
import com.shary.app.Field as FieldProto
import com.shary.app.Request as RequestProto
import com.shary.app.User as UserProto
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.StatusDataSentDb
import com.shary.app.core.domain.types.enums.deserialize
import com.shary.app.core.domain.types.enums.serialize
import com.shary.app.core.domain.types.valueobjects.Purpose
import java.time.Instant

// ======================================================================
// Field ↔ Domain (encrypted at-rest / in-store via LocalVault)
// ======================================================================


/** Encrypts a domain Field into proto using LocalVault (on-the-fly derivation). */
fun FieldDomain.toProto(codec: FieldCodec): FieldProto =
    FieldProto.newBuilder()
        .setKey(codec.encode(key, Purpose.Key))
        .setValue(codec.encode(value, Purpose.Value))
        .setKeyAlias(codec.encode(keyAlias, Purpose.Alias))
        .setTag(codec.encode(tag.serialize(), Purpose.Tag)) // Saves name + color
        .setDateAdded(dateAdded.toEpochMilli())
        .setIsMcpShareable(isMcpShareable)
        .build()

/** Decrypts a Field proto into the domain model using LocalVault (on-the-fly derivation). */
fun FieldProto.toDomainResult(codec: FieldCodec): Result<FieldDomain> =
    runCatching {
        val raw = codec.decode(tag, Purpose.Tag).ifBlank { "Unknown" }
        FieldDomain(
            key       = codec.decode(key, Purpose.Key),
            value     = codec.decode(value, Purpose.Value),
            keyAlias  = codec.decode(keyAlias, Purpose.Alias),
            tag       = Tag.deserialize(raw), // recover name + color
            dateAdded = Instant.ofEpochMilli(dateAdded),
            isMcpShareable = isMcpShareable
        )
    }

fun FieldProto.toDomain(codec: FieldCodec): FieldDomain =
    toDomainResult(codec).getOrThrow()

// Convenience for collections
fun List<FieldProto>.toDomainFields(codec: FieldCodec): List<FieldDomain> =
    mapNotNull { it.toDomainResult(codec).getOrNull() }

fun List<FieldDomain>.toProtoFields(codec: FieldCodec): List<FieldProto> =
    map { it.toProto(codec) }

// ======================================================================
// Request ↔ Domain (delegates to Field mappers with vault/creds)
// ======================================================================

fun RequestProto.toDomain(codec: FieldCodec): RequestDomain {
    val stableId = id.trim().ifEmpty {
        stableLegacyRequestId(
            user = user,
            requesterId = requesterId,
            dateAddedEpochMillis = dateAdded,
            owned = owned,
            recipients = recipientsList,
            fieldIdentityParts = fieldsList.map { it.key }
        )
    }
    return RequestDomain(
        fields = fieldsList.toDomainFields(codec),
        dateAdded = Instant.ofEpochMilli(dateAdded),
        user = user,
        recipients = recipientsList, //recipientsList.map { it.toDomain() },
        owned = owned,
        responded = responded,
        id = stableId,
        requesterId = requesterId,
        deliveryChannel = deliveryChannel.toRequestDeliveryChannelOrNull(),
        deliveryOutcomes = deliveryOutcomesList.mapNotNull { outcome ->
            val recipient = outcome.recipient.trim()
            val status = outcome.status.toDeliveryStatusOrNull()
            if (recipient.isEmpty() || status == null) null else {
                RequestDeliveryOutcome(recipient = recipient, status = status)
            }
        },
        deliveryUpdatedAt = deliveryUpdatedAt.takeIf { it > 0L }?.let(Instant::ofEpochMilli)
    )
}

fun RequestDomain.toProto(codec: FieldCodec): RequestProto {
    val builder = RequestProto.newBuilder()
        .setId(stableRequestId())
        .addAllFields(fields.toProtoFields(codec))
        .setDateAdded(dateAdded.toEpochMilli())
        .setUser(user)
        .addAllRecipients(recipients)
        .setOwned(owned)
        .setResponded(responded)
        .setRequesterId(requesterId)
        .addAllDeliveryOutcomes(
            deliveryOutcomes.map { outcome ->
                com.shary.app.RequestDeliveryOutcome.newBuilder()
                    .setRecipient(outcome.recipient.trim())
                    .setStatus(outcome.status.name)
                    .build()
            }
        )

    deliveryChannel?.let { builder.deliveryChannel = it.name }
    deliveryUpdatedAt?.let { builder.deliveryUpdatedAt = it.toEpochMilli() }
    return builder.build()
}

private fun String.toRequestDeliveryChannelOrNull(): RequestDeliveryChannel? =
    runCatching { RequestDeliveryChannel.valueOf(trim()) }.getOrNull()

private fun String.toDeliveryStatusOrNull(): StatusDataSentDb? =
    runCatching { StatusDataSentDb.valueOf(trim()) }.getOrNull()

// ======================================================================
// User ↔ Domain (sin cifrado; igual que antes)
// ======================================================================

fun UserProto.toDomain(): UserDomain =
    UserDomain(
        username  = username.trim(),
        email     = email.trim(),
        dateAdded = Instant.ofEpochMilli(dateAdded),
        tag       = if (tag.isBlank()) Tag.Unknown else Tag.deserialize(tag)
    )

fun UserDomain.toProto(): UserProto =
    UserProto.newBuilder()
        .setUsername(username)
        .setEmail(email)
        .setDateAdded(dateAdded.toEpochMilli())
        .setTag(if (tag == Tag.Unknown) "" else tag.serialize())
        .build()
