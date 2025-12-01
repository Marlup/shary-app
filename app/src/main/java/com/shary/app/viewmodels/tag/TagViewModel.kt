package com.shary.app.viewmodels.tag

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.types.enums.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags

    init {
        viewModelScope.launch {
            tagRepository.allTags.collect { _tags.value = it }
        }
    }

    fun addTag(name: String, color: Color) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.addTag(name, color)
        }
    }

    fun updateTag(name: String, color: Color) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.updateTag(name, color)
        }
    }

    fun removeTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.removeTag(name)
        }
    }
}
