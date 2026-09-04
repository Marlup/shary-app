package com.shary.app.application.session

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val SESSION_BACKGROUND_TIMEOUT_MS = 5 * 60 * 1000L

internal fun hasBackgroundSessionExpired(
    backgroundStartedAt: Long,
    now: Long,
    timeoutMs: Long = SESSION_BACKGROUND_TIMEOUT_MS
): Boolean = now - backgroundStartedAt >= timeoutMs

@Singleton
class SessionTimeoutCoordinator @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val sessionDataCoordinator: SessionDataCoordinator
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val closeMutex = Mutex()
    private val _sessionClosed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionClosed: SharedFlow<Unit> = _sessionClosed.asSharedFlow()
    private val _sessionClosing = MutableStateFlow(false)
    val sessionClosing: StateFlow<Boolean> = _sessionClosing.asStateFlow()

    private var backgroundStartedAt: Long? = null
    private var timeoutJob: Job? = null
    private var observing = false

    fun start() {
        if (observing) return
        observing = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        val startedAt = SystemClock.elapsedRealtime()
        backgroundStartedAt = startedAt
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(SESSION_BACKGROUND_TIMEOUT_MS)
            if (backgroundStartedAt == startedAt) closeSession()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val startedAt = backgroundStartedAt ?: return
        backgroundStartedAt = null
        timeoutJob?.cancel()
        timeoutJob = null
        if (hasBackgroundSessionExpired(startedAt, SystemClock.elapsedRealtime())) {
            requestClose()
        }
    }

    fun requestClose() {
        _sessionClosing.value = true
        scope.launch { closeSession() }
    }

    private suspend fun closeSession() {
        _sessionClosing.value = true
        try {
            closeMutex.withLock {
                backgroundStartedAt = null
                timeoutJob = null
                sessionDataCoordinator.clear()
                runCatching { authenticationService.logoutForRelogin() }
                _sessionClosed.emit(Unit)
            }
        } finally {
            _sessionClosing.value = false
        }
    }
}
