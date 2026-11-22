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
    private val search = AppSearch(apps)

    @Test
    fun `blank query returns all apps`() {
        assertEquals(apps, search.filter(""))
        assertEquals(apps, search.filter(" \t\u00a0 "))
        assertEquals(apps, search.filter(null))
    }

    @Test
    fun `substring match is case insensitive`() {
        assertEquals(listOf("Chrome"), search.filter("ch").map { it.label })
        assertEquals(listOf("Chrome"), search.filter("CHR").map { it.label })
    }

    @Test
    fun `query is trimmed`() {
        assertEquals(listOf("Chrome"), search.filter("  chrome  ").map { it.label })
    }

    @Test
    fun `no match returns empty list`() {
        assertTrue(search.filter("zzz").isEmpty())
    }

    @Test
    fun `match keeps original list order`() {
        assertEquals(listOf("Camera", "Calculator"), search.filter("ca").map { it.label })
    }

    @Test
    fun `accents and spacing match without changing displayed labels or order`() {
        val names = listOf("Café\u00a0Reader", "Cafe\u0301 Reader Pro", "Cafe Notes")
        val search = AppSearch(names.map(::app))

        assertEquals(names.take(2), search.filter("CAFE\t   READER").map { it.label })
        assertEquals(listOf(names.last()), search.filter("café\u202fnotes").map { it.label })
        assertTrue(search.filter("reader cafe").isEmpty())
    }

    @Test
    fun `fullwidth spelling and both forms of Greek sigma match`() {
        val apps = listOf(app("Ｃａｆｅ"), app("ΤΕΛΟΣ"))
        val search = AppSearch(apps)

        assertEquals(listOf(apps[0]), search.filter("cafe"))
        assertEquals(listOf(apps[1]), search.filter("λος"))
        assertEquals(listOf(apps[1]), search.filter("λοσ"))
    }

    @Test
    fun `Russian yo is optional but short i and Japanese voicing stay distinct`() {
        val apps = listOf(app("Ёлки"), app("Йога"), app("ｶﾞｸ"))
        val search = AppSearch(apps)

        assertEquals(listOf(apps[0]), search.filter("елки"))
        assertTrue(search.filter("иога").isEmpty())
        assertEquals(listOf(apps[2]), search.filter("ガク"))
        assertTrue(search.filter("カク").isEmpty())
    }

    @Test
    fun `hidden entry is omitted from browsing and partial search only in its own profile`() {
        val personal = app("Café Reader")
        val work = personal.copy(key = personal.key.copy(userSerial = 10))
        val other = app("Cafe Notes")
        val search = AppSearch(listOf(other, personal, work), hiddenApps = setOf(personal.key))

        assertEquals(listOf(other, work), search.filter("  "))
        assertEquals(listOf(other, work), search.filter("cafe"))
        assertEquals(listOf(personal, work), search.filter("  CAFE\u00a0Reader "))
    }
}
