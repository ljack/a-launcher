package com.alauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives BOOT_COMPLETED to ensure launcher is ready after device restart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Launcher will be started by the system as the default HOME app.
            // This receiver ensures we're registered for boot events.
        }
    }
}
