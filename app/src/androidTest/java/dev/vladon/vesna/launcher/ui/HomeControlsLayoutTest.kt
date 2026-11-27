package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.workspace.MAX_WORKSPACE_PAGES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.FutureTask
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class HomeControlsLayoutTest {
    @Test
    fun allEssentialTargetsFitAtMaximumPagesAndArrowsReturnWhenRoomAllows() {
        val checkLayout = FutureTask<Unit> {
            val context = ApplicationProvider.getApplicationContext<Context>()
                .createConfigurationContext(Configuration().apply {
                    fontScale = 2f
                    densityDpi = 160
                })
            val root = LayoutInflater.from(context).inflate(R.layout.activity_home, null) as ViewGroup
            val controls = root.findViewById<ViewGroup>(R.id.home_controls)
            val picker = root.findViewById<WorkspacePageIndicator>(R.id.workspace_page_indicator)
            val previous = root.findViewById<View>(R.id.workspace_previous_page)
            val next = root.findViewById<View>(R.id.workspace_next_page)
            root.findViewById<View>(R.id.workspace_page_controls).visibility = View.VISIBLE
            picker.bind(MAX_WORKSPACE_PAGES - 1, MAX_WORKSPACE_PAGES)

            val density = context.resources.displayMetrics.density
            fun layout(widthDp: Int) {
                val width = (widthDp * density).roundToInt()
                val height = (640 * density).roundToInt()
                root.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, width, height)
                assertTrue("Control row extends outside Home", controls.left >= 0 && controls.right <= root.width)
            }
            val bounds = Rect()
            val target = (48 * density).roundToInt()
            fun assertContained(view: View) {
                view.getDrawingRect(bounds)
                controls.offsetDescendantRectToMyCoords(view, bounds)
                assertTrue("Touch target $bounds is smaller than 48 dp", bounds.width() >= target && bounds.height() >= target)
                assertTrue("Touch target $bounds is clipped", Rect(0, 0, controls.width, controls.height).contains(bounds))
            }

            for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                root.layoutDirection = direction
                for (width in listOf(184, 220)) {
                    layout(width)
                    assertEquals(direction, controls.layoutDirection)
                    assertEquals(View.GONE, previous.visibility)
                    assertEquals(View.GONE, next.visibility)
                    listOf(picker).forEach(::assertContained)
                }

                layout(320)
                assertEquals(View.VISIBLE, previous.visibility)
                assertEquals(View.VISIBLE, next.visibility)
                listOf(previous, picker, next).forEach(::assertContained)
            }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(checkLayout)
        checkLayout.get()
    }
}
