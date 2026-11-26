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
import dev.vladon.vesna.launcher.settings.LabelFont
import dev.vladon.vesna.launcher.workspace.DockPages
import dev.vladon.vesna.launcher.workspace.DockPin
import kotlin.math.abs

/**
 * Bottom bar with favorite apps: [capacity] weighted slots per page, extra pins
 * flowing onto a second page in order. Weighted slots keep icons aligned with
 * the workspace columns.
 *
 * Long-pressing an icon starts a drag session: the view dims the icon, reports
 * progress in dock-local coordinates and lets [HomeActivity] decide the drop target
 * (another dock slot for reorder, or a workspace cell). Drop slots are highlighted
 * via [showSlotHighlight]. A horizontal swipe with no drag in progress flips the
 * page; pages navigate independently of the workspace.
 */
class DockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    var labelFontScalePercent: Int = 100
    var labelFont: LabelFont = LabelFont.INTER_MEDIUM

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

    /** Scales only the artwork of every tile; slots, labels and saved pins keep their size. */
    internal var iconScale = 1f
        set(value) {
            if (field == value) return
            field = value
            for (index in 0 until childCount) (getChildAt(index) as AppIconView).artworkScale = value
        }

    /** Slots per page; bound pins beyond the capacity wait on the second page. */
    internal var capacity: Int = DockPages.DEFAULT_CAPACITY
        set(value) {
            val coerced = DockPages.coerce(value)
            if (field == coerced) return
            field = coerced
            weightSum = coerced.toFloat()
            currentPage = DockPages.coercePage(currentPage, pins.size, coerced)
            renderPage()
        }

    /** Pages needed for the bound pins; at least one even when the dock is empty. */
    val pageCount: Int
        get() = DockPages.pageCount(pins.size, capacity)

    var currentPage: Int = 0
        private set

    private var pins: List<DockPin> = emptyList()

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

    /** True from ACTION_DOWN until a dock drag claims the gesture, so a sweep may page. */
    private var pageGesture = false
    private var pageMoved = false

    private companion object {
        const val PAGE_ANIMATION_MS = 150L
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x2E4CAF50
        style = Paint.Style.FILL
    }

    init {
        orientation = HORIZONTAL
        weightSum = capacity.toFloat()
        setBackgroundResource(R.drawable.dock_background)
        elevation = 6 * density
        val pad = (4 * density).toInt()
        setPadding(pad, pad, pad, pad)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.x
            downY = event.y
            // A new gesture may page the dock only while no drag owns the stream.
            pageGesture = dragSourceApp == null
            pageMoved = false
        }
        return super.dispatchTouchEvent(event)
    }

    // A horizontal sweep with no dock drag in progress flips the page. Long-press
    // starts a drag while the finger is still still, so an active drag always wins.
    // The sweep may begin while a tile owns the gesture (interception) or while the
    // bar itself owns it — for example when the page animation has carried the tiles
    // out from under the finger — so both paths feed the same detector.
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_MOVE) return false
        if (!sweepMayPage || pageMoved) return false
        if (!sweepStarted(event.x, event.y)) return false
        pageMoved = true
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (sweepMayPage && !pageMoved && sweepStarted(event.x, event.y)) pageMoved = true
            }
            MotionEvent.ACTION_UP -> {
                if (!pageMoved) return true
                pageMoved = false
                val forward = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                    event.x > downX
                } else {
                    event.x < downX
                }
                showPage(currentPage + if (forward) 1 else -1)
            }
            MotionEvent.ACTION_CANCEL -> pageMoved = false
        }
        return true
    }

    /** Paging stays off while a dock drag owns the stream or there is one page. */
    private val sweepMayPage: Boolean
        get() = pageGesture && dragSourceApp == null && pageCount > 1

    private fun sweepStarted(x: Float, y: Float): Boolean {
        val dx = x - downX
        return abs(dx) > dragSlop && abs(dx) > abs(y - downY)
    }

    /** Flips to [page] (clamped) and slides the incoming tiles in from the swipe edge. */
    internal fun showPage(page: Int) {
        val target = DockPages.coercePage(page, pins.size, capacity)
        if (target == currentPage) return
        val previous = currentPage
        currentPage = target
        renderPage()
        // Slide from the physical edge the content came from, mirrored in RTL.
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val cameFromEnd = if (rtl) target < previous else target > previous
        val offset = (if (cameFromEnd) 1 else -1) * width / 4f
        for (index in 0 until childCount) {
            val tile = getChildAt(index)
            tile.translationX = offset
            tile.animate().translationX(0f).setDuration(PAGE_ANIMATION_MS).start()
        }
    }

    /** The saved-order index behind a page-local child, for Home's focus and menus. */
    internal fun pinIndexAt(childIndex: Int): Int = currentPage * capacity + childIndex

    /** The page tile showing the saved-order pin at [pinIndex], if this page shows it. */
    internal fun childAtPin(pinIndex: Int): View? {
        val childIndex = pinIndex - currentPage * capacity
        return if (childIndex in 0 until childCount) getChildAt(childIndex) else null
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
        val clamped = slot?.coerceIn(0, capacity - 1)
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
        for (slot in 0 until capacity - 1) {
            val edge = slotOffset(slot + 1, contentWidth)
            // Keep the physical left edge inclusive, like View hit testing.
            val inSlot = if (rtl) fromStart <= edge else fromStart < edge
            if (inSlot) return slot
        }
        return capacity - 1
    }

    private fun slotOffset(slot: Int, contentWidth: Int): Int {
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

    /** Rebinds the saved pin order; the current page is clamped into the new pages. */
    fun bind(pins: List<DockPin>) {
        this.pins = pins
        currentPage = DockPages.coercePage(currentPage, pins.size, capacity)
        renderPage()
    }

    /** Re-renders the current dock page so notification dots follow the badge state (#200). */
    internal fun refreshNotificationDots() {
        renderPage()
    }

    /** Re-renders the current page so label typography follows (#173). */
    internal fun refreshLabelTypography(percent: Int, font: LabelFont) {
        labelFontScalePercent = percent
        labelFont = font
        renderPage()
    }

    private fun renderPage() {
        removeAllViews()
        for (pin in DockPages.page(pins, currentPage, capacity)) {
            val app = pin.app
            val view = AppIconView(context)
            view.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            view.bind(app)
            view.setLabelTypography(labelFontScalePercent, labelFont)
            view.artworkScale = iconScale
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
