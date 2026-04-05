package com.alauncher.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppEntity::class,
        UsageEventEntity::class,
        AppPositionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ALauncherDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun usageDao(): UsageDao
    abstract fun positionDao(): PositionDao

    companion object {
        fun create(context: Context): ALauncherDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ALauncherDatabase::class.java,
                "alauncher.db"
            ).build()
        }
    }
}
