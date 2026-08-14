package com.smarthome.monitor.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a floor in the smart home, stored in Firestore.
 */
data class Floor(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val imageAsset: String = "floor_ground",   // drawable resource name
    val gridCols: Int = 12,
    val gridRows: Int = 8,
    val order: Int = 0
)
