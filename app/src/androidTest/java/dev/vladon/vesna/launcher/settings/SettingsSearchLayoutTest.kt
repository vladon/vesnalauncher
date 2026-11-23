package dev.vladon.vesna.launcher.settings

import android.app.UiAutomation
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.workspace.GridCanvas
import dev.vladon.vesna.launcher.workspace.GridResizeResult
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class SettingsSearchLayoutTest {
    @Test
    fun shortDirectoryKeepsSearchEditableAndRetainsQueryWhenItExpands() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = instrumentation.targetContext
        val minimumHeight = (56 * context.resources.displayMetrics.density).roundToInt()
        val expanded = mutableStateOf(false)
        val draft = mutableStateOf("")
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    VesnaTheme(dark = false) {
                        // Model the foreground space left by a docked keyboard, without
                        // depending on the height of the emulator's installed keyboard.
                        Box(Modifier.fillMaxWidth().height(if (expanded.value) 520.dp else 144.dp)) {
                            SettingsScreen(
                                page = SettingsPage.ROOT,
                                query = draft.value,
                                onQueryChange = { draft.value = it },
                                searchTarget = null,
                                onNavigate = { _, _ -> },
                                onResetInputFocus = {},
                                versionName = "",
                                defaultHome = DefaultHomeState.AVAILABLE,
                                onSetDefaultHome = {},
                                themeMode = ThemeMode.SYSTEM,
                                onThemeSelected = {},
                                showAppNames = true,
                                onShowAppNamesChanged = {},
                                layoutLocked = false,
                                onLayoutLockedChanged = {},
                                grid = GridCanvas.DEFAULT,
                                onApplyGridResize = { GridResizeResult.APPLIED },
                                pinNewApps = false,
                                onPinNewAppsChanged = {},
                                drawerLayoutMode = DrawerLayoutMode.GRID,
                                onDrawerLayoutModeSelected = {},
                                drawerApps = emptyList(),
                                hiddenDrawerApps = emptySet(),
                                onDrawerAppHiddenChanged = { _, _ -> },
                            )
                        }
                    }
                }
            }
            fun editor() = automation.rootInActiveWindow?.findVisible { it.isEditable }
            assertTrue("The Settings editor did not fit in the short viewport", until {
                val field = editor() ?: return@until false
                val bounds = Rect().also(field::getBoundsInScreen)
                bounds.height() >= minimumHeight
            })
            val query = context.getString(R.string.settings_theme)
            assertTrue("The short editor should accept an accessibility text action", until {
                editor()?.setQuery(query) == true
            })
            assertTrue("The short editor did not accept the query", until { editor()?.text?.toString() == query })
            scenario.onActivity { expanded.value = true }
            assertTrue("The query or matching entry disappeared after expansion", until {
                editor()?.text?.toString() == query && automation.rootInActiveWindow?.findVisible {
                    !it.isEditable && it.text?.toString() == query
                } != null
            })
        }
    }

    @Test
    fun tabStartsInSearchAndKeyboardClearReturnsToTheEditor() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val clearLabel = instrumentation.targetContext.getString(R.string.drawer_clear_search)
        ActivityScenario.launch(SettingsActivity::class.java).use {
            fun find(predicate: (AccessibilityNodeInfo) -> Boolean) =
                automation.rootInActiveWindow?.findVisible(predicate)

            assertTrue(until { find { it.isEditable } != null })
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
            assertTrue("First Tab should reach Settings search", until {
                find { it.isEditable && it.isFocused } != null
            })
            instrumentation.sendStringSync("theme")
            assertTrue(until { find { it.isEditable && it.text?.toString() == "theme" } != null })
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
            assertTrue("Tab from the editor should reach Clear search", until {
                find { node -> node.isFocused && node.findVisible { child ->
                    child.contentDescription?.toString() == clearLabel
                } != null } != null
            })
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
            assertTrue("Clear search should return keyboard focus to the empty editor", until {
                find { it.isEditable && it.isFocused && it.text.isNullOrEmpty() } != null
            })
            instrumentation.sendStringSync("about")
            assertTrue("Typing should continue without another Tab", until {
                find { it.isEditable && it.text?.toString() == "about" } != null
            })
        }
    }

    @Test
    fun queryAndMatchingRowsSurviveRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val query = instrumentation.targetContext.getString(R.string.settings_theme)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            fun editor() = automation.rootInActiveWindow?.findVisible { it.isEditable }
            assertTrue(until { editor() != null })
            assertTrue(editor()!!.setQuery(query))
            fun showsMatch() = editor()?.text?.toString() == query &&
                automation.rootInActiveWindow?.findVisible { !it.isEditable && it.text?.toString() == query } != null

            assertTrue(until { showsMatch() })
            scenario.recreate()
            assertTrue("Recreation should retain the query and its result", until { showsMatch() })
        }
    }

    @Test
    fun equivalentSearchQueriesKeepTheTypedTextAndOpenTheCorrectSetting() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            lateinit var query: String
            lateinit var theme: String
            lateinit var appearance: String
            lateinit var highlight: String
            scenario.onActivity { activity ->
                query = if (activity.resources.configuration.locales[0].language == "ru") {
                    "темная\u00a0  тема"
                } else {
                    "dark\u00a0  theme"
                }
                theme = activity.getString(R.string.settings_theme)
                appearance = activity.getString(R.string.settings_appearance)
                highlight = activity.getString(R.string.settings_search_match)
            }
            fun find(predicate: (AccessibilityNodeInfo) -> Boolean) =
                automation.rootInActiveWindow?.findVisible(predicate)
            fun editor() = find { it.isEditable }
            fun result() = find { !it.isEditable && it.text?.toString() == theme }

            assertTrue(until { editor() != null })
            assertTrue(editor()!!.setQuery(query))
            assertTrue("The editor should preserve the typed query", until { editor()?.text?.toString() == query })
            assertTrue("Settings should find $theme for the equivalent query \"$query\"", until { result() != null })
            val card = generateSequence(result()) { it.parent }.first { it.isClickable }
            assertTrue(card.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue("The result should open Appearance and highlight Theme", until {
                find { it.text?.toString() == appearance } != null &&
                    find { it.text?.toString() == highlight } != null
            })
        }
    }

    @Test
    fun highlightedResultSurvivesRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val query = instrumentation.targetContext.getString(R.string.settings_theme)
        val highlight = instrumentation.targetContext.getString(R.string.settings_search_match)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            fun find(predicate: (AccessibilityNodeInfo) -> Boolean) =
                automation.rootInActiveWindow?.findVisible(predicate)
            fun result() = find { !it.isEditable && it.text?.toString() == query }
            fun isHighlighted() = find { it.text?.toString() == highlight } != null

            assertTrue(until { find { it.isEditable } != null })
            assertTrue(find { it.isEditable }!!.setQuery(query))
            assertTrue(until { result() != null })
            val card = generateSequence(result()) { it.parent }.first { it.isClickable }
            assertTrue(card.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue("Opening the result should highlight the matching setting", until { isHighlighted() })
            scenario.recreate()
            assertTrue("Recreation should retain the highlighted setting", until { isHighlighted() })
        }
    }

    @Test
    fun settingsDirectoryKeepsScrollPositionAfterRestoration() {
        verifyRestoredScroll(SettingsPage.ROOT, target = null, anchor = R.string.settings_about_detail)
    }

    @Test
    fun highlightedPageKeepsTheReadersPositionAfterRestoration() {
        verifyRestoredScroll(SettingsPage.APPEARANCE, target = R.string.settings_theme, anchor = R.string.settings_show_app_names_detail)
    }

    @Test
    fun workspaceGridRowHidesUntilThePersistedGridLoads() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = instrumentation.targetContext
        val title = context.getString(R.string.settings_workspace_grid)
        val lock = context.getString(R.string.settings_lock_layout)
        val grid = mutableStateOf<GridCanvas?>(null)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            composeGridFixture(scenario, grid) { GridResizeResult.APPLIED }
            fun find(predicate: (AccessibilityNodeInfo) -> Boolean) =
                automation.rootInActiveWindow?.findVisible(predicate)
            assertTrue(until { find { it.text?.toString() == lock } != null })
            assertTrue("The row must wait for the persisted grid instead of flashing the default", until {
                find { it.text?.toString() == title } == null
            })
            scenario.onActivity { grid.value = GridCanvas.DEFAULT }
            assertTrue("The loaded grid should appear with its localized dimensions", until {
                find { it.text?.toString() == title } != null &&
                    find { it.text?.toString() == gridSummary(context, 4, 5) } != null
            })
        }
    }

    @Test
    fun workspaceGridEditorCancelKeepsTheCurrentGridWithoutResizing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = instrumentation.targetContext
        val title = context.getString(R.string.settings_workspace_grid)
        val hint = context.getString(R.string.settings_workspace_grid_editor_hint)
        val apply = context.getString(R.string.settings_workspace_grid_apply)
        val cancel = context.getString(R.string.settings_workspace_grid_cancel)
        val summary = gridSummary(context, 4, 5)
        val grid = mutableStateOf<GridCanvas?>(GridCanvas.DEFAULT)
        val submissions = AtomicInteger(0)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            composeGridFixture(scenario, grid) { submissions.incrementAndGet(); GridResizeResult.APPLIED }
            fun find(predicate: (AccessibilityNodeInfo) -> Boolean) =
                automation.rootInActiveWindow?.findVisible(predicate)
            fun clickable(text: String) = find { it.text?.toString() == text }
                ?.let { node -> generateSequence(node) { it.parent }.firstOrNull { it.isClickable } }

            assertTrue(until { clickable(title) != null })
            assertTrue(clickable(title)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue("Choosing the row should open the choice editor", until {
                find { it.text?.toString() == hint } != null
            })
            val column = reveal(automation) { clickable("2") }
            assertTrue("The column choices should include 2", column != null)
            assertTrue(column!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            val discard = reveal(automation) { clickable(cancel) }
            assertTrue("Cancel should stay reachable while scrolled", discard != null)
            assertTrue(discard!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue("Cancel should close the editor", until { clickable(apply) == null })
            assertTrue("Cancel must not submit a resize", submissions.get() == 0)
            assertTrue("Cancel should keep the current dimensions", until {
                find { it.text?.toString() == summary } != null
            })
        }
    }

    @Test
    fun workspaceGridEditorKeyboardApplyReportsRejectionInPlace() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = instrumentation.targetContext
        val title = context.getString(R.string.settings_workspace_grid)
        val hint = context.getString(R.string.settings_workspace_grid_editor_hint)
        val apply = context.getString(R.string.settings_workspace_grid_apply)
        val failure = context.getString(R.string.settings_workspace_grid_too_many_items)
        val summary = gridSummary(context, 4, 5)
        val grid = mutableStateOf<GridCanvas?>(GridCanvas.DEFAULT)
        val submissions = AtomicInteger(0)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            composeGridFixture(scenario, grid) { submissions.incrementAndGet(); GridResizeResult.TOO_MANY_ITEMS }
            fun find(predicate: (AccessibilityNodeInfo) -> Boolean) =
                automation.rootInActiveWindow?.findVisible(predicate)
            fun clickable(text: String) = find { it.text?.toString() == text }
                ?.let { node -> generateSequence(node) { it.parent }.firstOrNull { it.isClickable } }

            assertTrue(until { clickable(title) != null })
            assertTrue(clickable(title)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue(until { find { it.text?.toString() == hint } != null })
            val button = reveal(automation) { clickable(apply) }
            assertTrue("Apply should stay reachable in a short viewport", button != null)
            assertTrue(button!!.performAction(AccessibilityNodeInfo.ACTION_FOCUS))
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
            assertTrue("Enter on the focused Apply should submit the draft", until { submissions.get() == 1 })
            assertTrue("A rejected grid should report in place and leave the editor open", reveal(automation) {
                find { it.text?.toString() == failure }
            } != null)
            assertTrue("A rejection must not change the current dimensions",
                reveal(automation, forward = false) {
                    find { it.text?.toString() == summary }
                } != null)
        }
    }

    @Test
    fun workspaceGridApplyGuardsAgainstDuplicateSubmits() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = instrumentation.targetContext
        val title = context.getString(R.string.settings_workspace_grid)
        val hint = context.getString(R.string.settings_workspace_grid_editor_hint)
        val apply = context.getString(R.string.settings_workspace_grid_apply)
        val summary = gridSummary(context, 4, 5)
        val grid = mutableStateOf<GridCanvas?>(GridCanvas.DEFAULT)
        val submissions = AtomicInteger(0)
        // The fake suspends (never blocks) on the gate, so the window keeps
        // answering accessibility queries while one resize is in flight.
        val gate = CompletableDeferred<Unit>()
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            composeGridFixture(scenario, grid) {
                submissions.incrementAndGet()
                gate.await()
                GridResizeResult.APPLIED
            }
            fun find(predicate: (AccessibilityNodeInfo) -> Boolean) =
                automation.rootInActiveWindow?.findVisible(predicate)
            fun clickable(text: String) = find { it.text?.toString() == text }
                ?.let { node -> generateSequence(node) { it.parent }.firstOrNull { it.isClickable } }

            assertTrue(until { clickable(title) != null })
            assertTrue(clickable(title)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue(until { find { it.text?.toString() == hint } != null })
            val button = reveal(automation) { clickable(apply) }
            assertTrue(button != null)
            assertTrue(button!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue("The resize should start exactly once", until { submissions.get() == 1 })
            assertTrue("Apply should disable while a resize is pending", until {
                clickable(apply)?.isEnabled == false
            })
            gate.complete(Unit)
            assertTrue("A successful resize should close the editor", until { clickable(apply) == null })
            assertTrue("A disabled Apply must not submit again", submissions.get() == 1)
            assertTrue(until { find { it.text?.toString() == summary } != null })
        }
    }

    private fun verifyRestoredScroll(page: SettingsPage, target: Int?, anchor: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val title = instrumentation.targetContext.getString(anchor)
        val registry = mutableStateOf(SaveableStateRegistry(null) { true })
        val visible = mutableStateOf(true)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    // Recreate the composition at a fixed short height with the values Android saves.
                    if (visible.value) {
                        CompositionLocalProvider(LocalSaveableStateRegistry provides registry.value) {
                            VesnaTheme(dark = false) {
                                Box(Modifier.fillMaxWidth().height(224.dp)) {
                                    SettingsScreen(
                                        page = page,
                                        query = "",
                                        onQueryChange = {},
                                        searchTarget = target,
                                        onNavigate = { _, _ -> },
                                        onResetInputFocus = {},
                                        versionName = "",
                                        defaultHome = DefaultHomeState.AVAILABLE,
                                        onSetDefaultHome = {},
                                        themeMode = ThemeMode.SYSTEM,
                                        onThemeSelected = {},
                                        showAppNames = true,
                                        onShowAppNamesChanged = {},
                                        layoutLocked = false,
                                        onLayoutLockedChanged = {},
                                        grid = GridCanvas.DEFAULT,
                                        onApplyGridResize = { GridResizeResult.APPLIED },
                                        pinNewApps = false,
                                        onPinNewAppsChanged = {},
                                        drawerLayoutMode = DrawerLayoutMode.GRID,
                                        onDrawerLayoutModeSelected = {},
                                        drawerApps = emptyList(),
                                        hiddenDrawerApps = emptySet(),
                                        onDrawerAppHiddenChanged = { _, _ -> },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            fun scroller() = automation.rootInActiveWindow?.findVisible {
                it.isScrollable && AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD in it.actionList
            }
            fun anchorBounds() = automation.rootInActiveWindow
                ?.findVisible { it.text?.lineSequence()?.any { line -> line == title } == true }
                ?.let { Rect().also(it::getBoundsInScreen) }

            assertTrue("The fixture must have more Settings content below the viewport", until { scroller() != null })
            automation.waitForIdle(250, 4_000)
            val initialBounds = anchorBounds()
            for (step in 0 until 10) {
                val region = scroller() ?: break
                val bounds = Rect().also(region::getBoundsInScreen)
                val x = bounds.centerX()
                val margin = bounds.height() / 8
                val swipe = "input swipe $x ${bounds.bottom - margin} $x ${bounds.top + margin} 300"
                ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(swipe)).use { it.readBytes() }
                automation.waitForIdle(300, 4_000)
            }
            assertTrue("The Settings list must be at its scroll limit before saving", until { scroller() == null })
            val scrolledBounds = requireNotNull(anchorBounds()) { "Scrolling should reveal $title" }
            assertTrue("Scrolling must move the anchor before saving", scrolledBounds != initialBounds)

            scenario.onActivity {
                registry.value = SaveableStateRegistry(registry.value.performSave()) { true }
                visible.value = false
            }
            assertTrue("The initial composition must leave the window", until { anchorBounds() == null })
            scenario.onActivity { visible.value = true }
            automation.waitForIdle(250, 4_000)
            val restored = until { anchorBounds() == scrolledBounds }
            assertTrue("Settings should restore the scrolled position on $page: expected $scrolledBounds, got ${anchorBounds()}", restored)
        }
    }

    private fun AccessibilityNodeInfo.setQuery(query: String) =
        performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
        })

    private fun AccessibilityNodeInfo.findVisible(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (isVisibleToUser && predicate(this)) return this
        for (index in 0 until childCount) getChild(index)?.findVisible(predicate)?.let { return it }
        return null
    }

    private fun until(condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            if (condition()) return true
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    /** Composes only the Home screen page with a caller-controlled grid and resize callback. */
    private fun composeGridFixture(
        scenario: ActivityScenario<SettingsActivity>,
        grid: MutableState<GridCanvas?>,
        resize: suspend (GridCanvas) -> GridResizeResult,
    ) {
        scenario.onActivity { activity ->
            activity.setContent {
                VesnaTheme(dark = false) {
                    Box(Modifier.fillMaxWidth().height(480.dp)) {
                        SettingsScreen(
                            page = SettingsPage.HOME_SCREEN,
                            query = "",
                            onQueryChange = {},
                            searchTarget = null,
                            onNavigate = { _, _ -> },
                            onResetInputFocus = {},
                            versionName = "",
                            defaultHome = DefaultHomeState.AVAILABLE,
                            onSetDefaultHome = {},
                            themeMode = ThemeMode.SYSTEM,
                            onThemeSelected = {},
                            showAppNames = true,
                            onShowAppNamesChanged = {},
                            layoutLocked = false,
                            onLayoutLockedChanged = {},
                            grid = grid.value,
                            onApplyGridResize = resize,
                            pinNewApps = false,
                            onPinNewAppsChanged = {},
                            drawerLayoutMode = DrawerLayoutMode.GRID,
                            onDrawerLayoutModeSelected = {},
                            drawerApps = emptyList(),
                            hiddenDrawerApps = emptySet(),
                            onDrawerAppHiddenChanged = { _, _ -> },
                        )
                    }
                }
            }
        }
    }

    private fun gridSummary(context: Context, columns: Int, rows: Int): String {
        val resources = context.resources
        return resources.getString(
            R.string.settings_workspace_grid_summary_fmt,
            resources.getQuantityString(R.plurals.workspace_grid_columns, columns, columns),
            resources.getQuantityString(R.plurals.workspace_grid_rows, rows, rows),
        )
    }

    /** Scrolls the page until the node appears, or gives up after several steps. */
    private fun reveal(
        automation: UiAutomation,
        forward: Boolean = true,
        find: () -> AccessibilityNodeInfo?,
    ): AccessibilityNodeInfo? {
        var attempts = 0
        while (true) {
            find()?.let { return it }
            val scrollArea = automation.rootInActiveWindow?.findVisible { it.isScrollable } ?: return null
            if (attempts++ >= 8) return null
            val bounds = Rect().also(scrollArea::getBoundsInScreen)
            val x = bounds.centerX()
            val margin = bounds.height() / 5
            val lower = bounds.bottom - margin
            val upper = bounds.top + margin
            val start = if (forward) lower else upper
            val end = if (forward) upper else lower
            ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("input swipe $x $start $x $end 300"),
            ).use { it.readBytes() }
            automation.waitForIdle(250, 4_000)
        }
    }
}
