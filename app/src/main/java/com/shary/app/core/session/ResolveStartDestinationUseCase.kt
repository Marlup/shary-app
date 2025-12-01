package com.shary.app.core.session

import com.shary.app.core.domain.types.enums.StartDestination


class ResolveStartDestinationUseCase(
    private val repo: SessionStatus
) {
    /**
     * Regla sencilla (ajústala a tu flujo):
     * - Si es primera ejecución → LOGUP
     * - Si hay credenciales + firma → HOME
     * - Si hay credenciales pero sin firma → LOGIN (para completar setup/auth)
     * - En otro caso → LOGUP
     */
    suspend operator fun invoke(): StartDestination {
        if (repo.isFirstRun()) return StartDestination.LOGUP

        val hasCreds = repo.hasValidCredentials()
        val hasSig = repo.hasActiveSignature()

        return when {
            hasCreds && !hasSig -> StartDestination.LOGIN
            else -> StartDestination.LOGUP
        }
    }
}
