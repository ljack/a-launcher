package com.alauncher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alauncher.notification.NotificationCaptureService
import com.alauncher.ui.screen.HomeScreen
import com.alauncher.ui.theme.ALauncherTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main launcher activity.
 * Registered as HOME category to act as the device launcher.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var hasPromptedNotificationAccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasPromptedNotificationAccess =
            savedInstanceState?.getBoolean("prompted_notif", false) ?: false
        setContent {
            ALauncherTheme {
                HomeScreen(
                    onRequestNotificationAccess = { openNotificationListenerSettings() }
                )
            }
        }
        // Prompt once on first launch if not granted
        if (!hasPromptedNotificationAccess && !isNotificationListenerEnabled()) {
            hasPromptedNotificationAccess = true
            openNotificationListenerSettings()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("prompted_notif", hasPromptedNotificationAccess)
    }

    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    companion object {
        fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
            val componentName = ComponentName(context, NotificationCaptureService::class.java)
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(componentName.flattenToString())
        }
    }

    private fun isNotificationListenerEnabled(): Boolean =
        Companion.isNotificationListenerEnabled(this)
}
