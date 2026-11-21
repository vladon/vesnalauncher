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
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val essentialWidth = 2 * (target + gap)
        val pickerWidth = pagePicker.preferredWidth
        val compact = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED &&
            availableWidth < essentialWidth + 2 * target + pickerWidth

        if (compact && (previous.hasFocus() || next.hasFocus())) pagePicker.requestFocus()
        previous.isVisible = !compact
        next.isVisible = !compact
        // Reserve space for the drawer button, which LinearLayout measures after this group.
        pageControls.layoutParams.width = if (compact) {
            minOf(pickerWidth, (availableWidth - essentialWidth).coerceAtLeast(target))
        } else ViewGroup.LayoutParams.WRAP_CONTENT
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
