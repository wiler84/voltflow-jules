package com.example.voltflow.notifications

import com.example.voltflow.data.UserPreferencesStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VoltflowFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            UserPreferencesStore(applicationContext).setPushToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Voltflow update"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new account update."
        val type = message.data["type"] ?: "system"

        NotificationHelper(applicationContext).dispatch(
            title = title,
            body = body,
            type = type,
        )
    }
}
