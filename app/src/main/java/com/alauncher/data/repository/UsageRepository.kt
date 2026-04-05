package com.alauncher.data.repository

import com.alauncher.data.db.UsageDao
import com.alauncher.data.db.UsageEventEntity
import com.alauncher.data.model.GravityScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for tracking app usage and computing gravity scores.
 */
@Singleton
class UsageRepository @Inject constructor(
    private val usageDao: UsageDao,
) {
    /** Record an app launch event. */
    suspend fun recordLaunch(packageName: String) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        usageDao.insert(
            UsageEventEntity(
                packageName = packageName,
                timestamp = now,
                hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
            )
        )
    }

    /** Compute gravity scores for all apps. */
    suspend fun computeGravityScores(): Map<String, GravityScore> = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val launchCounts = usageDao.getLaunchCounts(thirtyDaysAgo)
        val maxCount = launchCounts.maxOfOrNull { it.count }?.toFloat() ?: 1f

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        launchCounts.associate { lc ->
            val events = usageDao.getForPackage(lc.packageName)
            val frequency = lc.count / maxCount

            // Recency: exponential decay, most recent launch matters most
            val recency = if (events.isNotEmpty()) {
                val mostRecent = events.first().timestamp
                val ageMs = System.currentTimeMillis() - mostRecent
                val ageHours = ageMs / (1000.0 * 60 * 60)
                (1.0 / (1.0 + ageHours / 24.0)).toFloat()
            } else 0f

            // Time-of-day: how often this app is used at this hour
            val hourMatches = events.count { it.hourOfDay == currentHour }
            val timeOfDay = if (events.isNotEmpty()) {
                hourMatches.toFloat() / events.size
            } else 0f

            val composite = frequency * 0.4f + recency * 0.4f + timeOfDay * 0.2f

            lc.packageName to GravityScore(
                packageName = lc.packageName,
                frequency = frequency,
                recency = recency,
                timeOfDayWeight = timeOfDay,
                composite = composite,
            )
        }
    }
}
