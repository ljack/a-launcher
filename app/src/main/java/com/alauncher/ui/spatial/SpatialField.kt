package com.alauncher.ui.spatial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.alauncher.data.model.LauncherApp
import com.alauncher.ui.theme.OrbGlow
import com.alauncher.ui.theme.OrbGlowSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computed position for an app orb in the spatial field.
 */
private data class OrbPos(
    val rawX: Float,
    val rawY: Float,
    val ring: Int,
)

/**
 * The main spatial field composable.
 * Renders apps in a golden-angle spiral centered on the screen.
 * Includes animated connection lines between nearby apps.
 *
 * All gestures (pan, zoom, tap) handled in one unified pointer input
 * to avoid conflicts between parent transform and child tap handlers.
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

    // Subtle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathe",
    )

    // Pre-compute raw positions (before pan/zoom transform)
    val goldenAngle = PI * (3.0 - sqrt(5.0))
    val orbSizePx = 220f
    val spreadFactor = orbSizePx * 0.85f

    val rawPositions = remember(apps.size) {
        apps.mapIndexed { index, _ ->
            val angle = index * goldenAngle
            val radius = spreadFactor * sqrt((index + 1).toFloat())
            OrbPos(
                rawX = (radius * cos(angle)).toFloat(),
                rawY = (radius * sin(angle)).toFloat(),
                ring = index / 8,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { fieldSize = it }
            .pointerInput(apps) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val downPos = firstDown.position
                    var totalPan = Offset.Zero
                    var totalZoom = 1f
                    var gestureStarted = false
                    var isMultiTouch = false

                    // Track movement to distinguish tap from drag
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes

                        if (pointers.size >= 2) {
                            isMultiTouch = true
                        }

                        if (isMultiTouch) {
                            // Pinch-to-zoom + pan
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            val newScale = (scale * zoom).coerceIn(0.15f, 5f)
                            scale = newScale
                            panOffset += pan

                            gestureStarted = true
                            pointers.forEach { it.consume() }
                        } else if (pointers.size == 1) {
                            val change = pointers[0]
                            val dragDelta = change.position - downPos
                            totalPan += event.calculatePan()

                            // Start panning after small threshold
                            if (abs(dragDelta.x) > 15f || abs(dragDelta.y) > 15f) {
                                gestureStarted = true
                            }

                            if (gestureStarted) {
                                panOffset += event.calculatePan()
                                change.consume()
                            }
                        }
                    } while (pointers.any { it.pressed })

                    // If minimal movement and single touch → it's a tap
                    if (!gestureStarted && !isMultiTouch && fieldSize.width > 0) {
                        // Hit-test: find which app was tapped
                        val centerX = fieldSize.width / 2f
                        val centerY = fieldSize.height / 2f
                        val tapX = downPos.x
                        val tapY = downPos.y

                        val hitRadius = orbSizePx * scale * 0.4f

                        for (i in rawPositions.indices) {
                            val pos = rawPositions[i]
                            val sx = (pos.rawX) * scale + centerX + panOffset.x
                            val sy = (pos.rawY) * scale + centerY + panOffset.y
                            val dist = hypot(tapX - sx, tapY - sy)
                            if (dist < hitRadius && i < apps.size) {
                                onAppTap(apps[i])
                                break
                            }
                        }
                    }
                }
            }
    ) {
        if (fieldSize.width > 0 && fieldSize.height > 0) {
            val centerX = fieldSize.width / 2f
            val centerY = fieldSize.height / 2f
            val currentScale = scale

            // Compute screen positions
            val screenPositions = rawPositions.map { pos ->
                Offset(
                    x = pos.rawX * currentScale + centerX + panOffset.x,
                    y = pos.rawY * currentScale + centerY + panOffset.y,
                )
            }

            // Draw connection lines between nearby apps
            Canvas(modifier = Modifier.fillMaxSize()) {
                val connectionDistance = 250f * currentScale
                val maxConnections = 80

                var connectionCount = 0
                for (i in screenPositions.indices) {
                    if (connectionCount >= maxConnections) break
                    val a = screenPositions[i]
                    if (a.x < -100 || a.x > size.width + 100 ||
                        a.y < -100 || a.y > size.height + 100
                    ) continue

                    for (j in (i + 1) until minOf(screenPositions.size, i + 12)) {
                        if (connectionCount >= maxConnections) break
                        val b = screenPositions[j]
                        val dist = hypot(a.x - b.x, a.y - b.y)
                        if (dist < connectionDistance) {
                            val alpha = (1f - dist / connectionDistance) * 0.12f
                            val pulseAlpha = alpha * (0.7f + 0.3f * sin(breathe * 2 * PI.toFloat() + i * 0.5f))
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        OrbGlow.copy(alpha = pulseAlpha),
                                        OrbGlowSecondary.copy(alpha = pulseAlpha),
                                    ),
                                    start = a,
                                    end = b,
                                ),
                                start = a,
                                end = b,
                                strokeWidth = 1.5f,
                                blendMode = BlendMode.Screen,
                            )
                            connectionCount++
                        }
                    }
                }
            }

            // Draw orbs
            rawPositions.forEachIndexed { index, pos ->
                if (index >= apps.size) return@forEachIndexed
                val app = apps[index]
                val screenPos = screenPositions[index]
                val margin = orbSizePx * currentScale * 2

                if (screenPos.x > -margin && screenPos.x < fieldSize.width + margin &&
                    screenPos.y > -margin && screenPos.y < fieldSize.height + margin
                ) {
                    Layout(
                        content = {
                            AppOrb(
                                app = app,
                                scale = currentScale,
                                ringIndex = pos.ring,
                                breathePhase = breathe,
                            )
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = screenPos.x - orbSizePx / 2f
                                translationY = screenPos.y - orbSizePx / 2f
                                scaleX = currentScale
                                scaleY = currentScale
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
