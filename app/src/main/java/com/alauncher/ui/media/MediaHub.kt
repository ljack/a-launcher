package com.alauncher.ui.media

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alauncher.media.MediaSource
import com.alauncher.media.MediaState
import com.alauncher.ui.theme.OrbGlow
import com.alauncher.ui.theme.OrbGlowSecondary
import com.alauncher.ui.theme.TextPrimary
import com.alauncher.ui.theme.TextSecondary

/**
 * Media Control Hub — shows at the top of the launcher when media is playing.
 * Compact indicator when collapsed, full controls when expanded.
 */
@Composable
fun MediaHub(
    mediaState: MediaState,
    modifier: Modifier = Modifier,
) {
    if (!mediaState.hasMedia) return

    var expanded by remember { mutableStateOf(false) }
    val source = mediaState.activeSource ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Collapsed: compact indicator
        CompactMediaIndicator(
            source = source,
            sourceCount = mediaState.allSources.size,
            onClick = { expanded = !expanded },
        )

        // Expanded: full controls
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            ExpandedMediaControls(
                state = mediaState,
                onSourceSelected = { /* switch active source */ },
            )
        }
    }
}

@Composable
private fun CompactMediaIndicator(
    source: MediaSource,
    sourceCount: Int,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val appLabel = remember(source.packageName) {
        try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(source.packageName, 0)
            ).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            source.packageName
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        OrbGlow.copy(alpha = 0.15f),
                        OrbGlowSecondary.copy(alpha = 0.1f),
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // Play/pause indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (source.isPlaying) OrbGlowSecondary else OrbGlow)
        )

        Spacer(Modifier.width(12.dp))

        // Track info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.title ?: "Unknown",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(source.artist ?: appLabel)
                    source.remainingMs?.let { ms ->
                        append(" · ${formatDuration(ms)} left")
                    }
                },
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Quick controls
        IconButton(
            onClick = { source.skipPrev() },
            modifier = Modifier.size(32.dp),
        ) {
            Text("⏮", fontSize = 14.sp)
        }
        IconButton(
            onClick = { source.togglePlayPause() },
            modifier = Modifier.size(36.dp),
        ) {
            Text(
                text = if (source.isPlaying) "⏸" else "▶",
                fontSize = 18.sp,
            )
        }
        IconButton(
            onClick = { source.skipNext() },
            modifier = Modifier.size(32.dp),
        ) {
            Text("⏭", fontSize = 14.sp)
        }

        // Source count badge
        if (sourceCount > 1) {
            Spacer(Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(OrbGlow.copy(alpha = 0.3f)),
            ) {
                Text(
                    text = "$sourceCount",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    // Progress bar
    if (source.durationMs != null && source.durationMs > 0) {
        LinearProgressIndicator(
            progress = { source.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp)),
            color = OrbGlowSecondary,
            trackColor = OrbGlow.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun ExpandedMediaControls(
    state: MediaState,
    onSourceSelected: (MediaSource) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OrbGlow.copy(alpha = 0.1f),
                        Color.Transparent,
                    )
                )
            )
            .padding(12.dp),
    ) {
        // Source switcher — show all active media apps
        if (state.allSources.size > 1) {
            Text(
                text = "Sources",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            val context = LocalContext.current
            state.allSources.forEach { source ->
                val label = remember(source.packageName) {
                    try {
                        context.packageManager.getApplicationLabel(
                            context.packageManager.getApplicationInfo(source.packageName, 0)
                        ).toString()
                    } catch (_: PackageManager.NameNotFoundException) {
                        source.packageName
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSourceSelected(source) }
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (source == state.activeSource) OrbGlowSecondary
                                else TextSecondary.copy(alpha = 0.3f)
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            color = if (source == state.activeSource) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        source.title?.let {
                            Text(
                                text = it,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = if (source.isPlaying) "▶" else "⏸",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
