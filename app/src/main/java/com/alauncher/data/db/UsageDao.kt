package com.alauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(event: UsageEventEntity)

    @Query("SELECT * FROM usage_events WHERE packageName = :packageName ORDER BY timestamp DESC")
    suspend fun getForPackage(packageName: String): List<UsageEventEntity>

    @Query("SELECT * FROM usage_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<UsageEventEntity>

    @Query("""
        SELECT packageName, COUNT(*) as count 
        FROM usage_events 
        WHERE timestamp > :since 
        GROUP BY packageName 
        ORDER BY count DESC
    """)
    suspend fun getLaunchCounts(since: Long): List<LaunchCount>

    @Query("DELETE FROM usage_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

data class LaunchCount(
    val packageName: String,
    val count: Int,
)
