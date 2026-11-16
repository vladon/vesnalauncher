package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.GridCanvas
import kotlin.math.abs
import kotlin.math.max

/**
 * The workspace: a fixed 4x5 grid of [AppIconView] children, one per occupied cell.
 * Cell geometry comes from [GridCanvas]; taps launch apps, long presses open item
 * menus, a long press on empty space opens the launcher menu, and an upward fling
 * anywhere opens the drawer.
 */
class WorkspaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    var onAppClick: ((LauncherApp) -> Unit)? = null
    var onAppLongClick: ((View, LauncherApp) -> Boolean)? = null
    var onEmptyAreaLongClick: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null

    /** Persisted after a drag-and-drop: move the app to the target cell (or swap). */
    var onItemMove: ((LauncherApp, Int) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val gutterPx = (8 * density).toInt()
    private val cellHPadPx = (4 * density).toInt()
    private val cellVPadPx = (4 * density).toInt()

    // Active drag-and-drop state (see beginDrag). While a drag is running the grid
    // intercepts the gesture and the swipe-up detector is bypassed.
    private var dragSourceCell = -1
    private var dragSourceApp: LauncherApp? = null
    private var dragAnchor: View? = null
    private var dragTargetCell: Int? = null
    private var dragMoved = false
    private var lastCellW = 0
    private var lastCellH = 0

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = HIGHLIGHT_COLOR
        style = Paint.Style.FILL
    }

    // Observes the gesture stream without stealing touches from icon children.
    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                val dx = abs(e2.x - (e1?.x ?: e2.x))
                if (velocityY < -FLING_VELOCITY && dx < MAX_FLING_DRIFT_DP * density) {
                    onSwipeUp?.invoke()
                    return true
                }
                return false
            }
        },
    )

    init {
        // Long press on any empty part of the grid.
        isLongClickable = true
        setOnLongClickListener {
            onEmptyAreaLongClick?.invoke()
            true
        }
    }

    // Touch-event plumbing: a child (icon) holding the gesture routes MOVE/UP through
    // onInterceptTouchEvent, while empty-area gestures (no child target) go straight
    // to onTouchEvent. Feeding the detector from both covers flings starting anywhere
    // without double-feeding any single event.
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (dragSourceApp != null) {
            // An active drag takes the gesture over from the icon (which receives
            // ACTION_CANCEL); subsequent events arrive in onTouchEvent.
            return true
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> gestureDetector.onTouchEvent(ev)
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (dragSourceApp != null) {
            handleDragTouch(ev)
            return true
        }
        gestureDetector.onTouchEvent(ev)
        return super.onTouchEvent(ev)
    }

    /** Starts a drag for the item at [cell]; the item dims until it is dropped. */
    private fun beginDrag(cell: Int, app: LauncherApp, anchor: View) {
        dragSourceCell = cell
        dragSourceApp = app
        dragAnchor = anchor
        dragMoved = false
        dragTargetCell = null
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        anchor.alpha = DRAGGED_ALPHA
    }

    private fun handleDragTouch(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                dragMoved = true
                val cell = GridCanvas.cellAtPoint(
                    ev.x,
                    ev.y,
                    paddingLeft + gutterPx.toFloat(),
                    paddingTop + gutterPx.toFloat(),
                    lastCellW.toFloat(),
                    lastCellH.toFloat(),
                )
                if (cell != dragTargetCell) {
                    dragTargetCell = cell
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> finishDrag(cancelled = false)
            MotionEvent.ACTION_CANCEL -> finishDrag(cancelled = true)
        }
    }

    private fun finishDrag(cancelled: Boolean) {
        val anchor = dragAnchor
        val app = dragSourceApp
        val from = dragSourceCell
        val target = if (cancelled) null else dragTargetCell
        val moved = dragMoved
        resetDrag()
        anchor?.alpha = 1f
        invalidate()
        if (app == null || anchor == null) return
        if (!cancelled && moved && target != null && target != from) {
            onItemMove?.invoke(app, target)
        } else if (!moved) {
            // A long press without movement keeps the context menu behavior.
            onAppLongClick?.invoke(anchor, app)
        }
    }

    private fun resetDrag() {
        dragSourceApp = null
        dragAnchor = null
        dragSourceCell = -1
        dragTargetCell = null
        dragMoved = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cell = dragTargetCell ?: return
        if (lastCellW <= 0 || lastCellH <= 0) return
        val inset = (HIGHLIGHT_INSET_DP * density).toInt()
        val radius = HIGHLIGHT_RADIUS_DP * density
        val x = paddingLeft + gutterPx + GridCanvas.columnOf(cell) * lastCellW + inset
        val y = paddingTop + gutterPx + GridCanvas.rowOf(cell) * lastCellH + inset
        canvas.drawRoundRect(
            x.toFloat(),
            y.toFloat(),
            (x + lastCellW - 2 * inset).toFloat(),
            (y + lastCellH - 2 * inset).toFloat(),
            radius,
            radius,
            highlightPaint,
        )
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
                beginDrag(cell, app, anchor)
                // The drag (or, without movement, the context menu) handles the rest.
                true
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = max(suggestedMinimumWidth, getDefaultSize(suggestedMinimumWidth, widthMeasureSpec))
        val height = max(suggestedMinimumHeight, getDefaultSize(suggestedMinimumHeight, heightMeasureSpec))
        setMeasuredDimension(width, height)

        val cellW = cellWidth(width)
        val cellH = cellHeight(height)
        lastCellW = cellW
        lastCellH = cellH
        // Guard against degenerate layouts (tiny windows) where the padding would
        // exceed the cell.
        val childW = (cellW - 2 * cellHPadPx).coerceAtLeast(1)
        val childH = (cellH - 2 * cellVPadPx).coerceAtLeast(1)
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

    private companion object {
        const val FLING_VELOCITY = 1200f
        const val MAX_FLING_DRIFT_DP = 100f
        const val DRAGGED_ALPHA = 0.45f
        const val HIGHLIGHT_INSET_DP = 6
        const val HIGHLIGHT_RADIUS_DP = 20f
        const val HIGHLIGHT_COLOR = 0x2E4CAF50
    }
}
