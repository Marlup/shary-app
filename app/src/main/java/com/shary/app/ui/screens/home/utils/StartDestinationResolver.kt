package com.shary.app.ui.screens.home.utils

import android.content.Context
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.types.enums.StartDestination
import com.shary.app.viewmodels.authentication.AuthenticationViewModel


/**
 * Resuelve el destino inicial sin usar ViewModel.
 * Evalúa la firma y credenciales en memoria.
 */
object StartDestinationResolver {
    fun resolve(
        context: Context,
        authModel: AuthenticationViewModel,
        credentialsStore: CredentialsStore
    ): StartDestination {
        val hasSignature = authModel.isSignatureActive(context)
        val hasCredentials = credentialsStore.hasCredentials(context)

        return when {
            !hasSignature -> StartDestination.LOGUP
            !hasCredentials -> StartDestination.LOGIN
            else -> StartDestination.LOGUP
        }
    }
}

