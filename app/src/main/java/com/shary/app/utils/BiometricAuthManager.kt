package com.shary.app.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricAuthManager(
    private val context: Context,
    private val activity: FragmentActivity,
    private val onAuthSuccess: () -> Unit
) {
    private val executor: Executor = ContextCompat.getMainExecutor(context)

    fun authenticate(): String? {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            return when (canAuthenticate) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "No hay biometría configurada. Por favor, actívala en los ajustes."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    "Este dispositivo no tiene hardware biométrico."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    "El hardware biométrico no está disponible actualmente."
                else -> "Error desconocido al verificar biometría."
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación requerida")
            .setSubtitle("Usa huella, rostro o PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Puedes loguear el error si quieres, pero no mostramos desde aquí
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
        return null // todo OK
    }
}
