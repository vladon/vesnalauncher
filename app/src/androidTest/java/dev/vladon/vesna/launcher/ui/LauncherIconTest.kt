package dev.vladon.vesna.launcher.ui

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.settings.SettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    @Test
    fun installedIconUsesTheOffWhiteFieldAndGreenLeaf() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val installedIcon = context.packageManager.getActivityIcon(ComponentName(context, SettingsActivity::class.java))
        for (drawable in listOf(installedIcon, context.getDrawable(R.mipmap.ic_launcher_round))) {
            val icon = drawable as AdaptiveIconDrawable
            assertEquals("Off-White adaptive icon field", 0xFFF8FAF6.toInt(), icon.background.pixelAt(54, 54))
            assertEquals("Green Vesna leaf", 0xFF90D442.toInt(), icon.foreground.pixelAt(73, 34))
            if (Build.VERSION.SDK_INT >= 33) {
                val silhouette = checkNotNull(icon.monochrome)
                assertEquals("Opaque leaf for system tinting", Color.BLACK, silhouette.pixelAt(73, 34))
                assertEquals("Background excluded from the silhouette", Color.TRANSPARENT, silhouette.pixelAt(54, 17))
            }
        }
    }

    private fun Drawable.pixelAt(x: Int, y: Int): Int {
        val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, bitmap.width, bitmap.height)
        draw(Canvas(bitmap))
        return bitmap.getPixel(x, y)
    }
}
