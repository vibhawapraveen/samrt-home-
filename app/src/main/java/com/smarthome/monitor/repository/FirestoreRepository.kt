package com.smarthome.monitor.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.smarthome.monitor.SmartHomeApp
import com.smarthome.monitor.model.Device
import com.smarthome.monitor.model.Floor
import com.smarthome.monitor.model.UsageLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository handling all Firestore read/write operations for floors and devices.
 * Uses callbackFlow to expose Firestore real-time snapshots as Kotlin Flows.
 */
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val homeId = SmartHomeApp.HOME_ID

    // ─────────────────── Floor Operations ───────────────────

    /**
     * Real-time flow of all floors ordered by their display order.
     */
    fun getFloorsFlow(): Flow<List<Floor>> = callbackFlow {
        val registration: ListenerRegistration = db
            .collection("homes/$homeId/floors")
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val floors = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Floor::class.java)
                } ?: emptyList()
                trySend(floors)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Add a new floor to the home.
     */
    suspend fun addFloor(floor: Floor): String {
        val ref = db.collection("homes/$homeId/floors").document()
        ref.set(floor.copy(id = ref.id)).await()
        return ref.id
    }

    /**
     * Delete a floor and all its devices.
     */
    suspend fun deleteFloor(floorId: String) {
        db.collection("homes/$homeId/floors").document(floorId).delete().await()
    }

    // ─────────────────── Device Operations ───────────────────

    /**
     * Real-time flow of ALL devices in the home.
     */
    fun getDevicesFlow(): Flow<List<Device>> = callbackFlow {
        val registration: ListenerRegistration = db
            .collection("homes/$homeId/devices")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val devices = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Device::class.java)
                } ?: emptyList()
                trySend(devices)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time flow of devices filtered by floor.
     */
    fun getDevicesForFloorFlow(floorId: String): Flow<List<Device>> = callbackFlow {
        val registration: ListenerRegistration = db
            .collection("homes/$homeId/devices")
            .whereEqualTo("floorId", floorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val devices = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Device::class.java)
                } ?: emptyList()
                trySend(devices)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Add a new device to the home.
     */
    suspend fun addDevice(device: Device): String {
        val ref = db.collection("homes/$homeId/devices").document()
        ref.set(device.copy(id = ref.id, lastUpdated = Timestamp.now())).await()
        return ref.id
    }

    /**
     * Toggle the main ON/OFF state of a device.
     */
    suspend fun toggleDeviceState(deviceId: String, newState: String) {
        val update = mutableMapOf<String, Any>(
            "state" to newState,
            "lastUpdated" to Timestamp.now()
        )
        // Stamp onSince when turning ON so the client-side safety cutoff can track elapsed time.
        // (Replaces the Cloud Function behaviour since functions are not deployed.)
        if (newState == "ON") {
            update["onSince"] = Timestamp.now()
        } else {
            update["onSince"] = com.google.firebase.firestore.FieldValue.delete()
        }
        db.collection("homes/$homeId/devices")
            .document(deviceId)
            .update(update)
            .await()
    }

    /**
     * Update a specific switch state within a MULTI_SWITCH device.
     */
    suspend fun updateSwitchState(deviceId: String, switchKey: String, newState: String) {
        db.collection("homes/$homeId/devices")
            .document(deviceId)
            .update(
                mapOf(
                    "switchStates.$switchKey" to newState,
                    "lastUpdated" to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Update schedule for a LIGHT device.
     */
    suspend fun updateDeviceSchedule(
        deviceId: String,
        onTime: String?,
        offTime: String?,
        enabled: Boolean
    ) {
        db.collection("homes/$homeId/devices")
            .document(deviceId)
            .update(
                mapOf(
                    "scheduleOnTime" to onTime,
                    "scheduleOffTime" to offTime,
                    "scheduleEnabled" to enabled,
                    "lastUpdated" to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Update max on duration for safety-critical devices (IRON).
     */
    suspend fun updateMaxOnDuration(deviceId: String, minutes: Int) {
        db.collection("homes/$homeId/devices")
            .document(deviceId)
            .update(
                mapOf(
                    "maxOnDurationMinutes" to minutes,
                    "lastUpdated" to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Delete a device.
     */
    suspend fun deleteDevice(deviceId: String) {
        db.collection("homes/$homeId/devices").document(deviceId).delete().await()
    }

    // ─────────────────── Usage Logs ───────────────────

    /**
     * Read a device's current onSince before turning it OFF.
     * Needed to compute session duration in the client-side cutoff path.
     */
    suspend fun getDeviceOnSince(deviceId: String): Timestamp? {
        val doc = db.collection("homes/$homeId/devices").document(deviceId).get().await()
        return doc.getTimestamp("onSince")
    }

    /**
     * Write a usage session log and increment the device's totalOnTimeMinutes.
     * Replaces what the Cloud Function onDeviceStateWrite did on device turn-OFF.
     */
    suspend fun logUsageSession(
        deviceId: String,
        deviceName: String,
        deviceType: String,
        startTime: Timestamp,
        endTime: Timestamp,
        autoCutoff: Boolean
    ) {
        val durationMinutes = Math.round((endTime.seconds - startTime.seconds) / 60.0).toLong()

        // Write usage log document
        val logRef = db.collection("homes/$homeId/usageLogs").document()
        logRef.set(
            mapOf(
                "deviceId" to deviceId,
                "deviceName" to deviceName,
                "deviceType" to deviceType,
                "startTime" to startTime,
                "endTime" to endTime,
                "durationMinutes" to durationMinutes,
                "autoCutoff" to autoCutoff
            )
        ).await()

        // Increment totalOnTimeMinutes on the device document
        db.collection("homes/$homeId/devices").document(deviceId).update(
            mapOf(
                "totalOnTimeMinutes" to com.google.firebase.firestore.FieldValue.increment(durationMinutes)
            )
        ).await()
    }

    /**
     * Real-time flow of recent usage logs (last 30 days), ordered newest-first.
     * Updates automatically whenever a new log is written.
     */
    fun getUsageLogsFlow(): Flow<List<UsageLog>> = callbackFlow {
        val thirtyDaysAgo = Timestamp(
            System.currentTimeMillis() / 1000 - (30 * 24 * 3600), 0
        )
        val registration = db.collection("homes/$homeId/usageLogs")
            .whereGreaterThan("startTime", thirtyDaysAgo)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val logs = snapshot?.documents?.mapNotNull { it.toObject(UsageLog::class.java) } ?: emptyList()
                trySend(logs)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Fetch recent usage logs for the reports screen (last 30 days).
     */
    suspend fun getRecentUsageLogs(): List<UsageLog> {
        val thirtyDaysAgo = Timestamp(
            System.currentTimeMillis() / 1000 - (30 * 24 * 3600), 0
        )
        return db.collection("homes/$homeId/usageLogs")
            .whereGreaterThan("startTime", thirtyDaysAgo)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(200)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(UsageLog::class.java) }
    }

    // ─────────────────── Seeding ───────────────────

    /**
     * Seeds the Firestore database with sample floors and devices.
     * Call only once during initial setup.
     */
    suspend fun seedDemoData() {
        val floorsRef = db.collection("homes/$homeId/floors")
        val devicesRef = db.collection("homes/$homeId/devices")

        // Check if already seeded
        val existing = floorsRef.limit(1).get().await()
        if (!existing.isEmpty) return

        // Floors
        val groundFloorRef = floorsRef.document("floor_ground")
        groundFloorRef.set(Floor(id = "floor_ground", name = "Ground Floor", imageAsset = "floor_ground", gridCols = 12, gridRows = 8, order = 0)).await()

        val firstFloorRef = floorsRef.document("floor_first")
        firstFloorRef.set(Floor(id = "floor_first", name = "First Floor", imageAsset = "floor_first", gridCols = 12, gridRows = 8, order = 1)).await()

        val basementRef = floorsRef.document("floor_basement")
        basementRef.set(Floor(id = "floor_basement", name = "Basement", imageAsset = "floor_basement", gridCols = 10, gridRows = 6, order = 2)).await()

        // Devices – Ground Floor
        val groundDevices = listOf(
            Device(name = "Living Room Main", type = "OUTLET", floorId = "floor_ground", roomName = "Living Room", gridX = 2, gridY = 3),
            Device(name = "Kitchen Appliance", type = "OUTLET", floorId = "floor_ground", roomName = "Kitchen", gridX = 8, gridY = 2),
            Device(name = "Living Room Lights", type = "LIGHT", floorId = "floor_ground", roomName = "Living Room", gridX = 3, gridY = 2, scheduleOnTime = "18:00", scheduleOffTime = "23:00", scheduleEnabled = true),
            Device(name = "Kitchen Lights", type = "LIGHT", floorId = "floor_ground", roomName = "Kitchen", gridX = 7, gridY = 3, scheduleOnTime = "06:30", scheduleOffTime = "09:00", scheduleEnabled = false),
            Device(name = "Front Door Camera", type = "CAMERA", floorId = "floor_ground", roomName = "Entrance", gridX = 1, gridY = 1, cameraStreamUrl = "rtsp://mock/cam1", cameraSnapshotUrl = "https://picsum.photos/640/360?random=1"),
            Device(name = "Living Room Gang", type = "MULTI_SWITCH", floorId = "floor_ground", roomName = "Living Room", gridX = 4, gridY = 4, switchCount = 3, switchStates = mapOf("switch_1" to "OFF", "switch_2" to "OFF", "switch_3" to "OFF")),
            Device(name = "Laundry Iron", type = "IRON", floorId = "floor_ground", roomName = "Laundry", gridX = 9, gridY = 6, maxOnDurationMinutes = 30)
        )

        // Devices – First Floor
        val firstFloorDevices = listOf(
            Device(name = "Master Bedroom Lights", type = "LIGHT", floorId = "floor_first", roomName = "Master Bedroom", gridX = 3, gridY = 3, scheduleOnTime = "22:00", scheduleOffTime = "07:00", scheduleEnabled = true),
            Device(name = "Study Room Outlet", type = "OUTLET", floorId = "floor_first", roomName = "Study", gridX = 8, gridY = 3),
            Device(name = "Bedroom Gang Switch", type = "MULTI_SWITCH", floorId = "floor_first", roomName = "Bedroom 2", gridX = 5, gridY = 2, switchCount = 2, switchStates = mapOf("switch_1" to "OFF", "switch_2" to "OFF")),
            Device(name = "Hallway Camera", type = "CAMERA", floorId = "floor_first", roomName = "Hallway", gridX = 6, gridY = 1, cameraStreamUrl = "rtsp://mock/cam2", cameraSnapshotUrl = "https://picsum.photos/640/360?random=2"),
            Device(name = "Bedroom Iron", type = "IRON", floorId = "floor_first", roomName = "Master Bedroom", gridX = 4, gridY = 5, maxOnDurationMinutes = 20)
        )

        // Devices – Basement
        val basementDevices = listOf(
            Device(name = "Server Room Outlet", type = "OUTLET", floorId = "floor_basement", roomName = "Server Room", gridX = 5, gridY = 3),
            Device(name = "Basement Camera", type = "CAMERA", floorId = "floor_basement", roomName = "Basement", gridX = 2, gridY = 2, cameraStreamUrl = "rtsp://mock/cam3", cameraSnapshotUrl = "https://picsum.photos/640/360?random=3"),
            Device(name = "Basement Lights", type = "LIGHT", floorId = "floor_basement", roomName = "Basement", gridX = 4, gridY = 4, scheduleEnabled = false)
        )

        val allDevices = groundDevices + firstFloorDevices + basementDevices
        allDevices.forEach { device ->
            val ref = devicesRef.document()
            ref.set(device.copy(id = ref.id, lastUpdated = Timestamp.now())).await()
        }
    }
}
