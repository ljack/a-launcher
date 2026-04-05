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
import androidx.compose.foundation.gestures.calculateCentroid
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
 * Clamp pan offset so content stays visible.
 * When zoomed out (content smaller than screen), pan is locked to center.
 * When zoomed in, pan is limited so content edges remain on screen.
 */
private fun clampPan(
    pan: Offset,
    scale: Float,
    appCount: Int,
    spreadFactor: Float,
    fieldSize: IntSize,
): Offset {
    if (appCount == 0 || fieldSize.width == 0) return pan
    val contentRadius = spreadFactor * sqrt(appCount.toFloat()) * scale
    // Allow panning so any edge app can reach the screen center
    val maxPanX = contentRadius
    val maxPanY = contentRadius
    return Offset(
        x = pan.x.coerceIn(-maxPanX, maxPanX),
        y = pan.y.coerceIn(-maxPanY, maxPanY),
    )
}

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
    interactive: Boolean = true,
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    var lastTapTime by remember { mutableStateOf(0L) }

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
        val positions = apps.mapIndexed { index, _ ->
            val angle = index * goldenAngle
            val radius = spreadFactor * sqrt((index + 1).toFloat())
            OrbPos(
                rawX = (radius * cos(angle)).toFloat(),
                rawY = (radius * sin(angle)).toFloat(),
                ring = index / 8,
            )
        }
        // Center the spiral: offset all positions so the bounding box center is at (0,0)
        if (positions.isNotEmpty()) {
            val minX = positions.minOf { it.rawX }
            val maxX = positions.maxOf { it.rawX }
            val minY = positions.minOf { it.rawY }
            val maxY = positions.maxOf { it.rawY }
            val offsetX = (minX + maxX) / 2f
            val offsetY = (minY + maxY) / 2f
            positions.map { it.copy(rawX = it.rawX - offsetX, rawY = it.rawY - offsetY) }
        } else positions
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { fieldSize = it }
            .then(if (!interactive) Modifier else Modifier
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
                            // Only process zoom when 2+ fingers are actually down
                            // This prevents wild values when one finger lifts first
                            val activePointers = pointers.count { it.pressed }

                            if (activePointers >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid()

                                // Guard: ignore extreme per-frame zoom spikes
                                if (zoom in 0.5f..2.0f) {
                                    val oldScale = scale
                                    val newScale = (oldScale * zoom).coerceIn(0.2f, 5f)
                                    val scaleDelta = newScale / oldScale

                                    // Anchor zoom to pinch centroid
                                    val cx = fieldSize.width / 2f
                                    val cy = fieldSize.height / 2f
                                    panOffset = Offset(
                                        x = centroid.x - cx + (panOffset.x - centroid.x + cx) * scaleDelta + pan.x,
                                        y = centroid.y - cy + (panOffset.y - centroid.y + cy) * scaleDelta + pan.y,
                                    )

                                    scale = newScale
                                    panOffset = clampPan(panOffset, scale, rawPositions.size, spreadFactor, fieldSize)
                                } else {
                                    // Still apply pan even if zoom was extreme
                                    panOffset += pan
                                    panOffset = clampPan(panOffset, scale, rawPositions.size, spreadFactor, fieldSize)
                                }
                            }
                            // When down to 1 finger after pinch, just pan (no zoom)
                            else if (activePointers == 1) {
                                panOffset += event.calculatePan()
                            }

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
                                panOffset = clampPan(panOffset, scale, rawPositions.size, spreadFactor, fieldSize)
                                change.consume()
                            }
                        }
                    } while (pointers.any { it.pressed })

                    // Clamp pan — applied on release
                    panOffset = clampPan(panOffset, scale, rawPositions.size, spreadFactor, fieldSize)

                    // If minimal movement and single touch → it's a tap
                    if (!gestureStarted && !isMultiTouch && fieldSize.width > 0) {
                        val now = System.currentTimeMillis()

                        // Double-tap: reset to default view
                        if (now - lastTapTime < 300L) {
                            scale = 1f
                            panOffset = Offset.Zero
                            lastTapTime = 0L
                        } else {
                            lastTapTime = now

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
                                    lastTapTime = 0L // Don't treat next tap as double
                                    break
                                }
                            }
                        }
                    }
                }
            })
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
                                // Position orb center at screenPos
                                // The scale is applied around transformOrigin (center by default)
                                // so we offset by half the SCALED size
                                translationX = screenPos.x - (orbSizePx / 2f) * currentScale
                                translationY = screenPos.y - (orbSizePx / 2f) * currentScale
                                scaleX = currentScale
                                scaleY = currentScale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
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
