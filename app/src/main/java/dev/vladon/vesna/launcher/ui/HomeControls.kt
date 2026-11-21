package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import dev.vladon.vesna.launcher.R

/** Keeps the page picker, Home options and app drawer reachable in narrow windows. */
internal class HomeControls(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val target = resources.getDimensionPixelSize(R.dimen.home_control_target)
    private val gap = resources.getDimensionPixelSize(R.dimen.home_control_gap)
    private val preferredGutter = resources.getDimensionPixelSize(R.dimen.home_control_gutter)
    private lateinit var pageControls: View
    private lateinit var pagePicker: WorkspacePageIndicator
    private lateinit var previous: View
    private lateinit var next: View

    override fun onFinishInflate() {
        super.onFinishInflate()
        pageControls = findViewById(R.id.workspace_page_controls)
        pagePicker = findViewById(R.id.workspace_page_indicator)
        previous = findViewById(R.id.workspace_previous_page)
        next = findViewById(R.id.workspace_next_page)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val bounded = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED
        val hasPicker = pageControls.isVisible
        val otherControlsWidth = 2 * (target + gap)
        val essentials = otherControlsWidth + if (hasPicker) target else -gap
        // Give up outer space before shrinking any of the essential touch targets.
        val gutter = if (bounded) {
            minOf(preferredGutter, ((width - essentials) / 2).coerceAtLeast(0))
        } else preferredGutter
        if (paddingLeft != gutter) setPadding(gutter, 0, gutter, 0)
        val contentWidth = width - 2 * gutter
        val pickerWidth = pagePicker.preferredWidth
        val compact = hasPicker && bounded && contentWidth < otherControlsWidth + pickerWidth + 2 * target

        if (compact && (previous.hasFocus() || next.hasFocus())) pagePicker.requestFocus()
        previous.isVisible = !compact
        next.isVisible = !compact
        // Reserve space for the drawer button, which LinearLayout measures after this group.
        pageControls.layoutParams.width = if (compact) {
            minOf(pickerWidth, (contentWidth - otherControlsWidth).coerceAtLeast(target))
        } else ViewGroup.LayoutParams.WRAP_CONTENT
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
