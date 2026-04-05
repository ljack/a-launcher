package com.alauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives app install/uninstall/update broadcasts.
 * Delegates to the app repository via the ViewModel.
 *
 * Note: In a full implementation, this would use a shared event bus
 * or WorkManager to notify the repository. For now, the HomeViewModel
 * also registers a runtime receiver.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // App installed or updated — will be picked up on next refresh
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // App uninstalled — will be picked up on next refresh
            }
        }
    }
}
