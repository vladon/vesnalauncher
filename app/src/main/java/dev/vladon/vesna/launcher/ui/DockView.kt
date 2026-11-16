package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.GridCanvas

/**
 * Bottom bar with up to [GridCanvas.DOCK_CAPACITY] favorite apps. Weighted slots
 * keep icons aligned with the workspace columns.
 */
class DockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    var onAppClick: ((LauncherApp) -> Unit)? = null
    var onAppLongClick: ((android.view.View, LauncherApp) -> Boolean)? = null

    private val density = resources.displayMetrics.density

    init {
        orientation = HORIZONTAL
        weightSum = GridCanvas.DOCK_CAPACITY.toFloat()
        setBackgroundResource(R.drawable.dock_background)
        elevation = 6 * density
        val pad = (4 * density).toInt()
        setPadding(pad, pad, pad, pad)
    }

    fun bind(apps: List<LauncherApp>) {
        removeAllViews()
        for (app in apps.take(GridCanvas.DOCK_CAPACITY)) {
            val view = AppIconView(context)
            view.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            view.bind(app)
            view.setOnClickListener { onAppClick?.invoke(app) }
            view.setOnLongClickListener { anchor -> onAppLongClick?.invoke(anchor, app) ?: true }
            addView(view)
        }
    }
}
