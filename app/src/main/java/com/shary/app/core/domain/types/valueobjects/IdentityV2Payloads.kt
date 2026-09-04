package com.shary.app.core.domain.types.valueobjects

import kotlinx.serialization.Serializable

@Serializable
data class IdentityProofPayload(
    val verification_b64: String,
    val signature_b64: String
)

@Serializable
data class RegisterIdentityV2Payload(
    val email: String,
    val pub_kex_b64: String,
    val pub_sign_b64: String,
    val client_ts: Long,
    val proof: IdentityProofPayload
)

@Serializable
data class RotateIdentityV2Payload(
    val email: String,
    val new_pub_kex_b64: String,
    val new_pub_sign_b64: String,
    val nonce: String,
    val old_key_signature_b64: String,
    val new_key_signature_b64: String,
    val client_ts: Long
)

@Serializable
data class DeleteIdentityV2Payload(
    val email: String,
    val client_ts: Long,
    val signature_b64: String
)

