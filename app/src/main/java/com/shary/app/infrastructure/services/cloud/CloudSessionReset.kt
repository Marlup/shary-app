package com.shary.app.infrastructure.services.cloud

import com.shary.app.core.domain.interfaces.states.CloudState

internal fun CloudState.clearSessionState(): CloudState {
    localKeys.values.forEach { key -> key.fill(0) }
    localKeys.clear()
    return CloudState()
}
