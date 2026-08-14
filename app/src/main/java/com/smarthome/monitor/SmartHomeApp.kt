package com.smarthome.monitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp

class SmartHomeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val safetyChannel = NotificationChannel(
                CHANNEL_SAFETY_ALERTS,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical safety alerts from smart home devices"
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General smart home notifications"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(listOf(safetyChannel, generalChannel))
        }
    }

    companion object {
        const val CHANNEL_SAFETY_ALERTS = "safety_alerts"
        const val CHANNEL_GENERAL = "general"
        const val HOME_ID = "home_001" // Default home ID
    }
}
