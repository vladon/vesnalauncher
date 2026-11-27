package dev.vladon.vesna.launcher.workspace

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import dev.vladon.vesna.launcher.data.WidgetDao
import dev.vladon.vesna.launcher.data.WidgetEntity

/**
 * The single widget host owned by Vesna (ADR-WIDGET-1): one stable host ID,
 * the ID allocation/release, the listening lifecycle tied to Home visibility,
 * and the startup reconciliation that removes orphaned host IDs without
 * touching committed placements.
 */
class WidgetHostManager(context: Context, private val widgetDao: WidgetDao) {

    companion object {
        /** The stable Vesna host ID (device-local; independent of page order). */
        const val HOST_ID = 1108
    }

    private val appContext = context.applicationContext
    /** The stable Vesna host ID (device-local; independent of page order). */
    private val host = AppWidgetHost(appContext, HOST_ID)
    private val manager = AppWidgetManager.getInstance(appContext)

    /** Starts delivering widget updates while Home is visible. */
    fun startListening() {
        host.startListening()
    }

    /** Stops delivering updates when Home is not visible. */
    fun stopListening() {
        host.stopListening()
    }

    /** Allocates a host ID for a pending placement (never commits a row). */
    fun allocateId(): Int = host.allocateAppWidgetId()

    /** Releases a host ID (cancellation or committed removal). */
    fun deleteId(appWidgetId: Int) {
        host.deleteAppWidgetId(appWidgetId)
    }

    /** True when Android still knows the provider for this binding. */
    fun isBound(appWidgetId: Int): Boolean =
        manager.getAppWidgetInfo(appWidgetId) != null

    /**
     * The startup reconciliation (ADR-WIDGET-1): removes unused host IDs
     * individually (never deleteAllHosts), then removes committed rows whose
     * bindings are confirmed lost — without touching the other rows.
     */
    suspend fun reconcile() {
        val committed = widgetDao.all()
        val committedIds = committed.map { it.appWidgetId }.toSet()
        val hostIds = host.appWidgetIds.toSet()

        for (id in hostIds - committedIds) {
            host.deleteAppWidgetId(id)
        }
        for (row in committed) {
            if (!hostIds.contains(row.appWidgetId)) {
                widgetDao.deleteById(row.id)
            }
        }
    }

    /** The host access for the diagnostics and the tests. */
    internal fun host(): AppWidgetHost = host
}
