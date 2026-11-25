package dev.vladon.vesna.launcher.ui

import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.DockPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The cold-start two-phase catalogue (#195): the fast phase decodes icons only
 * for the saved layout, so tiles can briefly carry a null icon. Views must
 * render those tiles with their label and no crash until the second pass fills
 * the artwork in.
 */
@RunWith(AndroidJUnit4::class)
class NullIconCatalogueTest {

    private fun <T : View> sized(view: T): T {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 200, 200)
        return view
    }

    @Test
    fun tilesWithoutIconsStillShowTheirLabel() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val withoutIcon = LauncherApp(AppKey("qa.nullicon", "Main"), "Null Icon App")
        var result: Result<Unit>? = null
        instrumentation.runOnMainSync {
            result = runCatching {
                val tile = sized(AppIconView(instrumentation.targetContext))
                tile.bind(withoutIcon)
                assertEquals(
                    "The label renders even while the icon is pending",
                    "Null Icon App",
                    tile.findViewById<TextView>(R.id.app_label).text,
                )
                assertEquals("Null Icon App", tile.contentDescription)
            }
        }
        result!!.getOrThrow()
    }

    @Test
    fun dockBindsAMixedIconCatalogue() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val withoutIcon = LauncherApp(AppKey("qa.nullicon", "Main"), "Null Icon App")
        val withIcon = LauncherApp(AppKey("qa.withicon", "Main"), "With Icon")
        var result: Result<Unit>? = null
        instrumentation.runOnMainSync {
            result = runCatching {
                val dock = sized(DockView(instrumentation.targetContext))
                dock.bind(listOf(DockPin(1L, withoutIcon), DockPin(2L, withIcon)))
                assertEquals(2, dock.childCount)
                assertTrue(
                    "The pending-icon pin keeps its description",
                    dock.getChildAt(0).contentDescription?.contains("Null Icon App") == true,
                )
            }
        }
        result!!.getOrThrow()
    }
}
