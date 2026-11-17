package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.LinearLayout
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.GridCanvas

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

    /** The user lifted a dock icon by long-pressing it; a drag session has begun. */
    var onDragStarted: ((slot: Int, app: LauncherApp, anchor: android.view.View) -> Unit)? = null

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
    private var boundApps: List<LauncherApp> = emptyList()

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

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (dragSourceApp != null) {
            // An active drag takes the gesture over from the icon.
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (dragSourceApp != null) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    dragMoved = true
                    onDragProgress?.invoke(ev.x, ev.y)
                }
                MotionEvent.ACTION_UP -> endDrag(ev.x, ev.y, cancelled = false)
                MotionEvent.ACTION_CANCEL -> endDrag(ev.x, ev.y, cancelled = true)
            }
            return true
        }
        return super.onTouchEvent(ev)
    }

    /** Starts a drag for the dock icon at [slot]; the item dims until it is dropped. */
    private fun beginDrag(slot: Int, app: LauncherApp, anchor: android.view.View) {
        dragSourceApp = app
        dragAnchor = anchor
        dragMoved = false
        dragTargetSlot = null
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        anchor.alpha = 0.45f
        onDragStarted?.invoke(slot, app, anchor)
    }

    /** Shows (slot != null) or hides the drop-target highlight for drag sessions. */
    fun showSlotHighlight(slot: Int?) {
        val clamped = slot?.coerceIn(0, GridCanvas.DOCK_CAPACITY - 1)
        if (clamped != dragTargetSlot) {
            dragTargetSlot = clamped
            invalidate()
        }
    }

    /** The dock slot whose column contains dock-local [x], clamped into the dock. */
    fun slotAtLocalX(x: Float): Int {
        val slotWidth = width / GridCanvas.DOCK_CAPACITY.toFloat()
        if (slotWidth <= 0f || width <= 0) return 0
        return (x / slotWidth).toInt().coerceIn(0, GridCanvas.DOCK_CAPACITY - 1)
    }

    /** Number of currently bound dock apps (drags must respect dock capacity). */
    val boundCount: Int get() = boundApps.size

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val slot = dragTargetSlot ?: return
        if (width <= 0) return
        val slotWidth = width / GridCanvas.DOCK_CAPACITY.toFloat()
        val inset = (6 * density).toInt()
        val radius = 20f * density
        val x = paddingLeft + slot * slotWidth + inset
        canvas.drawRoundRect(
            x,
            paddingTop + inset.toFloat(),
            x + slotWidth - 2 * inset,
            height - paddingBottom - inset.toFloat(),
            radius,
            radius,
            highlightPaint,
        )
    }

    fun bind(apps: List<LauncherApp>) {
        boundApps = apps
        removeAllViews()
        for (app in apps.take(GridCanvas.DOCK_CAPACITY)) {
            val view = AppIconView(context)
            view.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            view.bind(app)
            view.setOnClickListener { onAppClick?.invoke(app) }
            view.setOnLongClickListener { anchor ->
                beginDrag(boundApps.indexOf(app), app, anchor)
                true
            }
            addView(view)
        }
    }

    private fun endDrag(x: Float, y: Float, cancelled: Boolean) {
        val app = dragSourceApp
        val anchor = dragAnchor
        val moved = dragMoved
        dragSourceApp = null
        dragAnchor = null
        dragTargetSlot = null
        dragMoved = false
        anchor?.alpha = 1f
        invalidate()
        if (app == null || anchor == null) return
        onDragReleased?.invoke(x, y, moved, cancelled, app, anchor)
    }
}
