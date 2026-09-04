package com.shary.app.viewmodels.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ContactEditorOperation {
    ADD,
    UPDATE
}

sealed interface ContactEditorOperationState {
    data object Idle : ContactEditorOperationState

    data class Saving(
        val operation: ContactEditorOperation,
        val identity: String
    ) : ContactEditorOperationState

    data class Saved(
        val operation: ContactEditorOperation,
        val identity: String
    ) : ContactEditorOperationState

    data class Failed(
        val operation: ContactEditorOperation,
        val identity: String,
        val message: String
    ) : ContactEditorOperationState
}

class ContactEditorOperationController {
    private val _state = MutableStateFlow<ContactEditorOperationState>(ContactEditorOperationState.Idle)
    val state: StateFlow<ContactEditorOperationState> = _state.asStateFlow()

    fun claim(operation: ContactEditorOperation, identity: String): Boolean {
        val normalizedIdentity = normalizeIdentity(identity)
        while (true) {
            val current = _state.value
            if (current is ContactEditorOperationState.Saving) return false
            if (_state.compareAndSet(
                    current,
                    ContactEditorOperationState.Saving(operation, normalizedIdentity)
                )
            ) {
                return true
            }
        }
    }

    fun succeed(operation: ContactEditorOperation, identity: String) {
        transitionFromSaving(operation, identity) { normalizedIdentity ->
            ContactEditorOperationState.Saved(operation, normalizedIdentity)
        }
    }

    fun fail(operation: ContactEditorOperation, identity: String, message: String) {
        transitionFromSaving(operation, identity) { normalizedIdentity ->
            ContactEditorOperationState.Failed(
                operation = operation,
                identity = normalizedIdentity,
                message = message.ifBlank { "Unable to save the contact." }
            )
        }
    }

    fun reset() {
        while (true) {
            val current = _state.value
            if (current is ContactEditorOperationState.Saving ||
                current is ContactEditorOperationState.Idle
            ) {
                return
            }
            if (_state.compareAndSet(current, ContactEditorOperationState.Idle)) return
        }
    }

    private inline fun transitionFromSaving(
        operation: ContactEditorOperation,
        identity: String,
        terminalState: (String) -> ContactEditorOperationState
    ) {
        val normalizedIdentity = normalizeIdentity(identity)
        while (true) {
            val current = _state.value
            if (current !is ContactEditorOperationState.Saving ||
                current.operation != operation ||
                current.identity != normalizedIdentity
            ) {
                return
            }
            if (_state.compareAndSet(current, terminalState(normalizedIdentity))) return
        }
    }

    private fun normalizeIdentity(identity: String): String = identity.trim().lowercase()
}
