package com.alauncher.ui.screen

import android.graphics.RenderEffect
import android.graphics.Shader
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alauncher.ui.common.AquaZone
import com.alauncher.ui.common.WaveEdge
import com.alauncher.ui.media.MediaHub
import com.alauncher.ui.search.SearchOverlay
import com.alauncher.ui.search.SearchTrigger
import com.alauncher.ui.spatial.SpatialField

/**
 * Main home screen of the launcher.
 *
 * Two SpatialField renders:
 * 1. Full-screen normal (sharp orbs in the clear zone)
 * 2. Full-screen blurred, masked to only show in aqua zones
 *
 * The blurred field is drawn with a mask that clips to the
 * top and bottom zones, so only underwater orbs appear blurred.
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
    var totalHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { totalHeight = it.height },
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // Layer 1: Normal spatial field (full screen, sharp)
            SpatialField(
                apps = uiState.apps,
                onAppTap = { app -> viewModel.launchApp(app) },
                modifier = Modifier.fillMaxSize(),
            )

            // Layer 2: Blurred spatial field, masked to aqua zones only
            if (topZoneHeight > 0 || bottomZoneHeight > 0) {
                SpatialField(
                    apps = uiState.apps,
                    onAppTap = { },
                    interactive = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            renderEffect = RenderEffect
                                .createBlurEffect(50f, 50f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                        .drawWithContent {
                            // Only draw in the top and bottom zones
                            val topH = topZoneHeight.toFloat()
                            val botH = bottomZoneHeight.toFloat()
                            val totalH = size.height

                            clipRect(
                                left = 0f,
                                top = 0f,
                                right = size.width,
                                bottom = topH,
                            ) {
                                this@drawWithContent.drawContent()
                            }
                            clipRect(
                                left = 0f,
                                top = totalH - botH,
                                right = size.width,
                                bottom = totalH,
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        },
                )
            }

            // Layer 3: Aqua tint overlays (wave + tint, no blur — content is crisp)
            Column(modifier = Modifier.fillMaxSize()) {
                AquaZone(
                    waveEdge = WaveEdge.Bottom,
                    alpha = 0.55f,
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
                    alpha = 0.55f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .onSizeChanged { bottomZoneHeight = it.height },
                ) {
                    SearchTrigger(onClick = { viewModel.openSearch() })
                }
            }

            // Layer 4: Search Overlay
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

/**
 * Clips drawing to a rectangle and draws content inside.
 */
private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.clipRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    drawContext.canvas.save()
    drawContext.canvas.clipRect(left, top, right, bottom)
    block()
    drawContext.canvas.restore()
}
