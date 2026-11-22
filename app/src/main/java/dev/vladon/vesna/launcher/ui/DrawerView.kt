package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
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
    private val header: LinearLayout
    private val closeButton: View
    private val searchBar: View
    private val searchField: EditText
    private val searchIcon: Drawable?
    private val searchVerticalPadding: Int
    private val headerGap: Int
    private var showsSearchIcon = true
    private val clearSearchButton: View
    private val noResultsView: TextView
    private val adapter: DrawerAppsAdapter
    private val density = resources.displayMetrics.density

    /** A decisive downward drag travels at least this far (3x touch slop). */
    private val minFlingDistancePx =
        3 * ViewConfiguration.get(context).scaledTouchSlop

    private var appSearch: AppSearch? = null
    private var pendingAppFocus: AppKey? = null
    private var focusSubmission = 0
    private var quickLaunchResult: LauncherApp? = null
    private var enterTarget: LauncherApp? = null
    private var searchEnterKey = KeyEvent.KEYCODE_UNKNOWN
    private var resetScrollOnCommit = false
    private val commitScrollReset = Runnable(::resetScrollAfterFilter)
    private var refreshAnchor: AppKey? = null

    private data class Viewport(val position: Int, val offset: Int)

    private var pendingViewport: Viewport? = null

    internal var isOpen = false
        private set

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
            onAppsCommitted = ::keepVisibleRowAfterRefresh,
        )
        list = findViewById(R.id.drawer_list)
        list.layoutManager = grid
        appNames = GridAppNames(list)
        list.adapter = adapter
        installDrawerFlingToClose()

        noResultsView = findViewById(R.id.drawer_no_results)
        searchField = findViewById(R.id.drawer_search)
        searchBar = findViewById(R.id.drawer_search_bar)
        header = findViewById(R.id.drawer_header)
        headerGap = (header.layoutParams as MarginLayoutParams).bottomMargin
        searchIcon = searchField.compoundDrawablesRelative[0]
        searchVerticalPadding = searchField.paddingTop
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
        if (resetScroll) {
            pendingViewport = null
            resetScrollOnCommit = true
        }
        val query = searchField.text?.toString().orEmpty()
        val search = appSearch
        val filtered = search?.filter(query).orEmpty()
        // ListAdapter may still be applying its diff when Enter is pressed.
        quickLaunchResult = if (query.isNotBlank()) filtered.singleOrNull() else null
        if (search != null && (pendingAppFocus != null || (pendingViewport != null && filtered.isNotEmpty()))) {
            val submission = ++focusSubmission
            adapter.submitList(filtered) {
                if (resetScrollOnCommit) resetScrollAfterFilter()
                if (adapter.itemCount > 0) {
                    pendingViewport?.let { viewport ->
                        pendingViewport = null
                        grid.scrollToPositionWithOffset(
                            viewport.position.coerceAtMost(adapter.itemCount - 1), viewport.offset,
                        )
                    }
                }
                if (pendingAppFocus != null) {
                    // A newer filter can supersede this commit before its holders are laid out.
                    list.doOnPreDraw {
                        if (focusSubmission == submission) restoreCommittedAppFocus(submission)
                    }
                }
            }
        } else {
            adapter.submitList(filtered, if (resetScrollOnCommit) commitScrollReset else null)
        }
        clearSearchButton.isVisible = query.isNotEmpty()
        noResultsView.isVisible = query.isNotBlank() && filtered.isEmpty()
    }

    internal fun focusedAppKey(): AppKey? {
        if (!isOpen) return null
        val tile = list.focusedChild ?: return null
        return adapter.currentList.getOrNull(list.getChildAdapterPosition(tile))?.key
    }

    internal fun saveResultViewport(state: Bundle) {
        if (!isOpen || resetScrollOnCommit) return
        val position = grid.findFirstVisibleItemPosition()
        val tile = grid.findViewByPosition(position)
        val viewport = if (tile != null) {
            Viewport(position, grid.getDecoratedTop(tile) - list.paddingTop)
        } else {
            pendingViewport
        } ?: return
        state.putInt(SCROLL_POSITION, viewport.position)
        state.putInt(SCROLL_OFFSET, viewport.offset)
    }

    internal fun restoreResults(state: Bundle, focusedApp: AppKey?) {
        if (!isOpen) return
        if (state.containsKey(SCROLL_POSITION)) {
            pendingViewport = Viewport(state.getInt(SCROLL_POSITION), state.getInt(SCROLL_OFFSET))
            // The saved query arrives through the text watcher, but it isn't a user edit.
            resetScrollOnCommit = false
        }
        pendingAppFocus = focusedApp
        if (appSearch != null && (pendingViewport != null || focusedApp != null)) applyFilter(resetScroll = false)
    }

    private fun restoreCommittedAppFocus(submission: Int) {
        val key = pendingAppFocus ?: return
        val position = adapter.currentList.indexOfFirst { it.key == key }
        val tile = list.findViewHolderForAdapterPosition(position)?.itemView
        if (position < 0 || tile != null || isInTouchMode) {
            pendingAppFocus = null
            tile?.requestFocus()
            return
        }
        // The previous viewport may no longer contain the result after a reflow.
        list.scrollToPosition(position)
        list.doOnPreDraw {
            if (focusSubmission == submission && pendingAppFocus == key) {
                pendingAppFocus = null
                list.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
            }
        }
    }

    private fun resetScrollAfterFilter() {
        resetScrollOnCommit = false
        refreshAnchor = null
        list.scrollToPosition(0)
    }

    private fun keepVisibleRowAfterRefresh() {
        if (!isOpen || resetScrollOnCommit || !list.canScrollVertically(-1)) {
            refreshAnchor = null
            return
        }
        val top = list.paddingTop
        val bottom = list.height - list.paddingBottom
        // The diff has updated adapter positions, but the attached children still
        // describe the viewport the user is looking at, even if they scrolled
        // while the diff was running. Prefer the previous anchor only if it is
        // still on the first visible row; a refresh can move it sideways.
        val apps = adapter.currentList
        var anchorPosition = RecyclerView.NO_POSITION
        var anchorTop = 0
        for (i in 0 until grid.childCount) {
            val tile = grid.getChildAt(i) ?: continue
            val tileTop = grid.getDecoratedTop(tile)
            if (grid.getDecoratedBottom(tile) <= top || tileTop >= bottom) continue
            if (anchorPosition != RecyclerView.NO_POSITION && tileTop != anchorTop) break
            val position = list.getChildAdapterPosition(tile)
            if (position == RecyclerView.NO_POSITION) continue
            if (anchorPosition == RecyclerView.NO_POSITION) {
                anchorPosition = position
                anchorTop = tileTop
            }
            if (apps[position].key == refreshAnchor) {
                anchorPosition = position
                break
            }
        }
        refreshAnchor = apps.getOrNull(anchorPosition)?.key
        if (anchorPosition != RecyclerView.NO_POSITION) {
            grid.scrollToPositionWithOffset(anchorPosition, anchorTop - top)
        }
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
        pendingAppFocus = null
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
        list.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var startedAtTop = false
            private var downY = 0f
            private var downElapsedMs = 0L

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startedAtTop = !rv.canScrollVertically(-1)
                        downY = e.y
                        downElapsedMs = SystemClock.uptimeMillis()
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> startedAtTop = false
                    MotionEvent.ACTION_UP -> {
                        // A swipe that scrolls back to the top is still a scroll.
                        val dismiss = startedAtTop &&
                            SystemClock.uptimeMillis() - downElapsedMs in 1..MAX_FLING_DURATION_MS &&
                            e.y - downY > minFlingDistancePx &&
                            !rv.canScrollVertically(-1)
                        startedAtTop = false
                        if (dismiss) close()
                    }
                }
                // Observe without taking the scroll away from RecyclerView.
                return false
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
        updateHeader(height - content.paddingTop - content.paddingBottom, compact)
        content.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
    }

    private fun updateHeader(availableHeight: Int, narrow: Boolean) {
        val searchHeight = maxOf(searchBar.minimumHeight, searchField.lineHeight + 2 * searchVerticalPadding)
        val minResultHeight = (48 * density + 0.5f).toInt()
        val oneRow = availableHeight < closeButton.layoutParams.height + searchHeight + headerGap + minResultHeight
        val orientation = if (oneRow) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        if (header.orientation != orientation) {
            header.orientation = orientation
            closeButton.updateLayoutParams<LinearLayout.LayoutParams> {
                gravity = if (oneRow) Gravity.CENTER_VERTICAL else Gravity.CENTER_HORIZONTAL
            }
            searchBar.updateLayoutParams<LinearLayout.LayoutParams> {
                width = if (oneRow) 0 else LayoutParams.MATCH_PARENT
                weight = if (oneRow) 1f else 0f
            }
        }
        val gap = if (oneRow) (availableHeight - searchHeight).coerceIn(0, headerGap) else headerGap
        if ((header.layoutParams as MarginLayoutParams).bottomMargin != gap) {
            header.updateLayoutParams<MarginLayoutParams> { bottomMargin = gap }
        }
        // On the shortest IME viewport, cut padding before clipping the query.
        val padding = ((availableHeight - searchField.lineHeight) / 2).coerceIn(0, searchVerticalPadding)
        if (searchField.paddingTop != padding) {
            searchField.setPaddingRelative(searchField.paddingStart, padding, searchField.paddingEnd, padding)
        }
        val showIcon = !(oneRow && narrow)
        if (showIcon != showsSearchIcon) {
            showsSearchIcon = showIcon
            searchField.setCompoundDrawablesRelative(if (showIcon) searchIcon else null, null, null, null)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val content = getChildAt(0) ?: return
        content.layout(0, 0, measuredWidth, measuredHeight)
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    private companion object {
        const val SCROLL_POSITION = "drawer_scroll_position"
        const val SCROLL_OFFSET = "drawer_scroll_offset"
        const val ANIMATION_MS = 250L
        const val MAX_FLING_DURATION_MS = 400L
    }
}
