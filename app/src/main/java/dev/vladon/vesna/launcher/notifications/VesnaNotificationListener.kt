package dev.vladon.vesna.launcher.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Tracks which packages hold active notifications so tiles can draw badges.
 * The system binds this service only after the user grants notification
 * access in system settings; revoking the grant disconnects the service and
 * clears the tracked state.
 *
 * Only [StatusBarNotification.getPackageName] and [StatusBarNotification.key]
 * are read — never titles, texts, or extras. The key identifies the
 * notification so per-package active-notification counts stay exact; no
 * message content is ever touched (ADR-NOTIF-1 content surface).
 */
class VesnaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        rebuild()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        NotificationBadges.addNotification(sbn.key, sbn.packageName)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // The key is gone from the active set; a rebuild re-counts any sibling
        // notifications of the same package that are still active.
        rebuild()
    }

    override fun onListenerDisconnected() {
        NotificationBadges.clear()
    }

    private fun rebuild() {
        val keysByPackage = activeNotifications.orEmpty()
            .groupBy({ it.packageName }, { it.key }).mapValues { it.value.toSet() }
        NotificationBadges.setNotifications(keysByPackage)
    }
}
