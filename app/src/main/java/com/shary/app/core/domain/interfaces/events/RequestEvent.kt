package com.shary.app.core.domain.interfaces.events

sealed interface RequestEvent {
    data class FetchedFromCloud(val matchedCount: Int) : RequestEvent
    data class CloudInboxLoaded(val count: Int) : RequestEvent
    data object CloudInboxEmpty : RequestEvent
    data class CloudInboxAccepted(
        val importedKeyCount: Int,
        val backendAcknowledged: Boolean
    ) : RequestEvent
    data class CloudInboxRejected(val backendAcknowledged: Boolean) : RequestEvent
    data class FetchError(val throwable: Throwable) : RequestEvent
}
