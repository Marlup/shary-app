package com.shary.app.core.domain.interfaces.events

sealed interface RequestEvent {
    data class FetchedFromCloud(val matchedCount: Int) : RequestEvent
    data class FetchError(val throwable: Throwable) : RequestEvent
}