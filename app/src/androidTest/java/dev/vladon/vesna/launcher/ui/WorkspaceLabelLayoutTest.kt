package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.View.MeasureSpec
import android.widget.TextView
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

@RunWith(AndroidJUnit4::class)
class WorkspaceLabelLayoutTest {
    @Test
    fun shortCellsFitCompleteScaledLinesWithoutLosingTapTargets() {
        val checkLayout = FutureTask<Unit>(::verifyCompactLayout)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(checkLayout)
        checkLayout.get()
    }

    private fun verifyCompactLayout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
            .createConfigurationContext(Configuration().apply { fontScale = 2f })
        val density = context.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).roundToInt()

        val appName = "Calendar and scheduling"
        val folderName = "Семейные фотографии"
        val workspace = WorkspaceView(context)
        workspace.bind(
            GridCanvas.DEFAULT,
            mapOf(
                0 to CellContent.App(LauncherApp(AppKey("qa.calendar", "Main"), appName, ColorDrawable(Color.BLUE))),
                1 to CellContent.FolderCell(Folder(1L, folderName, cellIndex = 1), memberCount = 2),
            ),
        )
        fun layout(height: Int, width: Int = 320) {
            val widthPx = dp(width)
            val heightPx = dp(height)
            workspace.measure(
                MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY),
            )
            workspace.layout(0, 0, widthPx, heightPx)
        }

        val tiles = listOf(workspace.getChildAt(0), workspace.getChildAt(1))
        val labels = tiles.map { it.findViewById<TextView>(R.id.app_label) }
        val artwork = tiles.map { it.findViewById<View>(R.id.app_icon) }
        layout(height = 560)
        val fullLineHeights = labels.map { it.height }
        val fullArtworkHeights = artwork.map { it.height }

        layout(height = 376)
        labels.forEachIndexed { i, label ->
            assertEquals("${label.text} should have room for the whole scaled line", fullLineHeights[i], label.height)
            assertTrue(label.layout.getEllipsisCount(0) > 0)
            assertTrue(label.bottom <= tiles[i].height)
            assertTrue(artwork[i].height in 1 until fullArtworkHeights[i])
            assertTrue(tiles[i].width >= dp(48) && tiles[i].height >= dp(48))
        }
        assertEquals(listOf(appName, folderName), tiles.map { it.contentDescription })

        // Landscape tiles can be shorter than the preferred artwork alone.
        layout(height = 560, width = 640)
        val landscapeLineHeights = labels.map { it.height }
        layout(height = 270, width = 640)
        labels.forEachIndexed { i, label ->
            assertEquals("Landscape must fit a complete label line", landscapeLineHeights[i], label.height)
            assertTrue(label.bottom <= tiles[i].height)
            assertTrue(artwork[i].height in 1 until fullArtworkHeights[i])
        }

        // A wide tile with no space above its label must still draw the app or folder art.
        for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
            labels.forEachIndexed { i, label ->
                val tile = tiles[i]
                val icon = artwork[i]
                val height = landscapeLineHeights[i] + tile.paddingTop + tile.paddingBottom
                val width = tile.width
                tile.layoutDirection = direction
                tile.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
                )
                tile.layout(0, 0, width, height)
                assertEquals("The scaled label must stay complete", landscapeLineHeights[i], label.height)
                assertEquals("Artwork must use the available height", landscapeLineHeights[i], icon.height)
                val gap = if (direction == View.LAYOUT_DIRECTION_RTL) icon.left - label.right else label.left - icon.right
                assertEquals(dp(4), gap)
                for (child in listOf(icon, label)) {
                    assertTrue(child.left >= tile.paddingLeft && child.right <= width - tile.paddingRight)
                    assertTrue(child.top >= tile.paddingTop && child.bottom <= height - tile.paddingBottom)
                }
            }
        }

        layout(height = 560)
        assertEquals(fullArtworkHeights, artwork.map { it.height })
    }
}
