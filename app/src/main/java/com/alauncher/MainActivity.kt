package com.alauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.alauncher.ui.screen.HomeScreen
import com.alauncher.ui.theme.ALauncherTheme

/**
 * Main launcher activity.
 * Registered as HOME category to act as the device launcher.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ALauncherTheme {
                HomeScreen()
            }
        }
    }
}
