package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Root view of the launcher. Delegates touch interception to the active drag
 * session: when the coordinator arms a session (for example after a long-press in
 * the drawer that must close mid-gesture), the layer takes the stream over from
 * the child and drives the drag through [dragTouchHandler].
 */
class LauncherDragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    /** Called on every event while a session may want to intercept MOVE events. */
    var dragInterceptor: ((MotionEvent) -> Boolean)? = null

    /** Receives root-owned touch events and cancellation of the original stream. */
    var dragTouchHandler: ((MotionEvent) -> Unit)? = null

    internal var isDragActive: (() -> Boolean)? = null
    private var discardTouchStream = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) discardTouchStream = false
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
        // Intercepting a move sends a synthetic CANCEL to the child only. A
        // CANCEL delivered to the root ends the drag, even before interception.
        val cancelled = ev.actionMasked == MotionEvent.ACTION_CANCEL
        val handled = super.dispatchTouchEvent(ev)
        if (cancelled) dragTouchHandler?.invoke(ev)
        return handled
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
        dragInterceptor?.invoke(ev) ?: false

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked != MotionEvent.ACTION_CANCEL) dragTouchHandler?.invoke(ev)
        return true
    }
}
