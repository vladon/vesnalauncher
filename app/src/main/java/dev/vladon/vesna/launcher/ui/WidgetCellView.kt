package dev.vladon.vesna.launcher.ui

import android.content.Context
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
