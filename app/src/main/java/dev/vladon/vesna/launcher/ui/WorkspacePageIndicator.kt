package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import dev.vladon.vesna.launcher.R
import kotlin.math.ceil

/** A noninteractive page position indicator; the adjacent buttons own navigation. */
class WorkspacePageIndicator(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val primary = context.getColor(R.color.workspace_page_primary)
    private val outline = context.getColor(R.color.workspace_page_outline)
    private var selected = -1
    private var count = 0

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
            resolveSize(ceil((count * STEP_DP + STEP_DP) * density).toInt(), widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val step = STEP_DP * density
        val y = height / 2f
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        for (index in 0 until count) {
            val slot = if (rtl) count - 1 - index else index
            val x = width / 2f + (slot - (count - 1) / 2f) * step
            if (index == selected) {
                paint.color = primary
                val radius = 3f * density
                canvas.drawRoundRect(x - step / 2, y - radius, x + step / 2, y + radius, radius, radius, paint)
            } else {
                paint.color = outline
                canvas.drawCircle(x, y, 2.5f * density, paint)
            }
        }
    }

    private companion object {
        const val STEP_DP = 12f
    }
}
