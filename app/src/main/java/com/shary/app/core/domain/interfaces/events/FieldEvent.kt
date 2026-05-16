package com.shary.app.core.domain.interfaces.events

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag

// UI events surfaced by the ViewModel
sealed interface FieldEvent {
    // Keep payloads small and stable for UI; prefer keys instead of whole objects when possible
    data class Saved(val field: FieldDomain) : FieldEvent
    data class AlreadyExists(val key: String) : FieldEvent
    data class Deleted(val key: String) : FieldEvent
    data class MultiDeleted(val keys: List<FieldDomain>) : FieldEvent
    data class ValueUpdated(val key: String) : FieldEvent
    data class ValueRecovered(val key: String) : FieldEvent
    data class AliasUpdated(val key: String) : FieldEvent
    data class TagUpdated(val key: String, val tag: Tag) : FieldEvent
    data object PasswordChanged : FieldEvent
    data class Error(val throwable: Throwable) : FieldEvent
    data class FetchedFromCloud(
        val count: Int,
        val loadedKeys: List<String> = emptyList(),
        val preDownloadedKeys: List<String> = emptyList()
    ) : FieldEvent
    data class CloudInboxLoaded(val count: Int) : FieldEvent
    data object CloudInboxEmpty : FieldEvent
    data class CloudInboxRejected(val backendAcknowledged: Boolean) : FieldEvent
    data class CloudInboxAccepted(val backendAcknowledged: Boolean) : FieldEvent
    data object NoNewFields : FieldEvent
    data class FetchError(val throwable: Throwable) : FieldEvent
}
