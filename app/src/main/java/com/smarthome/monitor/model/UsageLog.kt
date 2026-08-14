package com.smarthome.monitor.model

import com.google.firebase.Timestamp

/**
 * Represents a logged usage session for reporting purposes.
 */
data class UsageLog(
    val deviceId: String = "",
    val deviceName: String = "",
    val deviceType: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val durationMinutes: Long = 0L,
    val autoCutoff: Boolean = false  // true if ended by safety system
)
