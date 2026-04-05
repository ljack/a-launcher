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

    /** Lens radius in pixels (set from dp conversion) */
    var radiusPx by mutableFloatStateOf(250f)

    /** Y offset above the finger */
    var fingerOffsetY by mutableFloatStateOf(-160f)

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
        // Lens stays visible at last position
    }

    fun dismiss() {
        active = false
        dragging = false
    }

    fun adjustMagnification(zoomDelta: Float) {
        magnification = (magnification * zoomDelta).coerceIn(1.5f, 4f)
    }
}

@Composable
fun rememberMagnifyState(): MagnifyState {
    return remember { MagnifyState() }
}
