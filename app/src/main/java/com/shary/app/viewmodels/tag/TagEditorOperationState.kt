package com.shary.app.viewmodels.tag

import com.shary.app.core.domain.types.enums.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TagEditorOperation {
    ADD,
    UPDATE
}

sealed interface TagEditorOperationState {
    data object Idle : TagEditorOperationState

    data class Saving(
        val operation: TagEditorOperation,
        val identity: String
    ) : TagEditorOperationState

    data class Saved(
        val operation: TagEditorOperation,
        val identity: String,
        val tag: Tag
    ) : TagEditorOperationState

    data class Failed(
        val operation: TagEditorOperation,
        val identity: String,
        val message: String
    ) : TagEditorOperationState
}

class TagEditorOperationController {
    private val _state = MutableStateFlow<TagEditorOperationState>(TagEditorOperationState.Idle)
    val state: StateFlow<TagEditorOperationState> = _state.asStateFlow()

    fun claim(operation: TagEditorOperation, identity: String): Boolean {
        val normalizedIdentity = normalizeIdentity(identity)
        while (true) {
            val current = _state.value
            if (current is TagEditorOperationState.Saving) return false
            if (_state.compareAndSet(
                    current,
                    TagEditorOperationState.Saving(operation, normalizedIdentity)
                )
            ) {
                return true
            }
        }
    }

    fun succeed(operation: TagEditorOperation, identity: String, tag: Tag) {
        transitionFromSaving(operation, identity) { normalizedIdentity ->
            TagEditorOperationState.Saved(operation, normalizedIdentity, tag)
        }
    }

    fun fail(operation: TagEditorOperation, identity: String, message: String) {
        transitionFromSaving(operation, identity) { normalizedIdentity ->
            TagEditorOperationState.Failed(
                operation = operation,
                identity = normalizedIdentity,
                message = message.ifBlank { "Unable to save the tag." }
            )
        }
    }

    fun reset() {
        while (true) {
            val current = _state.value
            if (current is TagEditorOperationState.Saving || current is TagEditorOperationState.Idle) {
                return
            }
            if (_state.compareAndSet(current, TagEditorOperationState.Idle)) return
        }
    }

    private inline fun transitionFromSaving(
        operation: TagEditorOperation,
        identity: String,
        terminalState: (String) -> TagEditorOperationState
    ) {
        val normalizedIdentity = normalizeIdentity(identity)
        while (true) {
            val current = _state.value
            if (current !is TagEditorOperationState.Saving ||
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
