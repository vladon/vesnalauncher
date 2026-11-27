package dev.vladon.vesna.launcher.ui

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Work tab slice: the Work tab lists only managed-profile apps and the
 * Apps tab excludes them; without a managed profile the list passes through
 * unchanged (the drawer is tabbed on every managed profile — full Nova parity).
 */
class DrawerWorkTabSliceTest {

    private val personal = LauncherApp(AppKey("qa.personal", "Main", userSerial = 10L), "Personal App")
    private val work = LauncherApp(AppKey("qa.workapp", "Main", userSerial = 11L), "Work App")
    private val apps = listOf(personal, work)

    @Test
    fun workTabKeepsManagedAppsOnly() {
        assertEquals(listOf(work), WorkTabSlice.apply(apps, workProfileUserSerial = 11L, workTabActive = true))
    }

    @Test
    fun appsTabExcludesManagedApps() {
        assertEquals(listOf(personal), WorkTabSlice.apply(apps, workProfileUserSerial = 11L, workTabActive = false))
    }

    @Test
    fun noProfilePassesEverythingThrough() {
        assertEquals(apps, WorkTabSlice.apply(apps, workProfileUserSerial = null, workTabActive = true))
    }
}
