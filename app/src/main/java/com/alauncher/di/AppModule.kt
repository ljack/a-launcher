package com.alauncher.di

import android.content.Context
import com.alauncher.data.db.ALauncherDatabase
import com.alauncher.data.db.AppDao
import com.alauncher.data.db.UsageDao
import com.alauncher.data.db.PositionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ALauncherDatabase {
        return ALauncherDatabase.create(context)
    }

    @Provides
    fun provideAppDao(db: ALauncherDatabase): AppDao = db.appDao()

    @Provides
    fun provideUsageDao(db: ALauncherDatabase): UsageDao = db.usageDao()

    @Provides
    fun providePositionDao(db: ALauncherDatabase): PositionDao = db.positionDao()
}
