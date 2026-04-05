package com.alauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE hidden = 0 ORDER BY label ASC")
    fun observeAll(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE hidden = 0 ORDER BY label ASC")
    suspend fun getAll(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Update
    suspend fun update(app: AppEntity)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM apps WHERE packageName NOT IN (:packageNames)")
    suspend fun deleteNotIn(packageNames: List<String>)
}
