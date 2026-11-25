package dev.vladon.vesna.launcher.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Tracks which packages hold active notifications so tiles can draw dots
 * (#200: dots, no texts). The system binds this service only after the user
 * grants notification access in system settings; revoking the grant
 * disconnects the service and clears the tracked set.
 *
 * Only [StatusBarNotification.getPackageName] is read — never titles, texts,
 * or extras — and the package set is presence-based, so group summaries and
 * per-notification counts never matter.
 */
class VesnaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        rebuild()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        NotificationBadges.addPackage(sbn.packageName)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Another notification of the same package may still be active, so a
        // removal can only shrink the set through a rebuild.
        rebuild()
    }

    override fun onListenerDisconnected() {
        NotificationBadges.clear()
    }

    private fun rebuild() {
        val set = activeNotifications.orEmpty().map { it.packageName }.toSet()
        android.util.Log.d("VesnaBadges", "rebuild: $set")
        NotificationBadges.setPackages(set)
    }
}
