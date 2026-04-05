package com.alauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
 * Layout:
 * - SpatialField fills the entire screen (behind everything)
 * - Blurred copies of the spatial field under aqua zones
 * - Media Hub overlays at top with aqua/underwater effect
 * - Search Trigger overlays at bottom with aqua/underwater effect
 * - Search Overlay floats on top of everything
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

    var topZoneHeight by remember { mutableStateOf(0) }
    var bottomZoneHeight by remember { mutableStateOf(0) }
    var totalHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current

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
            // Layer 1: Spatial Field — fills entire screen, normal (clear zone)
            SpatialField(
                apps = uiState.apps,
                onAppTap = { app -> viewModel.launchApp(app) },
                modifier = Modifier.fillMaxSize(),
            )

            // Layer 2: Blurred spatial field, only visible in top aqua zone
            if (topZoneHeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { topZoneHeight.toDp() })
                        .clipToBounds()
                ) {
                    SpatialField(
                        apps = uiState.apps,
                        onAppTap = { },
                        modifier = Modifier
                            .fillMaxSize()
                            .height(with(density) { totalHeight.toDp() })
                            .blur(16.dp),
                    )
                }
            }

            // Layer 3: Blurred spatial field, only visible in bottom aqua zone
            if (bottomZoneHeight > 0 && totalHeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { bottomZoneHeight.toDp() })
                        .align(Alignment.BottomCenter)
                        .clipToBounds()
                ) {
                    SpatialField(
                        apps = uiState.apps,
                        onAppTap = { },
                        modifier = Modifier
                            .fillMaxSize()
                            .height(with(density) { totalHeight.toDp() })
                            .graphicsLayer {
                                // Offset the field up so it aligns with the actual field position
                                translationY = -(totalHeight - bottomZoneHeight).toFloat()
                            }
                            .blur(16.dp),
                    )
                }
            }

            // Layer 4: Aqua overlays (tint + wave on top of blurred field)
            Column(modifier = Modifier.fillMaxSize()) {
                AquaZone(
                    waveEdge = WaveEdge.Bottom,
                    alpha = 0.6f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .onSizeChanged { topZoneHeight = it.height },
                ) {
                    MediaHub(
                        mediaState = mediaState,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                AquaZone(
                    waveEdge = WaveEdge.Top,
                    alpha = 0.6f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .onSizeChanged { bottomZoneHeight = it.height },
                ) {
                    SearchTrigger(
                        onClick = { viewModel.openSearch() },
                    )
                }
            }

            // Layer 5: Search Overlay
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
