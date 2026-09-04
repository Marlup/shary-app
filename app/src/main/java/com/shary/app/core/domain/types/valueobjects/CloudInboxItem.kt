package com.shary.app.core.domain.types.valueobjects

data class CloudInboxItem(
    val payload: DataPayload,
    val previewFieldNames: List<String> = emptyList(),
    val importDiff: com.shary.app.application.cloud.CloudImportDiff? = null
) {
    val senderHash: String get() = payload.user
    val createdAt: Long get() = payload.creationAt
    val expiresAt: Long get() = payload.expiresAt
}
