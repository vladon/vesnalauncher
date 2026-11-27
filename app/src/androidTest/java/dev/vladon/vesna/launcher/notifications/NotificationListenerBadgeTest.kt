package dev.vladon.vesna.launcher.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The badge pipeline's bookkeeping: presence + per-package active-notification
 * counts (key metadata only), Vesna's own package skipped, the master toggle
 * gating badge rendering, and the rebuild semantics behind
 * [VesnaNotificationListener]'s callbacks (the service wrapper itself is
 * covered end to end on the QA AVD).
 */
@RunWith(AndroidJUnit4::class)
class NotificationListenerBadgeTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        NotificationBadges.init(context)
        NotificationBadges.enabled = true
        NotificationBadges.clear()
    }

    @Test
    fun addNotificationAppearsAndClearRemovesIt() {
        NotificationBadges.addNotification("qa.chatty-1", "qa.chatty")
        assertTrue("The posted package enters the set", NotificationBadges.shows("qa.chatty"))
        NotificationBadges.clear()
        assertFalse("Clear removes the badge", NotificationBadges.shows("qa.chatty"))
    }

    @Test
    fun ownPackageNeverGetsABadge() {
        NotificationBadges.addNotification("own-1", context.packageName)
        assertFalse(NotificationBadges.shows(context.packageName))
        assertEquals(emptyMap<String, Int>(), NotificationBadges.counts.value)
    }

    @Test
    fun toggleGatesBadgeRendering() {
        NotificationBadges.addNotification("qa.chatty-1", "qa.chatty")
        NotificationBadges.enabled = false
        assertFalse("The toggle mutes badges", NotificationBadges.shows("qa.chatty"))
        NotificationBadges.enabled = true
        assertTrue(NotificationBadges.shows("qa.chatty"))
    }

    @Test
    fun countsTrackActiveNotificationsPerPackage() {
        NotificationBadges.addNotification("qa.chatty-1", "qa.chatty")
        NotificationBadges.addNotification("qa.chatty-2", "qa.chatty")
        assertEquals(2, NotificationBadges.count("qa.chatty"))
        NotificationBadges.removeNotification("qa.chatty-1", "qa.chatty")
        assertEquals(1, NotificationBadges.count("qa.chatty"))
        assertTrue(NotificationBadges.shows("qa.chatty"))
        NotificationBadges.removeNotification("qa.chatty-2", "qa.chatty")
        assertFalse("Zero active notifications clear the badge", NotificationBadges.shows("qa.chatty"))
    }

    @Test
    fun rebuildReplacesTheWholeCountMap() {
        NotificationBadges.addNotification("qa.first-1", "qa.first")
        NotificationBadges.addNotification("qa.second-1", "qa.second")
        // A removal triggers the listener's rebuild with the still-active keys.
        NotificationBadges.setNotifications(mapOf("qa.second" to setOf("qa.second-1")))
        assertEquals(mapOf("qa.second" to 1), NotificationBadges.counts.value)
        NotificationBadges.setNotifications(emptyMap())
        assertTrue(NotificationBadges.counts.value.isEmpty())
    }

    @Test
    fun rebuildFiltersOwnPackageAndPreservesCounts() {
        NotificationBadges.setNotifications(
            mapOf(
                "qa.a" to setOf("qa.a-1"),
                context.packageName to setOf("own-1", "own-2"),
                "qa.b" to setOf("qa.b-1", "qa.b-2"),
            ),
        )
        assertEquals(setOf("qa.a", "qa.b"), NotificationBadges.packages.value)
        assertTrue(NotificationBadges.shows("qa.a"))
        assertFalse(NotificationBadges.shows(context.packageName))
        assertEquals(2, NotificationBadges.count("qa.b"))
    }

    @Test
    fun folderRecycleClearsTheBadge() {
        // A tile recycled from a badged app into non-app content (a folder)
        // must not keep the stale badge (the v0.4.90 fix).
        val tile = dev.vladon.vesna.launcher.ui.AppIconView(context)
        tile.setNotificationDot(true)
        assertTrue(tile.notificationDotShowing)
        tile.bindContent(null, "Folder", "Folder")
        assertFalse("The recycled tile loses the stale badge", tile.notificationDotShowing)
    }
}
