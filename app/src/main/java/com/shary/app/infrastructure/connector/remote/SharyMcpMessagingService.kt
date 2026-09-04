package com.shary.app.infrastructure.connector.remote

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SharyMcpMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "shary_mcp_operation") {
            RemoteMcpWorkScheduler.enqueueNow(applicationContext)
        }
    }

    override fun onNewToken(token: String) {
        RemoteMcpWorkScheduler.enqueueNow(applicationContext)
    }

    override fun onDeletedMessages() {
        RemoteMcpWorkScheduler.enqueueNow(applicationContext)
    }
}
