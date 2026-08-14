package com.smarthome.monitor.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object CustomIcons {
    val Switch: ImageVector
        get() = ImageVector.Builder(
            name = "switch", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 960f, viewportHeight = 960f
        ).apply {
            addGroup(name = "group", translationY = 960f)
            addPath(
                pathData = addPathNodes("M240-80q-33 0-56.5-23.5T160-160v-640q0-33 23.5-56.5T240-880h480q33 0 56.5 23.5T800-800v640q0 33-23.5 56.5T720-80H240Zm0-80h480v-640H240v640Zm120-120h240q17 0 28.5-11.5T640-320v-320q0-17-11.5-28.5T600-680H360q-17 0-28.5 11.5T320-640v320q0 17 11.5 28.5T360-280Zm40-80v-160h160v160H400Zm80-350q13 0 21.5-8.5T510-740q0-13-8.5-21.5T480-770q-13 0-21.5 8.5T450-740q0 13 8.5 21.5T480-710Zm0 520q13 0 21.5-8.5T510-220q0-13-8.5-21.5T480-250q-13 0-21.5 8.5T450-220q0 13 8.5 21.5T480-190Zm0-290Z"),
                fill = SolidColor(Color.White)
            )
            clearGroup()
        }.build()

    val Iron: ImageVector
        get() = ImageVector.Builder(
            name = "iron", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 960f, viewportHeight = 960f
        ).apply {
            addGroup(name = "group", translationY = 960f)
            addPath(
                pathData = addPathNodes("M160-320h440v-120H240q-33 0-56.5 23.5T160-360v40Zm440 0v-120 120Zm240-400q17 0 28.5 11.5T880-680q0 17-11.5 28.5T840-640t-28.5 11.5Q800-617 800-600v160q0 50-35 85t-85 35v40q0 17-11.5 28.5T640-240H120q-17 0-28.5-11.5T80-280v-80q0-66 47-113t113-47h360v-40q0-17-11.5-28.5T560-600H400q-8 0-15.5 3.5T372-588q-5 5-12.5 8t-15.5 3q-17 0-28.5-11.5T304-617q0-8 3-15.5t8-12.5q17-17 38.5-26t46.5-9h160q50 0 85 35t35 85v160q17 0 28.5-11.5T720-440v-160q0-50 35-85t85-35Z"),
                fill = SolidColor(Color.White)
            )
            clearGroup()
        }.build()

    val Camera: ImageVector
        get() = ImageVector.Builder(
            name = "nest_cam_indoor", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 960f, viewportHeight = 960f
        ).apply {
            addGroup(name = "group", translationY = 960f)
            addPath(
                pathData = addPathNodes("M423.5-543.5Q400-567 400-600t23.5-56.5Q447-680 480-680t56.5 23.5Q560-633 560-600t-23.5 56.5Q513-520 480-520t-56.5-23.5ZM280-80q0-33 23.5-56.5T360-160h80q0-63-30.5-117T326-366q-60-39-93-101t-33-133q0-117 81.5-198.5T480-880q117 0 198.5 81.5T760-600q0 72-34 134t-94 101q-53 34-82.5 88T520-160h80q33 0 56.5 23.5T680-80H280Zm341.5-378.5Q680-517 680-600t-58.5-141.5Q563-800 480-800t-141.5 58.5Q280-683 280-600t58.5 141.5Q397-400 480-400t141.5-58.5Z"),
                fill = SolidColor(Color.White)
            )
            clearGroup()
        }.build()

    val HomeLogo: ImageVector
        get() = ImageVector.Builder(
            name = "home_iot_device", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 960f, viewportHeight = 960f
        ).apply {
            addGroup(name = "group", translationY = 960f)
            addPath(
                pathData = addPathNodes("M221-240q42 0 71-29t29-71q0-24-11-47t-33-37l-36-24v-252q0-9-5.5-14.5T221-720q-9 0-14.5 5.5T201-700v252l-36 24q-22 15-33 37t-11 47q0 42 29 71t71 29Zm0 80q-75 0-127.5-52T41-340q0-48 22-86t58-62v-212q0-42 29-71t71-29q42 0 71 29t29 71v212q36 24 58 62t22 86q0 75-52.5 127.5T221-160Zm327-200q-48-33-78-85t-30-115q0-100 70-170t170-70q100 0 170 70t70 170q0 63-30 115t-78 85H548Zm28-80h212q27-24 39.5-54t12.5-66q0-66-47-113t-113-47q-66 0-113 47t-47 113q0 36 14.5 66t41.5 54Zm104 280q-24 0-42-18t-18-42h120q0 24-18 42t-42 18Zm-80-80q-17 0-28.5-11.5T560-280q0-17 11.5-28.5T600-320h160q17 0 28.5 11.5T800-280q0 17-11.5 28.5T760-240H600ZM221-340Zm459-240Z"),
                fill = SolidColor(Color.White)
            )
            clearGroup()
        }.build()
}
