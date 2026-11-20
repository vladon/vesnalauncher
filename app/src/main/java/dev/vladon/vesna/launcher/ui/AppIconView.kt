package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp

/**
 * An app icon with its label, used by the workspace, the dock, and the drawer.
 * Renders each app's icon from a fresh drawable so views never share one
 * mutable Drawable instance.
 */
class AppIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val iconView: ImageView
    private val labelView: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val pad = (6 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        // Ripple feedback for taps and long presses.
        val value = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
        if (value.resourceId != 0) setBackgroundResource(value.resourceId)
        inflate(context, R.layout.view_app_icon, this)
        iconView = findViewById(R.id.app_icon)
        labelView = findViewById(R.id.app_label)
    }

    /** Tiles on an opaque drawer or folder surface don't need wallpaper label treatment. */
    internal fun useSurfaceLabel() {
        labelView.setTextColor(context.getColor(R.color.home_control_on_surface))
        labelView.background = null
    }

    fun bind(app: LauncherApp) {
        bindContent(
            icon = freshDrawable(app.icon),
            label = app.label,
            description = app.label,
        )
    }

    /** Low-level bind for non-app content (folders). */
    fun bindContent(icon: Drawable?, label: String, description: String) {
        iconView.setImageDrawable(freshDrawable(icon))
        labelView.text = label
        contentDescription = description
    }

    private fun freshDrawable(drawable: Drawable?): Drawable? =
        drawable?.constantState?.newDrawable() ?: drawable
}
