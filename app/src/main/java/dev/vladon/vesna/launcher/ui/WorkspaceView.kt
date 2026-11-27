package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.ScaleGestureDetector
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.accessibility.AccessibilityNodeInfo
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.notifications.NotificationBadges
import dev.vladon.vesna.launcher.settings.LabelFont
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.GridCanvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * The workspace: one [AppIconView] child per occupied cell of the [GridCanvas]
 * geometry adopted by [bind] ([GridCanvas.DEFAULT] until then).
 *
 * Touch model:
 * - tap launches an app or opens a folder; a long press on empty space opens the launcher menu; an upward
 *   fling, or a slow upward drag long enough, opens the drawer and a horizontal swipe selects an adjacent page;
 * - long-pressing an icon starts a drag session: the view dims the icon, reports
 *   progress in workspace-local coordinates and lets [HomeActivity] decide the drop
 *   target (grid cell or dock); highlights are drawn via [showDropHighlight];
 * - once an app drag passes the touch slop its pointer stream is promoted to this
 *   view, so page rebinds (edge dwell previews) cannot cancel the gesture; folder
 *   tiles and stationary holds keep the child-owned stream.
 */
class WorkspaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    var onAppClick: ((LauncherApp) -> Unit)? = null
    internal var onAppOptions: ((Int, LauncherApp, View) -> Unit)? = null

    /** A workspace cell was tapped: an app (launch) or a folder (open). */
    var onCellClick: ((CellContent) -> Unit)? = null
    var isLayoutLocked: () -> Boolean = { false }
    var labelFontScalePercent: Int = 100
    var labelFont: LabelFont = LabelFont.INTER_MEDIUM
    var onEmptyAreaLongClick: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null

    /** Creates the live host view for a committed widget (ENG-102 slice 2). */
    var widgetViewProvider: ((appWidgetId: Int) -> View?)? = null

    /** The long-press options for a placed widget (ENG-102 slice 2b). */
    var onWidgetOptions: ((dev.vladon.vesna.launcher.data.WidgetEntity, View) -> Unit)? = null

    /** The pending widget move: set by the options, consumed by the next cell tap. */
    var widgetMoveTarget: dev.vladon.vesna.launcher.data.WidgetEntity? = null
    var onWidgetMoveCellPicked: ((dev.vladon.vesna.launcher.data.WidgetEntity, Int) -> Unit)? = null

    /** #228: the gesture-only desktop — the workspace exposes the drawer and
     *  the Home options as custom accessibility actions for TalkBack users. */
    var onGestureOpenDrawer: (() -> Unit)? = null
    var onGestureHomeOptions: (() -> Unit)? = null

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(
            AccessibilityNodeInfo.AccessibilityAction(
                AccessibilityNodeInfo.ACTION_CLICK,
                context.getString(R.string.a11y_open_drawer),
            ),
        )
        info.addAction(
            AccessibilityNodeInfo.AccessibilityAction(
                AccessibilityNodeInfo.ACTION_LONG_CLICK,
                context.getString(R.string.a11y_home_options),
            ),
        )
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        when (action) {
            AccessibilityNodeInfo.ACTION_CLICK -> {
                onGestureOpenDrawer?.invoke()
                return true
            }
            AccessibilityNodeInfo.ACTION_LONG_CLICK -> {
                onGestureHomeOptions?.invoke()
                return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    /** #183: a bound gesture was recognised on an empty Home cell. */
    var onGesture: ((dev.vladon.vesna.launcher.settings.GestureId) -> Unit)? = null

    /** Test/inspection probe: the last gesture this view recognised. */
    internal var lastFiredGesture: dev.vladon.vesna.launcher.settings.GestureId? = null
        private set

    private fun fireGesture(gesture: dev.vladon.vesna.launcher.settings.GestureId) {
        lastFiredGesture = gesture
        onGesture?.invoke(gesture)
    }

    private var twoFingerActive = false
    private var twoFingerStartY = 0f
    private var twoFingerFired = false
    private var twoFingerStartSpan = 0f
    private var twoFingerLastSpan = 0f

    /** The pinch verdict at the stream end: the span ratio against the start. */
    private fun resolvePinch() {
        if (twoFingerFired || !twoFingerActive || twoFingerStartSpan <= 0f) return
        twoFingerFired = true
        val ratio = twoFingerLastSpan / twoFingerStartSpan
        if (ratio < 0.85f) {
            fireGesture(dev.vladon.vesna.launcher.settings.GestureId.PINCH_IN)
        } else if (ratio > 1.15f) {
            fireGesture(dev.vladon.vesna.launcher.settings.GestureId.PINCH_OUT)
        }
    }

    /**
     * The gesture stream gate: never during a drag, a one-finger page swipe or a
     * long press. The empty-grid requirement is deliberately NOT here — a dense
     * grid lands the first DOWN on a tile, and the two-finger gestures must work
     * over tiles (the double-tap keeps its empty-grid rule via lastDownEmpty).
     */
    private fun twoFingerGestureAllowed(): Boolean =
        !dragPromoted && !pageSwipe && !longPressed

    /**
     * Whether the LAST ACTION_DOWN landed on the empty grid. The detector's
     * onDoubleTap fires at the second DOWN, before that down's own empty flag
     * is set — so the double-tap gate reads the previous down's result.
     */
    private var lastDownEmpty = false

    var onSwipePage: ((forward: Boolean) -> Unit)? = null

    /** Raw horizontal drag delta while a page swipe is in progress (#180 phase 2). */
    var onPageDrag: ((dx: Float) -> Unit)? = null

    /** The swipe stream ended (UP or CANCEL); dx is the final delta. */
    var onPageDragEnd: ((dx: Float) -> Unit)? = null

    /** The displacement that commits a page swipe, in pixels. */
    internal val pageSwipeThresholdPx: Int
        get() = (PAGE_SWIPE_DP * density).toInt()

    /** Opens folder options after a hold without drag, or from keyboard/accessibility. */
    var onFolderLongClick: ((Folder, View) -> Unit)? = null
    /** Reports movement and returns whether the currently bound page accepts folder drops. */
    internal var onFolderDragProgress: ((Float, Float) -> Boolean)? = null
    /** A null cell cancels the drop, including a release outside a valid target. */
    internal var onFolderDragReleased: ((Folder, Int?) -> Unit)? = null

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
    private val dragSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var boundContent: Map<Int, CellContent> = emptyMap()

    /** Geometry every measure, draw and hit test resolves against. */
    private var grid: GridCanvas = GridCanvas.DEFAULT

    internal var showAppNames = true
        set(value) {
            if (field == value) return
            field = value
            for (index in 0 until childCount) {
                val view = getChildAt(index) as AppIconView
                if (boundContent[view.tag as Int] is CellContent.App) view.setAppNameVisible(value)
            }
        }

    /** Scales only the artwork of every tile; cells, labels and touch targets keep their size. */
    internal var iconScale = 1f
        set(value) {
            if (field == value) return
            field = value
            for (index in 0 until childCount) (getChildAt(index) as AppIconView).artworkScale = value
        }

    private var lastCellW = 0
    private var lastCellH = 0

    // Active drag-and-drop state. While a drag is running the grid intercepts the
    // gesture and the swipe-up detector is bypassed.
    private var dragContent: CellContent? = null
    private var dragAnchor: View? = null
    private var dragTargetCell: Int? = null
    private var dragMoved = false

    /** True once a lifted tile's pointer stream was promoted to this view. */
    private var dragPromoted = false

    internal val isDragging: Boolean get() = dragAnchor != null
    internal var reservedCellsMask = 0
        set(value) {
            if (field == value) return
            field = value
            val target = dragTargetCell
            if (target != null && isCellReserved(target)) showDropHighlight(null)
        }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(context.vesnaPaletteColor(R.attr.workspace_page_primary), HIGHLIGHT_ALPHA)
        style = Paint.Style.FILL
    }

    // Observe the whole stream, whether an app child or the empty grid owns it.
    private var downX = 0f
    private var downY = 0f
    private var pageSwipe = false
    private var pageSwipeInterrupted = false
    private var longPressed = false

    /** The fast swipe-up path already answered this gesture. */
    private var swipeUpSent = false

    /** True when the DOWN landed on the empty grid, whose own long press opens the launcher menu. */
    private var emptyAreaPressed = false

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // #183: the double tap binds on the empty grid only; tiles own their taps.
                if (lastDownEmpty && !dragPromoted) {
                    fireGesture(dev.vladon.vesna.launcher.settings.GestureId.DOUBLE_TAP)
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                val dx = abs(e2.x - (e1?.x ?: e2.x))
                if (velocityY < -FLING_VELOCITY && dx < MAX_FLING_DRIFT_DP * density) {
                    swipeUpSent = true
                    onSwipeUp?.invoke()
                    return true
                }
                return false
            }
        },
    )

    init {
        setWillNotDraw(false) // The grid paints the drag target behind its children.
        // Long press on any empty part of the grid.
        isLongClickable = true
        setOnLongClickListener {
            longPressed = true
            onEmptyAreaLongClick?.invoke()
            true
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        // The pinch resolves at the stream end before any branch returns early.
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) resolvePinch()
        if (action == MotionEvent.ACTION_DOWN) {
            downX = ev.x
            downY = ev.y
            pageSwipe = false
            pageSwipeInterrupted = false
            longPressed = false
            swipeUpSent = false
            emptyAreaPressed = false
            dragPromoted = false
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            // A different finger may occupy index zero later in this stream.
            pageSwipeInterrupted = true
            // #183: an empty-grid two-finger gesture session (never during a
            // drag or a one-finger page swipe).

            if (twoFingerGestureAllowed() && ev.pointerCount == 2) {
                twoFingerActive = true
                twoFingerFired = false
                twoFingerStartY = minOf(ev.getY(0), ev.getY(1))
                twoFingerStartSpan = abs(ev.getX(0) - ev.getX(1))
                twoFingerLastSpan = twoFingerStartSpan
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            // Android keeps the empty grid's long press pending until the pointer leaves the
            // whole view, so a slow swipe must end it as soon as it passes the slop. A tile's
            // own long press keeps Android's tolerance for a finger that wanders while holding.
            if (emptyAreaPressed && !longPressed && hypot(ev.x - downX, ev.y - downY) > dragSlop) {
                super.cancelLongPress()
            }
            if (twoFingerActive && ev.pointerCount >= 2 && !twoFingerFired) {
                twoFingerLastSpan = abs(ev.getX(0) - ev.getX(1))
                if (minOf(ev.getY(0), ev.getY(1)) <= twoFingerStartY - TWO_FINGER_SWIPE_DP * density) {
                    twoFingerFired = true
                    fireGesture(dev.vladon.vesna.launcher.settings.GestureId.TWO_FINGER_SWIPE_UP)
                }
            }
            if (!longPressed && !pageSwipe && !pageSwipeInterrupted) {
                val dx = abs(ev.x - downX)
                pageSwipe = dx >= PAGE_SWIPE_DP * density && dx > abs(ev.y - downY) * 1.5f
                if (pageSwipe) {
                    // Cancel the pressed child or the empty grid before handling release.
                    val cancel = MotionEvent.obtain(ev)
                    cancel.action = MotionEvent.ACTION_CANCEL
                    gestureDetector.onTouchEvent(cancel)
                    super.dispatchTouchEvent(cancel)
                    cancel.recycle()
                }
            }
        }
        if (dragPromoted) {
            // The stream belongs to this view: children may be rebound or removed
            // mid-drag, so the remaining MOVE/UP/CANCEL must never reach one.
            when (action) {
                MotionEvent.ACTION_MOVE -> reportDragProgress(ev.x, ev.y)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    dragAnchor?.let { endDrag(ev.x, ev.y, cancelled = action == MotionEvent.ACTION_CANCEL, anchor = it) }
                // A second finger cancels the session; the stream is still swallowed
                // until it fully terminates so no residual event finds a new child.
                MotionEvent.ACTION_POINTER_DOWN -> cancelDrag()
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) dragPromoted = false
            return true
        }
        if (pageSwipe) {
            // Once intercepted, consume the rest of the stream even if it cannot page.
            if (action == MotionEvent.ACTION_MOVE && !pageSwipeInterrupted) {
                onPageDrag?.invoke(ev.x - downX)
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                val dx = ev.x - downX
                if (action == MotionEvent.ACTION_UP && !pageSwipeInterrupted &&
                    abs(dx) >= PAGE_SWIPE_DP * density
                ) {
                    val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
                    onSwipePage?.invoke((dx < 0f) != rtl)
                }
                // The follow layer settles whether or not the page commits.
                onPageDragEnd?.invoke(dx)
            }
            return true
        }
        if (!longPressed) gestureDetector.onTouchEvent(ev)
        if (action == MotionEvent.ACTION_UP && !longPressed && !swipeUpSent) longSwipeUp(ev)
        if (action == MotionEvent.ACTION_UP && !longPressed && !swipeUpSent && !pageSwipeInterrupted) longSwipeDown(ev)
        val alreadyPromoted = dragPromoted
        val handled = super.dispatchTouchEvent(ev)
        if (!alreadyPromoted && dragPromoted) {
            // The child listener promoted a drag during that move: cancel the
            // old target now that its dispatch has unwound. Its listener is gone, so
            // this cancel cannot end the active session.
            val cancel = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
            super.dispatchTouchEvent(cancel)
            cancel.recycle()
        }
        if (action == MotionEvent.ACTION_DOWN) lastDownEmpty = emptyAreaPressed
        return handled
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // The widget-move pick mode consumes a tap on any cell, including the
        // empty ones that own no child view.
        if (widgetMoveTarget != null && event.actionMasked == MotionEvent.ACTION_UP) {
            val target = widgetMoveTarget ?: return super.onTouchEvent(event)
            val cell = cellAtLocal(event.x, event.y)
            if (cell != null) {
                widgetMoveTarget = null
                onWidgetMoveCellPicked?.invoke(target, cell)
                return true
            }
        }
        // Only the empty grid reaches this path; a tile owns and consumes its own stream.
        if (event.actionMasked == MotionEvent.ACTION_DOWN) emptyAreaPressed = true
        return super.onTouchEvent(event)
    }

    /** A slow upward drag opens the drawer as well, once it has travelled far enough. */
    private fun longSwipeUp(up: MotionEvent) {
        val drift = abs(up.x - downX)
        // A short workspace, such as landscape or split screen, asks for half of its height instead,
        // but never less than the touch slop, so a tap never opens the drawer.
        val distance = max(
            dragSlop.toFloat(),
            min(SWIPE_UP_DISTANCE_DP * density, (height - paddingTop - paddingBottom) / 2f),
        )
        if (!pageSwipeInterrupted && downY - up.y >= distance && drift < MAX_FLING_DRIFT_DP * density) {
            onSwipeUp?.invoke()
        }
    }

    /** #183: the slow downward drag on an empty grid fires the bound swipe-down gesture. */
    private fun longSwipeDown(up: MotionEvent) {
        val drift = abs(up.x - downX)
        val distance = max(
            dragSlop.toFloat(),
            min(SWIPE_UP_DISTANCE_DP * density, (height - paddingTop - paddingBottom) / 2f),
        )
        if (!pageSwipeInterrupted && up.y - downY >= distance && drift < MAX_FLING_DRIFT_DP * density) {
            fireGesture(dev.vladon.vesna.launcher.settings.GestureId.SWIPE_DOWN)
        }
    }

    // Until promotion the dragged tile keeps the stream, so a stationary hold
    // can still open its menu on UP. Moving beyond slop promotes either tile type.
    private val dragTouchListener =
        View.OnTouchListener { view, event ->
            if (dragContent == null) return@OnTouchListener false
            val workspaceX = view.left + event.x
            val workspaceY = view.top + event.y
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    dragMoved = dragMoved || max(abs(workspaceX - downX), abs(workspaceY - downY)) > dragSlop
                    if (dragMoved) {
                        promoteDragStream(view)
                        reportDragProgress(workspaceX, workspaceY)
                    }
                }
                MotionEvent.ACTION_UP -> endDrag(workspaceX, workspaceY, cancelled = false, anchor = view)
                MotionEvent.ACTION_CANCEL -> endDrag(workspaceX, workspaceY, cancelled = true, anchor = view)
            }
            true
        }

    private fun reportDragProgress(x: Float, y: Float) {
        when (dragContent) {
            is CellContent.App -> onDragProgress?.invoke(x, y)
            is CellContent.WidgetCell -> {}
            is CellContent.FolderCell -> {
                val ready = onFolderDragProgress?.invoke(x, y) != false
                showDropHighlight(if (ready) folderDropTarget(x, y) else null)
            }
            null -> Unit
        }
    }

    /**
     * Promotes a drag to a workspace-owned pointer stream: the anchor listener
     * stops so the synthetic cancel that [dispatchTouchEvent] sends after the move
     * has unwound clears the old child target without ending the session. [bind] can
     * then replace children mid-drag; the anchor reference stays as the drag identity
     * and its alpha is restored at release.
     */
    private fun promoteDragStream(anchor: View) {
        if (dragPromoted) return
        dragPromoted = true
        anchor.setOnTouchListener(null)
    }

    /** Starts a drag for the icon at [cell]; the item dims until it is dropped. */
    private fun beginDrag(cell: Int, content: CellContent, anchor: View) {
        longPressed = true
        if (isLayoutLocked()) {
            when (content) {
                is CellContent.App -> onAppOptions?.invoke(cell, content.app, anchor)
                is CellContent.FolderCell -> onFolderLongClick?.invoke(content.folder, anchor)
                is CellContent.WidgetCell -> onWidgetOptions?.invoke(content.widget, anchor)
            }
            return
        }
        dragContent = content
        dragAnchor = anchor
        dragMoved = false
        dragTargetCell = null
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        anchor.alpha = DRAGGED_ALPHA
        // The parent takes the gesture over: deliver a synthetic CANCEL so the
        // child clears its pressed/ripple state, then install the drag listener.
        val cancel = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        anchor.dispatchTouchEvent(cancel)
        cancel.recycle()
        anchor.setOnTouchListener(dragTouchListener)
        if (content is CellContent.App) onDragStarted?.invoke(cell, content.app, anchor)
    }

    /** Ends the drag session: restores the icon, clears highlights, reports release. */
    private fun endDrag(x: Float, y: Float, cancelled: Boolean, anchor: View) {
        val content = dragContent
        val moved = dragMoved
        dragContent = null
        dragAnchor = null
        dragTargetCell = null
        dragMoved = false
        anchor.setOnTouchListener(null)
        anchor.alpha = 1f
        invalidate()
        when (content) {
            is CellContent.App -> onDragReleased?.invoke(x, y, moved, cancelled, content.app, anchor)
            is CellContent.FolderCell -> if (moved || cancelled) {
                val cell = if (cancelled || isLayoutLocked()) null else folderDropTarget(x, y)
                onFolderDragReleased?.invoke(content.folder, cell)
            } else {
                onFolderLongClick?.invoke(content.folder, anchor)
            }
            is CellContent.WidgetCell -> {}
            null -> Unit
        }
    }

    internal fun folderDropTarget(x: Float, y: Float): Int? =
        cellAtLocal(x, y)?.takeUnless { it in boundContent || isCellReserved(it) }

    internal fun cancelDrag() {
        val anchor = dragAnchor ?: return
        endDrag(0f, 0f, cancelled = true, anchor)
    }

    /**
     * Restores the dragged icon's full alpha before a rebind may reuse its view for
     * another pin; Home calls this just before rendering an authorized preview page.
     * The session keeps the anchor as its drag identity, so a release still restores
     * the alpha and reports the old view without opening its menu ([dragMoved] latched
     * true at promotion).
     */
    internal fun clearDragAnchorHighlight() {
        dragAnchor?.alpha = 1f
    }

    /** Shows (cell != null) or hides the drop-target highlight for drag sessions. */
    fun showDropHighlight(cell: Int?) {
        if (cell != dragTargetCell) {
            dragTargetCell = cell
            invalidate()
        }
    }

    /** The saved cell containing the point, or null when outside the grid. */
    fun cellAtLocal(x: Float, y: Float): Int? {
        val visualCell = grid.cellAtPoint(
            x,
            y,
            paddingLeft + gutterPx.toFloat(),
            paddingTop + gutterPx.toFloat(),
            lastCellW.toFloat(),
            lastCellH.toFloat(),
        ) ?: return null
        return grid.rowOf(visualCell) * grid.columns + orientedColumn(visualCell)
    }

    /** Whether a valid grid cell is held by a saved shortcut that is not drawn. */
    internal fun isCellReserved(cell: Int): Boolean = reservedCellsMask and (1 shl cell) != 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cell = dragTargetCell ?: return
        if (lastCellW <= 0 || lastCellH <= 0) return
        val inset = (HIGHLIGHT_INSET_DP * density).toInt()
        val radius = HIGHLIGHT_RADIUS_DP * density
        val x = cellLeft(cell, lastCellW) + inset
        val y = paddingTop + gutterPx + grid.rowOf(cell) * lastCellH + inset
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

    /**
     * Adopts [grid], then reconciles children with the current cell contents. A
     * geometry change ends the active drag, drops the stale highlight and
     * reschedules layout; an unchanged grid leaves a live drag alone.
     */
    fun bind(grid: GridCanvas, cells: Map<Int, CellContent>, isDragPagePreview: Boolean = false) {
        val draggingFolder = (dragContent as? CellContent.FolderCell)?.folder
        if (!isDragPagePreview && draggingFolder != null &&
            (cells[draggingFolder.cellIndex] as? CellContent.FolderCell)?.folder?.id != draggingFolder.id
        ) cancelDrag()
        if (grid != this.grid) {
            cancelDrag()
            showDropHighlight(null)
            this.grid = grid
            invalidate()
            requestLayout()
        }
        boundContent = cells
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            val cell = child.tag as? Int
            if (cell == null || cell !in cells) removeViewAt(i)
        }
        for ((cell, content) in cells) {
            val existing = findChildForCell(cell)
            val view = existing ?: if (content is CellContent.WidgetCell) {
                WidgetCellView(context).also {
                    it.tag = cell
                    addView(it)
                }
            } else {
                AppIconView(context).also {
                    // The grid already insets each cell. Keep the tile's vertical
                    // space for the icon and a complete line of scaled label text.
                    it.setPadding(it.paddingLeft, 0, it.paddingRight, 0)
                    it.tag = cell
                    it.artworkScale = iconScale
                    addView(it)
                }
            }
            if (view is WidgetCellView && content is CellContent.WidgetCell) {
                view.spanX = content.widget.spanX
                view.spanY = content.widget.spanY
                // A rebind keeps the live host view; only a fresh cell installs one.
                if (view.childCount == 0) {
                    widgetViewProvider?.invoke(content.widget.appWidgetId)?.let { view.installHost(it) }
                }
                view.contentDescription = context.getString(R.string.widget_cell_description)
                // The long press arrives through the cell's own gesture
                // detector: the host view consumes ordinary taps.
                view.onWidgetLongPress = { onWidgetOptions?.invoke(content.widget, view) }
                val moveTarget = widgetMoveTarget
                view.setOnClickListener {
                    if (moveTarget != null) {
                        onWidgetMoveCellPicked?.invoke(moveTarget, cell)
                    }
                }
                continue
            }
            val iconView = view as AppIconView
            when (content) {
                is CellContent.App -> {
                    iconView.bind(content.app)
                    iconView.setLabelTypography(labelFontScalePercent, labelFont)
                    iconView.setAppNameVisible(showAppNames)
                    view.setOnClickListener { onAppClick?.invoke(content.app) }
                    view.bindOptions(
                        onOptions = { anchor -> onAppOptions?.invoke(cell, content.app, anchor) },
                        onTouchHold = { anchor -> beginDrag(cell, content, anchor) },
                    )
                }
                is CellContent.WidgetCell -> {} // Unreachable: widgets continue above.
                is CellContent.FolderCell -> {
                    iconView.bindContent(
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_folder_cell),
                        label = content.folder.title,
                        description = content.folder.title,
                    )
                    // A tile reused from an app still needs to show the folder's own name.
                    iconView.setAppNameVisible(true)
                    view.setOnClickListener { onCellClick?.invoke(content) }
                    view.bindOptions(
                        onOptions = { anchor -> onFolderLongClick?.invoke(content.folder, anchor) },
                        onTouchHold = { anchor -> beginDrag(cell, content, anchor) },
                    )
                }
            }
        }
    }

    /**
     * #180: captures the current page rendering as a bitmap for the slide
     * transition. Call before switching the page content.
     */
    fun capturePageBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return bitmap
    }

    /** Rebinds label typography on the existing workspace tiles (#173). */
    internal fun refreshLabelTypography(percent: Int, font: LabelFont) {
        labelFontScalePercent = percent
        labelFont = font
        for (i in 0 until childCount) {
            (getChildAt(i) as? AppIconView)?.setLabelTypography(percent, font)
        }
    }

    /** Rebinds notification dots on the existing workspace tiles (#200). */
    internal fun refreshNotificationDots() {
        for (i in 0 until childCount) {
            val view = getChildAt(i) as? AppIconView ?: continue
            val cell = view.tag as? Int ?: continue
            val content = boundContent[cell] ?: continue
            if (content is CellContent.App) {
                view.setNotificationDot(NotificationBadges.shows(content.app.packageName))
            }
        }
    }

    /** The visible content bound to [cell], if any. */
    fun contentAt(cell: Int): CellContent? = boundContent[cell]

    internal fun findChildForCell(cell: Int): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if ((child.tag as? Int) == cell) return child
        }
        return null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = max(suggestedMinimumWidth, getDefaultSize(suggestedMinimumWidth, widthMeasureSpec))
        val height = max(suggestedMinimumHeight, getDefaultSize(suggestedMinimumHeight, heightMeasureSpec))
        setMeasuredDimension(width, height)

        val cellW = cellWidth(width)
        val cellH = cellHeight(height)
        if (dragContent is CellContent.FolderCell && (cellW != lastCellW || cellH != lastCellH)) cancelDrag()
        lastCellW = cellW
        lastCellH = cellH
        // Guard against degenerate layouts (tiny windows) where the padding would
        // exceed the cell.
        val childW = (cellW - 2 * cellHPadPx).coerceAtLeast(1)
        val childH = (cellH - 2 * cellVPadPx).coerceAtLeast(1)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val spans = child as? WidgetCellView
            val childWSpan = if (spans == null) childW else spans.spanX * cellW - 2 * cellHPadPx
            val childHSpan = if (spans == null) childH else spans.spanY * cellH - 2 * cellVPadPx
            child.measure(
                MeasureSpec.makeMeasureSpec(childWSpan.coerceAtLeast(1), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childHSpan.coerceAtLeast(1), MeasureSpec.EXACTLY),
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
            val spans = child as? WidgetCellView
            val xPad = if (spans == null) cellHPadPx else 0
            val yPad = if (spans == null) cellVPadPx else 0
            val x = cellLeft(cell, cellW) + xPad
            val y = paddingTop + gutterPx + grid.rowOf(cell) * cellH + yPad
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
        }
    }

    // Column reflection is its own inverse: both rendering and hit testing use it.
    private fun orientedColumn(cell: Int): Int {
        val column = grid.columnOf(cell)
        return if (layoutDirection == LAYOUT_DIRECTION_RTL) grid.columns - 1 - column else column
    }

    private fun cellLeft(cell: Int, cellWidth: Int): Int =
        paddingLeft + gutterPx + orientedColumn(cell) * cellWidth

    private fun cellWidth(totalWidth: Int): Int =
        (totalWidth - paddingLeft - paddingRight - 2 * gutterPx) / grid.columns

    private fun cellHeight(totalHeight: Int): Int =
        (totalHeight - paddingTop - paddingBottom - 2 * gutterPx) / grid.rows

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    private companion object {
        const val FLING_VELOCITY = 1200f
        const val MAX_FLING_DRIFT_DP = 100f
        const val PAGE_SWIPE_DP = 56f
        const val TWO_FINGER_SWIPE_DP = 80f
        const val SWIPE_UP_DISTANCE_DP = 360f
        const val DRAGGED_ALPHA = 0.45f
        const val HIGHLIGHT_INSET_DP = 6
        const val HIGHLIGHT_RADIUS_DP = 12f
        const val HIGHLIGHT_ALPHA = 41 // 16% dragged state layer.
    }
}
