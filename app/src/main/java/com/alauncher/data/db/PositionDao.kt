package com.alauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Query("SELECT * FROM app_positions")
    fun observeAll(): Flow<List<AppPositionEntity>>

    @Query("SELECT * FROM app_positions")
    suspend fun getAll(): List<AppPositionEntity>

    @Query("SELECT * FROM app_positions WHERE packageName = :packageName")
    suspend fun getForPackage(packageName: String): AppPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(position: AppPositionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(positions: List<AppPositionEntity>)

    @Query("DELETE FROM app_positions WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
