package com.alauncher.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.alauncher.data.model.LauncherApp
import com.alauncher.ui.theme.OrbGlow
import com.alauncher.ui.theme.OrbGlowSecondary
import com.alauncher.ui.theme.SurfaceCard
import com.alauncher.ui.theme.TextPrimary
import com.alauncher.ui.theme.TextSecondary

/**
 * Full-screen search overlay.
 * Slides up from bottom, shows search input and results.
 */
@Composable
fun SearchOverlay(
    visible: Boolean,
    query: String,
    results: List<LauncherApp>,
    onQueryChange: (String) -> Unit,
    onAppSelected: (LauncherApp) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                    onDismiss()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp)
                    .clickable(enabled = false) {} // prevent click-through
            ) {
                // Search input
                SearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                Spacer(Modifier.height(8.dp))

                // Hint
                if (query.isEmpty()) {
                    Text(
                        text = "Search apps · Use -term to exclude · NOT games",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                } else {
                    Text(
                        text = "${results.size} result${if (results.size != 1) "s" else ""}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Results
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(results, key = { it.packageName }) { app ->
                        SearchResultItem(
                            app = app,
                            onClick = { onAppSelected(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        OrbGlow.copy(alpha = 0.12f),
                        OrbGlowSecondary.copy(alpha = 0.08f),
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        if (query.isEmpty()) {
            Text(
                text = "Search apps...",
                color = TextSecondary.copy(alpha = 0.5f),
                fontSize = 16.sp,
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 16.sp,
            ),
            singleLine = true,
            cursorBrush = SolidColor(OrbGlowSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}

@Composable
private fun SearchResultItem(
    app: LauncherApp,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // App icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceCard),
        ) {
            app.icon?.let { drawable ->
                Image(
                    bitmap = drawable.toBitmap(40, 40).asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // App info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
