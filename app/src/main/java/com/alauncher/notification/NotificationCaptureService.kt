package com.alauncher.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * NotificationListenerService implementation.
 * Required for MediaSessionManager access and future persistent notifications (F6).
 * User must grant notification access in Settings.
 */
class NotificationCaptureService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // TODO: F6 — capture and persist notifications
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // TODO: F6 — mark as removed but keep in local storage
    }
}
