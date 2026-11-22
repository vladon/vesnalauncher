package dev.vladon.vesna.launcher.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.withTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.data.FolderEntity
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
class FolderRecreationTest {
    @Test
    fun openFolderSurvivesRecreationButDismissedFolderDoesNotReopen() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val graph = VesnaApplication.graph(instrumentation.targetContext)
        val controller = graph.controller
        val initial = withTimeout(10_000) {
            controller.state.first { it.pageIds.isNotEmpty() && it.apps.distinctBy { app -> app.accessibilityLabel }.size >= 3 }
        }
        val apps = initial.apps.distinctBy { it.accessibilityLabel }.take(3)
        val members = listOf(apps[0]) + List(40) { apps[1] } + apps[2]
        val title = "QA folder continuity"
        val db = graph.database
        val pages = db.workspacePageDao()
        val folders = db.folderDao()
        val items = db.workspaceItemDao()
        val (pageId, folderId, pinIds) = db.withTransaction {
            // A private page keeps this independent of the contents and capacity of the selected page.
            val pageId = pages.insert(WorkspacePageEntity(rank = pages.getAll().last().rank + 1))
            // Give default Home focus a different target from the invoking folder.
            val app = apps[1]
            val leadingPinId = items.insert(WorkspaceItemEntity(
                packageName = app.key.packageName,
                className = app.key.className,
                userSerial = app.key.userSerial,
                container = Container.WORKSPACE.name,
                cellIndex = 0,
                pageId = pageId,
            ))
            val folderId = folders.insert(FolderEntity(title = title, cellIndex = 1, pageId = pageId))
            val memberIds = members.mapIndexed { index, app ->
                items.insert(WorkspaceItemEntity(
                    packageName = app.key.packageName,
                    className = app.key.className,
                    userSerial = app.key.userSerial,
                    container = Container.WORKSPACE.name,
                    cellIndex = index,
                    folderId = folderId,
                ))
            }
            Triple(pageId, folderId, memberIds + leadingPinId)
        }

        fun focusedRoot() = automation.windows.firstOrNull { it.isFocused }?.root

        fun folderIsOpen(): Boolean = focusedRoot()?.findVisible { it.text?.toString() == title } != null

        fun member(label: String) = focusedRoot()?.findVisible { it.contentDescription?.toString() == label }

        fun memberIsVisible(label: String) = member(label) != null

        fun sendKey(keyCode: Int) {
            ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("input keyevent $keyCode"),
            ).use { it.readBytes() }
        }

        fun homeFolder() = focusedRoot()?.findVisible { it.contentDescription?.toString() == title }

        fun homeIsVisible(): Boolean = homeFolder() != null

        val serviceInfo = automation.serviceInfo
        val previousFlags = serviceInfo.flags
        serviceInfo.flags = previousFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        try {
            automation.serviceInfo = serviceInfo
            controller.selectWorkspacePage(pageId)
            withTimeout(10_000) { controller.state.first { it.activePageId == pageId } }
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                await("Fixture Home should render", ::homeIsVisible)
                fun openFolder() {
                    sendKey(KeyEvent.KEYCODE_TAB)
                    scenario.onActivity { activity ->
                        val workspace = activity.findViewById<WorkspaceView>(R.id.workspace)
                        val folder = (0 until workspace.childCount).map(workspace::getChildAt)
                            .single { it.contentDescription?.toString() == title }
                        assertTrue("The invoking folder must accept keyboard focus", folder.requestFocus())
                    }
                    sendKey(KeyEvent.KEYCODE_ENTER)
                }
                openFolder()
                val first = apps.first().accessibilityLabel
                val last = apps.last().accessibilityLabel
                await("The fixture folder should open at the start") { folderIsOpen() && memberIsVisible(first) }
                sendKey(KeyEvent.KEYCODE_TAB)
                for (step in members.indices) {
                    if (memberIsVisible(last)) break
                    val grid = requireNotNull(focusedRoot()?.findVisible {
                        it.viewIdResourceName?.endsWith(":id/folder_list") == true
                    })
                    automation.executeAndWaitForEvent(
                        { assertTrue(grid.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) },
                        { it.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED },
                        3_000,
                    ).recycle()
                    automation.waitForIdle(150, 3_000)
                }
                assertTrue("The last row must be visible before recreation", memberIsVisible(last) && !memberIsVisible(first))
                assertTrue(requireNotNull(member(last)).performAction(AccessibilityNodeInfo.ACTION_FOCUS))
                await("The last member must have keyboard focus before recreation") { member(last)?.isFocused == true }
                scenario.recreate()
                await("The open folder should retain its scrolled row after recreation") {
                    folderIsOpen() && memberIsVisible(last) && !memberIsVisible(first)
                }
                await("Keyboard focus should return to the same member after recreation") { member(last)?.isFocused == true }

                sendKey(KeyEvent.KEYCODE_BACK)
                await("Back should dismiss the folder", ::homeIsVisible)
                await("Keyboard focus should return to the invoking Home folder after dismissal") {
                    homeFolder()?.isFocused == true
                }
                scenario.recreate()
                await("An intentionally dismissed folder should stay closed", ::homeIsVisible)
                openFolder()
                await("A newly opened folder should start at the first row") {
                    folderIsOpen() && memberIsVisible(first) && !memberIsVisible(last)
                }
            }
        } finally {
            serviceInfo.flags = previousFlags
            automation.serviceInfo = serviceInfo
            controller.selectWorkspacePage(initial.activePageId)
            db.withTransaction {
                pinIds.forEach { items.deleteById(it) }
                folders.deleteById(folderId)
                pages.deleteIfEmpty(pageId)
            }
            withTimeout(10_000) {
                controller.state.first { it.activePageId == initial.activePageId && pageId !in it.pageIds }
            }
        }
    }

    private fun AccessibilityNodeInfo.findVisible(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (isVisibleToUser && predicate(this)) return this
        for (index in 0 until childCount) getChild(index)?.findVisible(predicate)?.let { return it }
        return null
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            if (condition()) return
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError(message)
    }
}
