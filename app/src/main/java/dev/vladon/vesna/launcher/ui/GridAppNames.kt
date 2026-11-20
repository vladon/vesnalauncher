package dev.vladon.vesna.launcher.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** Updates visible tiles without rebinding the adapter or changing focus or scroll position. */
internal class GridAppNames(private val grid: RecyclerView) : RecyclerView.OnChildAttachStateChangeListener {
    var visible = true
        set(value) {
            if (field == value) return
            field = value
            for (index in 0 until grid.childCount) {
                (grid.getChildAt(index) as AppIconView).setAppNameVisible(value)
            }
        }

    init {
        grid.addOnChildAttachStateChangeListener(this)
    }

    override fun onChildViewAttachedToWindow(view: View) {
        // RecyclerView can attach a cached tile without binding it again.
        (view as AppIconView).setAppNameVisible(visible)
    }

    override fun onChildViewDetachedFromWindow(view: View) = Unit
}
