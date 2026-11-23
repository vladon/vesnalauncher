package dev.vladon.vesna.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.FutureTask

/** The rounded square re-masks adaptive artwork only; legacy icons keep their pixels. */
@RunWith(AndroidJUnit4::class)
class RoundedSquareIconTest {
    @Before
    fun setUp() {
        IconShape.roundedSquare = true
    }

    @After
    fun tearDown() {
        IconShape.roundedSquare = false
    }

    @Test
    fun roundedSquareFillsTheCornersAndKeepsTheCenterAndBadge() = onMain {
        val shaped = render(RoundedSquareIcon(adaptiveIcon()))

        // A quarter in from a corner is outside the system circle but inside the rounded square.
        assertEquals(Color.BLUE, shaped.getPixel(SIZE / 4, SIZE / 4))
        // The very corner stays transparent, so the mask is rounded and not a full square.
        assertEquals(Color.TRANSPARENT, shaped.getPixel(2, 2))
        // Foreground content and a work badge keep their places inside the visible area.
        assertEquals(Color.GREEN, shaped.getPixel(SIZE / 2, SIZE / 2))
        val badge = SIZE * 4 / 5
        assertEquals(Color.WHITE, shaped.getPixel(badge, badge))
    }

    @Test
    fun tilesWrapOnlyAdaptiveArtwork() = onMain {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val adaptive = LauncherApp(AppKey("qa.shape.adaptive", "Main"), "Adaptive", adaptiveIcon())
        val legacy = LauncherApp(AppKey("qa.shape.legacy", "Main"), "Legacy", ColorDrawable(Color.RED))
        val tile = AppIconView(context)
        val icon = tile.findViewById<ImageView>(R.id.app_icon)

        tile.bind(adaptive)
        assertTrue(icon.drawable is RoundedSquareIcon)
        tile.bind(legacy)
        assertFalse("Legacy artwork is not re-masked", icon.drawable is RoundedSquareIcon)

        IconShape.roundedSquare = false
        tile.bind(adaptive)
        assertFalse("The system default leaves the system icon alone", icon.drawable is RoundedSquareIcon)
    }

    /** A blue background and a foreground holding a centered green square and a white work badge. */
    private fun adaptiveIcon(): AdaptiveIconDrawable = AdaptiveIconDrawable(ColorDrawable(Color.BLUE), BadgeForeground())

    private class BadgeForeground : Drawable() {
        private val paint = Paint()

        override fun draw(canvas: Canvas) {
            // A layer is 1.5 times the visible icon and shows its middle two units of three.
            val d = bounds
            val unit = d.width() / 3f
            paint.color = Color.GREEN
            canvas.drawRect(d.centerX() - unit / 3f, d.centerY() - unit / 3f, d.centerX() + unit / 3f, d.centerY() + unit / 3f, paint)
            paint.color = Color.WHITE
            canvas.drawRect(d.left + unit * 1.9f, d.top + unit * 1.9f, d.left + unit * 2.4f, d.top + unit * 2.4f, paint)
        }

        override fun setAlpha(alpha: Int) = Unit

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private fun render(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, SIZE, SIZE)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun onMain(check: () -> Unit) {
        val task = FutureTask<Unit> { check() }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(task)
        task.get()
    }

    private companion object {
        const val SIZE = 100
    }
}
