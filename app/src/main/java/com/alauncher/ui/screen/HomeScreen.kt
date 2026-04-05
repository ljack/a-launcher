package com.alauncher.ui.screen

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
import com.alauncher.ui.common.createRefractionEffect
import com.alauncher.ui.media.MediaHub
import com.alauncher.ui.search.SearchOverlay
import com.alauncher.ui.search.SearchTrigger
import com.alauncher.ui.spatial.SpatialField

/**
 * Main home screen of the launcher.
 *
 * Snell-Descartes refraction shader distorts the spatial field
 * in the aqua glass zones, creating realistic underwater/glass effect.
 * Liquid glass tint overlays on top for the frosted look.
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
            // Spatial field with Snell-Descartes refraction shader
            SpatialField(
                apps = uiState.apps,
                onAppTap = { app -> viewModel.launchApp(app) },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (totalWidth > 0 && totalHeight > 0 &&
                            (topZoneHeight > 0 || bottomZoneHeight > 0)
                        ) {
                            val effect = createRefractionEffect(
                                width = totalWidth.toFloat(),
                                height = totalHeight.toFloat(),
                                time = refractionTime,
                                topZoneEnd = topZoneHeight.toFloat(),
                                bottomZoneStart = (totalHeight - bottomZoneHeight).toFloat(),
                                refractiveIndex = 1.33f,  // water
                                waveAmplitude = 12f,
                                waveFrequency = 3f,
                                edgeHeight = 45f,   // glass thickness at edge
                                edgeWidth = 140f,    // lens zone width
                            )
                            if (effect != null) {
                                renderEffect = effect.asComposeRenderEffect()
                            }
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
