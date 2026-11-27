package dev.vladon.vesna.launcher.workspace

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.data.WidgetEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The ADR-WIDGET-1 contract checks on the real host: the allocate/release
 * mechanics, the reconciliation of orphaned host IDs, and the committed
 * record surviving a reconcile (the ordering guarantees).
 */
@RunWith(AndroidJUnit4::class)
class WidgetHostLifecycleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = AppWidgetManager.getInstance(context)
    private val hostManager = WidgetHostManager(context, dev.vladon.vesna.launcher.VesnaApplication.graph(context).widgetDao)

    private fun committedRows(): List<WidgetEntity> = runBlocking {
        dev.vladon.vesna.launcher.VesnaApplication.graph(context).widgetDao.all()
    }

    @Test
    fun allocateGivesFreshIdAndDeleteRemovesIt() = runBlocking {
        val before = hostManager.host().appWidgetIds.toList()
        val id = hostManager.host().allocateAppWidgetId()
        assertNotEquals("Allocated ID must be fresh", 0, id)
        assertTrue("Allocated ID must not collide with the pre-existing set", id !in before)
        assertTrue("Host must hold the allocated ID", id in hostManager.host().appWidgetIds.toList())
        hostManager.host().deleteAppWidgetId(id)
        assertTrue("Deleted ID must leave the host set", id !in hostManager.host().appWidgetIds.toList())
    }

    @Test
    fun reconcileRemovesOrphanedHostIdWithoutRow() = runBlocking {
        val dao = dev.vladon.vesna.launcher.VesnaApplication.graph(context).widgetDao
        val boundId = hostManager.host().allocateAppWidgetId()
        val rowId = dao.insert(
            WidgetEntity(
                appWidgetId = boundId,
                providerPackage = "dev.vladon.vesna.launcher",
                providerClass = "dev.vladon.vesna.launcher.workspace.WidgetFixtureProvider",
                pageId = 1,
                cellIndex = 0,
            ),
        )
        val orphanId = hostManager.host().allocateAppWidgetId()
        hostManager.reconcile()
        assertTrue("Orphaned host ID (no committed row) must be reconciled away", orphanId !in hostManager.host().appWidgetIds.toList())
        assertTrue("Reconcile must not touch the committed row", committedRows().any { it.id == rowId })
        assertTrue("Reconcile must keep the bound host ID", boundId in hostManager.host().appWidgetIds.toList())
        // Cleanup.
        dao.deleteById(rowId)
        hostManager.host().deleteAppWidgetId(boundId)
    }

    @Test
    fun committedRowSurvivesReconcile() = runBlocking {
        val id = hostManager.host().allocateAppWidgetId()
        val dao = dev.vladon.vesna.launcher.VesnaApplication.graph(context).widgetDao
        val rowId = dao.insert(
            WidgetEntity(
                appWidgetId = id,
                providerPackage = "dev.vladon.vesna.launcher",
                providerClass = "dev.vladon.vesna.launcher.workspace.WidgetFixtureProvider",
                pageId = 1,
                cellIndex = 0,
            ),
        )
        hostManager.reconcile()
        assertTrue("A committed row must survive the reconcile", committedRows().any { it.id == rowId })
        assertEquals("The committed host ID must stay bound", id, hostManager.host().appWidgetIds.firstOrNull { it == id } ?: -1)
        // Cleanup.
        dao.deleteById(rowId)
        hostManager.host().deleteAppWidgetId(id)
    }

    @Test
    fun deletionOrderIsRecordFirstThenBinding() = runBlocking {
        val id = hostManager.host().allocateAppWidgetId()
        val dao = dev.vladon.vesna.launcher.VesnaApplication.graph(context).widgetDao
        val rowId = dao.insert(
            WidgetEntity(
                appWidgetId = id,
                providerPackage = "dev.vladon.vesna.launcher",
                providerClass = "dev.vladon.vesna.launcher.workspace.WidgetFixtureProvider",
                pageId = 1,
                cellIndex = 0,
            ),
        )
        // The committed-removal ordering: the Room record first, then the binding.
        dao.deleteById(rowId)
        assertTrue("The record must be gone before the binding release", committedRows().none { it.appWidgetId == id })
        hostManager.host().deleteAppWidgetId(id)
        assertTrue("The binding must be released last", id !in hostManager.host().appWidgetIds.toList())
    }
}
