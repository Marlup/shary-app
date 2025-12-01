package com.shary.app.viewmodels.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


class NotificationViewModel @Inject constructor() : ViewModel() {
    private val _events = MutableSharedFlow<Map<String, String>>()
    val events: SharedFlow<Map<String, String>> = _events

    fun handleMessage(data: Map<String, String>) {
        viewModelScope.launch { _events.emit(data) }
    }
}
