package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.settings.PageIndicator
import kotlin.math.ceil

/** A page picker button whose dots show the current page and total count. */
class WorkspacePageIndicator(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val primary = context.vesnaPaletteColor(R.attr.workspace_page_primary)
    private val outline = context.vesnaPaletteColor(R.attr.vesna_outline)
    private var selected = -1
    private var count = 0

    /** #174: the drawing style; Counter keeps today's dots plus the bar. */
    internal var mode: PageIndicator = PageIndicator.COUNTER
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    internal val preferredWidth: Int
        get() = ceil(((count + 1) * STEP_DP).coerceAtMost(MAX_WIDTH_DP) * density)
            .toInt().coerceAtLeast(suggestedMinimumWidth)

    override fun getAccessibilityClassName(): CharSequence = Button::class.java.name

    fun bind(position: Int, pageCount: Int) {
        if (position == selected && pageCount == count) return
        if (pageCount != count) requestLayout()
        selected = position
        count = pageCount
        contentDescription = if (position >= 0) {
            context.getString(R.string.workspace_page_position, position + 1, count)
        } else null
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(preferredWidth, widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val step = minOf(STEP_DP * density, width / (count + 1f))
        val y = height / 2f
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        if (mode == PageIndicator.LINE) {
            // A thin baseline with the active page's segment highlighted.
            val start = width / 2f - count * step / 2f
            paint.color = outline
            paint.strokeWidth = 2f * density
            canvas.drawLine(start, y, start + count * step, y, paint)
            val slot = if (rtl) count - 1 - selected else selected
            if (selected in 0 until count) {
                paint.color = primary
                paint.strokeWidth = 3.5f * density
                val segmentStart = start + slot * step + step * 0.2f
                canvas.drawLine(segmentStart, y, segmentStart + step * 0.6f, y, paint)
            }
            return
        }
        for (index in 0 until count) {
            val slot = if (rtl) count - 1 - index else index
            val x = width / 2f + (slot - (count - 1) / 2f) * step
            if (index == selected) {
                paint.color = primary
                if (mode == PageIndicator.DOT) {
                    canvas.drawCircle(x, y, 3f * density, paint)
                } else {
                    val radius = 3f * density
                    canvas.drawRoundRect(x - step / 2, y - radius, x + step / 2, y + radius, radius, radius, paint)
                }
            } else {
                paint.color = outline
                canvas.drawCircle(x, y, 2.5f * density, paint)
            }
        }
    }

    private companion object {
        const val STEP_DP = 12f
        // Leave 16 dp at each side of the full Home control row on a 320 dp display.
        const val MAX_WIDTH_DP = 80f
    }
}
