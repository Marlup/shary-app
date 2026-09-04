package com.shary.app.viewmodels.authentication

import androidx.lifecycle.ViewModel
import com.shary.app.application.session.SessionTimeoutCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionTimeoutCoordinator: SessionTimeoutCoordinator
) : ViewModel() {
    val sessionClosed = sessionTimeoutCoordinator.sessionClosed
    val sessionClosing = sessionTimeoutCoordinator.sessionClosing

    fun closeSession() {
        sessionTimeoutCoordinator.requestClose()
    }
}
