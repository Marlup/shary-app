package com.shary.app.viewmodels.field

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FieldEditorOperation {
    ADD,
    UPDATE,
    RECOVER
}

sealed interface FieldEditorOperationState {
    data object Idle : FieldEditorOperationState

    data class Saving(
        val operation: FieldEditorOperation,
        val key: String
    ) : FieldEditorOperationState

    data class Saved(
        val operation: FieldEditorOperation,
        val key: String
    ) : FieldEditorOperationState

    data class Failed(
        val operation: FieldEditorOperation,
        val key: String,
        val message: String
    ) : FieldEditorOperationState
}

class FieldEditorOperationController {
    private val _state = MutableStateFlow<FieldEditorOperationState>(FieldEditorOperationState.Idle)
    val state: StateFlow<FieldEditorOperationState> = _state.asStateFlow()

    fun claim(operation: FieldEditorOperation, key: String): Boolean {
        val normalizedKey = key.trim()
        while (true) {
            val current = _state.value
            if (current is FieldEditorOperationState.Saving) return false
            if (_state.compareAndSet(
                    current,
                    FieldEditorOperationState.Saving(operation, normalizedKey)
                )
            ) {
                return true
            }
        }
    }

    fun succeed(operation: FieldEditorOperation, key: String) {
        transitionFromSaving(operation, key) { normalizedKey ->
            FieldEditorOperationState.Saved(operation, normalizedKey)
        }
    }

    fun fail(operation: FieldEditorOperation, key: String, message: String) {
        transitionFromSaving(operation, key) { normalizedKey ->
            FieldEditorOperationState.Failed(
                operation = operation,
                key = normalizedKey,
                message = message.ifBlank { "Unable to save the field." }
            )
        }
    }

    fun reset() {
        while (true) {
            val current = _state.value
            if (current is FieldEditorOperationState.Saving || current is FieldEditorOperationState.Idle) {
                return
            }
            if (_state.compareAndSet(current, FieldEditorOperationState.Idle)) return
        }
    }

    private inline fun transitionFromSaving(
        operation: FieldEditorOperation,
        key: String,
        terminalState: (String) -> FieldEditorOperationState
    ) {
        val normalizedKey = key.trim()
        while (true) {
            val current = _state.value
            if (current !is FieldEditorOperationState.Saving ||
                current.operation != operation ||
                current.key != normalizedKey
            ) {
                return
            }
            if (_state.compareAndSet(current, terminalState(normalizedKey))) return
        }
    }
}
