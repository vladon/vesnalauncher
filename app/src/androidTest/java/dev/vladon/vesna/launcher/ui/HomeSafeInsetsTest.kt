package dev.vladon.vesna.launcher.ui

import android.graphics.Rect
import android.view.View
import android.view.View.MeasureSpec
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.DockPin
import dev.vladon.vesna.launcher.workspace.GridCanvas
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class HomeSafeInsetsTest {
    @Test
    fun outsideWorkspaceAndDockTilesStayClearOfSideBars() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<LauncherDragLayer>(R.id.launcher_root)
                val workspace = activity.findViewById<WorkspaceView>(R.id.workspace)
                val dock = activity.findViewById<DockView>(R.id.dock)
                val density = root.resources.displayMetrics.density
                fun dp(value: Int) = (value * density).roundToInt()
                val width = dp(720)
                val height = dp(400)
                val left = dp(54)
                val right = dp(48)
                val pins = List(4) { DockPin(it + 1L, LauncherApp(AppKey("qa.side$it", "Main"), "App $it")) }
                workspace.bind(GridCanvas.DEFAULT, mapOf(0 to CellContent.App(pins.first().app), 3 to CellContent.App(pins.last().app)))
                dock.bind(pins)
                ViewCompat.dispatchApplyWindowInsets(root, WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(left, 0, right, 0))
                    .build())
                root.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
                root.layout(0, 0, width, height)
                for (container in listOf(workspace, dock)) {
                    for (index in 0 until container.childCount) {
                        val tile = container.getChildAt(index)
                        val bounds = Rect(0, 0, tile.width, tile.height)
                        root.offsetDescendantRectToMyCoords(tile, bounds)
                        assertTrue("${tile.contentDescription} in ${container.javaClass.simpleName} overlaps a side bar: $bounds",
                            bounds.left >= left && bounds.right <= width - right)
                    }
                }
            }
        }
    }
}
