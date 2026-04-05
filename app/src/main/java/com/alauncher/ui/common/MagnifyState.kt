package com.alauncher.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * State holder for the magnifying glass lens.
 */
class MagnifyState {
    /** Whether the lens is currently visible */
    var active by mutableStateOf(false)

    /** Center position of the lens in screen pixels */
    var center by mutableStateOf(Offset.Zero)

    /** Current magnification level */
    var magnification by mutableFloatStateOf(2.5f)

    /** Whether the user is currently dragging the lens */
    var dragging by mutableStateOf(false)

    /** The app index currently highlighted under touch (for launch indicator) */
    var highlightedAppIndex by mutableStateOf(-1)

    /** Screen position of the highlighted app (for drawing the indicator) */
    var highlightedAppPos by mutableStateOf(Offset.Zero)

    /** Lens radius in pixels */
    var radiusPx by mutableFloatStateOf(550f)

    /** Base radius (before pinch-resize) */
    private val baseRadiusPx = 550f
    private val minRadiusPx = 200f
    private val maxRadiusPx = 550f

    /** Y offset above the finger */
    var fingerOffsetY by mutableFloatStateOf(-180f)

    fun activate(fingerPos: Offset) {
        center = Offset(fingerPos.x, fingerPos.y + fingerOffsetY)
        active = true
        dragging = true
    }

    fun moveTo(fingerPos: Offset) {
        center = Offset(fingerPos.x, fingerPos.y + fingerOffsetY)
    }

    fun release() {
        dragging = false
        highlightedAppIndex = -1
    }

    fun dismiss() {
        active = false
        dragging = false
        // Reset to defaults for next activation
        magnification = 2.5f
        radiusPx = baseRadiusPx
    }

    /** Returns true if the given point is inside the lens circle */
    fun isInsideLens(point: Offset): Boolean {
        if (!active) return false
        val dx = point.x - center.x
        val dy = point.y - center.y
        return (dx * dx + dy * dy) <= radiusPx * radiusPx
    }

    /** Adjust both magnification and lens size with pinch */
    fun adjustWithPinch(zoomDelta: Float) {
        magnification = (magnification * zoomDelta).coerceIn(1.5f, 5f)
        radiusPx = (radiusPx * zoomDelta).coerceIn(minRadiusPx, maxRadiusPx)
    }
}

@Composable
fun rememberMagnifyState(): MagnifyState {
    return remember { MagnifyState() }
}
