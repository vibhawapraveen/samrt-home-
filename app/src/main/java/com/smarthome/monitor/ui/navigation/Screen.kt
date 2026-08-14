package com.smarthome.monitor.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Floor : Screen("floor/{floorId}") {
        fun createRoute(floorId: String) = "floor/$floorId"
    }
    object Camera : Screen("camera/{deviceId}") {
        fun createRoute(deviceId: String) = "camera/$deviceId"
    }
    object Reports : Screen("reports")
    object Settings : Screen("settings")
}
