package dev.vladon.vesna.launcher.ui

import android.os.SystemClock
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.settings.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device regression for the drawer search (R-DRAWER-100.4 / R-DRAWER-101.1,
 * OBS-DRAWER-001/002): mixed-case query narrows results, no match shows the empty
 * state, clearing the query restores the full list.
 *
 * The production [DrawerView] is hosted on [SettingsActivity] (Vesna theme, no
 * live app-state collector) by adding it to the window's content frame; nothing
 * feeds it real apps, so the injected fixture stays in place deterministically.
 *
 * The unsorted fixture tests DrawerView's supplied order before and after filtering.
 */
@RunWith(AndroidJUnit4::class)
class DrawerSearchTest {

    private val fixture = listOf(
        app("dev.vladon.fixture.marigold", ".MarigoldActivity", "Marigold"),
        app("dev.vladon.fixture.quokka", ".QuokkaActivity", "Quokka"),
        app("dev.vladon.fixture.elder", ".ElderActivity", "Elderberry"),
        app("dev.vladon.fixture.alpha", ".AlphaActivity", "Aardwolf"),
        app("dev.vladon.fixture.fig", ".FigActivity", "Fig"),
        app("dev.vladon.fixture.cherry", ".CherryActivity", "Cherry"),
        app("dev.vladon.fixture.amber", ".AmberActivity", "amber fox"),
    )

    private var scenario: ActivityScenario<SettingsActivity>? = null
    private var drawer: DrawerView? = null

    private fun app(pkg: String, cls: String, label: String) =
        LauncherApp(key = AppKey(pkg, cls), label = label, icon = null)

    @Before
    fun setUp() {
        val launched = ActivityScenario.launch(SettingsActivity::class.java)
        try {
            launched.onActivity { activity ->
                val content = activity.findViewById<FrameLayout>(android.R.id.content)
                val host = DrawerView(activity)
                host.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                // The drawer already carries an 8dp elevation, so it draws over the
                // Compose settings screen that shares the content frame.
                content.addView(host)
                host.submitApps(fixture)
                host.open()
                drawer = host
            }
            scenario = launched
        } catch (t: Throwable) {
            drawer = null
            // Teardown must be safe even when setup failed mid-way.
            runCatching { launched.close() }
            throw t
        }
    }

    @After
    fun tearDown() {
        drawer = null
        runCatching { scenario?.close() }
        scenario = null
    }

    @Test
    fun searchNarrowsShowsEmptyStateAndRestore() {
        // The unfiltered drawer preserves the supplied order.
        assertVisibleLabels(fixture.map { it.label })
        assertNoResults(false)

        // Mixed-case query narrows to a single entry.
        setQuery("eLd")
        assertVisibleLabels(listOf("Elderberry"))
        assertNoResults(false)

        // Mixed-case query hitting a lowercase fixture label.
        setQuery("AMber")
        assertVisibleLabels(listOf("amber fox"))
        assertNoResults(false)

        // Both matches keep their supplied order.
        setQuery("rY")
        assertVisibleLabels(listOf("Elderberry", "Cherry"))
        assertNoResults(false)

        // No match shows the empty state.
        setQuery("zzz")
        assertVisibleLabels(emptyList())
        assertNoResults(true)

        // Clearing the query restores the full list.
        setQuery("")
        assertVisibleLabels(fixture.map { it.label })
        assertNoResults(false)
    }

    /** Runs [block] on the main thread with the DrawerView added in setUp. */
    private fun onDrawer(block: (DrawerView) -> Unit) {
        val host = drawer
        scenario!!.onActivity { block(host!!) }
    }

    private fun setQuery(text: String) {
        onDrawer { drawer ->
            drawer.findViewById<EditText>(R.id.drawer_search).setText(text)
        }
    }

    /**
     * Waits (bounded) until ListAdapter's ASYNC DiffUtil dispatch lands in
     * [androidx.recyclerview.widget.ListAdapter.getCurrentList] and then asserts
     * on the main thread. Adapter existence alone proves nothing about the diff;
     * the expected snapshot content does.
     */
    private fun assertVisibleLabels(expected: List<String>) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        var lastActual: List<String> = emptyList()
        while (SystemClock.uptimeMillis() < deadline) {
            var matched = false
            onDrawer { drawer ->
                val list = drawer.findViewById<RecyclerView>(R.id.drawer_list)
                val adapter =
                    list.adapter as? androidx.recyclerview.widget.ListAdapter<LauncherApp, *>
                lastActual = adapter?.currentList?.map { it.label } ?: emptyList()
                matched = lastActual == expected
            }
            if (matched) return
            SystemClock.sleep(50)
        }
        throw AssertionError(
            "Drawer contents did not settle within 5s; expected=$expected actual=$lastActual",
        )
    }

    private fun assertNoResults(shown: Boolean) {
        onDrawer { host ->
            val empty = host.findViewById<TextView>(R.id.drawer_no_results)
            assertEquals(shown, empty.visibility == TextView.VISIBLE)
        }
    }
}
