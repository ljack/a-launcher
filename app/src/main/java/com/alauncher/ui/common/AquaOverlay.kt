package com.alauncher.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alauncher.ui.theme.OrbGlowSecondary
import kotlin.math.PI
import kotlin.math.sin

/**
 * Liquid glass tint colors.
 */
val GlassTint = Color(0xFF122838)      // Dark teal glass base
val GlassHighlight = Color(0xFFA0D8E8) // Bright refraction highlight
val GlassEdge = Color(0xFF70E0F0)      // Bright edge glow

/**
 * Draws an aqua/underwater overlay with a wavy edge.
 * Content behind appears tinted and blurred as if underwater.
 *
 * @param waveEdge Which edge has the wave (Top or Bottom)
 * @param waveAmplitude Height of the wave ripple
 * @param alpha Overall opacity of the aqua tint
 */
@Composable
fun AquaZone(
    modifier: Modifier = Modifier,
    waveEdge: WaveEdge = WaveEdge.Bottom,
    waveAmplitude: Dp = 12.dp,
    alpha: Float = 0.75f,
    content: @Composable () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aquaWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val ampPx = waveAmplitude.toPx()
                drawAquaBackground(
                    waveEdge = waveEdge,
                    wavePhase = wavePhase,
                    waveAmplitude = ampPx,
                    alpha = alpha,
                )
            }
    ) {
        content()
    }
}

enum class WaveEdge { Top, Bottom }

private fun DrawScope.drawAquaBackground(
    waveEdge: WaveEdge,
    wavePhase: Float,
    waveAmplitude: Float,
    alpha: Float,
) {
    val w = size.width
    val h = size.height

    val path = Path()

    when (waveEdge) {
        WaveEdge.Bottom -> {
            // Solid at top, wavy edge at bottom
            path.moveTo(0f, 0f)
            path.lineTo(w, 0f)
            path.lineTo(w, h - waveAmplitude)

            // Draw wave along bottom edge
            val steps = 60
            for (i in steps downTo 0) {
                val x = w * i / steps
                val wave = sin(wavePhase + x / w * 3 * PI.toFloat()) * waveAmplitude
                path.lineTo(x, h - waveAmplitude + wave)
            }
            path.close()
        }
        WaveEdge.Top -> {
            // Wavy edge at top, solid at bottom
            path.moveTo(0f, waveAmplitude)

            // Draw wave along top edge
            val steps = 60
            for (i in 0..steps) {
                val x = w * i / steps
                val wave = sin(wavePhase + x / w * 3 * PI.toFloat()) * waveAmplitude
                path.lineTo(x, waveAmplitude + wave)
            }

            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
        }
    }

    // Layer 1: Glass tint base
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = when (waveEdge) {
                WaveEdge.Bottom -> listOf(
                    GlassTint.copy(alpha = alpha * 0.85f),
                    GlassTint.copy(alpha = alpha * 0.6f),
                    GlassTint.copy(alpha = alpha * 0.15f),
                )
                WaveEdge.Top -> listOf(
                    GlassTint.copy(alpha = alpha * 0.15f),
                    GlassTint.copy(alpha = alpha * 0.6f),
                    GlassTint.copy(alpha = alpha * 0.85f),
                )
            }
        ),
    )
    // Layer 2: Glass highlight — bright refraction at the solid edge
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = when (waveEdge) {
                WaveEdge.Bottom -> listOf(
                    GlassHighlight.copy(alpha = alpha * 0.15f),
                    GlassHighlight.copy(alpha = alpha * 0.04f),
                    Color.Transparent,
                )
                WaveEdge.Top -> listOf(
                    Color.Transparent,
                    GlassHighlight.copy(alpha = alpha * 0.04f),
                    GlassHighlight.copy(alpha = alpha * 0.15f),
                )
            }
        ),
    )

    // Subtle bright edge along the wave (caustic-like highlight)
    val edgePath = Path()
    val steps = 60
    when (waveEdge) {
        WaveEdge.Bottom -> {
            for (i in 0..steps) {
                val x = w * i / steps
                val wave = sin(wavePhase + x / w * 3 * PI.toFloat()) * waveAmplitude
                val y = h - waveAmplitude + wave
                if (i == 0) edgePath.moveTo(x, y) else edgePath.lineTo(x, y)
            }
        }
        WaveEdge.Top -> {
            for (i in 0..steps) {
                val x = w * i / steps
                val wave = sin(wavePhase + x / w * 3 * PI.toFloat()) * waveAmplitude
                val y = waveAmplitude + wave
                if (i == 0) edgePath.moveTo(x, y) else edgePath.lineTo(x, y)
            }
        }
    }

    drawPath(
        path = edgePath,
        color = GlassEdge.copy(alpha = 0.6f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
    )
}
