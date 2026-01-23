package com.shary.app.infrastructure.mappers

import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.Field as FieldProto
import com.shary.app.Request as RequestProto
import com.shary.app.User as UserProto
import com.shary.app.core.domain.types.enums.Tag
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
        .build()

/** Decrypts a Field proto into the domain model using LocalVault (on-the-fly derivation). */
fun FieldProto.toDomain(codec: FieldCodec): FieldDomain =
    try {
        val raw = codec.decode(tag, Purpose.Tag).ifBlank { "Unknown" }
        FieldDomain(
            key       = codec.decode(key, Purpose.Key),
            value     = codec.decode(value, Purpose.Value),
            keyAlias  = codec.decode(keyAlias, Purpose.Alias),
            tag       = Tag.deserialize(raw), // recover name + color
            dateAdded = Instant.ofEpochMilli(dateAdded)
        )
} catch (_: Exception) {
    // Fallback for corrupted registers or invalid credentials: avoids breaking the flow
    FieldDomain(
        key       = key,                     // it will remain encrypted (useful for diagnostics)
        value     = value,
        keyAlias  = keyAlias,
        tag       = Tag.Unknown,
        dateAdded = Instant.ofEpochMilli(dateAdded)
    )
}

// Convenience for collections
fun List<FieldProto>.toDomainFields(codec: FieldCodec): List<FieldDomain> =
    map { it.toDomain(codec) }

fun List<FieldDomain>.toProtoFields(codec: FieldCodec): List<FieldProto> =
    map { it.toProto(codec) }

// ======================================================================
// Request ↔ Domain (delegates to Field mappers with vault/creds)
// ======================================================================

fun RequestProto.toDomain(codec: FieldCodec): RequestDomain =
    RequestDomain(
        fields = fieldsList.toDomainFields(codec),
        dateAdded = Instant.ofEpochMilli(dateAdded),
        user = user,
        recipients = recipientsList, //recipientsList.map { it.toDomain() },
        owned = owned,
        responded = responded
    )

fun RequestDomain.toProto(codec: FieldCodec): RequestProto =
    RequestProto.newBuilder()
        .addAllFields(fields.toProtoFields(codec))
        .setDateAdded(dateAdded.toEpochMilli())
        .setUser(user)
        .addAllRecipients(recipients)
        .setOwned(owned)
        .setResponded(responded)
        .build()

// ======================================================================
// User ↔ Domain (sin cifrado; igual que antes)
// ======================================================================

fun UserProto.toDomain(): UserDomain =
    UserDomain(
        username  = username.trim(),
        email     = email.trim(),
        dateAdded = Instant.ofEpochMilli(dateAdded)
    )

fun UserDomain.toProto(): UserProto =
    UserProto.newBuilder()
        .setUsername(username)
        .setEmail(email)
        .setDateAdded(dateAdded.toEpochMilli())
        .build()
