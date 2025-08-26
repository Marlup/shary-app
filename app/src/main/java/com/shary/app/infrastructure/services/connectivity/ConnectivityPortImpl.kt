package com.shary.app.infrastructure.services.connectivity

import com.shary.app.core.domain.interfaces.ports.CloudStatusDataSource
import com.shary.app.core.domain.interfaces.ports.ConnectivityPort
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.infrastructure.di.DependenciesContainer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive

/**
 * Regla de decisión:
 *  - Si Firestore emite boolean -> usamos ese valor.
 *  - Si Firestore emite null    -> fallback a ping periódico (CloudService.sendPing()).
 */

@Singleton
class ConnectivityPortImpl @Inject constructor(
    private val cloudService: CloudService,
    private val statusDataSource: CloudStatusDataSource,
    @DependenciesContainer.ConnectivityPingPeriod private val pingPeriodMillis: Long
) : ConnectivityPort {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Ping periódico con delay (sin APIs obsoletas)
    private val pingFlow: StateFlow<Boolean> =
        flow {
            // primer valor inmediato
            emit(runCatching { cloudService.sendPing() }.getOrElse { false })
            while (currentCoroutineContext().isActive) {
                delay(pingPeriodMillis)
                val ok = runCatching { cloudService.sendPing() }.getOrElse { false }
                emit(ok)
            }
        }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    // true/false desde Firestore; null => desconocido
    private val firestoreFlow: Flow<Boolean?> =
        statusDataSource.firestoreOnline.distinctUntilChanged()

    private val _isCloudOnline: StateFlow<Boolean> =
        combine(firestoreFlow.onStart { emit(null) }, pingFlow) { fs, ping ->
            fs ?: ping
        }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    override val isCloudOnline: StateFlow<Boolean> = _isCloudOnline
}

