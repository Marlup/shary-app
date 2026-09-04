package com.shary.app.infrastructure.connector.transport

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoopbackServer @Inject constructor(
    private val router: LoopbackConnectorRouter
) {
    suspend fun handle(request: LoopbackRequest): LoopbackResponse = router.handle(request)
}
