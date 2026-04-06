package com.alauncher.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Liquid glass bar that shows the name of the app currently
 * highlighted in the magnifying glass. Tap to launch.
 */
@Composable
fun MagnifyAppBar(
    appLabel: String?,
    visible: Boolean,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && appLabel != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        val pillShape = RoundedCornerShape(28.dp)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A3040).copy(alpha = 0.85f),
                            Color(0xFF0A2030).copy(alpha = 0.9f),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF40E0D0).copy(alpha = 0.5f),
                            Color(0xFF40E0D0).copy(alpha = 0.15f),
                        )
                    ),
                    shape = pillShape,
                )
                .clickable(onClick = onLaunch)
                .padding(vertical = 16.dp, horizontal = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Open",
                    color = Color(0xFF40E0D0).copy(alpha = 0.7f),
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = appLabel ?: "",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "➜",
                    color = Color(0xFF40E0D0).copy(alpha = 0.7f),
                    fontSize = 18.sp,
                )
            }
        }
    }
}
