package com.shary.app.core.session


/**
 * Puerto de dominio: NO depende de Android.
 */
interface SessionStatus {
    /** ¿Existe un usuario con credenciales válidas en el dispositivo? */
    suspend fun hasValidCredentials(): Boolean

    /** ¿Existe firma/clave criptográfica activa y utilizable? */
    suspend fun hasActiveSignature(): Boolean

    /** ¿El usuario ya completó la onboarding/logup inicial? (si aplica) */
    suspend fun isFirstRun(): Boolean
}