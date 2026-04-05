package com.alauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.alauncher.data.db.AppDao
import com.alauncher.data.db.AppEntity
import com.alauncher.data.model.LauncherApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for discovering and managing installed apps.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
) {
    private val pm: PackageManager = context.packageManager

    /** Observe all visible apps as domain models. */
    fun observeApps(): Flow<List<LauncherApp>> {
        return appDao.observeAll().map { entities ->
            entities.mapNotNull { entity -> entityToLauncherApp(entity) }
        }
    }

    /** Refresh the app database from PackageManager. */
    suspend fun refreshApps() = withContext(Dispatchers.IO) {
        val launchableApps = queryLaunchableApps()
        val entities = launchableApps.map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            AppEntity(
                packageName = activityInfo.packageName,
                activityName = activityInfo.name,
                label = resolveInfo.loadLabel(pm).toString(),
            )
        }
        appDao.insertAll(entities)
        // Remove uninstalled apps
        val currentPackages = entities.map { it.packageName }
        appDao.deleteNotIn(currentPackages)
    }

    /** Handle app install. */
    suspend fun onAppAdded(packageName: String) = withContext(Dispatchers.IO) {
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return@withContext
        val activityInfo = launchIntent.resolveActivityInfo(pm, 0) ?: return@withContext
        val label = activityInfo.loadLabel(pm).toString()
        appDao.insertAll(listOf(
            AppEntity(
                packageName = packageName,
                activityName = activityInfo.name,
                label = label,
            )
        ))
    }

    /** Handle app uninstall. */
    suspend fun onAppRemoved(packageName: String) {
        appDao.deleteByPackage(packageName)
    }

    /** Get launch intent for an app. */
    fun getLaunchIntent(packageName: String): Intent? {
        return pm.getLaunchIntentForPackage(packageName)
    }

    private fun queryLaunchableApps(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }

    private fun entityToLauncherApp(entity: AppEntity): LauncherApp? {
        val icon = try {
            pm.getApplicationIcon(entity.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return LauncherApp(
            packageName = entity.packageName,
            activityName = entity.activityName,
            label = entity.label,
            icon = icon,
        )
    }
}
