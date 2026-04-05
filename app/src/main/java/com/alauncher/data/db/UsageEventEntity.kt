package com.alauncher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records a single app launch event for gravity scoring.
 */
@Entity(tableName = "usage_events")
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val sessionDurationMs: Long? = null,
)
