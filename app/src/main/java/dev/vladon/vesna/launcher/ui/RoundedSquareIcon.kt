package dev.vladon.vesna.launcher.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/** The shape Vesna gives adaptive app icons; set from the Appearance setting before tiles bind. */
internal object IconShape {
    @Volatile
    var roundedSquare = false
}

/**
 * Draws an adaptive icon's own layers inside a rounded square instead of the
 * system mask. The layers keep their adaptive insets, so foreground content
 * and any profile badge stay where the system puts them. Legacy artwork never
 * reaches this class.
 */
internal class RoundedSquareIcon(private val source: AdaptiveIconDrawable) : Drawable() {
    private val mask = Path()
    private val maskBounds = RectF()
    private val layerBounds = Rect()

    override fun onBoundsChange(bounds: Rect) {
        maskBounds.set(bounds)
        val radius = bounds.width() * CORNER_FRACTION
        mask.reset()
        mask.addRoundRect(maskBounds, radius, radius, Path.Direction.CW)
        // An adaptive icon shows the middle 72 of 108 layer units, a quarter of the icon on each side.
        val dx = bounds.width() / 4
        val dy = bounds.height() / 4
        layerBounds.set(bounds.left - dx, bounds.top - dy, bounds.right + dx, bounds.bottom + dy)
    }

    override fun draw(canvas: Canvas) {
        val checkpoint = canvas.save()
        canvas.clipPath(mask)
        drawLayer(source.background, canvas)
        drawLayer(source.foreground, canvas)
        canvas.restoreToCount(checkpoint)
    }

    /** State, layout direction, alpha and color filters reach the layers through [source]. */
    override fun isStateful() = source.isStateful

    override fun onStateChange(state: IntArray): Boolean {
        source.state = state
        return true
    }

    override fun onLayoutDirectionChanged(direction: Int): Boolean {
        source.layoutDirection = direction
        return true
    }

    override fun setAlpha(alpha: Int) = source.setAlpha(alpha)

    override fun setColorFilter(colorFilter: ColorFilter?) = source.setColorFilter(colorFilter)

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getOutline(outline: Outline) {
        val bounds = bounds
        outline.setRoundRect(bounds, bounds.width() * CORNER_FRACTION)
    }

    private fun drawLayer(layer: Drawable?, canvas: Canvas) {
        if (layer == null) return
        layer.bounds = layerBounds
        layer.draw(canvas)
    }

    private companion object {
        const val CORNER_FRACTION = 0.22f
    }
}
