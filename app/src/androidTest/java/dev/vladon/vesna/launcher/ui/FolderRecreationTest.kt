package dev.vladon.vesna.launcher.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.KeyEvent
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
        val initial = withTimeout(10_000) { controller.state.first { it.pageIds.isNotEmpty() && it.apps.size >= 2 } }
        val apps = initial.apps.take(2)
        val title = "QA folder continuity"
        val db = graph.database
        val pages = db.workspacePageDao()
        val folders = db.folderDao()
        val items = db.workspaceItemDao()
        val (pageId, folderId, memberIds) = db.withTransaction {
            // A private page keeps this independent of the contents and capacity of the selected page.
            val pageId = pages.insert(WorkspacePageEntity(rank = pages.getAll().last().rank + 1))
            val folderId = folders.insert(FolderEntity(title = title, cellIndex = 0, pageId = pageId))
            val memberIds = apps.mapIndexed { index, app ->
                items.insert(WorkspaceItemEntity(
                    packageName = app.key.packageName,
                    className = app.key.className,
                    userSerial = app.key.userSerial,
                    container = Container.WORKSPACE.name,
                    cellIndex = index,
                    folderId = folderId,
                ))
            }
            Triple(pageId, folderId, memberIds)
        }

        fun focusedRoot() = automation.windows.firstOrNull { it.isFocused }?.root

        fun folderIsOpen(): Boolean {
            val root = focusedRoot() ?: return false
            return root.hasVisibleNode { it.text?.toString() == title } && apps.all { app ->
                root.hasVisibleNode { it.contentDescription?.toString() == app.accessibilityLabel }
            }
        }

        fun homeIsVisible(): Boolean = focusedRoot()?.hasVisibleNode {
            it.contentDescription?.toString() == title
        } == true

        val serviceInfo = automation.serviceInfo
        val previousFlags = serviceInfo.flags
        serviceInfo.flags = previousFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        try {
            automation.serviceInfo = serviceInfo
            controller.selectWorkspacePage(pageId)
            withTimeout(10_000) { controller.state.first { it.activePageId == pageId } }
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                await("Fixture Home should render", ::homeIsVisible)
                scenario.onActivity { activity ->
                    val workspace = activity.findViewById<WorkspaceView>(R.id.workspace)
                    val folder = (0 until workspace.childCount).map(workspace::getChildAt)
                        .single { it.contentDescription?.toString() == title }
                    assertTrue(folder.performClick())
                }
                await("The fixture folder should open", ::folderIsOpen)
                scenario.recreate()
                await("The open folder and its members should survive recreation", ::folderIsOpen)

                ParcelFileDescriptor.AutoCloseInputStream(
                    automation.executeShellCommand("input keyevent ${KeyEvent.KEYCODE_BACK}"),
                ).use { it.readBytes() }
                await("Back should dismiss the folder", ::homeIsVisible)
                scenario.recreate()
                await("An intentionally dismissed folder should stay closed", ::homeIsVisible)
            }
        } finally {
            serviceInfo.flags = previousFlags
            automation.serviceInfo = serviceInfo
            controller.selectWorkspacePage(initial.activePageId)
            db.withTransaction {
                memberIds.forEach { items.deleteById(it) }
                folders.deleteById(folderId)
                pages.deleteIfEmpty(pageId)
            }
            withTimeout(10_000) {
                controller.state.first { it.activePageId == initial.activePageId && pageId !in it.pageIds }
            }
        }
    }

    private fun AccessibilityNodeInfo.hasVisibleNode(predicate: (AccessibilityNodeInfo) -> Boolean): Boolean {
        if (isVisibleToUser && predicate(this)) return true
        for (index in 0 until childCount) if (getChild(index)?.hasVisibleNode(predicate) == true) return true
        return false
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
