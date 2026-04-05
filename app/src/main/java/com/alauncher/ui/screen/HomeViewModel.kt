package com.alauncher.ui.screen

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alauncher.data.model.AppPosition
import com.alauncher.data.model.LauncherApp
import com.alauncher.data.repository.AppRepository
import com.alauncher.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

data class HomeUiState(
    val apps: List<LauncherApp> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val usageRepository: UsageRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.observeApps(),
        _isLoading,
    ) { apps, loading ->
        val gravityScores = usageRepository.computeGravityScores()
        val positionedApps = assignPositions(apps, gravityScores)
        HomeUiState(apps = positionedApps, isLoading = loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch {
            appRepository.refreshApps()
            _isLoading.value = false
        }
    }

    /** Launch an app and record the usage event. */
    fun launchApp(app: LauncherApp) {
        val intent = appRepository.getLaunchIntent(app.packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        viewModelScope.launch {
            usageRepository.recordLaunch(app.packageName)
        }
    }

    /** Handle a new app being installed. */
    fun onAppAdded(packageName: String) {
        viewModelScope.launch { appRepository.onAppAdded(packageName) }
    }

    /** Handle an app being uninstalled. */
    fun onAppRemoved(packageName: String) {
        viewModelScope.launch { appRepository.onAppRemoved(packageName) }
    }

    /**
     * Assign spatial positions to apps based on gravity scores.
     * High-gravity apps go near center, others spiral outward.
     */
    private fun assignPositions(
        apps: List<LauncherApp>,
        gravityScores: Map<String, com.alauncher.data.model.GravityScore>,
    ): List<LauncherApp> {
        // Sort by gravity score descending
        val sorted = apps.sortedByDescending { gravityScores[it.packageName]?.composite ?: 0f }

        val centerX = 540f // Will be dynamic based on screen size
        val centerY = 960f

        return sorted.mapIndexed { index, app ->
            val score = gravityScores[app.packageName]?.composite ?: 0f
            // Spiral layout: higher gravity = closer to center
            val ring = index / 8 // 8 apps per ring
            val angleInRing = (index % 8) * (Math.PI * 2 / 8)
            val radius = 80f + ring * 100f // Inner ring at 80dp, expanding outward

            val x = centerX + (radius * cos(angleInRing)).toFloat() - 32f
            val y = centerY + (radius * sin(angleInRing)).toFloat() - 32f

            app.copy(
                gravityScore = score,
                position = AppPosition(
                    x = x,
                    y = y,
                    homeX = x,
                    homeY = y,
                ),
            )
        }
    }
}
