package dev.vladon.vesna.launcher.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    private fun app(label: String) = LauncherApp(
        key = AppKey("pkg.$label", "pkg.$label.MainActivity"),
        label = label,
    )

    private val apps = listOf(app("Chrome"), app("Camera"), app("Calculator"), app("YouTube"))

    @Test
    fun `blank query returns all apps`() {
        assertEquals(apps, AppSearch.filter(apps, ""))
        assertEquals(apps, AppSearch.filter(apps, "   "))
        assertEquals(apps, AppSearch.filter(apps, null))
    }

    @Test
    fun `substring match is case insensitive`() {
        assertEquals(listOf("Chrome"), AppSearch.filter(apps, "ch").map { it.label })
        assertEquals(listOf("Chrome"), AppSearch.filter(apps, "CHR").map { it.label })
    }

    @Test
    fun `query is trimmed`() {
        assertEquals(listOf("Chrome"), AppSearch.filter(apps, "  chrome  ").map { it.label })
    }

    @Test
    fun `no match returns empty list`() {
        assertTrue(AppSearch.filter(apps, "zzz").isEmpty())
    }

    @Test
    fun `match keeps original list order`() {
        assertEquals(listOf("Camera", "Calculator"), AppSearch.filter(apps, "ca").map { it.label })
    }
}
