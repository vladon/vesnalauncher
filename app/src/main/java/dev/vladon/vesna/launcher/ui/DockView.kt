package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.DockPin
import dev.vladon.vesna.launcher.workspace.GridCanvas
import kotlin.math.abs

/**
 * Bottom bar with up to [GridCanvas.DOCK_CAPACITY] favorite apps. Weighted slots
 * keep icons aligned with the workspace columns.
 *
 * Long-pressing an icon starts a drag session: the view dims the icon, reports
 * progress in dock-local coordinates and lets [HomeActivity] decide the drop target
 * (another dock slot for reorder, or a workspace cell). Drop slots are highlighted
 * via [showSlotHighlight].
 */
class DockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    var onAppClick: ((LauncherApp) -> Unit)? = null
    internal var onAppOptions: ((DockPin, View) -> Unit)? = null

    internal var showAppNames = true
        set(value) {
            if (field == value) return
            field = value
            for (index in 0 until childCount) {
                (getChildAt(index) as AppIconView).setAppNameVisible(value)
            }
        }

    /** The user lifted a dock icon by long-pressing it; a drag session has begun. */
    var isLayoutLocked: () -> Boolean = { false }

    var onDragStarted: ((pin: DockPin, anchor: View) -> Unit)? = null

    /** Drag progress in dock-local coordinates. */
    var onDragProgress: ((Float, Float) -> Unit)? = null

    /**
     * The finger was released: [moved] is false when it never left the touch slop
     * (the item menu should open); [cancelled] is true when the system cancelled
     * the gesture (no commit, no menu).
     */
    var onDragReleased: ((x: Float, y: Float, moved: Boolean, cancelled: Boolean, app: LauncherApp, anchor: android.view.View) -> Unit)? =
        null

    private val density = resources.displayMetrics.density
    private val dragSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragSourceApp: LauncherApp? = null
    private var dragAnchor: android.view.View? = null
    private var dragTargetSlot: Int? = null
    private var dragMoved = false

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x2E4CAF50
        style = Paint.Style.FILL
    }

    init {
        orientation = HORIZONTAL
        weightSum = GridCanvas.DOCK_CAPACITY.toFloat()
        setBackgroundResource(R.drawable.dock_background)
        elevation = 6 * density
        val pad = (4 * density).toInt()
        setPadding(pad, pad, pad, pad)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.x
            downY = event.y
        }
        return super.dispatchTouchEvent(event)
    }

    // The dragged dock icon keeps the touch stream; observed via an OnTouchListener
    // installed on the anchor. Intercepting on UP would swallow the event.
    private val dragTouchListener =
        View.OnTouchListener { view, event ->
            if (dragSourceApp == null) return@OnTouchListener false
            val dockX = view.left + event.x
            val dockY = view.top + event.y
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    // Match Home's drag threshold; finger drift during a hold
                    // must not replace the shortcut menu with a drop.
                    dragMoved = dragMoved || abs(dockX - downX) > dragSlop || abs(dockY - downY) > dragSlop
                    if (dragMoved) onDragProgress?.invoke(dockX, dockY)
                }
                MotionEvent.ACTION_UP -> endDrag(dockX, dockY, cancelled = false, anchor = view)
                MotionEvent.ACTION_CANCEL -> endDrag(dockX, dockY, cancelled = true, anchor = view)
            }
            true
        }

    /** Starts a drag for the selected dock pin; the item dims until drop. */
    private fun beginDrag(pin: DockPin, anchor: View) {
        if (isLayoutLocked()) {
            onAppOptions?.invoke(pin, anchor)
            return
        }
        dragSourceApp = pin.app
        dragAnchor = anchor
        dragMoved = false
        dragTargetSlot = null
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        anchor.alpha = 0.45f
        // The parent takes the gesture over: deliver a synthetic CANCEL so the
        // child clears its pressed/ripple state, then install the drag listener.
        anchor.dispatchTouchEvent(
            MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0),
        )
        anchor.setOnTouchListener(dragTouchListener)
        onDragStarted?.invoke(pin, anchor)
    }

    /** Shows (slot != null) or hides the drop-target highlight for drag sessions. */
    fun showSlotHighlight(slot: Int?) {
        val clamped = slot?.coerceIn(0, GridCanvas.DOCK_CAPACITY - 1)
        if (clamped != dragTargetSlot) {
            dragTargetSlot = clamped
            invalidate()
        }
    }

    /** Logical slot from the start edge, matching the weighted children in either direction. */
    fun slotAtLocalX(x: Float): Int {
        val contentWidth = width - paddingLeft - paddingRight
        if (contentWidth <= 0) return 0
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val fromStart = if (rtl) width - paddingRight - x else x - paddingLeft
        for (slot in 0 until GridCanvas.DOCK_CAPACITY - 1) {
            val edge = slotOffset(slot + 1, contentWidth)
            // Keep the physical left edge inclusive, like View hit testing.
            val inSlot = if (rtl) fromStart <= edge else fromStart < edge
            if (inSlot) return slot
        }
        return GridCanvas.DOCK_CAPACITY - 1
    }

    private fun slotOffset(slot: Int, contentWidth: Int): Int {
        val capacity = GridCanvas.DOCK_CAPACITY
        // LinearLayout distributes leftover pixels to the last weighted children.
        val narrowSlots = capacity - contentWidth % capacity
        return slot * (contentWidth / capacity) + (slot - narrowSlots).coerceAtLeast(0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val slot = dragTargetSlot ?: return
        val contentWidth = width - paddingLeft - paddingRight
        if (contentWidth <= 0) return
        val start = slotOffset(slot, contentWidth)
        val end = slotOffset(slot + 1, contentWidth)
        val left = if (layoutDirection == LAYOUT_DIRECTION_RTL) width - paddingRight - end else paddingLeft + start
        val inset = (6 * density).toInt()
        val radius = 20f * density
        val x = (left + inset).toFloat()
        canvas.drawRoundRect(
            x,
            paddingTop + inset.toFloat(),
            (left + end - start - inset).toFloat(),
            height - paddingBottom - inset.toFloat(),
            radius,
            radius,
            highlightPaint,
        )
    }

    fun bind(pins: List<DockPin>) {
        val count = minOf(pins.size, GridCanvas.DOCK_CAPACITY)
        removeAllViews()
        for (index in 0 until count) {
            val pin = pins[index]
            val app = pin.app
            val view = AppIconView(context)
            view.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            view.bind(app)
            view.setAppNameVisible(showAppNames)
            view.setOnClickListener { onAppClick?.invoke(app) }
            view.bindOptions(
                onOptions = { anchor -> onAppOptions?.invoke(pin, anchor) },
                onTouchHold = { anchor -> beginDrag(pin, anchor) },
            )
            addView(view)
        }
    }

    internal fun cancelDrag() {
        val anchor = dragAnchor ?: return
        endDrag(0f, 0f, cancelled = true, anchor)
    }

    private fun endDrag(x: Float, y: Float, cancelled: Boolean, anchor: View) {
        val app = dragSourceApp
        val moved = dragMoved
        dragSourceApp = null
        dragAnchor = null
        dragTargetSlot = null
        dragMoved = false
        anchor.setOnTouchListener(null)
        anchor.alpha = 1f
        invalidate()
        if (app == null) return
        onDragReleased?.invoke(x, y, moved, cancelled, app, anchor)
    }
}
