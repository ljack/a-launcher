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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
 *
 * Performance: bitmap is cached via remember(app.packageName), NOT
 * re-created every frame. Glow colors and brush are also cached.
 */
@Composable
fun AppOrb(
    app: LauncherApp,
    scale: Float = 1f,
    ringIndex: Int = 0,
    modifier: Modifier = Modifier,
) {
    // Inner apps are larger, outer apps are smaller
    val sizeMultiplier = (1f - (ringIndex * 0.03f).coerceAtMost(0.35f))
    val iconSize = (50 * sizeMultiplier).dp
    val orbSize = (58 * sizeMultiplier).dp

    // Glow color — cached per ringIndex
    val glowColor = remember(ringIndex) {
        val t = (ringIndex / 15f).coerceIn(0f, 1f)
        Color(
            red = OrbGlow.red * (1f - t) + OrbGlowSecondary.red * t,
            green = OrbGlow.green * (1f - t) + OrbGlowSecondary.green * t,
            blue = OrbGlow.blue * (1f - t) + OrbGlowSecondary.blue * t,
            alpha = 1f,
        )
    }

    // Cache the bitmap — only re-create if the app changes
    val iconBitmap: ImageBitmap? = remember(app.packageName) {
        app.icon?.let { drawable ->
            val bitmapSize = (50 * sizeMultiplier).toInt().coerceAtLeast(1)
            drawable.toBitmap(bitmapSize, bitmapSize).asImageBitmap()
        }
    }

    val borderAlpha = (0.2f + app.gravityScore * 0.4f).coerceIn(0.1f, 0.5f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(max = 84.dp),
    ) {
        // Icon container with glowing border — single Box, no nested glow layer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(orbSize)
                .border(
                    width = 1.5.dp,
                    color = glowColor.copy(alpha = borderAlpha),
                    shape = CircleShape,
                )
                .clip(CircleShape)
                .background(Color(0xFF151522)),
        ) {
            iconBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = app.label,
                    modifier = Modifier.size(iconSize * 0.88f),
                )
            }
        }
        // Label — hide only when zoomed out significantly
        if (scale > 0.3f) {
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
