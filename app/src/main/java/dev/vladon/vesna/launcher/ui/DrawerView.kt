package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppSearch
import dev.vladon.vesna.launcher.apps.LauncherApp

/**
 * Full-height app drawer above Home. Opens by button or swipe-up, and closes
 * by button, downward fling, or Android Back.
 */
class DrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    var onAppClick: ((LauncherApp) -> Boolean)? = null
    internal var onItemOptions: ((LauncherApp, View) -> Unit)? = null

    /** Long-press on a drawer item arms a drag from the drawer (drag layer drives). */
    var onItemDragStarted: ((LauncherApp, View) -> Unit)? = null

    /** Released without moving: the app menu should open for this item. */
    var onItemDragReleased: ((LauncherApp, View) -> Unit)? = null
    var onItemDragMoved: ((LauncherApp, View) -> Unit)? = null
    var onOpenedChanged: ((Boolean) -> Unit)? = null

    internal var showAppNames: Boolean
        get() = appNames.visible
        set(value) { appNames.visible = value }

    private val list: RecyclerView
    private val grid = GridLayoutManager(context, MAX_APP_GRID_COLUMNS)
    private val appNames: GridAppNames
    private val closeButton: View
    private val searchBar: View
    private val searchField: EditText
    private val clearSearchButton: View
    private val noResultsView: TextView
    private val adapter: DrawerAppsAdapter
    private val density = resources.displayMetrics.density

    /** A decisive downward drag travels at least this far (3x touch slop). */
    private val minFlingDistancePx =
        3 * ViewConfiguration.get(context).scaledTouchSlop

    private var appSearch = AppSearch(emptyList())
    private var quickLaunchResult: LauncherApp? = null
    private var enterTarget: LauncherApp? = null
    private var searchEnterKey = KeyEvent.KEYCODE_UNKNOWN
    private var resetScrollOnCommit = false
    private val commitScrollReset = Runnable(::resetScrollAfterFilter)

    private var isOpen = false

    init {
        inflate(context, R.layout.view_drawer, this)
        setBackgroundResource(R.color.vesna_surface)
        elevation = 8 * density

        adapter = DrawerAppsAdapter(
            onItemClick = ::launchApp,
            onItemOptions = { app, anchor ->
                hideKeyboard()
                onItemOptions?.invoke(app, anchor)
            },
            onItemTouchHold = { app, anchor ->
                hideKeyboard()
                onItemDragStarted?.invoke(app, anchor)
                // While armed, the drag layer owns subsequent MOVE events; the UP
                // (no movement) arrives here and opens the app menu. The listener
                // removes itself in both terminal branches, so a recycled holder
                // never fires it with a stale app.
                anchor.setOnTouchListener { v, ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_UP -> {
                            v.setOnTouchListener(null)
                            onItemDragReleased?.invoke(app, v)
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            v.setOnTouchListener(null)
                            true
                        }
                        else -> false
                    }
                }
            },
        )
        list = findViewById(R.id.drawer_list)
        list.layoutManager = grid
        appNames = GridAppNames(list)
        list.adapter = adapter
        installDrawerFlingToClose()

        noResultsView = findViewById(R.id.drawer_no_results)
        searchField = findViewById(R.id.drawer_search)
        searchBar = findViewById(R.id.drawer_search_bar)
        searchField.setOnFocusChangeListener { _, focused ->
            searchBar.isActivated = focused
            if (!focused) {
                searchEnterKey = KeyEvent.KEYCODE_UNKNOWN
                enterTarget = null
            }
        }
        searchField.setOnKeyListener { _, keyCode, event ->
            if ((keyCode != KeyEvent.KEYCODE_ENTER && keyCode != KeyEvent.KEYCODE_NUMPAD_ENTER) ||
                event.flags and (KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_EDITOR_ACTION) != 0
            ) return@setOnKeyListener false

            val canHandle = event.hasNoModifiers() &&
                BaseInputConnection.getComposingSpanStart(searchField.text) < 0
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        // Keep Enter in search even without a unique match: TextView can
                        // otherwise move focus to a result that the adapter is replacing.
                        searchEnterKey = if (canHandle) keyCode else KeyEvent.KEYCODE_UNKNOWN
                        enterTarget = if (canHandle) quickLaunchResult else null
                    }
                    searchEnterKey == keyCode
                }
                KeyEvent.ACTION_UP -> {
                    if (searchEnterKey != keyCode) return@setOnKeyListener false
                    val target = enterTarget
                    val result = quickLaunchResult
                    searchEnterKey = KeyEvent.KEYCODE_UNKNOWN
                    enterTarget = null
                    if (!event.isCanceled && canHandle && result != null && result.key == target?.key) launchApp(result)
                    true
                }
                else -> false
            }
        }
        clearSearchButton = findViewById(R.id.drawer_clear_search)
        clearSearchButton.setOnClickListener {
            // Restore focus before the cleared query hides the button.
            searchField.requestFocus()
            searchField.text.clear()
        }
        searchField.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun afterTextChanged(s: android.text.Editable?) = applyFilter(resetScroll = true)
            },
        )
        searchField.setOnEditorActionListener { field, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                field.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }

        closeButton = findViewById(R.id.drawer_close)
        closeButton.setOnClickListener { close() }
        ViewCompat.setAccessibilityPaneTitle(this, context.getString(R.string.drawer_title))
        updateDrawerFocusability()
        visibility = GONE
        animate().setDuration(ANIMATION_MS).setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isOpen && !searchField.hasFocus() && event.action == KeyEvent.ACTION_DOWN &&
            !event.isCtrlPressed && !event.isMetaPressed && event.isPrintingKey && event.unicodeChar != 0 &&
            searchField.requestFocus()
        ) {
            // Append to the existing query and let EditText handle the original key,
            // including modifiers and composing accents.
            searchField.setSelection(searchField.length())
        }
        return super.dispatchKeyEvent(event)
    }

    private fun applyFilter(resetScroll: Boolean) {
        // A package refresh can supersede a pending search diff. Carry the reset
        // forward to the result list that actually commits.
        if (resetScroll) resetScrollOnCommit = true
        val query = searchField.text?.toString().orEmpty()
        val filtered = appSearch.filter(query)
        // ListAdapter may still be applying its diff when Enter is pressed.
        quickLaunchResult = if (query.isNotBlank()) filtered.singleOrNull() else null
        adapter.submitList(filtered, if (resetScrollOnCommit) commitScrollReset else null)
        clearSearchButton.isVisible = query.isNotEmpty()
        noResultsView.isVisible = query.isNotBlank() && filtered.isEmpty()
    }

    private fun resetScrollAfterFilter() {
        resetScrollOnCommit = false
        list.scrollToPosition(0)
    }

    private fun launchApp(app: LauncherApp) {
        if (onAppClick?.invoke(app) == true) hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(searchField.windowToken, 0)
    }

    fun submitApps(apps: List<LauncherApp>) {
        appSearch = AppSearch(apps)
        applyFilter(resetScroll = false)
    }

    /** Keeps drawer controls and results within the visible area, including above the IME. */
    internal fun applyContentInsets(insets: Insets) {
        getChildAt(0).setPadding(insets.left, insets.top, insets.right, insets.bottom)
    }

    fun open() {
        if (isOpen) return
        animate().cancel()
        isOpen = true
        if (visibility != VISIBLE) translationY = height.toFloat()
        visibility = VISIBLE
        updateDrawerFocusability()
        if (height > 0) animate().translationY(0f).start()
        onOpenedChanged?.invoke(true)
        closeButton.requestFocus()
    }

    fun close() {
        if (!isOpen) return
        animate().cancel()
        isOpen = false
        updateDrawerFocusability()
        searchField.setText("")
        searchField.clearFocus()
        hideKeyboard()
        if (height == 0) {
            visibility = GONE
        } else {
            animate().translationY(height.toFloat())
                .withEndAction { if (!isOpen) visibility = GONE }
                .start()
        }
        onOpenedChanged?.invoke(false)
    }

    private fun updateDrawerFocusability() {
        descendantFocusability = if (isOpen) FOCUS_BEFORE_DESCENDANTS else FOCUS_BLOCK_DESCENDANTS
        importantForAccessibility =
            if (isOpen) IMPORTANT_FOR_ACCESSIBILITY_AUTO else IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (isOpen && oldh == 0) {
            translationY = h.toFloat()
            animate().translationY(0f).start()
        }
    }

    /**
     * Closes the drawer on a fast downward drag from the top of the list. Uses wall
     * time and raw distance instead of VelocityTracker: injected events (and some
     * RV-dispatched streams) carry identical event times, which makes tracker-based
     * velocities collapse to zero.
     */
    private fun installDrawerFlingToClose() {
        var downY = 0f
        var downElapsedMs = 0L

        fun handle(e: MotionEvent) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = e.y
                    downElapsedMs = SystemClock.uptimeMillis()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dy = e.y - downY
                    val elapsedMs = SystemClock.uptimeMillis() - downElapsedMs
                    // Only from the top of the list; otherwise the drag is an
                    // upward scroll gesture and must keep scrolling.
                    if (elapsedMs in 1..MAX_FLING_DURATION_MS &&
                        dy > minFlingDistancePx &&
                        !list.canScrollVertically(-1)
                    ) {
                        close()
                    }
                }
            }
        }

        list.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Returning false keeps RecyclerView scrolling fully intact.
                handle(e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // Events after RecyclerView's own scroll interception arrive here.
                handle(e)
            }
        })
    }

    // The drawer fills its parent; the inflated content uses the same bounds.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
        val content = getChildAt(0)
        val availableWidth = width - content.paddingLeft - content.paddingRight - list.paddingLeft - list.paddingRight
        val density = resources.displayMetrics.density
        val columns = appGridColumnCount(availableWidth, density)
        val compact = columns < MAX_APP_GRID_COLUMNS
        if (compact != (grid.spanCount < MAX_APP_GRID_COLUMNS)) {
            searchField.setHint(if (compact) R.string.drawer_search_hint_compact else R.string.drawer_search_hint)
            val sideMargin = ((if (compact) 8 else 16) * density + 0.5f).toInt()
            searchBar.updateLayoutParams<MarginLayoutParams> {
                leftMargin = sideMargin
                rightMargin = sideMargin
            }
        }
        grid.spanCount = columns
        content.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val content = getChildAt(0) ?: return
        content.layout(0, 0, measuredWidth, measuredHeight)
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    private companion object {
        const val ANIMATION_MS = 250L
        const val MAX_FLING_DURATION_MS = 400L
    }
}
