package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Root view of the launcher. After a drawer item recognizes a long press,
 * the layer takes over only once the pointer moves beyond touch slop.
 */
class LauncherDragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    /** Whether a drawer hold is armed and eligible to become a drag. */
    internal var isDrawerHoldArmed: (() -> Boolean)? = null

    /** Receives root-owned touch events and cancellation of the original stream. */
    var dragTouchHandler: ((MotionEvent) -> Unit)? = null

    internal var isDragActive: (() -> Boolean)? = null
    private val dragSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var interceptedMove = false
    private var discardTouchStream = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            discardTouchStream = false
            downX = ev.x
            downY = ev.y
        }
        if (discardTouchStream) return true
        if (action != MotionEvent.ACTION_CANCEL && ev.pointerCount > 1 && isDragActive?.invoke() == true) {
            // Cancel before ViewGroup can split the pointers or hand the drag to another finger.
            discardTouchStream = true
            val cancel = MotionEvent.obtain(ev)
            cancel.action = MotionEvent.ACTION_CANCEL
            try {
                dispatchStream(cancel)
            } finally {
                cancel.recycle()
            }
            return true
        }
        return dispatchStream(ev)
    }

    private fun dispatchStream(ev: MotionEvent): Boolean {
        interceptedMove = false
        val handled = super.dispatchTouchEvent(ev)
        // ViewGroup gives the old target a CANCEL instead of delivering the
        // intercepted MOVE to us. Process it after that target is cancelled.
        val report = interceptedMove || ev.actionMasked == MotionEvent.ACTION_CANCEL
        interceptedMove = false
        if (report) dragTouchHandler?.invoke(ev)
        return handled
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        interceptedMove = ev.actionMasked == MotionEvent.ACTION_MOVE &&
            (abs(ev.x - downX) > dragSlop || abs(ev.y - downY) > dragSlop) &&
            isDrawerHoldArmed?.invoke() == true
        return interceptedMove
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!interceptedMove && ev.actionMasked != MotionEvent.ACTION_CANCEL) dragTouchHandler?.invoke(ev)
        return true
    }
}
