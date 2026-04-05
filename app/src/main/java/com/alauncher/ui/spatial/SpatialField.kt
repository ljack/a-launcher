package com.alauncher.ui.spatial

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.alauncher.data.model.LauncherApp
import kotlin.math.roundToInt

/**
 * The main spatial field composable.
 * Renders apps as orbs positioned according to their spatial coordinates.
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.3f, 3f)
                    panOffset += pan
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = panOffset.x
                translationY = panOffset.y
            }
    ) {
        val density = LocalDensity.current
        apps.forEach { app ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (app.position.x * density.density).roundToInt(),
                            (app.position.y * density.density).roundToInt(),
                        )
                    }
                    .pointerInput(app.packageName) {
                        detectTapGestures(
                            onTap = { onAppTap(app) }
                        )
                    }
            ) {
                AppOrb(app = app)
            }
        }
    }
}
