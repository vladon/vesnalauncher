package dev.vladon.vesna.launcher.ui

import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.os.SystemClock
import android.util.SparseArray
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.settings.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

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
        app("dev.vladon.fixture.reader", ".ReaderActivity", "Café\u00a0Reader"),
        app("dev.vladon.fixture.pro", ".ProActivity", "Cafe\u0301 Reader Pro"),
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

        // Use the visible control, not setText, to prove the user can recover.
        onDrawer { host ->
            val clear = host.findViewById<View>(R.id.drawer_clear_search)
            assertTrue(clear.isShown)
            clear.performClick()
            assertTrue(host.findViewById<EditText>(R.id.drawer_search).hasFocus())
            assertEquals(View.GONE, clear.visibility)
        }
        assertVisibleLabels(fixture.map { it.label })
        assertNoResults(false)
    }

    @Test
    fun fastScrollerCanAdvancePastSectionsThatShareTheBottomViewport() {
        val apps = List(100) { app("fixture.fast", ".A$it", "Aster $it") } +
            listOf(
                app("fixture.fast", ".B", "Birch"),
                app("fixture.fast", ".C", "Clover"),
                app("fixture.fast", ".Yu", "Юла"),
            )
        onDrawer { it.submitApps(apps) }
        assertVisibleLabels(apps.map { it.label })
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        fun rail(host: DrawerView) = host.findViewById<View>(R.id.drawer_fast_scroller)

        onDrawer { host ->
            assertTrue(rail(host).isShown)
            assertTrue(rail(host).requestFocus())
            val args = Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, 1f) }
            assertTrue(rail(host).performAccessibilityAction(
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id, args,
            ))
        }
        instrumentation.waitForIdleSync()
        onDrawer { host ->
            val list = host.findViewById<RecyclerView>(R.id.drawer_list)
            assertTrue("The jump must bring Birch into the viewport",
                (0 until list.childCount).any { list.getChildAt(it).contentDescription == "Birch" })
            assertEquals("B", ViewCompat.getStateDescription(rail(host)))
        }
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        instrumentation.waitForIdleSync()
        onDrawer { host ->
            assertEquals("C", ViewCompat.getStateDescription(rail(host)))
            assertTrue(rail(host).performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
            assertEquals("Ю", ViewCompat.getStateDescription(rail(host)))
        }
        setQuery("birch")
        assertVisibleLabels(listOf("Birch"))
        onDrawer { host ->
            assertTrue("A filter must remove the rail from navigation", !rail(host).isShown)
            host.findViewById<View>(R.id.drawer_clear_search).performClick()
        }
        assertVisibleLabels(apps.map { it.label })
        instrumentation.waitForIdleSync()
        onDrawer { host ->
            val list = host.findViewById<RecyclerView>(R.id.drawer_list)
            assertTrue(rail(host).isShown)
            assertEquals(0, (list.layoutManager as GridLayoutManager).findFirstVisibleItemPosition())
        }
    }

    @Test
    fun internationalQueryKeepsOriginalLabelsAndUsesTheUpdatedAppList() {
        val query = "  CAFE   READER  "
        setQuery(query)
        assertVisibleLabels(listOf("Café\u00a0Reader", "Cafe\u0301 Reader Pro"))
        assertNoResults(false)

        val renamed = fixture.map { app ->
            if (app.key.packageName == "dev.vladon.fixture.reader") app.copy(label = "Café Tools") else app
        }
        onDrawer { it.submitApps(renamed) }
        assertVisibleLabels(listOf("Cafe\u0301 Reader Pro"))
        onDrawer { assertEquals(query, it.findViewById<EditText>(R.id.drawer_search).text.toString()) }

        setQuery("cafe tools")
        assertVisibleLabels(listOf("Café Tools"))
    }

    @Test
    fun profileDescriptionsDistinguishCopiesAndUpdateWithoutChangingSearch() {
        val personal = fixture.first()
        val work = personal.copy(key = personal.key.copy(userSerial = 10), badgedLabel = "Marigold (Work)")
        onDrawer { it.submitApps(listOf(personal, work)) }
        setQuery("Marigold")
        assertFirstDrawerRow(listOf("Marigold", "Marigold (Work)"))

        onDrawer {
            it.showAppNames = false
            it.submitApps(listOf(personal, work.copy(badgedLabel = "Marigold (Managed)")))
        }
        assertFirstDrawerRow(listOf("Marigold", "Marigold (Managed)"))

        setQuery("Managed")
        assertVisibleLabels(emptyList())
        assertNoResults(true)
    }

    @Test
    fun typingFromDrawerControlsStartsAndContinuesSearch() {
        // A real navigation key leaves touch mode so drawer buttons can receive focus.
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        onDrawer { host ->
            val search = host.findViewById<EditText>(R.id.drawer_search)
            assertTrue(host.findViewById<View>(R.id.drawer_close).requestFocus())
            typeKey(host, KeyEvent.KEYCODE_Q, KeyEvent.META_CTRL_ON)
            assertEquals("", search.text.toString())
            assertTrue(host.findViewById<View>(R.id.drawer_close).hasFocus())
            typeKey(host, KeyEvent.KEYCODE_Q, KeyEvent.META_SHIFT_ON)
            assertTrue(search.hasFocus())
            assertEquals("Q", search.text.toString())
        }
        assertVisibleLabels(listOf("Quokka"))

        onDrawer { host ->
            val search = host.findViewById<EditText>(R.id.drawer_search)
            search.setSelection(0)
            assertTrue(host.findViewById<View>(R.id.drawer_clear_search).requestFocus())
            typeKey(host, KeyEvent.KEYCODE_U)
            assertTrue(search.hasFocus())
            assertEquals("Qu", search.text.toString())
        }
        assertVisibleLabels(listOf("Quokka"))
    }

    @Test
    fun enterLaunchesOnlyTheLatestUniqueNonblankMatch() {
        val launched = mutableListOf<AppKey>()
        setQuery("quokka")
        assertVisibleLabels(listOf("Quokka"))

        onDrawer { host ->
            host.onAppClick = { launched.add(it.key) }
            val search = host.findViewById<EditText>(R.id.drawer_search)
            search.requestFocus()
            typeKey(host, KeyEvent.KEYCODE_ENTER)
            assertEquals(listOf(fixture[1].key), launched)

            // A pending adapter diff must not let the old Quokka result launch.
            search.setText("cafe reader")
            typeKey(host, KeyEvent.KEYCODE_ENTER)
            assertTrue(search.hasFocus())
            search.setText("no matches")
            typeKey(host, KeyEvent.KEYCODE_ENTER)
            assertTrue(search.hasFocus())
            assertEquals(listOf(fixture[1].key), launched)

            val onlyApp = fixture[2]
            host.submitApps(listOf(onlyApp))
            search.setText(" ")
            typeKey(host, KeyEvent.KEYCODE_ENTER)
            assertEquals(listOf(fixture[1].key), launched)

            search.setText("elderberry")
            search.onEditorAction(EditorInfo.IME_ACTION_DONE)
            assertEquals(listOf(fixture[1].key), launched)
            search.requestFocus()
            typeKey(host, KeyEvent.KEYCODE_NUMPAD_ENTER)
            assertEquals(listOf(fixture[1].key, onlyApp.key), launched)
        }
    }

    @Test
    fun releasingEnterDoesNotLaunchAnotherAppIfTheMatchChangesWhileHeld() {
        val launched = mutableListOf<AppKey>()
        onDrawer { host ->
            host.onAppClick = { launched.add(it.key) }
            val search = host.findViewById<EditText>(R.id.drawer_search)
            search.requestFocus()
            search.setText("quokka")
            val time = SystemClock.uptimeMillis()
            host.dispatchKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0))
            search.setText("elderberry")
            host.dispatchKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0))
            assertTrue(launched.isEmpty())
            typeKey(host, KeyEvent.KEYCODE_ENTER)
            assertEquals(listOf(fixture[2].key), launched)
        }
    }

    @Test
    fun quickDownwardSwipeScrollsBackBeforeItCanCloseTheDrawer() {
        val apps = List(96) { app("dev.vladon.fixture.tool$it", ".Main", "Tool $it") }
        onDrawer { it.submitApps(apps) }
        assertVisibleLabels(apps.map { it.label })
        assertFirstVisibleApp(apps.first().label, offset = 0)

        onDrawer { host ->
            val list = host.findViewById<RecyclerView>(R.id.drawer_list)
            list.scrollBy(0, 4 * ViewConfiguration.get(list.context).scaledTouchSlop)
            assertTrue("The swipe must begin below the top", list.canScrollVertically(-1))

            swipeDown(list, MotionEvent.ACTION_UP)
            assertTrue("The swipe must actually reach the top", !list.canScrollVertically(-1))
            assertNotEquals(
                "Returning to the top must not dismiss the drawer",
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                host.importantForAccessibility,
            )

            swipeDown(list, MotionEvent.ACTION_CANCEL)
            assertNotEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, host.importantForAccessibility)

            swipeDown(list, MotionEvent.ACTION_UP)
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, host.importantForAccessibility)
        }
    }

    @Test
    fun appRefreshKeepsTheVisibleRowButClearingSearchResetsToTheNewestTop() {
        val tools = List(96) { app("dev.vladon.fixture.tool$it", ".Main", "Tool $it") }
        val before = List(4) { app("dev.vladon.fixture.before$it", ".Main", "Before $it") }
        val added = List(4) { app("dev.vladon.fixture.added$it", ".Main", "Tool added $it") }
        val latest = app("dev.vladon.fixture.latest", ".Main", "Latest")
        onDrawer { it.submitApps(before + tools) }
        setQuery("tool")
        assertVisibleLabels(tools.map { it.label })
        onDrawer { host ->
            val list = host.findViewById<RecyclerView>(R.id.drawer_list)
            (list.layoutManager as GridLayoutManager).scrollToPositionWithOffset(40, -12)
        }
        assertFirstVisibleApp(tools[40].label, offset = -12)

        onDrawer { it.submitApps(before + added + tools) }
        assertVisibleLabels((added + tools).map { it.label })
        assertFirstVisibleApp(tools[40].label, offset = -12)

        onDrawer { it.submitApps(before + tools) }
        assertVisibleLabels(tools.map { it.label })
        assertFirstVisibleApp(tools[40].label, offset = -12)

        onDrawer { host ->
            host.findViewById<View>(R.id.drawer_clear_search).performClick()
            // The refresh may replace the pending async result of clearing the query.
            host.submitApps(listOf(latest) + before + tools)
        }
        assertVisibleLabels((listOf(latest) + before + tools).map { it.label })
        assertFirstVisibleApp(latest.label, offset = 0)
    }

    @Test
    fun recreatedDrawerAnchorsToTheSameAppAndProfileAfterResultsChange() {
        val tools = List(96) { app("dev.vladon.fixture.tool$it", ".Main", "Tool $it") }
        val other = app("dev.vladon.fixture.other", ".Main", "Other")
        val personal = tools[40]
        val work = personal.copy(key = personal.key.copy(userSerial = 10), badgedLabel = "Work Tool 40")
        val catalogue = listOf(other) + tools.map { if (it == personal) work else it }
        onDrawer { it.submitApps(catalogue) }
        setQuery("tool")
        assertVisibleLabels(tools.map { it.label })
        onDrawer { host ->
            val grid = host.findViewById<RecyclerView>(R.id.drawer_list).layoutManager as GridLayoutManager
            grid.scrollToPositionWithOffset(40, -12)
        }
        assertFirstVisibleApp(work.accessibilityLabel, offset = -12)

        fun recreateWith(apps: List<LauncherApp>) {
            onDrawer { host ->
                val viewState = SparseArray<Parcelable>().also(host::saveHierarchyState)
                val resultState = Bundle().also(host::saveResultViewport)
                val content = host.parent as ViewGroup
                val params = host.layoutParams
                content.removeView(host)
                val replacement = DrawerView(host.context)
                content.addView(replacement, params)
                replacement.open()
                replacement.restoreHierarchyState(viewState)
                replacement.restoreResults(resultState, focusedApp = null)
                replacement.submitApps(emptyList())
                drawer = replacement
            }
            assertVisibleLabels(emptyList())
            onDrawer { assertEquals("tool", it.findViewById<EditText>(R.id.drawer_search).text.toString()) }
            onDrawer { it.submitApps(apps) }
        }

        // The personal copy now occupies the old slot; the work copy is a row down.
        val added = List(3) { app("dev.vladon.fixture.new$it", ".Main", "New Tool $it") }
        val changed = listOf(other) + tools.take(41) + added + work + tools.drop(41)
        recreateWith(changed)
        assertFirstVisibleApp(work.accessibilityLabel, offset = -12)

        recreateWith(changed.filterNot { it.key == work.key })
        assertFirstVisibleApp(tools[41].accessibilityLabel, offset = -12)

        onDrawer { it.findViewById<View>(R.id.drawer_clear_search).performClick() }
        assertFirstVisibleApp(other.label, offset = 0)
    }

    @Test
    fun refreshingByOneAppKeepsTheFirstVisibleShortcutAtTheSameHeight() {
        // Leave enough rows below Tool 60 that even a tall viewport won't clamp its offset.
        val tools = List(160) { app("dev.vladon.fixture.tool$it", ".Main", "Tool $it") }
        val anchored = tools[40]
        onDrawer { it.submitApps(tools) }
        setQuery("tool")
        assertVisibleLabels(tools.map { it.label })
        onDrawer { host ->
            val list = host.findViewById<RecyclerView>(R.id.drawer_list)
            (list.layoutManager as GridLayoutManager).scrollToPositionWithOffset(40, -12)
        }
        assertFirstVisibleApp(anchored.label, offset = -12)

        val fewer = tools.drop(1)
        onDrawer { it.submitApps(fewer) }
        assertVisibleLabels(fewer.map { it.label })
        assertAppOffset(anchored.label, offset = -12)

        onDrawer { it.submitApps(tools) }
        assertVisibleLabels(tools.map { it.label })
        assertAppOffset(anchored.label, offset = -12)

        // Scrolling to a different row gives the next refresh a new anchor.
        onDrawer { host ->
            val list = host.findViewById<RecyclerView>(R.id.drawer_list)
            (list.layoutManager as GridLayoutManager).scrollToPositionWithOffset(60, -20)
        }
        assertFirstVisibleApp(tools[60].label, offset = -20)
        onDrawer { it.submitApps(fewer) }
        assertVisibleLabels(fewer.map { it.label })
        assertAppOffset(tools[60].label, offset = -20)
        onDrawer { assertEquals("tool", it.findViewById<EditText>(R.id.drawer_search).text.toString()) }
    }

    @Test
    fun drawerFitsTilesInsideSideInsetsAndKeepsTheFocusedAppOnResize() {
        var minCellWidth = 0
        onDrawer { host ->
            minCellWidth = ceil(64 * host.resources.displayMetrics.density).toInt()
            host.layoutParams = host.layoutParams.apply { width = minCellWidth * 4 }
        }
        assertFirstDrawerRow(fixture.take(4).map { it.label }, minCellWidth)

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        onDrawer { host ->
            val list = host.findViewById<RecyclerView>(R.id.drawer_list)
            assertTrue(list.findViewHolderForAdapterPosition(0)!!.itemView.requestFocus())
            val side = minCellWidth / 4
            host.applyContentInsets(Insets.of(side, 0, side, 0))
        }
        assertFirstDrawerRow(fixture.take(3).map { it.label }, minCellWidth, focused = fixture.first().label)

        onDrawer { host ->
            host.applyContentInsets(Insets.NONE)
            host.layoutParams = host.layoutParams.apply { width = minCellWidth * 2 + minCellWidth / 2 }
        }
        assertFirstDrawerRow(fixture.take(2).map { it.label }, minCellWidth, focused = fixture.first().label)

        onDrawer { host -> host.layoutParams = host.layoutParams.apply { width = minCellWidth * 4 } }
        assertFirstDrawerRow(fixture.take(4).map { it.label }, minCellWidth, focused = fixture.first().label)
    }

    @Test
    fun shortKeyboardViewportKeepsSearchAndActionsUsable() {
        setQuery("eld")
        assertVisibleLabels(listOf("Elderberry"))
        onDrawer { host ->
            val search = host.findViewById<EditText>(R.id.drawer_search)
            val close = host.findViewById<View>(R.id.drawer_close)
            val clear = host.findViewById<View>(R.id.drawer_clear_search)
            assertTrue(search.requestFocus())
            val editor = EditorInfo()
            search.onCreateInputConnection(editor)
            assertTrue("The keyboard must leave the in-app editor and results visible",
                editor.imeOptions and EditorInfo.IME_FLAG_NO_FULLSCREEN != 0)
            fun layoutAboveKeyboard(heightDp: Int): Int {
                val visibleHeight = ceil(heightDp * host.resources.displayMetrics.density).toInt()
                host.applyContentInsets(Insets.of(0, 0, 0, host.height - visibleHeight))
                host.measure(
                    View.MeasureSpec.makeMeasureSpec(host.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(host.height, View.MeasureSpec.EXACTLY),
                )
                host.layout(host.left, host.top, host.right, host.bottom)
                return visibleHeight
            }
            val visibleHeight = layoutAboveKeyboard(86)

            fun bounds(view: View) = Rect().apply {
                view.getDrawingRect(this)
                host.offsetDescendantRectToMyCoords(view, this)
            }
            assertTrue("The search field lost its minimum height", search.height >= search.minimumHeight)
            assertTrue("Close overlaps search", !Rect.intersects(bounds(close), bounds(search)))
            assertTrue("Controls extend under the keyboard", maxOf(
                bounds(search).bottom, bounds(close).bottom, bounds(clear).bottom,
            ) <= visibleHeight)
            assertTrue("Search results have no viewport", host.findViewById<View>(R.id.drawer_list).height > 0)
            assertEquals("eld", search.text.toString())
            assertTrue(search.hasFocus())

            search.setText("noSuchVesnaAppZZ")
            val feedbackBottom = layoutAboveKeyboard(108)
            val noResults = host.findViewById<TextView>(R.id.drawer_no_results)
            assertTrue(noResults.isShown)
            assertTrue("The no-results message is clipped above the keyboard",
                noResults.layout.height <= noResults.height -
                    noResults.compoundPaddingTop - noResults.compoundPaddingBottom &&
                    bounds(noResults).bottom <= feedbackBottom)
            clear.performClick()
            assertEquals("", search.text.toString())
            assertTrue(search.hasFocus())
            host.applyContentInsets(Insets.NONE)
        }
        assertVisibleLabels(fixture.map { it.label })
    }

    private fun assertFirstDrawerRow(expected: List<String>, minWidth: Int = 0, focused: String? = null) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        var labels: List<String> = emptyList()
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            onDrawer { host ->
                val list = host.findViewById<RecyclerView>(R.id.drawer_list)
                val first = list.getChildAt(0) ?: return@onDrawer
                val row = (0 until list.childCount).map(list::getChildAt).takeWhile { it.top == first.top }
                labels = row.map { it.contentDescription.toString() }
                ready = labels == expected && row.all { it.width >= minWidth } &&
                    (focused == null || list.findFocus()?.contentDescription?.toString() == focused)
            }
            if (ready) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Expected row $expected with tiles at least $minWidth px wide, found $labels")
    }

    private fun assertFirstVisibleApp(expected: String, offset: Int) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        var actual: CharSequence? = null
        var actualOffset: Int? = null
        while (SystemClock.uptimeMillis() < deadline) {
            onDrawer { host ->
                val list = host.findViewById<RecyclerView>(R.id.drawer_list)
                val layout = list.layoutManager as GridLayoutManager
                val child = layout.findViewByPosition(layout.findFirstVisibleItemPosition())
                actual = child?.contentDescription
                actualOffset = child?.let { layout.getDecoratedTop(it) - list.paddingTop }
            }
            if (expected == actual && offset == actualOffset) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Expected $expected at offset $offset, found $actual at $actualOffset")
    }

    private fun assertAppOffset(label: String, offset: Int) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        var actual: Int? = null
        while (SystemClock.uptimeMillis() < deadline) {
            onDrawer { host ->
                val list = host.findViewById<RecyclerView>(R.id.drawer_list)
                val layout = list.layoutManager as GridLayoutManager
                val view = (0 until list.childCount).firstNotNullOfOrNull { index ->
                    list.getChildAt(index).takeIf { it.contentDescription == label }
                }
                actual = view?.let { layout.getDecoratedTop(it) - list.paddingTop }
            }
            if (actual == offset) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Expected $label at offset $offset after refresh, found it at $actual")
    }

    private fun swipeDown(list: RecyclerView, endAction: Int) {
        val start = SystemClock.uptimeMillis()
        val x = list.width * 0.5f
        fun dispatch(action: Int, y: Float) {
            val event = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action, x, y, 0)
            try {
                list.dispatchTouchEvent(event)
            } finally {
                event.recycle()
            }
        }
        dispatch(MotionEvent.ACTION_DOWN, list.height * 0.2f)
        dispatch(MotionEvent.ACTION_MOVE, list.height * 0.4f)
        dispatch(MotionEvent.ACTION_MOVE, list.height * 0.6f)
        SystemClock.sleep(2)
        dispatch(endAction, list.height * 0.7f)
    }

    private fun typeKey(host: View, keyCode: Int, metaState: Int = 0) {
        val time = SystemClock.uptimeMillis()
        host.dispatchKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
        host.dispatchKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0, metaState))
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
