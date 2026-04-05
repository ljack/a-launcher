package com.alauncher.ui.screen

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alauncher.data.model.GravityScore
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

data class HomeUiState(
    val apps: List<LauncherApp> = emptyList(),
    val gravityScores: Map<String, GravityScore> = emptyMap(),
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
        // Sort by gravity score descending — highest gravity first
        val sorted = apps.sortedByDescending { gravityScores[it.packageName]?.composite ?: 0f }
        HomeUiState(
            apps = sorted.map { app ->
                app.copy(gravityScore = gravityScores[app.packageName]?.composite ?: 0f)
            },
            gravityScores = gravityScores,
            isLoading = loading,
        )
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
}
