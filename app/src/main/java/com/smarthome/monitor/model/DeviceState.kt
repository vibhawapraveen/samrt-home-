package com.smarthome.monitor.model

import androidx.compose.ui.graphics.Color

/**
 * Represents the operational state of a smart home device.
 */
enum class DeviceState(val displayName: String) {
    ON("On"),
    OFF("Off"),
    ERROR("Error"),
    DISCONNECTED("Disconnected");

    companion object {
        fun fromString(value: String): DeviceState {
            return entries.find { it.name == value } ?: DISCONNECTED
        }
    }
}
