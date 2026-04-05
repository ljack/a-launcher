package com.alauncher.ui.spatial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.alauncher.data.model.LauncherApp
import com.alauncher.ui.theme.OrbGlow
import com.alauncher.ui.theme.OrbGlowSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The main spatial field composable.
 * Renders apps in a golden-angle spiral centered on the screen.
 * Includes animated connection lines between nearby apps.
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

    // Pre-compute positions for connection lines
    val goldenAngle = PI * (3.0 - sqrt(5.0))
    val orbSizePx = 220f
    val spreadFactor = orbSizePx * 0.85f

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
            val currentScale = animatedScale.value

            // Compute all positions once
            data class OrbPos(val x: Float, val y: Float, val screenX: Float, val screenY: Float, val ring: Int)

            val positions = remember(apps.size, currentScale, panOffset, fieldSize) {
                apps.mapIndexed { index, _ ->
                    val angle = index * goldenAngle
                    val radius = spreadFactor * sqrt((index + 1).toFloat())
                    val rawX = centerX + (radius * cos(angle)).toFloat()
                    val rawY = centerY + (radius * sin(angle)).toFloat()
                    val sx = (rawX - centerX) * currentScale + centerX + panOffset.x
                    val sy = (rawY - centerY) * currentScale + centerY + panOffset.y
                    OrbPos(rawX, rawY, sx, sy, ring = index / 8)
                }
            }

            // Draw connection lines between nearby apps
            Canvas(modifier = Modifier.fillMaxSize()) {
                val connectionDistance = 250f * currentScale
                val maxConnections = 80 // limit for performance

                var connectionCount = 0
                for (i in positions.indices) {
                    if (connectionCount >= maxConnections) break
                    val a = positions[i]
                    // Only draw connections for visible orbs
                    if (a.screenX < -100 || a.screenX > size.width + 100 ||
                        a.screenY < -100 || a.screenY > size.height + 100
                    ) continue

                    for (j in (i + 1) until minOf(positions.size, i + 12)) {
                        if (connectionCount >= maxConnections) break
                        val b = positions[j]
                        val dist = hypot(a.screenX - b.screenX, a.screenY - b.screenY)
                        if (dist < connectionDistance) {
                            val alpha = (1f - dist / connectionDistance) * 0.12f
                            // Breathing pulse: connections gently pulse
                            val pulseAlpha = alpha * (0.7f + 0.3f * sin(breathe * 2 * PI.toFloat() + i * 0.5f).toFloat())
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        OrbGlow.copy(alpha = pulseAlpha),
                                        OrbGlowSecondary.copy(alpha = pulseAlpha),
                                    ),
                                    start = Offset(a.screenX, a.screenY),
                                    end = Offset(b.screenX, b.screenY),
                                ),
                                start = Offset(a.screenX, a.screenY),
                                end = Offset(b.screenX, b.screenY),
                                strokeWidth = 1.5f,
                                blendMode = BlendMode.Screen,
                            )
                            connectionCount++
                        }
                    }
                }
            }

            // Draw orbs
            positions.forEachIndexed { index, pos ->
                val app = apps[index]
                val margin = orbSizePx * currentScale * 2
                if (pos.screenX > -margin && pos.screenX < fieldSize.width + margin &&
                    pos.screenY > -margin && pos.screenY < fieldSize.height + margin
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
                                translationX = pos.screenX - orbSizePx / 2f
                                translationY = pos.screenY - orbSizePx / 2f
                                scaleX = currentScale
                                scaleY = currentScale
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
