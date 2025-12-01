package com.shary.app.core.domain.interfaces.ports

import kotlinx.coroutines.flow.StateFlow

// Read-only ephemeral token (from Firestore state)
interface AuthPort {
    /** Emits the current ephemeral token provided by Firestore; null if none. */
    val authToken: StateFlow<String?>
}


