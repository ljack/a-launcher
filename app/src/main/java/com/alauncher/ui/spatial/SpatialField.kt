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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.alauncher.data.model.LauncherApp
import com.alauncher.ui.common.MagnifyState
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
    return Offset(
        x = pan.x.coerceIn(-contentRadius, contentRadius),
        y = pan.y.coerceIn(-contentRadius, contentRadius),
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
 *
 * PERFORMANCE:
 * - ONE graphicsLayer on the field container handles all pan/zoom
 * - Pan/zoom changes only update the GPU transform matrix, no recomposition
 * - Orbs positioned at fixed raw coordinates, only recompose on app data change
 * - Connection lines drawn in Canvas (draw-scope only, no composition)
 * - Visibility culling skips off-screen orbs
 */
@Composable
fun SpatialField(
    apps: List<LauncherApp>,
    onAppTap: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    magnifyState: MagnifyState? = null,
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    var lastTapTime by remember { mutableStateOf(0L) }

    // Breathing animation — only consumed in graphicsLayer (GPU-only, no recomposition)
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathe",
    )

    val goldenAngle = PI * (3.0 - sqrt(5.0))
    val orbSizePx = 220f
    val spreadFactor = orbSizePx * 0.85f

    // Pre-compute raw positions — only changes when app count changes
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
            .then(
                if (!interactive) Modifier
                else Modifier.pointerInput(apps, magnifyState) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val downPos = firstDown.position
                        val downTime = System.currentTimeMillis()
                        var gestureStarted = false
                        var isMultiTouch = false
                        var isMagnifyDrag = false
                        var magnifyActivated = false

                        // Check if tap is inside an active (non-dragging) magnify lens
                        val mag = magnifyState
                        val tappedInsideLens = mag != null && mag.active && !mag.dragging &&
                            hypot(downPos.x - mag.center.x, downPos.y - mag.center.y) < mag.radiusPx

                        if (tappedInsideLens && mag != null) {
                            // Re-grab the lens to move it
                            mag.dragging = true
                            isMagnifyDrag = true
                            magnifyActivated = true
                        }

                        // Check if tap is outside an active lens → dismiss
                        val tappedOutsideLens = mag != null && mag.active && !mag.dragging &&
                            hypot(downPos.x - mag.center.x, downPos.y - mag.center.y) >= mag.radiusPx

                        do {
                            val event = awaitPointerEvent()
                            val pointers = event.changes
                            val elapsed = System.currentTimeMillis() - downTime

                            if (pointers.size >= 2) isMultiTouch = true

                            if (isMultiTouch) {
                                val activePointers = pointers.count { it.pressed }
                                if (activePointers >= 2) {
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()

                                    // Check if pinch centroid is inside the magnify lens
                                    val centroid = event.calculateCentroid()
                                    val pinchInsideLens = mag != null && mag.active &&
                                        mag.isInsideLens(centroid)

                                    if (pinchInsideLens && mag != null && zoom in 0.5f..2.0f) {
                                        // Pinch inside lens: adjust magnification + size
                                        mag.adjustWithPinch(zoom)
                                        gestureStarted = true
                                        magnifyActivated = true
                                        pointers.forEach { it.consume() }
                                    } else if (zoom in 0.5f..2.0f) {
                                        val oldScale = scale
                                        val newScale = (oldScale * zoom).coerceIn(0.2f, 5f)
                                        val scaleDelta = newScale / oldScale
                                        val cx = fieldSize.width / 2f
                                        val cy = fieldSize.height / 2f
                                        panOffset = Offset(
                                            x = centroid.x - cx + (panOffset.x - centroid.x + cx) * scaleDelta + pan.x,
                                            y = centroid.y - cy + (panOffset.y - centroid.y + cy) * scaleDelta + pan.y,
                                        )
                                        scale = newScale
                                        panOffset = clampPan(panOffset, scale, rawPositions.size, spreadFactor, fieldSize)
                                        gestureStarted = true
                                        pointers.forEach { it.consume() }
                                    } else {
                                        panOffset += pan
                                        panOffset = clampPan(panOffset, scale, rawPositions.size, spreadFactor, fieldSize)
                                        gestureStarted = true
                                        pointers.forEach { it.consume() }
                                    }
                                } else if (activePointers == 1) {
                                    panOffset += event.calculatePan()
                                    gestureStarted = true
                                    pointers.forEach { it.consume() }
                                }
                            } else if (pointers.size == 1) {
                                val change = pointers[0]
                                val dragDelta = change.position - downPos
                                val totalDrag = hypot(dragDelta.x, dragDelta.y)

                                // Long-press detection: >300ms with <15px movement
                                if (!magnifyActivated && !gestureStarted &&
                                    mag != null && elapsed > 300 && totalDrag < 15f
                                ) {
                                    mag.activate(change.position)
                                    magnifyActivated = true
                                    isMagnifyDrag = true
                                    change.consume()
                                }

                                if (isMagnifyDrag && mag != null) {
                                    // Dragging the magnify lens
                                    mag.moveTo(change.position)
                                    change.consume()
                                } else if (!magnifyActivated) {
                                    // Normal pan
                                    if (totalDrag > 15f) {
                                        gestureStarted = true
                                    }
                                    if (gestureStarted) {
                                        panOffset += event.calculatePan()
                                        panOffset = clampPan(panOffset, scale, rawPositions.size, spreadFactor, fieldSize)
                                        change.consume()
                                    }
                                }
                            }
                        } while (pointers.any { it.pressed })

                        panOffset = clampPan(panOffset, scale, rawPositions.size, spreadFactor, fieldSize)

                        // Release magnify drag (lens stays visible)
                        if (isMagnifyDrag && mag != null) {
                            mag.release()
                            return@awaitEachGesture
                        }

                        // Tap outside lens → dismiss
                        if (tappedOutsideLens && !gestureStarted && mag != null) {
                            mag.dismiss()
                            return@awaitEachGesture
                        }

                        // Tap detection (normal)
                        if (!gestureStarted && !isMultiTouch && !magnifyActivated && fieldSize.width > 0) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 300L) {
                                scale = 1f
                                panOffset = Offset.Zero
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                val centerX = fieldSize.width / 2f
                                val centerY = fieldSize.height / 2f
                                val hitRadius = orbSizePx * scale * 0.4f
                                for (i in rawPositions.indices) {
                                    val pos = rawPositions[i]
                                    val sx = pos.rawX * scale + centerX + panOffset.x
                                    val sy = pos.rawY * scale + centerY + panOffset.y
                                    val dist = hypot(downPos.x - sx, downPos.y - sy)
                                    if (dist < hitRadius && i < apps.size) {
                                        onAppTap(apps[i])
                                        lastTapTime = 0L
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            )
    ) {
        if (fieldSize.width > 0 && fieldSize.height > 0) {
            val centerX = fieldSize.width / 2f
            val centerY = fieldSize.height / 2f
            val currentScale = scale
            val currentPan = panOffset

            // Glow halos + connection lines — single Canvas pass, no composition
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glowRadius = 55f * currentScale
                val currentBreathe = breathe

                // Pass 1: Glow halos behind everything
                for (i in rawPositions.indices) {
                    if (i >= apps.size) break
                    val pos = rawPositions[i]
                    val sx = pos.rawX * currentScale + centerX + currentPan.x
                    val sy = pos.rawY * currentScale + centerY + currentPan.y
                    // Cull off-screen
                    if (sx < -glowRadius * 2 || sx > size.width + glowRadius * 2 ||
                        sy < -glowRadius * 2 || sy > size.height + glowRadius * 2
                    ) continue

                    // Glow color: purple (inner) → teal (outer)
                    val t = (pos.ring / 15f).coerceIn(0f, 1f)
                    val glowColor = Color(
                        red = OrbGlow.red * (1f - t) + OrbGlowSecondary.red * t,
                        green = OrbGlow.green * (1f - t) + OrbGlowSecondary.green * t,
                        blue = OrbGlow.blue * (1f - t) + OrbGlowSecondary.blue * t,
                    )

                    // Breathing alpha per orb
                    val phase = currentBreathe + pos.ring * 0.3f
                    val pulse = sin(phase) * 0.5f + 0.5f
                    val gravity = apps[i].gravityScore
                    val glowAlpha = (0.35f + gravity * 0.35f + pulse * 0.15f).coerceIn(0.25f, 0.75f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = glowAlpha),
                                glowColor.copy(alpha = glowAlpha * 0.3f),
                                Color.Transparent,
                            ),
                            center = Offset(sx, sy),
                            radius = glowRadius,
                        ),
                        radius = glowRadius,
                        center = Offset(sx, sy),
                        blendMode = BlendMode.Screen,
                    )
                }

                // Pass 2: Connection lines
                val connectionDistance = 250f * currentScale
                val maxConnections = 60
                var connectionCount = 0
                for (i in rawPositions.indices) {
                    if (connectionCount >= maxConnections) break
                    val posA = rawPositions[i]
                    val ax = posA.rawX * currentScale + centerX + currentPan.x
                    val ay = posA.rawY * currentScale + centerY + currentPan.y
                    if (ax < -100 || ax > size.width + 100 ||
                        ay < -100 || ay > size.height + 100
                    ) continue

                    for (j in (i + 1) until minOf(rawPositions.size, i + 12)) {
                        if (connectionCount >= maxConnections) break
                        val posB = rawPositions[j]
                        val bx = posB.rawX * currentScale + centerX + currentPan.x
                        val by = posB.rawY * currentScale + centerY + currentPan.y
                        val dist = hypot(ax - bx, ay - by)
                        if (dist < connectionDistance) {
                            val alpha = (1f - dist / connectionDistance) * 0.12f
                            val a = Offset(ax, ay)
                            val b = Offset(bx, by)
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        OrbGlow.copy(alpha = alpha),
                                        OrbGlowSecondary.copy(alpha = alpha),
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

            // Orbs — each positioned with fixed offset, only recompose on app data change
            rawPositions.forEachIndexed { index, pos ->
                if (index >= apps.size) return@forEachIndexed
                val app = apps[index]

                // Visibility culling
                val sx = pos.rawX * currentScale + centerX + currentPan.x
                val sy = pos.rawY * currentScale + centerY + currentPan.y
                val margin = orbSizePx * currentScale
                if (sx < -margin || sx > fieldSize.width + margin ||
                    sy < -margin || sy > fieldSize.height + margin
                ) return@forEachIndexed

                // Breathing: per-orb phase offset creates a wave across the spiral
                val ringIdx = pos.ring
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = sx - (orbSizePx / 2f) * currentScale
                            translationY = sy - (orbSizePx / 2f) * currentScale
                            scaleX = currentScale
                            scaleY = currentScale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)

                            // GPU-only breathing: subtle alpha + scale pulse
                            val phase = breathe + ringIdx * 0.3f
                            val pulse = sin(phase) * 0.5f + 0.5f // 0..1
                            alpha = 0.75f + pulse * 0.25f         // 0.75..1.0
                            val scalePulse = 1f + pulse * 0.03f   // 1.0..1.03
                            scaleX *= scalePulse
                            scaleY *= scalePulse
                        }
                ) {
                    // If orb is inside the magnify lens, force labels visible
                    val effectiveScale = if (magnifyState != null && magnifyState.active &&
                        magnifyState.isInsideLens(Offset(sx, sy))
                    ) {
                        1f // Always show labels inside magnifier
                    } else {
                        currentScale
                    }

                    AppOrb(
                        app = app,
                        scale = effectiveScale,
                        ringIndex = pos.ring,
                    )
                }
            }
        }
    }
}
