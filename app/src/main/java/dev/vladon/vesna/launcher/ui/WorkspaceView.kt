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
 * Cell geometry comes from [GridCanvas].
 *
 * Touch model:
 * - tap launches, a long press on empty space opens the launcher menu, an upward
 *   fling anywhere opens the drawer;
 * - long-pressing an icon starts a drag session: the view dims the icon, reports
 *   progress in workspace-local coordinates and lets [HomeActivity] decide the drop
 *   target (grid cell or dock); highlights are drawn via [showDropHighlight].
 */
class WorkspaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    var onAppClick: ((LauncherApp) -> Unit)? = null
    var onEmptyAreaLongClick: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null

    /** The user lifted an icon by long-pressing it; a drag session has begun. */
    var onDragStarted: ((cell: Int, app: LauncherApp, anchor: View) -> Unit)? = null

    /** Drag progress in workspace-local coordinates. */
    var onDragProgress: ((Float, Float) -> Unit)? = null

    /**
     * The finger was released: [moved] is false when it never left the touch slop
     * (the item menu should open, anchored at [anchor]); [cancelled] is true when
     * the system cancelled the gesture (no commit, no menu).
     */
    var onDragReleased: ((x: Float, y: Float, moved: Boolean, cancelled: Boolean, app: LauncherApp, anchor: View) -> Unit)? =
        null

    private val density = resources.displayMetrics.density
    private val gutterPx = (8 * density).toInt()
    private val cellHPadPx = (4 * density).toInt()
    private val cellVPadPx = (4 * density).toInt()

    private var boundApps: Map<Int, LauncherApp> = emptyMap()
    private var lastCellW = 0
    private var lastCellH = 0

    // Active drag-and-drop state. While a drag is running the grid intercepts the
    // gesture and the swipe-up detector is bypassed.
    private var dragSourceApp: LauncherApp? = null
    private var dragAnchor: View? = null
    private var dragTargetCell: Int? = null
    private var dragMoved = false

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = HIGHLIGHT_COLOR
        style = Paint.Style.FILL
    }

    // Touch-event plumbing: a child (icon) holding the gesture routes MOVE/UP through
    // onInterceptTouchEvent, while empty-area gestures (no child target) reach both
    // hooks with the same MotionEvent instance. Feeding from both hooks covers all
    // paths; the identity check prevents feeding any single event twice.
    private var lastIntercepted: MotionEvent? = null

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

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                lastIntercepted = ev
                gestureDetector.onTouchEvent(ev)
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev !== lastIntercepted) {
            gestureDetector.onTouchEvent(ev)
        }
        lastIntercepted = null
        return super.onTouchEvent(ev)
    }

    // The dragged icon keeps the touch stream, so the drag itself is observed by an
    // OnTouchListener installed on the anchor for the duration of the session.
    // Intercepting on UP would swallow the event (the parent never receives it).
    private val dragTouchListener =
        View.OnTouchListener { view, event ->
            if (dragSourceApp == null) return@OnTouchListener false
            val workspaceX = view.left + event.x
            val workspaceY = view.top + event.y
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    dragMoved = true
                    onDragProgress?.invoke(workspaceX, workspaceY)
                }
                MotionEvent.ACTION_UP -> endDrag(workspaceX, workspaceY, cancelled = false, anchor = view)
                MotionEvent.ACTION_CANCEL -> endDrag(workspaceX, workspaceY, cancelled = true, anchor = view)
            }
            true
        }

    /** Starts a drag for the icon at [cell]; the item dims until it is dropped. */
    private fun beginDrag(cell: Int, app: LauncherApp, anchor: View) {
        dragSourceApp = app
        dragAnchor = anchor
        dragMoved = false
        dragTargetCell = null
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        anchor.alpha = DRAGGED_ALPHA
        // The parent takes the gesture over: deliver a synthetic CANCEL so the
        // child clears its pressed/ripple state, then install the drag listener.
        anchor.dispatchTouchEvent(
            MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0),
        )
        anchor.setOnTouchListener(dragTouchListener)
        onDragStarted?.invoke(cell, app, anchor)
    }

    /** Ends the drag session: restores the icon, clears highlights, reports release. */
    private fun endDrag(x: Float, y: Float, cancelled: Boolean, anchor: View) {
        val app = dragSourceApp
        val moved = dragMoved
        dragSourceApp = null
        dragAnchor = null
        dragTargetCell = null
        dragMoved = false
        anchor.setOnTouchListener(null)
        anchor.alpha = 1f
        invalidate()
        if (app == null) return
        onDragReleased?.invoke(x, y, moved, cancelled, app, anchor)
    }

    /** Shows (cell != null) or hides the drop-target highlight for drag sessions. */
    fun showDropHighlight(cell: Int?) {
        if (cell != dragTargetCell) {
            dragTargetCell = cell
            invalidate()
        }
    }

    /** The grid cell containing the point, or null when outside the grid. */
    fun cellAtLocal(x: Float, y: Float): Int? = GridCanvas.cellAtPoint(
        x,
        y,
        paddingLeft + gutterPx.toFloat(),
        paddingTop + gutterPx.toFloat(),
        lastCellW.toFloat(),
        lastCellH.toFloat(),
    )

    /** Whether a bound icon occupies [cell]. */
    fun isCellOccupied(cell: Int): Boolean = boundApps.containsKey(cell)

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
        boundApps = cells
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

    private fun findChildForCell(cell: Int): AppIconView? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if ((child.tag as? Int) == cell) return child as AppIconView
        }
        return null
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

    private fun cellWidth(totalWidth: Int): Int =
        (totalWidth - paddingLeft - paddingRight - 2 * gutterPx) / GridCanvas.COLUMNS

    private fun cellHeight(totalHeight: Int): Int =
        (totalHeight - paddingTop - paddingBottom - 2 * gutterPx) / GridCanvas.ROWS

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    private companion object {
        const val FLING_VELOCITY = 1200f
        const val MAX_FLING_DRIFT_DP = 100f
        const val DRAGGED_ALPHA = 0.45f
        const val HIGHLIGHT_INSET_DP = 6
        const val HIGHLIGHT_RADIUS_DP = 20f
        const val HIGHLIGHT_COLOR = 0x2E4CAF50
    }
}
