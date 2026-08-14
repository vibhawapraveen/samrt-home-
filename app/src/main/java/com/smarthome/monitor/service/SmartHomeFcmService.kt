package com.smarthome.monitor.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smarthome.monitor.MainActivity
import com.smarthome.monitor.R
import com.smarthome.monitor.SmartHomeApp

/**
 * Firebase Cloud Messaging service that handles push notifications
 * from Cloud Functions (e.g., safety cutoff alerts).
 */
class SmartHomeFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Smart Home Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "A device event occurred"

        val channelId = if (remoteMessage.data["type"] == "SAFETY_CUTOFF") {
            SmartHomeApp.CHANNEL_SAFETY_ALERTS
        } else {
            SmartHomeApp.CHANNEL_GENERAL
        }

        showNotification(title, body, channelId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Send token to your backend if needed for targeted notifications
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
