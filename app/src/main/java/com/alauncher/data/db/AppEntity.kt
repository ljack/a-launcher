package com.alauncher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted record of an installed app.
 */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val activityName: String,
    val label: String,
    val firstSeen: Long = System.currentTimeMillis(),
    val hidden: Boolean = false,
)
