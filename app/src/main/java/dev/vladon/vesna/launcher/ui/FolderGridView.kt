package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.workspace.FolderPin
import kotlin.math.abs

/**
 * The member grid of an open folder. Touch works like any grid: taps launch,
 * holds open the item menu, and drags scroll.
 *
 * After touch slop, an armed hold previews ordering within the folder or
 * hands its coordinates to Home after leaving the panel. The grid retains
 * the original touch stream through release or cancellation.
 */
internal class FolderGridView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {

    /** Whether a member hold may become a drag; false keeps it a menu request. */
    var isMemberDragAllowed: () -> Boolean = { false }

    /** Exact item options for a stationary hold, or one under layout lock. */
    var onMemberHoldOptions: ((FolderPin, View) -> Unit)? = null

    /** A member hold armed a drag session; returning false refuses it. */
    var onMemberDragStarted: ((FolderPin) -> Boolean)? = null

    /** Real drag progress in raw screen coordinates. */
    var onMemberDragMoved: ((Float, Float) -> Unit)? = null

    /** An armed hold ended, including a stationary release that opens the menu. */
    var onMemberDragFinished: ((Float, Float, Boolean) -> Unit)? = null

    /** While true every touch is discarded (a queued drop is resolving). */
    var isMemberInputLocked: () -> Boolean = { false }

    private val grid = GridLayoutManager(context, MAX_APP_GRID_COLUMNS)
    private val dragSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val screenLocation = IntArray(2)
    private val density = resources.displayMetrics.density
    private val targetInset = 6 * density
    private val targetRadius = 12 * density
    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(context.getColor(R.color.workspace_page_primary), 41)
    }
    private val scrollZone = 24 * density
    private val scrollStep = (6 * density).toInt().coerceAtLeast(1)
    private var reorderTarget: View? = null
    private var reorderScrollDirection = 0
    private val scrollDuringReorder = Runnable {
        val direction = reorderScrollDirection
        reorderScrollDirection = 0
        if (session != null && direction != 0 && canScrollVertically(direction)) {
            scrollBy(0, direction * scrollStep)
            onMemberDragMoved?.invoke(lastRawX, lastRawY)
        }
    }

    /** One armed hold. [moved] latches at the first slop crossing. */
    private class Session(val pin: FolderPin, val anchor: View) {
        var moved = false
    }

    private var session: Session? = null
    private var discardStream = false
    private var downX = 0f
    private var downY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f

    init {
        layoutManager = grid
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            grid.spanCount = appGridColumnCount(availableWidth, resources.displayMetrics.density)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        lastRawX = event.rawX
        lastRawY = event.rawY
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.x
            downY = event.y
            discardStream = false
        }
        if (discardStream || isMemberInputLocked()) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                discardStream = false
            }
            return true
        }
        val active = session ?: return super.dispatchTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!active.moved && movedBeyondSlop(event)) active.moved = true
                if (active.moved) onMemberDragMoved?.invoke(event.rawX, event.rawY)
            }
            MotionEvent.ACTION_UP -> endMemberSession(active, cancelled = !active.moved, menu = !active.moved)
            MotionEvent.ACTION_CANCEL -> endMemberSession(active, cancelled = true, menu = false)
            MotionEvent.ACTION_POINTER_DOWN -> {
                // A second finger can never join a member drag.
                endMemberSession(active, cancelled = true, menu = false)
                discardStream = true
            }
        }
        // The grid owns the captured stream until it ends.
        return true
    }

    /** The tile's hold fired: arm a drag session, or keep the exact menu. */
    fun onMemberHold(pin: FolderPin, anchor: View) {
        if (session != null || discardStream || isMemberInputLocked()) return
        if (!isMemberDragAllowed()) {
            onMemberHoldOptions?.invoke(pin, anchor)
            return
        }
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        // The grid takes the stream over at the hold: deliver a synthetic CANCEL
        // through super so the tile drops its pressed/ripple state and
        // RecyclerView forgets the gesture, then keep ownership until the
        // stream ends.
        val cancel = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, downX, downY, 0)
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
        session = Session(pin, anchor)
        if (onMemberDragStarted?.invoke(pin) != true) {
            // The owner refused the session; swallow the rest of the stream.
            session = null
            discardStream = true
        }
    }

    fun previewReorder(rawX: Float, rawY: Float) {
        val source = session?.pin?.id ?: return
        getLocationOnScreen(screenLocation)
        val x = rawX - screenLocation[0]
        val y = rawY - screenLocation[1]
        setReorderTarget(memberViewAt(x, y)?.takeUnless { getChildViewHolder(it).itemId == source })
        setReorderScrollDirection(when {
            x < 0 || x >= width || y < 0 || y >= height -> 0
            y < scrollZone && canScrollVertically(-1) -> -1
            y >= height - scrollZone && canScrollVertically(1) -> 1
            else -> 0
        })
    }

    fun reorderTargetAt(rawX: Float, rawY: Float): Long? {
        getLocationOnScreen(screenLocation)
        val tile = memberViewAt(rawX - screenLocation[0], rawY - screenLocation[1]) ?: return null
        return getChildViewHolder(tile).itemId
    }

    fun clearReorderPreview() {
        setReorderTarget(null)
        setReorderScrollDirection(0)
    }

    private fun memberViewAt(x: Float, y: Float): View? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return findChildViewUnder(x, y)?.takeIf { getChildAdapterPosition(it) != NO_POSITION }
    }

    private fun setReorderTarget(tile: View?) {
        if (tile === reorderTarget) return
        reorderTarget = tile
        invalidate()
    }

    private fun setReorderScrollDirection(direction: Int) {
        if (direction == reorderScrollDirection) return
        if (reorderScrollDirection != 0) removeCallbacks(scrollDuringReorder)
        reorderScrollDirection = direction
        if (direction != 0) postOnAnimation(scrollDuringReorder)
    }

    override fun dispatchDraw(canvas: Canvas) {
        reorderTarget?.takeIf { it.parent === this }?.let { tile ->
            canvas.drawRoundRect(
                tile.x + targetInset, tile.y + targetInset,
                tile.x + tile.width - targetInset, tile.y + tile.height - targetInset,
                targetRadius, targetRadius, targetPaint,
            )
        }
        super.dispatchDraw(canvas)
    }

    /** Cancels an armed or active member drag; the stream remainder is discarded. */
    fun cancelMemberDrag(): Boolean {
        if (session == null) return false
        session = null
        discardStream = true
        onMemberDragFinished?.invoke(lastRawX, lastRawY, true)
        return true
    }

    /** Clears the session silently; the dialog (and its window) is going away. */
    fun clearMemberDrag() {
        session = null
        discardStream = false
        clearReorderPreview()
    }

    private fun endMemberSession(active: Session, cancelled: Boolean, menu: Boolean) {
        session = null
        onMemberDragFinished?.invoke(lastRawX, lastRawY, cancelled)
        if (menu) onMemberHoldOptions?.invoke(active.pin, active.anchor)
    }

    private fun movedBeyondSlop(event: MotionEvent): Boolean =
        abs(event.x - downX) > dragSlop || abs(event.y - downY) > dragSlop

    override fun onDetachedFromWindow() {
        clearMemberDrag()
        super.onDetachedFromWindow()
    }
}
