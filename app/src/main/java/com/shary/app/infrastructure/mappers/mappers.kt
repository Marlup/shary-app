package com.shary.app.infrastructure.mappers

import android.util.Log
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.RequestDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.Field as FieldProto
import com.shary.app.Request as RequestProto
import com.shary.app.User as UserProto
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.core.domain.types.valueobjects.Purpose
import java.time.Instant

// ======================================================================
// Field ↔ Domain (encrypted at-rest / in-store via LocalVault)
// ======================================================================

/** Decrypts a Field proto into the domain model using LocalVault (on-the-fly derivation). */
fun FieldProto.toDomain(
    codec: FieldCodec
): FieldDomain = try {

    FieldDomain(
        //key       = codec.decode(key, Purpose.Key.code).trim(),
        key       = codec.decode(key, Purpose.Key),
        value     = codec.decode(value, Purpose.Value),
        //keyAlias  = codec.decode(keyAlias, Purpose.Alias).ifBlank { null }?.trim(),
        keyAlias  = codec.decode(keyAlias, Purpose.Alias).ifBlank { null },
        tag       = UiFieldTag.fromString(
            codec.decode(tag, Purpose.Tag).ifBlank { "unknown" }
        ),
        dateAdded = Instant.ofEpochMilli(dateAdded)
    )
} catch (_: Exception) {
    // Fallback para registros dañados o credenciales no válidas: no romper flujo
    FieldDomain(
        key       = key,                     // seguirá cifrado (útil para diagnóstico)
        value     = value,
        keyAlias  = keyAlias.ifBlank { null },
        tag       = UiFieldTag.Unknown,
        dateAdded = Instant.ofEpochMilli(dateAdded)
    )
}

/** Encrypts a domain Field into proto using LocalVault (on-the-fly derivation). */
fun FieldDomain.toProto(codec: FieldCodec): FieldProto {
    Log.w("FieldDomain", "at start - toProto: $this")
    //val cleanKey = key.trim()
    val cleanKey = key
    //val cleanAlias = keyAlias?.trim().orEmpty()
    val cleanAlias = keyAlias.orEmpty()
    val tagStr = tag.toTagString()

    Log.w("FieldDomain", "before encryption - toProto: $this")

    val newField = FieldProto.newBuilder()
        .setKey(codec.encode(cleanKey, Purpose.Key))
        .setValue(codec.encode(value, Purpose.Value))
        .setKeyAlias(codec.encode(cleanAlias, Purpose.Alias))
        .setTag(codec.encode(tagStr, Purpose.Tag))
        .setDateAdded(dateAdded.toEpochMilli())
        .build()

    Log.w("FieldDomain", "after encryption and ToProto - toProto: $newField")
    return newField
}

// Convenience para colecciones
fun List<FieldProto>.toDomainFields(codec: FieldCodec): List<FieldDomain> =
    map { it.toDomain(codec) }

fun List<FieldDomain>.toProtoFields(codec: FieldCodec): List<FieldProto> =
    map { it.toProto(codec) }

// ======================================================================
// Request ↔ Domain (delegates to Field mappers with vault/creds)
// ======================================================================

fun RequestProto.toDomain(codec: FieldCodec): RequestDomain =
    RequestDomain(
        id        = id,
        fields    = fieldsList.toDomainFields(codec),
        dateAdded = Instant.ofEpochMilli(dateAdded)
    )

fun RequestDomain.toProto(codec: FieldCodec): RequestProto =
    RequestProto.newBuilder()
        .setId(id)
        .addAllFields(fields.toProtoFields(codec))
        .setDateAdded(dateAdded.toEpochMilli())
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
