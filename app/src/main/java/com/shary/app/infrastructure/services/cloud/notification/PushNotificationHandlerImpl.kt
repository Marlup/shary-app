package com.shary.app.infrastructure.services.cloud.notification

import android.util.Log
import com.shary.app.application.usecases.RegisterFcmTokenUseCase
import com.shary.app.core.domain.interfaces.handler.PushNotificationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationHandlerImpl @Inject constructor(
    private val useCase: RegisterFcmTokenUseCase
) : PushNotificationHandler {


    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = useCase(token)
            if (result) {
                Log.d("FCM", "New token registered successfully")
            } else {
                Log.w("FCM", "Failed to register new token")
            }
        }
    }


    override fun onMessage(data: Map<String, String>) {
        Log.d("PushHandler", "Received data payload: $data")
        // Route to appropriate ViewModel, show local notification, etc.
    }
}
