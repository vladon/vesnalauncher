package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
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
    private var pointerDown = false
    private var showOptions: ((View) -> Unit)? = null
    private var dragOnHold: ((View) -> Unit)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val pad = (6 * resources.displayMetrics.density).toInt()
        setBackgroundResource(R.drawable.app_tile_states)
        setPadding(pad, pad, pad, pad)
        inflate(context, R.layout.view_app_icon, this)
        iconView = findViewById(R.id.app_icon)
        labelView = findViewById(R.id.app_label)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val artwork = iconView.layoutParams
        val preferredWidth = artwork.width
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            // A fixed-width child would overflow the tile's padded content.
            val contentWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            artwork.width = minOf(preferredWidth, contentWidth.coerceAtLeast(0))
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Reserve the full scaled label line in short, fixed-height Home cells.
        // FIT_CENTER scales the art to either limit without cropping it.
        val line = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) labelView.layout else null
        if (line != null) {
            val lineHeight = line.getLineBottom(0) + labelView.compoundPaddingTop + labelView.compoundPaddingBottom
            val artworkHeight = iconView.measuredHeight - (lineHeight - labelView.measuredHeight)
            if (artworkHeight in 1 until iconView.measuredHeight) {
                val preferredHeight = artwork.height
                artwork.height = artworkHeight
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                artwork.height = preferredHeight
            }
        }
        artwork.width = preferredWidth
    }

    /** Tiles on an opaque drawer or folder surface don't need wallpaper label treatment. */
    internal fun useSurfaceLabel() {
        labelView.setTextColor(context.getColor(R.color.home_control_on_surface))
        labelView.background = null
    }

    internal fun setAppNameVisible(visible: Boolean) {
        // Keep the label's space so neither the artwork nor the touch target moves.
        labelView.visibility = if (visible) VISIBLE else INVISIBLE
    }

    internal fun bindOptions(
        onOptions: (View) -> Unit,
        onTouchHold: ((View) -> Unit)? = null,
    ) {
        if (showOptions == null) {
            setOnLongClickListener { anchor ->
                // A drag needs a real pointer stream to deliver MOVE, UP, or CANCEL.
                val drag = dragOnHold
                if (pointerDown && drag != null) {
                    drag(anchor)
                    true
                } else {
                    openOptions()
                }
            }
        }
        showOptions = onOptions
        dragOnHold = onTouchHold
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> pointerDown = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> pointerDown = false
        }
        val handled = super.dispatchTouchEvent(event)
        if (!handled && event.actionMasked == MotionEvent.ACTION_DOWN) pointerDown = false
        return handled
    }

    override fun onDetachedFromWindow() {
        pointerDown = false
        super.onDetachedFromWindow()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (isEnabled && showOptions != null) {
            info.removeAction(AccessibilityAction.ACTION_LONG_CLICK)
            info.addAction(AccessibilityAction(
                AccessibilityAction.ACTION_LONG_CLICK.id,
                context.getText(R.string.shortcut_options),
            ))
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (action == AccessibilityAction.ACTION_LONG_CLICK.id && openOptions()) return true
        return super.performAccessibilityAction(action, arguments)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isEnabled && showOptions != null &&
            (keyCode == KeyEvent.KEYCODE_MENU || (keyCode == KeyEvent.KEYCODE_F10 && event.isShiftPressed))
        ) {
            if (event.repeatCount == 0) event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_F10) &&
            event.isTracking && !event.isCanceled && openOptions()
        ) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun openOptions(): Boolean {
        val action = showOptions ?: return false
        if (!isEnabled) return false
        action(this)
        return true
    }

    fun bind(app: LauncherApp) {
        bindContent(
            icon = freshDrawable(app.icon),
            label = app.label,
            description = app.accessibilityLabel,
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
