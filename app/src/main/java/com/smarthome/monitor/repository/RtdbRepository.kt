package com.smarthome.monitor.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.smarthome.monitor.SmartHomeApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Notification event from the RTDB pushed by Cloud Functions.
 */
data class SafetyAlert(
    val deviceId: String = "",
    val deviceName: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val type: String = "SAFETY_CUTOFF"
)

/**
 * Repository for Firebase Realtime Database operations.
 * RTDB is used as a fast mirror for:
 * - Safety alert events from Cloud Functions
 * - Hardware simulator device state mirroring
 */
class RtdbRepository {

    private val rtdb = FirebaseDatabase.getInstance()
    private val homeId = SmartHomeApp.HOME_ID

    /**
     * Real-time flow of safety alerts pushed by Cloud Functions.
     */
    fun getSafetyAlertsFlow(): Flow<List<SafetyAlert>> = callbackFlow {
        val ref = rtdb.getReference("homes/$homeId/alerts")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alerts = snapshot.children.mapNotNull { child ->
                    child.getValue(SafetyAlert::class.java)
                }.sortedByDescending { it.timestamp }
                trySend(alerts)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Push a local notification event to RTDB (for simulator visibility).
     */
    suspend fun pushSimulatorEvent(deviceId: String, deviceName: String, event: String) {
        val ref = rtdb.getReference("homes/$homeId/simulatorEvents").push()
        val data = mapOf(
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "event" to event,
            "timestamp" to System.currentTimeMillis()
        )
        ref.setValue(data).await()
    }

    /**
     * Push a safety cutoff alert to the RTDB alerts node.
     * Mirrors what the Cloud Function checkSafetyCutoffs would push.
     */
    suspend fun pushSafetyAlert(deviceId: String, deviceName: String, message: String) {
        val ref = rtdb.getReference("homes/$homeId/alerts").push()
        val data = mapOf(
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "type" to "SAFETY_CUTOFF"
        )
        ref.setValue(data).await()
    }

    /**
     * Clear old safety alerts (cleanup utility).
     */
    suspend fun clearAlerts() {
        rtdb.getReference("homes/$homeId/alerts").removeValue().await()
    }
}
