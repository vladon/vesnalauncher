package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.View.MeasureSpec
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.GridCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.FutureTask
import kotlin.math.roundToInt

/** The icon size settings scale artwork only: tiles, labels, cells and list rows keep their size. */
@RunWith(AndroidJUnit4::class)
class IconScaleLayoutTest {

    @Test
    fun workspaceScaleGrowsOnlyTheArtworkAndStillFitsNarrowCells() = checkLayout { context ->
        val workspace = WorkspaceView(context)
        workspace.bind(
            GridCanvas.DEFAULT,
            mapOf(
                0 to CellContent.App(app(context, 0)),
                3 to CellContent.FolderCell(Folder(1L, "Folder", cellIndex = 3), memberCount = 2),
            ),
        )
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
        fun measureAndLayout(widthDp: Int) {
            workspace.measure(
                MeasureSpec.makeMeasureSpec(dp(widthDp), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(560), MeasureSpec.EXACTLY),
            )
            workspace.layout(0, 0, workspace.measuredWidth, workspace.measuredHeight)
        }
        fun tiles() = (0 until workspace.childCount).map(workspace::getChildAt)
        fun View.artwork() = findViewById<View>(R.id.app_icon)
        fun View.label() = findViewById<View>(R.id.app_label)

        measureAndLayout(320)
        val before = tiles().map { Triple(it.width to it.height, it.label().top to it.label().height, it.artwork().width) }
        assertTrue("The default is the unscaled artwork", before.all { it.third == dp(48) })

        workspace.iconScale = 1.3f
        measureAndLayout(320)
        tiles().forEachIndexed { index, tile ->
            assertEquals("Tile $index keeps its size", before[index].first, tile.width to tile.height)
            assertEquals("Tile $index keeps its label size", before[index].second.second, tile.label().height)
            assertEquals("Tile $index keeps its label below the artwork", tile.artwork().bottom + dp(4), tile.label().top)
            val limit = tile.width - tile.paddingLeft - tile.paddingRight
            assertEquals("Tile $index grows only its artwork", minOf((before[index].third * 1.3f + 0.5f).toInt(), limit), tile.artwork().width)
            assertTrue("Tile $index artwork grows", tile.artwork().width > before[index].third)
        }

        measureAndLayout(184)
        tiles().forEach { tile ->
            val artwork = tile.artwork()
            assertTrue("A narrow tile still fits the enlarged artwork",
                artwork.width > 0 && artwork.width <= tile.width - tile.paddingLeft - tile.paddingRight)
        }

        workspace.iconScale = 1f
        measureAndLayout(320)
        tiles().forEachIndexed { index, tile -> assertEquals(before[index].third, tile.artwork().width) }
    }

    @Test
    fun drawerListRowKeepsItsHeightAtEveryScale() = checkLayout { context ->
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
        val row = AppIconView(context).apply {
            useSurfaceLabel()
            useDrawerListRow()
            bind(app(context, 0))
        }
        val artwork = row.findViewById<View>(R.id.app_icon)
        for (scale in listOf(0.8f, 1f, 1.3f)) {
            row.artworkScale = scale
            row.measure(
                MeasureSpec.makeMeasureSpec(dp(320), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            row.layout(0, 0, row.measuredWidth, row.measuredHeight)
            assertEquals("Row height at scale $scale", dp(64), row.measuredHeight)
            val rowLimit = row.minimumHeight - row.paddingTop - row.paddingBottom
            assertEquals("Artwork at scale $scale", (dp(48) * scale + 0.5f).toInt().coerceAtMost(rowLimit), artwork.height)
            assertEquals("The artwork stays square at scale $scale", artwork.height, artwork.width)
            assertTrue("The artwork stays inside the row", artwork.top >= 0 && artwork.bottom <= row.height)
        }
    }

    @Test
    fun drawerGridTileKeepsItsCellBelowTheDefaultSize() = checkLayout { context ->
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
        fun tile(scale: Float) = AppIconView(context).apply {
            useSurfaceLabel()
            keepDefaultTileHeight()
            artworkScale = scale
            bind(app(context, 0))
            measure(
                MeasureSpec.makeMeasureSpec(dp(96), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            layout(0, 0, measuredWidth, measuredHeight)
        }
        val normal = tile(1f)
        val small = tile(0.8f)
        assertEquals("A smaller artwork leaves the cell as it was", normal.height, small.height)
        assertEquals("The cell keeps its width", dp(96), small.width)
        assertEquals("Artwork at 80 %", (dp(48) * 0.8f + 0.5f).toInt(), small.findViewById<View>(R.id.app_icon).width)
        assertTrue("The artwork sits inside the cell", small.findViewById<View>(R.id.app_icon).top >= small.paddingTop)
    }

    @Test
    fun recycledDrawerTileTakesTheCurrentScaleWhenRebound() = checkLayout { context ->
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
        val adapter = DrawerAppsAdapter(onItemClick = {}, onItemOptions = { _, _ -> }, onItemTouchHold = { _, _ -> }, onAppsCommitted = {})
        // A holder created while the drawer still used the default size.
        val tile = AppIconView(context).apply {
            useSurfaceLabel()
            keepDefaultTileHeight()
            artworkScale = adapter.iconScale
        }
        adapter.iconScale = 1.3f
        adapter.bindTile(tile, app(context, 0))
        assertEquals("A rebound tile uses the new size", (dp(48) * 1.3f + 0.5f).toInt(), tile.findViewById<View>(R.id.app_icon).layoutParams.width)
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
        AppKey("qa.icon.scale.$index", "Main"),
        "Calendar $index",
        context.getDrawable(android.R.drawable.ic_dialog_email),
    )
}
