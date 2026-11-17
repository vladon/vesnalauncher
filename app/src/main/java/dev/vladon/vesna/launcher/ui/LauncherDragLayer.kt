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

    /** Receives MOVE/UP/CANCEL events once the layer owns the drag stream. */
    var dragTouchHandler: ((MotionEvent) -> Unit)? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
        dragInterceptor?.invoke(ev) ?: false

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        dragTouchHandler?.invoke(ev)
        return true
    }
}
