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
 * The #200 badge pipeline's package bookkeeping: presence-only per package,
 * Vesna's own package skipped, the toggle gating dot rendering, and the
 * rebuild semantics behind [VesnaNotificationListener]'s callbacks (the
 * service wrapper itself is covered end to end on the QA AVD).
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
    fun addPackageAppearsAndClearRemovesIt() {
        NotificationBadges.addPackage("qa.chatty")
        assertTrue("The posted package enters the set", NotificationBadges.shows("qa.chatty"))
        NotificationBadges.clear()
        assertFalse("Clear removes the dot", NotificationBadges.shows("qa.chatty"))
    }

    @Test
    fun ownPackageNeverGetsADot() {
        NotificationBadges.addPackage(context.packageName)
        assertFalse(NotificationBadges.shows(context.packageName))
        assertEquals(emptySet<String>(), NotificationBadges.packages.value)
    }

    @Test
    fun toggleGatesDotRendering() {
        NotificationBadges.addPackage("qa.chatty")
        NotificationBadges.enabled = false
        assertFalse("The toggle mutes dots", NotificationBadges.shows("qa.chatty"))
        NotificationBadges.enabled = true
        assertTrue(NotificationBadges.shows("qa.chatty"))
    }

    @Test
    fun rebuildReplacesTheWholeSet() {
        NotificationBadges.addPackage("qa.first")
        NotificationBadges.addPackage("qa.second")
        // A removal triggers the listener's rebuild with the still-active set.
        NotificationBadges.setPackages(setOf("qa.second"))
        assertEquals(setOf("qa.second"), NotificationBadges.packages.value)
        NotificationBadges.setPackages(emptySet())
        assertTrue(NotificationBadges.packages.value.isEmpty())
    }

    @Test
    fun setPackagesFiltersOwnPackageAndPreservesPresence() {
        NotificationBadges.setPackages(setOf("qa.a", context.packageName, "qa.b"))
        assertEquals(setOf("qa.a", "qa.b"), NotificationBadges.packages.value)
        assertTrue(NotificationBadges.shows("qa.a"))
        assertFalse(NotificationBadges.shows(context.packageName))
    }
}
