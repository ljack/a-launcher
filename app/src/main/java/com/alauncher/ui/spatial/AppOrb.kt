package com.alauncher.ui.spatial

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.alauncher.data.model.LauncherApp
import com.alauncher.ui.theme.OrbGlow
import com.alauncher.ui.theme.OrbGlowSecondary
import kotlin.math.PI
import kotlin.math.sin

/**
 * Visual representation of an app in the spatial field.
 * Renders as a glowing circular orb with icon and label.
 * Inner rings get larger orbs with more glow, outer rings fade out.
 */
@Composable
fun AppOrb(
    app: LauncherApp,
    scale: Float = 1f,
    ringIndex: Int = 0,
    breathePhase: Float = 0f,
    modifier: Modifier = Modifier,
) {
    // Inner apps are larger, outer apps are smaller
    val sizeMultiplier = (1f - (ringIndex * 0.03f).coerceAtMost(0.35f))
    val iconSize = (50 * sizeMultiplier).dp
    val orbSize = (58 * sizeMultiplier).dp

    // Glow color transitions from purple (inner) to teal (outer)
    val t = (ringIndex / 15f).coerceIn(0f, 1f)
    val glowColor = remember(ringIndex) {
        Color(
            red = OrbGlow.red * (1f - t) + OrbGlowSecondary.red * t,
            green = OrbGlow.green * (1f - t) + OrbGlowSecondary.green * t,
            blue = OrbGlow.blue * (1f - t) + OrbGlowSecondary.blue * t,
            alpha = 1f,
        )
    }

    // Breathing effect per orb (slightly offset per ring for wave effect)
    val breatheOffset = sin((breathePhase + ringIndex * 0.15f) * 2f * PI.toFloat()) * 0.5f + 0.5f
    val glowAlpha = (0.25f + app.gravityScore * 0.5f + breatheOffset * 0.1f).coerceIn(0.15f, 0.85f)
    val glowSize = orbSize + (16 + breatheOffset * 8).dp

    // Border glow ring
    val borderAlpha = (0.2f + app.gravityScore * 0.4f + breatheOffset * 0.1f).coerceIn(0.1f, 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(max = 84.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Outer glow
            Box(
                modifier = Modifier
                    .size(glowSize)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = glowAlpha * 0.6f),
                                    glowColor.copy(alpha = glowAlpha * 0.2f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.minDimension / 2,
                            )
                        )
                    }
            )
            // Icon container with glowing border
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(orbSize)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                glowColor.copy(alpha = borderAlpha),
                                glowColor.copy(alpha = borderAlpha * 0.3f),
                            ),
                        ),
                        shape = CircleShape,
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E1E30),
                                Color(0xFF12121E),
                            )
                        )
                    ),
            ) {
                app.icon?.let { drawable ->
                    val bitmapSize = (iconSize.value).toInt().coerceAtLeast(1)
                    Image(
                        bitmap = drawable.toBitmap(bitmapSize, bitmapSize).asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(iconSize * 0.88f),
                    )
                }
            }
        }
        // Label — hide on very outer rings or when zoomed out
        if (ringIndex < 18 && scale > 0.35f) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (10 * sizeMultiplier).sp,
                    color = glowColor.copy(alpha = 0.7f + app.gravityScore * 0.3f),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
