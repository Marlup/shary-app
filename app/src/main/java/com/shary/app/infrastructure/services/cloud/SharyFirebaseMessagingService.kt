package com.shary.app.infrastructure.services.cloud

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shary.app.application.usecases.RegisterFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.shary.app.core.domain.interfaces.handler.PushNotificationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SharyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = registerFcmTokenUseCase(token)
            if (result) {
                Log.d("FCM", "New token registered successfully")
            } else {
                Log.w("FCM", "Failed to register new token")
            }
        }
    }
}

