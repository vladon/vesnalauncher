package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.GridCanvas
import kotlin.math.max

/**
 * The workspace: a fixed 4x5 grid of [AppIconView] children, one per occupied cell.
 * Cell geometry comes from [GridCanvas]; taps launch apps, long presses open item
 * menus, and a long press on empty space opens the launcher menu.
 */
class WorkspaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    var onAppClick: ((LauncherApp) -> Unit)? = null
    var onAppLongClick: ((View, LauncherApp) -> Boolean)? = null
    var onEmptyAreaLongClick: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val gutterPx = (8 * density).toInt()
    private val cellHPadPx = (4 * density).toInt()
    private val cellVPadPx = (4 * density).toInt()

    init {
        // Long press on any empty part of the grid.
        isLongClickable = true
        setOnLongClickListener {
            onEmptyAreaLongClick?.invoke()
            true
        }
    }

    /** Reconciles children with the current cell-to-app mapping. */
    fun bind(cells: Map<Int, LauncherApp>) {
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            val cell = child.tag as? Int
            if (cell == null || cell !in cells) removeViewAt(i)
        }
        for ((cell, app) in cells) {
            val existing = findChildForCell(cell)
            val view = existing ?: AppIconView(context).also {
                it.tag = cell
                addView(it)
            }
            view.bind(app)
            view.setOnClickListener { onAppClick?.invoke(app) }
            view.setOnLongClickListener { anchor ->
                // A handled child long press suppresses the empty-area menu.
                onAppLongClick?.invoke(anchor, app) ?: true
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = max(suggestedMinimumWidth, getDefaultSize(suggestedMinimumWidth, widthMeasureSpec))
        val height = max(suggestedMinimumHeight, getDefaultSize(suggestedMinimumHeight, heightMeasureSpec))
        setMeasuredDimension(width, height)

        val cellW = cellWidth(width)
        val cellH = cellHeight(height)
        val childW = cellW - 2 * cellHPadPx
        val childH = cellH - 2 * cellVPadPx
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(
                MeasureSpec.makeMeasureSpec(childW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childH, MeasureSpec.EXACTLY),
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        val cellW = cellWidth(width)
        val cellH = cellHeight(height)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val cell = child.tag as? Int ?: continue
            val x = paddingLeft + gutterPx + GridCanvas.columnOf(cell) * cellW + cellHPadPx
            val y = paddingTop + gutterPx + GridCanvas.rowOf(cell) * cellH + cellVPadPx
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
        }
    }

    private fun findChildForCell(cell: Int): AppIconView? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if ((child.tag as? Int) == cell) return child as AppIconView
        }
        return null
    }

    private fun cellWidth(totalWidth: Int): Int =
        (totalWidth - paddingLeft - paddingRight - 2 * gutterPx) / GridCanvas.COLUMNS

    private fun cellHeight(totalHeight: Int): Int =
        (totalHeight - paddingTop - paddingBottom - 2 * gutterPx) / GridCanvas.ROWS

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
}
