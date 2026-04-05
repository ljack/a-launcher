package com.alauncher.ui.spatial

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.alauncher.data.model.LauncherApp
import com.alauncher.ui.theme.OrbGlow
import com.alauncher.ui.theme.OrbGlowSecondary

/**
 * Visual representation of an app in the spatial field.
 * Renders as a glowing circular orb with icon and label.
 * Inner rings get larger orbs, outer rings get smaller.
 */
@Composable
fun AppOrb(
    app: LauncherApp,
    scale: Float = 1f,
    ringIndex: Int = 0,
    modifier: Modifier = Modifier,
) {
    // Inner apps are larger, outer apps are smaller
    val sizeMultiplier = (1f - (ringIndex * 0.04f).coerceAtMost(0.4f))
    val iconSize = (52 * sizeMultiplier).dp
    val orbSize = (60 * sizeMultiplier).dp

    // Glow intensity based on gravity score (visible even with no usage data)
    val baseGlow = 0.2f + (ringIndex.toFloat() / 30f).coerceAtMost(0.3f) // outer rings glow more teal
    val glowAlpha = (baseGlow + app.gravityScore * 0.5f).coerceIn(0.15f, 0.8f)
    val glowRadius = (12f + app.gravityScore * 20f).dp
    val glowColor = remember(app.gravityScore, ringIndex) {
        // Inner rings: warm purple glow. Outer rings: cool teal glow
        val t = (ringIndex / 20f).coerceIn(0f, 1f)
        Color(
            red = OrbGlow.red * (1f - t) + OrbGlowSecondary.red * t,
            green = OrbGlow.green * (1f - t) + OrbGlowSecondary.green * t,
            blue = OrbGlow.blue * (1f - t) + OrbGlowSecondary.blue * t,
            alpha = 1f,
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(max = 80.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glow effect behind the orb
            Box(
                modifier = Modifier
                    .size(orbSize + glowRadius)
                    .blur(glowRadius)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = glowAlpha),
                                    glowColor.copy(alpha = glowAlpha * 0.3f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.minDimension / 2,
                            )
                        )
                    }
            )
            // Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(orbSize)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            )
                        )
                    ),
            ) {
                app.icon?.let { drawable ->
                    val bitmapSize = (iconSize.value * 0.85f).toInt().coerceAtLeast(1)
                    Image(
                        bitmap = drawable.toBitmap(bitmapSize, bitmapSize).asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(iconSize * 0.85f),
                    )
                }
            }
        }
        // Label — hide on very outer rings or when zoomed out
        if (ringIndex < 15 && scale > 0.4f) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (10 * sizeMultiplier).sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
