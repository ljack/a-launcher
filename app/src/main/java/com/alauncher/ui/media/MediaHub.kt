package com.alauncher.ui.media

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Media Control Hub — liquid glass pill at the top of the launcher.
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

    val pillShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(pillShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.05f),
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.06f),
                    )
                ),
                shape = pillShape,
            )
    ) {
        // Collapsed: compact indicator
        CompactMediaIndicator(
            source = source,
            sourceCount = mediaState.allSources.size,
            onClick = { expanded = !expanded },
        )

        // Progress bar
        if (source.durationMs != null && source.durationMs > 0) {
            LinearProgressIndicator(
                progress = { source.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = Color.White.copy(alpha = 0.5f),
                trackColor = Color.White.copy(alpha = 0.08f),
            )
        }

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
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // Play/pause indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (source.isPlaying) Color(0xFF40E0D0)
                    else Color.White.copy(alpha = 0.5f)
                )
        )

        Spacer(Modifier.width(12.dp))

        // Track info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.title ?: "Unknown",
                color = Color.White.copy(alpha = 0.9f),
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
                color = Color.White.copy(alpha = 0.55f),
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
            Text("⏮", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
        }
        IconButton(
            onClick = { source.togglePlayPause() },
            modifier = Modifier.size(36.dp),
        ) {
            Text(
                text = if (source.isPlaying) "⏸" else "▶",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        IconButton(
            onClick = { source.skipNext() },
            modifier = Modifier.size(32.dp),
        ) {
            Text("⏭", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
        }

        // Source count badge
        if (sourceCount > 1) {
            Spacer(Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            ) {
                Text(
                    text = "$sourceCount",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Source switcher — show all active media apps
        if (state.allSources.size > 1) {
            Text(
                text = "SOURCES",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
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
                val isActive = source == state.activeSource

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isActive) Modifier.background(Color.White.copy(alpha = 0.06f))
                            else Modifier
                        )
                        .clickable { onSourceSelected(source) }
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color(0xFF40E0D0)
                                else Color.White.copy(alpha = 0.2f)
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = if (isActive) 0.9f else 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        source.title?.let {
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = if (source.isPlaying) "▶" else "⏸",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f),
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
