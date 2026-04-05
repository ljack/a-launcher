package com.alauncher.ui.screen

import android.graphics.RenderEffect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alauncher.ui.common.AquaZone
import com.alauncher.ui.common.WaveEdge
import com.alauncher.ui.common.createMagnifyEffect
import com.alauncher.ui.common.createRefractionEffect
import com.alauncher.ui.common.rememberMagnifyState
import com.alauncher.ui.media.MediaHub
import com.alauncher.ui.search.SearchOverlay
import com.alauncher.ui.search.SearchTrigger
import com.alauncher.ui.spatial.SpatialField

/**
 * Main home screen of the launcher.
 *
 * Layers:
 * 1. SpatialField — app orbs with glow/connections
 * 2. Refraction shader — Snell-Descartes for glass zones
 * 3. Magnify shader — liquid glass lens (when active)
 * 4. Aqua overlays — glass tint for media hub + search
 * 5. UI overlays — media hub, search trigger, search overlay
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onRequestNotificationAccess: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val searchVisible by viewModel.searchVisible.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    var topZoneHeight by remember { mutableIntStateOf(0) }
    var bottomZoneHeight by remember { mutableIntStateOf(0) }
    var totalWidth by remember { mutableIntStateOf(0) }
    var totalHeight by remember { mutableIntStateOf(0) }

    val magnifyState = rememberMagnifyState()

    // Animation time for the refraction wave
    val infiniteTransition = rememberInfiniteTransition(label = "refraction")
    val refractionTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(100_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "refractionTime",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged {
                totalWidth = it.width
                totalHeight = it.height
            },
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // Spatial field with combined shader effects
            SpatialField(
                apps = uiState.apps,
                onAppTap = { app -> viewModel.launchApp(app) },
                magnifyState = magnifyState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (totalWidth <= 0 || totalHeight <= 0) return@graphicsLayer

                        val w = totalWidth.toFloat()
                        val h = totalHeight.toFloat()

                        // Magnify shader takes priority when active
                        val effect = if (magnifyState.active) {
                            val hlPos = magnifyState.highlightedAppPos
                            val hasHl = magnifyState.highlightedAppIndex >= 0
                            createMagnifyEffect(
                                width = w, height = h,
                                centerX = magnifyState.center.x,
                                centerY = magnifyState.center.y,
                                radius = magnifyState.radiusPx,
                                magnification = magnifyState.magnification,
                                edgeWidth = 35f,
                                refractiveIndex = 1.5f,
                                highlightX = if (hasHl) hlPos.x else 0f,
                                highlightY = if (hasHl) hlPos.y else 0f,
                                highlightRadius = 30f,
                            )
                        } else if (topZoneHeight > 0 || bottomZoneHeight > 0) {
                            createRefractionEffect(
                                width = w, height = h,
                                time = refractionTime,
                                topZoneEnd = topZoneHeight.toFloat(),
                                bottomZoneStart = (totalHeight - bottomZoneHeight).toFloat(),
                                refractiveIndex = 1.33f,
                                waveAmplitude = 12f,
                                waveFrequency = 3f,
                                edgeHeight = 45f,
                                edgeWidth = 140f,
                            )
                        } else null

                        if (effect != null) {
                            renderEffect = effect.asComposeRenderEffect()
                        }
                    },
            )

            // Liquid glass overlays
            Column(modifier = Modifier.fillMaxSize()) {
                AquaZone(
                    waveEdge = WaveEdge.Bottom,
                    alpha = 0.7f,
                    showWaveEdge = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .onSizeChanged { topZoneHeight = it.height },
                ) {
                    MediaHub(mediaState = mediaState)
                }

                Spacer(modifier = Modifier.weight(1f))

                AquaZone(
                    waveEdge = WaveEdge.Top,
                    alpha = 0.7f,
                    showWaveEdge = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .onSizeChanged { bottomZoneHeight = it.height },
                ) {
                    SearchTrigger(onClick = { viewModel.openSearch() })
                }
            }

            // Search Overlay
            SearchOverlay(
                visible = searchVisible,
                query = searchQuery,
                results = searchResults,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onAppSelected = { viewModel.launchApp(it) },
                onDismiss = { viewModel.closeSearch() },
            )
        }
    }
}
