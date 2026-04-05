package com.alauncher.ui.spatial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.alauncher.data.model.LauncherApp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The main spatial field composable.
 * Renders apps in a golden-angle spiral centered on the screen.
 * Supports pan and zoom gestures.
 */
@Composable
fun SpatialField(
    apps: List<LauncherApp>,
    onAppTap: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }

    // Animate scale for smooth zoom
    val animatedScale = remember { Animatable(1f) }
    LaunchedEffect(scale) {
        animatedScale.animateTo(scale, spring(stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { fieldSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.2f, 4f)
                    panOffset += pan
                }
            }
    ) {
        if (fieldSize.width > 0 && fieldSize.height > 0) {
            val centerX = fieldSize.width / 2f
            val centerY = fieldSize.height / 2f

            // Golden angle spiral: apps spread naturally without overlapping
            val goldenAngle = PI * (3.0 - sqrt(5.0)) // ~137.5 degrees
            val orbSizePx = 220f // approximate size of each orb in pixels
            // Scale factor: how far apart orbs are
            val spreadFactor = orbSizePx * 0.85f

            apps.forEachIndexed { index, app ->
                // Golden angle spiral positioning
                val angle = index * goldenAngle
                val radius = spreadFactor * sqrt((index + 1).toFloat())

                val x = centerX + (radius * cos(angle)).toFloat() - orbSizePx / 2f
                val y = centerY + (radius * sin(angle)).toFloat() - orbSizePx / 2f

                // Apply pan and zoom via graphicsLayer on each orb
                val transformedX = (x - centerX) * animatedScale.value + centerX + panOffset.x
                val transformedY = (y - centerY) * animatedScale.value + centerY + panOffset.y

                // Only render if potentially visible (with generous margin)
                val margin = orbSizePx * animatedScale.value * 2
                if (transformedX > -margin && transformedX < fieldSize.width + margin &&
                    transformedY > -margin && transformedY < fieldSize.height + margin
                ) {
                    Layout(
                        content = {
                            AppOrb(
                                app = app,
                                scale = animatedScale.value,
                                ringIndex = (index / 8),
                            )
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = transformedX
                                translationY = transformedY
                                scaleX = animatedScale.value
                                scaleY = animatedScale.value
                            }
                            .pointerInput(app.packageName) {
                                detectTapGestures(
                                    onTap = { onAppTap(app) }
                                )
                            }
                    ) { measurables, constraints ->
                        val placeables = measurables.map { it.measure(constraints) }
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeables.forEach { it.place(0, 0) }
                        }
                    }
                }
            }
        }
    }
}
