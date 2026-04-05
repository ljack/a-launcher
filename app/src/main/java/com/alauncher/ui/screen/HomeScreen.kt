package com.alauncher.ui.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alauncher.ui.media.MediaHub
import com.alauncher.ui.search.SearchOverlay
import com.alauncher.ui.search.SearchTrigger
import com.alauncher.ui.spatial.SpatialField

/**
 * Main home screen of the launcher.
 * Layout: Media Hub (top) → Spatial Field (center) → Search Trigger (bottom)
 * Search overlay floats on top of everything.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top: Media Control Hub
                MediaHub(
                    mediaState = mediaState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                )

                // Center: Spatial Field (takes all remaining space)
                SpatialField(
                    apps = uiState.apps,
                    onAppTap = { app -> viewModel.launchApp(app) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                // Bottom: Search Trigger
                SearchTrigger(
                    onClick = { viewModel.openSearch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
            }

            // Search Overlay (floats on top)
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
