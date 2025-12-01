package com.shary.app.core.domain.interfaces.ports

import kotlinx.coroutines.flow.Flow

/**
 * Si Firestore provee un doc/flag de disponibilidad, publícalo aquí.
 * Cuando sea desconocido, emite null y caeremos al ping.
 */
interface CloudStatusDataSource {
    val firestoreOnline: Flow<Boolean?>   // true/false cuando hay señal; null si no disponible
}
