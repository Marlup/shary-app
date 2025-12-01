package com.shary.app.core.domain.interfaces.services


/**
 * CloudService: puerto de dominio para operaciones mínimas de identidad en la nube.
 * Mantiene al dominio desacoplado de Firebase u otros proveedores.
 */
interface FirebaseCloudService {
    /**
     * Autenticación anónima. Devuelve uid si la operación es exitosa.
     * Debe ser idempotente: si ya hay sesión, retorna el uid actual.
     */
    suspend fun signInAnonymously(): Result<String>

    /**
     * Devuelve el uid actual si hay sesión establecida.
     */
    fun currentUid(): String?

    /**
     * Cierra la sesión actual (si aplica).
     */
    suspend fun signOut(): Result<Unit>

    /**
     * (Opcional) Garantiza bootstrap mínimo del perfil en Firestore/RTDB
     * sin PII: por ejemplo, un doc con timestamps o flags de integridad.
     */
    suspend fun ensureBootstrapProfile(): Result<Unit>
}