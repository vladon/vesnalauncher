package dev.vladon.vesna.launcher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The Home orientation choices are stored by stable name and default to following the device. */
class HomeOrientationTest {

    @Test
    fun stableNamesRoundTrip() {
        assertEquals("system", HomeOrientation.SYSTEM.preferenceValue)
        assertEquals("portrait", HomeOrientation.PORTRAIT.preferenceValue)
        assertEquals(
            HomeOrientation.PORTRAIT,
            HomeOrientation.entries.firstOrNull { it.preferenceValue == "portrait" },
        )
        assertEquals(
            HomeOrientation.SYSTEM,
            HomeOrientation.entries.firstOrNull { it.preferenceValue == "system" },
        )
    }

    @Test
    fun unknownSavedValueFallsBackToFollowingTheDevice() {
        assertNull(HomeOrientation.entries.firstOrNull { it.preferenceValue == "landscape" })
        assertEquals(
            "The preference layer maps a missing or unknown name to the shipped default",
            HomeOrientation.SYSTEM,
            HomeOrientation.entries.firstOrNull { it.preferenceValue == "landscape" } ?: HomeOrientation.SYSTEM,
        )
    }
}
