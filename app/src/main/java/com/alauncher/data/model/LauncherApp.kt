package com.alauncher.data.model

import android.graphics.drawable.Drawable

/**
 * Domain model for an app in the spatial field.
 */
data class LauncherApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
    val gravityScore: Float = 0f,
    val position: AppPosition = AppPosition(),
    val notificationCount: Int = 0,
    val hasLayer: Boolean = false,
)

/**
 * Spatial position with physics for the force-directed layout.
 */
data class AppPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val homeX: Float = 0f,
    val homeY: Float = 0f,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val zLayer: Int = 0,
)

/**
 * Gravity score components.
 */
data class GravityScore(
    val packageName: String,
    val frequency: Float = 0f,
    val recency: Float = 0f,
    val timeOfDayWeight: Float = 0f,
    val composite: Float = 0f,
)
