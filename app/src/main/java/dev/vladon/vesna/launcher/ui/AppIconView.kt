package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
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
    private val labelGap: Int
    private var inlineLabel = false
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
        labelGap = (labelView.layoutParams as MarginLayoutParams).topMargin
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        inlineLabel = false
        val artwork = iconView.layoutParams
        val preferredWidth = artwork.width
        val widthLimit = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            // A fixed-width child would overflow the tile's padded content.
            artwork.width = minOf(preferredWidth, widthLimit)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Reserve the full scaled label line in short, fixed-height Home cells.
        // FIT_CENTER scales the art to either limit without cropping it.
        val line = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) labelView.layout else null
        if (line != null) {
            val lineHeight = line.getLineBottom(0) + labelView.compoundPaddingTop + labelView.compoundPaddingBottom
            if (labelView.measuredHeight < lineHeight) {
                val contentHeight = (measuredHeight - paddingTop - paddingBottom).coerceAtLeast(0)
                val preferredHeight = artwork.height
                val stackedArtHeight = contentHeight - lineHeight - labelGap
                val inlineArtSize = minOf(preferredWidth, preferredHeight, contentHeight)
                val contentWidth = measuredWidth - paddingLeft - paddingRight
                if (stackedArtHeight <= labelGap && contentHeight >= lineHeight &&
                    inlineArtSize > 0 && contentWidth >= 2 * inlineArtSize + labelGap
                ) {
                    // A short, wide cell has room beside the label even when it has none above it.
                    inlineLabel = true
                    val artSpec = MeasureSpec.makeMeasureSpec(inlineArtSize, MeasureSpec.EXACTLY)
                    iconView.measure(artSpec, artSpec)
                    labelView.measure(
                        MeasureSpec.makeMeasureSpec(contentWidth - inlineArtSize - labelGap, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.AT_MOST),
                    )
                } else {
                    artwork.height = stackedArtHeight.coerceIn(minOf(1, contentHeight), preferredHeight)
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                    artwork.height = preferredHeight
                }
            }
        }
        artwork.width = preferredWidth
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (!inlineLabel) {
            super.onLayout(changed, left, top, right, bottom)
            return
        }
        val artWidth = iconView.measuredWidth
        val labelWidth = labelView.measuredWidth
        val start = paddingLeft + (width - paddingLeft - paddingRight - artWidth - labelGap - labelWidth) / 2
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val artLeft = if (rtl) start + labelWidth + labelGap else start
        val labelLeft = if (rtl) start else start + artWidth + labelGap
        val artTop = paddingTop + (height - paddingTop - paddingBottom - iconView.measuredHeight) / 2
        val labelTop = paddingTop + (height - paddingTop - paddingBottom - labelView.measuredHeight) / 2
        iconView.layout(artLeft, artTop, artLeft + artWidth, artTop + iconView.measuredHeight)
        labelView.layout(labelLeft, labelTop, labelLeft + labelWidth, labelTop + labelView.measuredHeight)
    }

    /** Tiles on an opaque drawer or folder surface don't need wallpaper label treatment. */
    internal fun useSurfaceLabel() {
        labelView.setTextColor(context.getColor(R.color.home_control_on_surface))
        labelView.background = null
    }

    internal fun useDrawerListRow() {
        val density = resources.displayMetrics.density
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        minimumHeight = (64 * density + 0.5f).toInt()
        val inset = (16 * density + 0.5f).toInt()
        setPaddingRelative(inset, paddingTop, inset, paddingBottom)
        labelView.typeface = resources.getFont(R.font.inter_regular)
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        labelView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        labelView.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = inset
        }
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
