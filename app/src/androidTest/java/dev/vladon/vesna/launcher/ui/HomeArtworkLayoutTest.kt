package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.DockPin
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.GridCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.FutureTask
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class HomeArtworkLayoutTest {
    @Test
    fun narrowWorkspaceFitsAppAndFolderArtAndRestoresFullSize() = checkLayout { context ->
        val workspace = WorkspaceView(context)
        workspace.bind(GridCanvas.DEFAULT, mapOf(
            0 to CellContent.App(app(context, 0)),
            3 to CellContent.FolderCell(Folder(1L, "Folder", cellIndex = 3), memberCount = 2),
        ))
        assertArtworkFitsOnResize(workspace, MeasureSpec.EXACTLY, "Workspace")
    }

    @Test
    fun narrowDockFitsArtInsideWeightedTilesAndRestoresFullSize() = checkLayout { context ->
        val dock = DockView(context)
        dock.bind(List(4) { DockPin(it + 1L, app(context, it)) })
        assertArtworkFitsOnResize(dock, MeasureSpec.AT_MOST, "Dock")
        dock.layoutDirection = View.LAYOUT_DIRECTION_RTL
        assertArtworkFitsOnResize(dock, MeasureSpec.AT_MOST, "RTL dock")
    }

    private fun assertArtworkFitsOnResize(container: ViewGroup, heightMode: Int, surface: String) {
        val density = container.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).roundToInt()
        fun measureAndLayout(widthDp: Int) {
            container.measure(
                MeasureSpec.makeMeasureSpec(dp(widthDp), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(560), heightMode),
            )
            container.layout(0, 0, container.measuredWidth, container.measuredHeight)
        }
        fun assertFullSize() {
            repeat(container.childCount) { index ->
                val artwork = container.getChildAt(index).findViewById<View>(R.id.app_icon)
                assertEquals("$surface at standard width", dp(48), artwork.width)
            }
        }

        measureAndLayout(320)
        assertFullSize()
        measureAndLayout(184)
        repeat(container.childCount) { index ->
            val tile = container.getChildAt(index)
            val artwork = tile.findViewById<View>(R.id.app_icon)
            assertTrue("$surface clips the artwork for ${tile.contentDescription}",
                artwork.left >= tile.paddingLeft && artwork.right <= tile.width - tile.paddingRight)
            assertTrue("$surface should keep the artwork visible", artwork.width > 0 && artwork.height > 0)
        }
        measureAndLayout(320)
        assertFullSize()
    }

    private fun checkLayout(check: (Context) -> Unit) {
        val task = FutureTask<Unit> {
            val context = ApplicationProvider.getApplicationContext<Context>()
                .createConfigurationContext(Configuration().apply { fontScale = 2f })
            check(context)
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(task)
        task.get()
    }

    private fun app(context: Context, index: Int) = LauncherApp(
        AppKey("qa.home.art.$index", "Main"),
        "Calendar $index",
        context.getDrawable(android.R.drawable.ic_dialog_email),
    )
}
