package com.shary.app.utils

import android.content.Context
import android.os.Build
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
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuthenticate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricManager.canAuthenticate(authenticators)
        } else {
            // Android 10 and lower cannot reliably query the combined credential mode.
            BiometricManager.BIOMETRIC_SUCCESS
        }

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            return when (canAuthenticate) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "No biometric or device credential is configured."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    "This device does not support biometric authentication."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    "Biometric authentication is currently unavailable."
                else -> "Device authentication is unavailable."
            }
        }

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm Shary access")
            .setSubtitle("Use biometrics, PIN, pattern, or password")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptBuilder.setAllowedAuthenticators(authenticators)
        } else {
            @Suppress("DEPRECATION")
            promptBuilder.setDeviceCredentialAllowed(true)
        }
        val promptInfo = promptBuilder.build()

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
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
        return null
    }
}
