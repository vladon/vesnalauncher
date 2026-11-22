package dev.vladon.vesna.launcher.ui

import android.os.SystemClock
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.room.withTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import dev.vladon.vesna.launcher.data.WorkspacePageEntity
import dev.vladon.vesna.launcher.workspace.Container
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeShortcutRecreationTest {
    @Test
    fun keyboardFocusReturnsToTheSameWorkspaceCopy() = verifyShortcutFocus(Container.WORKSPACE, R.id.workspace)

    @Test
    fun keyboardFocusReturnsToTheSameDockCopy() = verifyShortcutFocus(Container.DOCK, R.id.dock)

    private fun verifyShortcutFocus(container: Container, groupId: Int) = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = VesnaApplication.graph(instrumentation.targetContext)
        val controller = graph.controller
        val db = graph.database
        val items = db.workspaceItemDao()
        val pages = db.workspacePageDao()
        val initial = withTimeout(10_000) { controller.state.first { it.pageIds.isNotEmpty() && it.apps.isNotEmpty() } }
        val app = initial.apps.first()
        val (pageId, pinIds, savedDock) = db.withTransaction {
            val pageId = pages.insert(WorkspacePageEntity(rank = pages.getAll().last().rank + 1))
            val savedDock = if (container == Container.DOCK) {
                items.getAll().filter { it.container == Container.DOCK.name && it.folderId == null }
            } else {
                emptyList()
            }
            // Move existing dock rows out of the way without replacing their pin identities.
            savedDock.forEachIndexed { index, item ->
                items.updatePlacement(item.id, Container.WORKSPACE.name, index + 8, pageId)
            }
            val pinPage = if (container == Container.WORKSPACE) pageId else null
            val pins = List(2) { index -> items.insert(pin(app.key, container, index, pinPage)) }
            Triple(pageId, pins, savedDock)
        }
        try {
            controller.selectWorkspacePage(pageId)
            withTimeout(10_000) {
                controller.state.first { state ->
                    state.activePageId == pageId && (container != Container.DOCK || state.dock.pins.map { it.id } == pinIds)
                }
            }
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                fun fixtureRendered(group: ViewGroup) = group.childCount == 2 && (0..1).all {
                    group.getChildAt(it).contentDescription?.toString() == app.accessibilityLabel
                }
                awaitGroup(scenario, groupId, "Both copies must render", ::fixtureRendered)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
                scenario.onActivity { activity ->
                    val group = activity.findViewById<ViewGroup>(groupId)
                    assertTrue("The second copy must accept keyboard focus", group.getChildAt(1).requestFocus())
                }
                scenario.recreate()
                awaitGroup(scenario, groupId, "Keyboard focus should return to the same $container copy") { group ->
                    fixtureRendered(group) && group.getChildAt(1).isFocused
                }
            }
        } finally {
            controller.selectWorkspacePage(initial.activePageId)
            db.withTransaction {
                pinIds.forEach { items.deleteById(it) }
                savedDock.forEach { items.updatePlacement(it.id, it.container, it.cellIndex, it.pageId) }
                pages.deleteIfEmpty(pageId)
            }
            withTimeout(10_000) {
                controller.state.first { it.activePageId == initial.activePageId && pageId !in it.pageIds }
            }
        }
    }

    private fun pin(key: AppKey, container: Container, cell: Int, pageId: Long?) = WorkspaceItemEntity(
        packageName = key.packageName,
        className = key.className,
        userSerial = key.userSerial,
        container = container.name,
        cellIndex = cell,
        pageId = pageId,
    )

    private fun awaitGroup(
        scenario: ActivityScenario<HomeActivity>,
        groupId: Int,
        message: String,
        condition: (ViewGroup) -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            var matches = false
            scenario.onActivity { matches = condition(it.findViewById(groupId)) }
            if (matches) return
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError(message)
    }
}
