package com.shary.app.viewmodels.tag

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.interfaces.repositories.TagCreateResult
import com.shary.app.core.domain.interfaces.repositories.TagUpdateResult
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.defaultMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags
    private val editorOperationController = TagEditorOperationController()
    val editorOperationState: StateFlow<TagEditorOperationState> =
        editorOperationController.state

    init {
        viewModelScope.launch {
            tagRepository.allTags.collect { _tags.value = it }
        }
    }

    fun submitAddTag(name: String, color: Color) {
        val operation = TagEditorOperation.ADD
        if (!editorOperationController.claim(operation, name)) return
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { tagRepository.addTag(name, color) }
            }
            result.onSuccess { outcome ->
                when (outcome) {
                    is TagCreateResult.Created ->
                        editorOperationController.succeed(operation, name, outcome.tag)
                    is TagCreateResult.AlreadyExists ->
                        editorOperationController.fail(
                            operation,
                            name,
                            "A tag named '${outcome.existing.name}' already exists"
                        )
                    is TagCreateResult.Invalid ->
                        editorOperationController.fail(operation, name, outcome.reason.defaultMessage())
                }
            }.onFailure { error ->
                editorOperationController.fail(
                    operation,
                    name,
                    error.message ?: "Unable to create the tag."
                )
            }
        }
    }

    fun submitUpdateTag(originalName: String, name: String, color: Color) {
        val operation = TagEditorOperation.UPDATE
        if (!editorOperationController.claim(operation, originalName)) return
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    tagRepository.updateTag(originalName, name, color)
                }
            }
            result.onSuccess { outcome ->
                when (outcome) {
                    is TagUpdateResult.Updated ->
                        editorOperationController.succeed(operation, originalName, outcome.tag)
                    is TagUpdateResult.Unchanged ->
                        editorOperationController.succeed(operation, originalName, outcome.tag)
                    is TagUpdateResult.NameAlreadyExists ->
                        editorOperationController.fail(
                            operation,
                            originalName,
                            "A tag named '${outcome.existing.name}' already exists"
                        )
                    TagUpdateResult.NotFound ->
                        editorOperationController.fail(
                            operation,
                            originalName,
                            "The original tag no longer exists"
                        )
                    is TagUpdateResult.Invalid ->
                        editorOperationController.fail(
                            operation,
                            originalName,
                            outcome.reason.defaultMessage()
                        )
                }
            }.onFailure { error ->
                editorOperationController.fail(
                    operation,
                    originalName,
                    error.message ?: "Unable to update the tag."
                )
            }
        }
    }

    fun resetEditorOperationState() = editorOperationController.reset()

    fun removeTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.removeTag(name)
        }
    }
}
