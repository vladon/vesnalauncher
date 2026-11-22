package dev.vladon.vesna.launcher.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.settings.SettingsActivity
import dev.vladon.vesna.launcher.settings.VesnaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class FolderRenameValidationTest {
    @Test
    fun rejectedNameRemainsVisibleAndAccessibleWhenEditorIsRecreated() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val automation = instrumentation.uiAutomation
        val error = context.getString(R.string.folder_name_required)
        val save = context.getString(R.string.folder_rename_save)
        val dialogTitle = context.getString(R.string.folder_rename)
        val editor = FolderRenameEditorState("Folder")
        val savedTitle = AtomicReference<String>()

        fun find(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
            val root = automation.rootInActiveWindow ?: return null
            if (root.findVisible { it.text?.toString() == dialogTitle } == null) return null
            return root.findVisible(predicate)
        }

        fun clickSave() {
            val label = await { find { it.text?.toString() == save } }
            val button = generateSequence(label) { it.parent }.first { it.isClickable }
            assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }

        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            fun show(dark: Boolean) = scenario.onActivity { activity ->
                activity.setContent {
                    VesnaTheme(dark = dark) {
                        FolderRenameDialog(editor, onDismiss = activity::finish, onSave = {
                            savedTitle.set(it)
                            activity.finish()
                        })
                    }
                }
            }

            show(dark = false)
            assertTrue(await { find { it.isEditable && it.isFocused } }.replaceText("   "))
            clickSave()
            await { find { it.isEditable && it.error?.toString() == error } }
            await { find { !it.isEditable && it.text?.toString() == error } }
            assertEquals(null, savedTitle.get())

            scenario.recreate()
            show(dark = true)
            val invalid = await { find { it.isEditable && it.error?.toString() == error } }
            await { find { !it.isEditable && it.text?.toString() == error } }
            assertTrue(invalid.replaceText("  Поездки  "))
            await { find { it.isEditable && it.error == null && it.text?.toString()?.contains("Поездки") == true } }
            clickSave()
            assertEquals("Поездки", await { savedTitle.get() })
        }
    }

    private fun AccessibilityNodeInfo.replaceText(text: String) =
        performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        })

    private fun AccessibilityNodeInfo.findVisible(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (isVisibleToUser && predicate(this)) return this
        for (index in 0 until childCount) getChild(index)?.findVisible(predicate)?.let { return it }
        return null
    }

    private fun <T : Any> await(find: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            find()?.let { return it }
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("Expected rename dialog state was not reached")
    }
}
