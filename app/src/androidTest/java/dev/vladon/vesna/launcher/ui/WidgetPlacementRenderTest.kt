package dev.vladon.vesna.launcher.ui

import androidx.room.withTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.data.WidgetEntity
import dev.vladon.vesna.launcher.data.WorkspacePageEntity
import dev.vladon.vesna.launcher.workspace.CellContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The ENG-102 slice-2 render contract: a committed widget placement (a bound
 * host ID plus a Room row) renders a [WidgetCellView] with the live host view
 * inside the workspace after the Home binds the state.
 */
@RunWith(AndroidJUnit4::class)
class WidgetPlacementRenderTest {

    @Test
    fun committedWidgetRendersAsWorkspaceChild() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = VesnaApplication.graph(instrumentation.targetContext)
        val db = graph.database
        val pages = db.workspacePageDao()
        val widgetDao = db.widgetDao()
        val hostManager = dev.vladon.vesna.launcher.workspace.WidgetHostManager(
            instrumentation.targetContext, widgetDao,
        )
        hostManager.startListening()

        val pageId = db.withTransaction {
            pages.insert(WorkspacePageEntity(rank = pages.getAll().last().rank + 1))
        }
        val id = hostManager.allocateId()
        val manager = android.appwidget.AppWidgetManager.getInstance(instrumentation.targetContext)
        val provider = android.content.ComponentName(
            instrumentation.targetContext,
            dev.vladon.vesna.launcher.workspace.WidgetFixtureProvider::class.java,
        )
        // The bind needs BIND_APPWIDGET (the platform signature permission the
        // default launchers are granted) or the per-widget user dialog. When the
        // device has not granted the launcher, the render check is skipped here
        // and verified in the manual QA walkthrough instead (VESNA-QA-WIDGET-PLACEMENT).
        val granted = manager.bindAppWidgetIdIfAllowed(id, provider)
        org.junit.Assume.assumeTrue(
            "The device has not granted the launcher the widget-bind privilege", granted,
        )
        assertTrue("The widget must be bound after the grant flow", manager.getAppWidgetInfo(id) != null)
        widgetDao.insert(
            WidgetEntity(
                appWidgetId = id,
                providerPackage = provider.packageName,
                providerClass = provider.className,
                pageId = pageId,
                cellIndex = 0,
                spanX = 2,
                spanY = 2,
            ),
        )

        // The controller must project the widget onto the private page.
        val state = withTimeout(10_000) {
            graph.controller.state.first {
                it.activePageId == pageId || it.workspaceCells.values.any { cell -> cell is CellContent.WidgetCell }
            }
        }
        assertTrue("The widget row must project as a WidgetCell", state.workspaceCells.values.any { it is CellContent.WidgetCell })

        ActivityScenario.launch(dev.vladon.vesna.launcher.ui.HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val workspace = activity.findViewById<android.view.ViewGroup>(R.id.workspace)
                val widgetViews = (0 until workspace.childCount).map { workspace.getChildAt(it) }
                    .filterIsInstance<WidgetCellView>()
                assertTrue("The workspace must render a WidgetCellView", widgetViews.isNotEmpty())
                assertEquals("The host view must be installed", 1, widgetViews[0].childCount)
                assertTrue("The host view must be a platform host", widgetViews[0].getChildAt(0) is android.appwidget.AppWidgetHostView)
            }
        }

        // Cleanup: record first, then the binding (ADR order).
        widgetDao.all().filter { it.appWidgetId == id }.forEach { widgetDao.deleteById(it.id) }
        hostManager.deleteId(id)
        hostManager.stopListening()
        Unit
    }
}
