package dev.vladon.vesna.launcher.notifications

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

/**
 * The notification badge: a solid circle at the artwork's top-end corner
 * (#200/#228 full-copy surface). Presence style draws the bare dot; the
 * numeric style draws the active-notification count inside it (counts are the
 * number of active notifications the listener already tracks — no message
 * content, ADR-NOTIF-1). [scale] applies the user's badge size setting.
 */
class BadgeDotDrawable(
    private val count: Int?,
    private val scale: Float,
    private val density: Float,
    badgeColor: Int,
) : Drawable() {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 9f * density * scale.coerceAtLeast(1f)
        isFakeBoldText = true
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val diameter = 10f * density * scale
        val margin = 2f * density
        val left = b.exactCenterX() - diameter / 2f
        val top = b.top + margin
        canvas.drawOval(RectF(left, top, left + diameter, top + diameter), fill)
        val c = count ?: return
        if (c > 0) {
            val label = if (c > 99) "99+" else c.toString()
            val y = top + diameter / 2f - (text.descent() - text.ascent()) / 2f - text.ascent()
            canvas.drawText(label, left + diameter / 2f, y, text)
        }
    }

    override fun setAlpha(alpha: Int) {
        fill.alpha = alpha
        text.alpha = alpha
    }

    override fun setColorFilter(filter: ColorFilter?) {
        fill.colorFilter = filter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
