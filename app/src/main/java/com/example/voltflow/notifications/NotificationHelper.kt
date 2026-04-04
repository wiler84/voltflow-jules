package com.example.voltflow.notifications

import android.Manifest
import android.graphics.BitmapFactory
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.voltflow.MainActivity
import com.example.voltflow.R
import kotlin.random.Random

private const val VOLTFLOW_ALERTS_CHANNEL = "voltflow_alerts"

class NotificationHelper(private val context: Context) {
    private val appContext = context.applicationContext

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(VOLTFLOW_ALERTS_CHANNEL)
        if (existing != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                VOLTFLOW_ALERTS_CHANNEL,
                "Voltflow Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Payments, security, and account updates"
            }
        )
    }

    fun dispatch(title: String, body: String, type: String = "system") {
        ensureChannels()
        if (isAppForegrounded()) {
            VoltflowNotificationCenter.publish(
                VoltflowNotificationEvent(title = title, body = body, type = type)
            )
            return
        }
        showSystemNotification(title, body, type)
    }

    fun showSystemNotification(title: String, body: String, type: String = "system") {
        ensureChannels()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(appContext, VOLTFLOW_ALERTS_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_voltflow)
            .setLargeIcon(BitmapFactory.decodeResource(appContext.resources, R.drawable.ic_launcher_icon_voltflow))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(appContext).notify(Random.nextInt(), notification)
    }

    private fun isAppForegrounded(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
    }
}
