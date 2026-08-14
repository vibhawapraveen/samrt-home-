package com.smarthome.monitor.model

/**
 * Enumeration of all supported device types in the Smart Home system.
 */
enum class DeviceType(val displayName: String, val description: String) {
    OUTLET("Power Outlet", "Single binary power outlet"),
    MULTI_SWITCH("Multi-Switch Unit", "Gang-box with multiple individually addressable switches"),
    IRON("Clothing Iron", "Safety-critical high-power appliance with auto-cutoff"),
    LIGHT("Smart Light", "Light bulb with optional on/off scheduling"),
    CAMERA("Security Camera", "Live monitoring camera with snapshot capability")
}
