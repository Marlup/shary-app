package com.shary.app.viewmodels.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


// TagViewModel.kt (reactive repo with suspend repo.addCustomTag)
@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    // Built-in + dynamic tags mapped to StateFlow (hot) for Compose
    val uiTags: StateFlow<List<UiFieldTag>> =
        tagRepository.allTags
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = UiFieldTag.entries
            )

    /** Non-suspend API for UI/helpers */
    fun addCustomTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.addCustomTag(name.trim())
        }
    }

    fun removeCustomTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.removeCustomTag(name.trim())
        }
    }
}


