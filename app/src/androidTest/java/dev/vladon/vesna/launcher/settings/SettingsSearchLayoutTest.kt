package dev.vladon.vesna.launcher.settings

import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class SettingsSearchLayoutTest {
    @Test
    fun shortDirectoryKeepsSearchEditableAndRetainsQueryWhenItExpands() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = instrumentation.targetContext
        val minimumHeight = (56 * context.resources.displayMetrics.density).roundToInt()
        val expanded = mutableStateOf(false)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    VesnaTheme(dark = false) {
                        // Model the foreground space left by a docked keyboard, without
                        // depending on the height of the emulator's installed keyboard.
                        Box(Modifier.fillMaxWidth().height(if (expanded.value) 520.dp else 144.dp)) {
                            SettingsScreen(
                                page = SettingsPage.ROOT,
                                onNavigate = {},
                                onResetInputFocus = {},
                                versionName = "",
                                defaultHome = DefaultHomeState.AVAILABLE,
                                onSetDefaultHome = {},
                                themeMode = ThemeMode.SYSTEM,
                                onThemeSelected = {},
                                showAppNames = true,
                                onShowAppNamesChanged = {},
                            )
                        }
                    }
                }
            }
            fun editor() = automation.rootInActiveWindow?.findVisible { it.isEditable }
            assertTrue("The Settings editor did not fit in the short viewport", until {
                val field = editor() ?: return@until false
                val bounds = Rect().also(field::getBoundsInScreen)
                bounds.height() >= minimumHeight
            })
            val query = context.getString(R.string.settings_theme)
            assertTrue(editor()!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
            }))
            assertTrue("The short editor did not accept the query", until { editor()?.text?.toString() == query })
            scenario.onActivity { expanded.value = true }
            assertTrue("The query or matching entry disappeared after expansion", until {
                editor()?.text?.toString() == query && automation.rootInActiveWindow?.findVisible {
                    !it.isEditable && it.text?.toString() == query
                } != null
            })
        }
    }

    private fun AccessibilityNodeInfo.findVisible(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (isVisibleToUser && predicate(this)) return this
        for (index in 0 until childCount) getChild(index)?.findVisible(predicate)?.let { return it }
        return null
    }

    private fun until(condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            if (condition()) return true
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }
}
