package com.alauncher.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Record of a media item that was played.
 * Tracks play count, first/last played times, and source app.
 */
@Entity(
    tableName = "media_history",
    indices = [
        Index(value = ["title", "artist", "packageName"], unique = true),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["playCount"]),
    ]
)
data class MediaHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String?,
    val album: String?,
    val packageName: String,
    val durationMs: Long?,
    val playCount: Int = 1,
    val firstPlayedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = System.currentTimeMillis(),
)
