package com.shary.app.core.domain.interfaces.ports

import kotlinx.coroutines.flow.StateFlow

// Read-only cloud connectivity & health status (based on Firestore signals)

/**
 * Si Firestore provee un doc/flag de disponibilidad, publícalo aquí.
 * Cuando sea desconocido, emite null y caeremos al ping.
 */
interface ConnectivityPort {
    /** True if Firestore indicates the cloud is online (not just device connectivity). */
    val isCloudOnline: StateFlow<Boolean?>
}
