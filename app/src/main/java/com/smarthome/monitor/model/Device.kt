package com.smarthome.monitor.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Represents a smart home device stored in Firestore.
 * Supports: OUTLET, MULTI_SWITCH, IRON, LIGHT, CAMERA device types.
 */
data class Device(
    @DocumentId
    val id: String = "",

    val name: String = "",
    val type: String = DeviceType.OUTLET.name,
    val floorId: String = "",
    val roomName: String = "",

    // Grid position on the floor plan
    val gridX: Int = 0,
    val gridY: Int = 0,

    // Current state: ON, OFF, ERROR, DISCONNECTED
    val state: String = DeviceState.OFF.name,

    // IRON-specific: max permissible ON duration in minutes
    val maxOnDurationMinutes: Int = 30,

    // Timestamp when device was turned ON (for IRON cutoff calculation)
    val onSince: Timestamp? = null,

    // LIGHT-specific scheduling (24h format "HH:mm")
    val scheduleOnTime: String? = null,
    val scheduleOffTime: String? = null,
    val scheduleEnabled: Boolean = false,

    // MULTI_SWITCH-specific: number of switches
    val switchCount: Int = 1,

    // MULTI_SWITCH switch states: "switch_1" -> "ON"/"OFF"
    val switchStates: Map<String, String> = emptyMap(),

    // CAMERA-specific
    val cameraStreamUrl: String? = null,
    val cameraSnapshotUrl: String? = null,

    // Metadata
    val lastUpdated: Timestamp? = null,
    val totalOnTimeMinutes: Long = 0L
)
