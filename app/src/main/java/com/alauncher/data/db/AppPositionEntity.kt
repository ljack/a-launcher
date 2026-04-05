package com.alauncher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted spatial position for an app in the spatial field.
 */
@Entity(tableName = "app_positions")
data class AppPositionEntity(
    @PrimaryKey val packageName: String,
    val x: Float,
    val y: Float,
    val homeX: Float,
    val homeY: Float,
    val zLayer: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
)
