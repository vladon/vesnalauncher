package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

/**
 * The workspace cell that hosts a committed app widget (ENG-102 slice 2).
 * Sized by [spanX]/[spanY] grid cells by [WorkspaceView] measure/layout; the
 * platform host view is installed once and survives rebinding.
 */
class WidgetCellView(context: Context) : FrameLayout(context) {

    var spanX: Int = 2
    var spanY: Int = 2

    /** Fired on a long press; taps still reach the live widget. */
    var onWidgetLongPress: (() -> Unit)? = null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                onWidgetLongPress?.invoke()
            }
        },
    )

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Observe the gesture for the cell options without stealing taps from
        // the platform host view.
        gestureDetector.onTouchEvent(ev)
        return false
    }

    /** Installs the host view once; a rebind keeps the live widget instance. */
    fun installHost(view: View) {
        if (childCount > 0) return
        addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }
}
