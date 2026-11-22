package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import kotlin.math.roundToInt

/** A separate touch target: scrubbing never competes with app tiles or drawer flings. */
internal class DrawerFastScroller(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private data class Section(val letter: String, val position: Int)

    private val density = resources.displayMetrics.density
    private val trackColor = context.getColor(R.color.vesna_outline)
    private val thumbColor = context.getColor(R.color.workspace_page_primary)
    private val focusColor = context.getColor(R.color.home_control_secondary)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var indexedApps: List<LauncherApp>? = null
    private val sections = ArrayList<Section>()
    private var activeSection = 0
    private var jumpedToSection = false
    private var dragging = false
    private var indicator: TextView? = null
    var onSectionSelected: ((Int) -> Unit)? = null

    val hasSections get() = sections.size > 1
    private val trackTop get() = width / 2f
    private val trackBottom get() = height - trackTop
    private val thumbY get() = trackTop + (trackBottom - trackTop) * activeSection / sections.lastIndex.coerceAtLeast(1)

    fun bindIndicator(indicator: TextView) {
        this.indicator = indicator
        indicator.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateIndicator() }
    }

    fun index(apps: List<LauncherApp>) {
        if (apps === indexedApps) return
        endDrag()
        indexedApps = apps
        jumpedToSection = false
        sections.clear()
        // Keep the drawer's existing order, including non-Latin labels and profile twins.
        var previousLetter = -1
        for (i in apps.indices) {
            val label = apps[i].label
            val first = label.indexOfFirst { !it.isWhitespace() }
            val codePoint = if (first < 0) 0 else label.codePointAt(first)
            val letter = if (Character.isLetter(codePoint)) Character.toUpperCase(codePoint) else '#'.code
            if (previousLetter != letter) {
                previousLetter = letter
                sections.add(Section(String(Character.toChars(letter)), i))
            }
        }
        activeSection = activeSection.coerceAtMost(sections.lastIndex.coerceAtLeast(0))
        updateSectionDescription()
        invalidate()
    }

    fun syncViewport(available: Boolean, firstPosition: Int, atBottom: Boolean, scrolled: Boolean) {
        if (!available) {
            jumpedToSection = false
            endDrag()
            visibility = INVISIBLE
            return
        }
        visibility = VISIBLE
        if (scrolled) jumpedToSection = false
        if (dragging || jumpedToSection) return
        val section = if (atBottom) sections.lastIndex else {
            val result = sections.binarySearchBy(firstPosition) { it.position }
            if (result >= 0) result else (-result - 2).coerceAtLeast(0)
        }
        if (activeSection != section) {
            activeSection = section
            updateSectionDescription()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val x = width / 2f
        paint.color = trackColor
        paint.strokeWidth = 2 * density
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x, trackTop, x, trackBottom, paint)
        paint.color = thumbColor
        val halfWidth = (if (dragging) 4 else 3) * density
        val halfHeight = 16 * density
        val y = thumbY
        canvas.drawRoundRect(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight, halfWidth, halfWidth, paint)
        if (isFocused) {
            val inset = 4 * density
            paint.color = focusColor
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(inset, inset, width - inset, height - inset, inset, inset, paint)
            paint.style = Paint.Style.FILL
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            if (!hasSections || height <= width) false else {
                parent.requestDisallowInterceptTouchEvent(true)
                dragging = true
                select(sectionAt(event.y), force = true)
                true
            }
        }
        MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
            if (dragging) select(sectionAt(event.y))
            if (event.actionMasked == MotionEvent.ACTION_UP) endDrag()
            true
        }
        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
            endDrag()
            true
        }
        else -> true
    }

    private fun sectionAt(y: Float): Int {
        val fraction = (y - trackTop) / (trackBottom - trackTop).coerceAtLeast(1f)
        return (fraction.coerceIn(0f, 1f) * sections.lastIndex).roundToInt()
    }

    private fun select(section: Int, force: Boolean = false): Boolean {
        if (section !in sections.indices || (!force && section == activeSection)) return false
        val apps = indexedApps ?: return false
        activeSection = section
        // Retain the chosen letter when the target shares a grid row with an earlier
        // section or layout clamps the final rows to the bottom of the viewport.
        jumpedToSection = true
        val position = if (section == sections.lastIndex) apps.lastIndex else sections[section].position
        onSectionSelected?.invoke(position)
        updateSectionDescription()
        updateIndicator()
        invalidate()
        return true
    }

    private fun updateIndicator() {
        val view = indicator ?: return
        view.visibility = if (dragging) VISIBLE else INVISIBLE
        if (!dragging) return
        view.text = sections[activeSection].letter
        view.translationY = (thumbY - view.height / 2f).coerceIn(0f, (height - view.height).coerceAtLeast(0).toFloat())
    }

    private fun endDrag() {
        if (!dragging) return
        dragging = false
        parent?.requestDisallowInterceptTouchEvent(false)
        updateIndicator()
        invalidate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val step = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> -1
            KeyEvent.KEYCODE_DPAD_DOWN -> 1
            else -> return super.onKeyDown(keyCode, event)
        }
        return select(activeSection + step) || super.onKeyDown(keyCode, event)
    }

    private fun updateSectionDescription() {
        ViewCompat.setStateDescription(this, sections.getOrNull(activeSection)?.letter)
    }

    override fun getAccessibilityClassName(): CharSequence = SeekBar::class.java.name

    // The non-deprecated RangeInfo constructor is unavailable on Android 8–10.
    @Suppress("DEPRECATION")
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (!hasSections) return
        info.rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(
            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0f, sections.lastIndex.toFloat(), activeSection.toFloat(),
        )
        info.isScrollable = true
        if (activeSection > 0) info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        if (activeSection < sections.lastIndex) info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        val section = when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> activeSection - 1
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> activeSection + 1
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id -> {
                val value = arguments?.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, Float.NaN)
                    ?: return false
                if (!value.isFinite()) return false
                value.roundToInt()
            }
            else -> return super.performAccessibilityAction(action, arguments)
        }
        return select(section)
    }
}
