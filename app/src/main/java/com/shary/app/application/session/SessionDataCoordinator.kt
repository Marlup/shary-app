package com.shary.app.application.session

import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.utils.log.AppLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val INITIAL_FIELD_BATCH_SIZE = 5
const val FIELD_DECRYPT_CHUNK_SIZE = 25

internal fun isInitialFieldBatchReady(
    loadedFieldCount: Int,
    loadComplete: Boolean
): Boolean = loadedFieldCount >= INITIAL_FIELD_BATCH_SIZE || loadComplete

data class SessionDataState(
    val fields: List<FieldDomain> = emptyList(),
    val users: List<UserDomain> = emptyList(),
    val attemptedFieldCount: Int = 0,
    val totalFieldCount: Int = 0,
    val failedFieldCount: Int = 0,
    val initialFieldsReady: Boolean = false,
    val fieldsComplete: Boolean = false,
    val fieldLoadError: String? = null,
    val started: Boolean = false
)

@Singleton
class SessionDataCoordinator @Inject constructor(
    private val fieldRepository: FieldRepository,
    private val userRepository: UserRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(SessionDataState())
    val state: StateFlow<SessionDataState> = _state.asStateFlow()

    private var fieldJob: Job? = null
    private var userJob: Job? = null

    fun start() {
        fieldJob?.cancel()
        userJob?.cancel()
        _state.value = SessionDataState(started = true)
        startFieldLoading(resetProgress = true)
        startUserLoading()
    }

    suspend fun startAndAwaitInitialFields() {
        start()
        state.filter { it.initialFieldsReady }.first()
    }

    fun refreshFields() {
        if (!_state.value.started) {
            start()
        } else {
            fieldJob?.cancel()
            startFieldLoading(resetProgress = false)
        }
    }

    fun refreshUsers() {
        if (!_state.value.started) {
            start()
        } else {
            userJob?.cancel()
            startUserLoading()
        }
    }

    fun clear() {
        fieldJob?.cancel()
        userJob?.cancel()
        fieldJob = null
        userJob = null
        _state.value = SessionDataState()
    }

    private fun startFieldLoading(resetProgress: Boolean) {
        if (resetProgress) {
            _state.update {
                it.copy(
                    fields = emptyList(),
                    attemptedFieldCount = 0,
                    totalFieldCount = 0,
                    failedFieldCount = 0,
                    initialFieldsReady = false,
                    fieldsComplete = false,
                    fieldLoadError = null
                )
            }
        }
        val startedAt = System.currentTimeMillis()
        fieldJob = scope.launch {
            var initialReadyLogged = false
            runCatching {
                fieldRepository.getFieldLoadSnapshots(
                    firstBatchSize = INITIAL_FIELD_BATCH_SIZE,
                    chunkSize = FIELD_DECRYPT_CHUNK_SIZE
                ).collect { snapshot ->
                    val ready = isInitialFieldBatchReady(snapshot.fields.size, snapshot.complete)
                    _state.update {
                        it.copy(
                            fields = snapshot.fields,
                            attemptedFieldCount = snapshot.attemptedCount,
                            totalFieldCount = snapshot.totalCount,
                            failedFieldCount = snapshot.failureCount,
                            initialFieldsReady = ready,
                            fieldsComplete = snapshot.complete,
                            fieldLoadError = null
                        )
                    }
                    if (ready && !initialReadyLogged) {
                        initialReadyLogged = true
                        AppLogger.info(
                            "StartupTrace",
                            "event=initial_field_batch_ready count=${snapshot.fields.size} " +
                                "total=${snapshot.totalCount} failures=${snapshot.failureCount} " +
                                "elapsed_ms=${System.currentTimeMillis() - startedAt}"
                        )
                    }
                }
                fieldRepository.getAllFields().collect { fields ->
                    _state.update {
                        it.copy(
                            fields = fields,
                            fieldsComplete = true,
                            initialFieldsReady = true
                        )
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                AppLogger.warn("SessionDataCoordinator", "event=field_load_failed")
                _state.update {
                    it.copy(
                        initialFieldsReady = true,
                        fieldsComplete = true,
                        fieldLoadError = error.message ?: "Unable to load fields"
                    )
                }
            }
        }
    }

    private fun startUserLoading() {
        userJob = scope.launch {
            runCatching {
                userRepository.users.collect { users ->
                    _state.update { it.copy(users = users) }
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                AppLogger.warn("SessionDataCoordinator", "event=user_load_failed")
            }
        }
    }
}
