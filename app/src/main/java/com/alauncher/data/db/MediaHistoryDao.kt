package com.alauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaHistoryDao {

    /** Upsert: if same title+artist+package exists, bump play count and update timestamp */
    @Query("""
        UPDATE media_history 
        SET playCount = playCount + 1, 
            lastPlayedAt = :now,
            durationMs = COALESCE(:durationMs, durationMs),
            album = COALESCE(:album, album)
        WHERE title = :title AND artist IS :artist AND packageName = :packageName
    """)
    suspend fun incrementPlayCount(
        title: String,
        artist: String?,
        packageName: String,
        album: String?,
        durationMs: Long?,
        now: Long = System.currentTimeMillis(),
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(entity: MediaHistoryEntity)

    /** Insert or update play count */
    suspend fun logPlay(
        title: String,
        artist: String?,
        album: String?,
        packageName: String,
        durationMs: Long?,
    ) {
        val updated = incrementPlayCount(title, artist, packageName, album, durationMs)
        if (updated == 0) {
            insertNew(
                MediaHistoryEntity(
                    title = title,
                    artist = artist,
                    album = album,
                    packageName = packageName,
                    durationMs = durationMs,
                )
            )
        }
    }

    /** Most recently played */
    @Query("SELECT * FROM media_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun recentlyPlayed(limit: Int = 50): Flow<List<MediaHistoryEntity>>

    /** Most played ever */
    @Query("SELECT * FROM media_history ORDER BY playCount DESC LIMIT :limit")
    fun mostPlayed(limit: Int = 50): Flow<List<MediaHistoryEntity>>

    /** Search history */
    @Query("SELECT * FROM media_history WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY playCount DESC")
    fun search(query: String): Flow<List<MediaHistoryEntity>>

    /** Total unique tracks played */
    @Query("SELECT COUNT(*) FROM media_history")
    suspend fun totalUniqueTracks(): Int

    /** Total play count across all tracks */
    @Query("SELECT SUM(playCount) FROM media_history")
    suspend fun totalPlayCount(): Int
}
