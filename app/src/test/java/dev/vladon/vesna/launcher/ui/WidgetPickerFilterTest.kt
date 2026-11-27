package dev.vladon.vesna.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The #149 catalog filter: label containment, case-insensitive, trimmed. */
class WidgetPickerFilterTest {

    private data class Row(val widget: String, val provider: String)

    private val labels: (Row) -> Pair<String, String> = { it.widget to it.provider }

    @Test
    fun `empty query keeps everything`() {
        val all = listOf(Row("Clock", "Google"), Row("Date", "Calendar"))
        assertEquals(all, filterByLabels(all, "", labels))
        assertEquals(all, filterByLabels(all, "   ", labels))
    }

    @Test
    fun `query matches widget or provider case insensitively`() {
        val all = listOf(Row("Clock", "Google"), Row("Date", "Calendar"))
        assertEquals(listOf(all[0]), filterByLabels(all, "clock", labels))
        assertEquals(listOf(all[1]), filterByLabels(all, "CALENDAR", labels))
        assertEquals(2, filterByLabels(all, "e", labels).size)
    }

    @Test
    fun `nonmatching query yields explicit empty list`() {
        val all = listOf(Row("Clock", "Google"))
        assertTrue(filterByLabels(all, "zzOneNoWidgetQA", labels).isEmpty())
    }
}
