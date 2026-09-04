package com.shary.app.viewmodels.clipboard

import androidx.lifecycle.ViewModel
import com.shary.app.core.domain.interfaces.services.ClipboardService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ClipboardViewModel @Inject constructor(
    private val clipboardService: ClipboardService
) : ViewModel() {
    fun copyText(
        label: String,
        text: String,
        sensitive: Boolean,
        clearAfterSeconds: Int
    ) {
        clipboardService.copyText(
            label = label,
            text = text,
            sensitive = sensitive,
            clearAfterSeconds = clearAfterSeconds
        )
    }
}
