package dev.vladon.vesna.launcher.ui

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.AppSearch
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.settings.DrawerBackgroundMode
import dev.vladon.vesna.launcher.settings.DrawerLayoutMode
import kotlin.math.abs

/**
 * Full-height app drawer above Home. Opens by button or swipe-up, and closes
 * by button, downward fling, or Android Back.
 */
class DrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    var isLayoutLocked: () -> Boolean = { false }

    var onAppClick: ((LauncherApp) -> Boolean)? = null

    /** Launched when the user taps a search action; the drawer stays out of network decisions. */
    var onSearchAction: ((Intent) -> Unit)? = null

    /** With the opt-in setting, an empty search offers actions that pass the query to other apps. */
    internal var externalSearchActions = false

    /**
     * Applies the drawer-only background choice; [dark] must be the effective night mode.
     * [DrawerBackgroundMode.THEME] keeps the palette-driven surface.
     */
    internal fun applyDrawerBackground(mode: DrawerBackgroundMode, dark: Boolean) {
        val argb = if (dark) mode.darkArgb else mode.lightArgb
        setBackgroundColor(if (argb != 0) argb else context.vesnaPaletteColor(R.attr.vesna_surface))
    }

    internal var closeOnLaunch: () -> Boolean = { false }
    internal var rememberPosition = false
        set(value) {
            field = value
            if (!value) {
                homeReturnViewport = null
                autoCloseViewport = null
            }
        }

    /** With the opt-in setting, opening the drawer focuses search and raises the keyboard. */
    internal var keyboardOnOpen = false
    internal var onItemOptions: ((LauncherApp, View) -> Unit)? = null

    /** Long-press on a drawer item arms a drag from the drawer (drag layer drives). */
    var onItemDragStarted: ((LauncherApp, View) -> Unit)? = null

    /** Released without moving: the app menu should open for this item. */
    var onItemDragReleased: ((LauncherApp, View) -> Unit)? = null
    var onItemDragMoved: ((LauncherApp, View) -> Unit)? = null
    var onOpenedChanged: ((Boolean) -> Unit)? = null

    internal var showAppNames: Boolean
        get() = appNames.visible
        set(value) {
            appNames.visible = value
            pager.showAppNames = value
        }

    internal var layoutMode = DrawerLayoutMode.GRID
        set(value) {
            if (field == value) return
            field = value
            adapter.singleColumn = value == DrawerLayoutMode.LIST
            pendingPageViewport = null
            homeReturnViewport = null
            autoCloseViewport = null
            if (value != DrawerLayoutMode.PAGED) pager.submitApps(emptyList())
            pager.scrollToApp(0)
            applyFilter(resetScroll = true)
            requestLayout()
        }

    /** Scales only the artwork of drawer tiles; cells, rows and labels keep their size. */
    internal var iconScale = 1f
        set(value) {
            if (field == value) return
            field = value
            adapter.iconScale = value
            pager.iconScale = value
        }

    /** Shows the paused-work banner with a resume action (work profile in quiet mode). */
    internal var workAppsPaused: Boolean
        get() = workPausedBanner.isVisible
        set(value) {
            if (workPausedBanner.isVisible == value) return
            workPausedBanner.isVisible = value
            turnOffRow.isVisible = workTabActive && !value && onTurnOffWorkApps != null
        }

    /** Invoked by the banner's resume action; the caller toggles the profile's quiet mode off. */
    internal var onResumeWorkApps: (() -> Unit)? = null

    /** Invoked by the Work tab's Turn off action; the caller pauses the profile. */
    internal var onTurnOffWorkApps: (() -> Unit)? = null
        set(value) {
            field = value
            turnOffRow.isVisible = workTabActive && !workAppsPaused && value != null
        }

    /**
     * The managed-profile serial backing the Work tab (#197); null when the device
     * has no managed profile and the drawer must stay a single untabbed list.
     */
    internal var workProfileUserSerial: Long? = null
        set(value) {
            if (field == value) return
            field = value
            workTabBar.isVisible = value != null && workTabEnabled
            if (workTabActive) {
                workTabActive = false
                applyFilter(resetScroll = true)
            }
        }

    /** Whether the drawer shows the Apps | Work tab bar (the #197 opt-in switch). */
    internal var workTabEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            workTabBar.isVisible = value && workProfileUserSerial != null
            if (workTabActive) {
                workTabActive = false
                applyFilter(resetScroll = true)
            }
        }

    /** True while the Work tab is selected (only managed-profile apps are listed). */
    var workTabActive: Boolean = false
        private set

    /** The apps the drawer currently shows (already tab- and search-filtered). */
    internal val visibleApps: List<LauncherApp>
        get() = adapter.currentList

    /** Rebinds visible tiles so notification dots follow the badge state (#200). */
    internal fun refreshNotificationDots() {
        adapter.notifyDataSetChanged()
    }

    /** Selects the tab the way the click handlers do (tests and focus restoration). */
    internal fun selectWorkTab(active: Boolean) {
        if (workTabActive == active) return
        workTabActive = active
        styleTabs()
        applyFilter(resetScroll = true)
    }

    private val list: RecyclerView
    private val pager: DrawerPager
    private val fastScroller: DrawerFastScroller
    private var hasFastScrollGutter = false
    private var compactHeader = false
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
    private val workPausedBanner: View
    private val workResumeButton: Button
    private val workTabBar: View
    private val tabApps: TextView
    private val tabWork: TextView
    private val turnOffRow: View
    private val searchActionsView: LinearLayout
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

    private data class Viewport(val app: AppKey?, val position: Int, val offset: Int)

    private var pendingViewport: Viewport? = null
    private var pendingPageViewport: Viewport? = null
    private var homeReturnViewport: Viewport? = null
    private var autoCloseViewport: Viewport? = null

    internal var isOpen = false
        private set

    init {
        inflate(context, R.layout.view_drawer, this)
        setBackgroundColor(context.vesnaPaletteColor(R.attr.vesna_surface))
        elevation = 8 * density

        adapter = DrawerAppsAdapter(
            onItemClick = ::launchApp,
            onItemOptions = { app, anchor ->
                hideKeyboard()
                onItemOptions?.invoke(app, anchor)
            },
            onItemTouchHold = hold@{ app, anchor ->
                if (!isOpen) return@hold
                hideKeyboard()
                if (isLayoutLocked()) {
                    onItemOptions?.invoke(app, anchor)
                    return@hold
                }
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
            onAppsCommitted = {
                keepVisibleRowAfterRefresh()
                syncFastScroller()
            },
        )
        list = findViewById(R.id.drawer_list)
        pager = findViewById(R.id.drawer_pager)
        pager.bindTile = adapter::bindTile
        list.layoutManager = grid
        appNames = GridAppNames(list)
        list.adapter = adapter
        fastScroller = findViewById(R.id.drawer_fast_scroller)
        fastScroller.bindIndicator(findViewById(R.id.drawer_section_indicator))
        fastScroller.onSectionSelected = { position ->
            if (list.scrollState != RecyclerView.SCROLL_STATE_IDLE) list.stopScroll()
            grid.scrollToPositionWithOffset(position, 0)
        }
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = syncFastScrollViewport(dy != 0)
        })
        list.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> syncFastScrollViewport() }
        installDrawerFlingToClose(list)
        installDrawerFlingToClose(pager)

        noResultsView = findViewById(R.id.drawer_no_results)
        workPausedBanner = findViewById(R.id.drawer_work_paused)
        workResumeButton = findViewById(R.id.drawer_work_resume)
        workResumeButton.setOnClickListener { onResumeWorkApps?.invoke() }
        workTabBar = findViewById(R.id.drawer_work_tab_bar)
        tabApps = findViewById(R.id.drawer_tab_apps)
        tabWork = findViewById(R.id.drawer_tab_work)
        turnOffRow = findViewById(R.id.drawer_turn_off_row)
        tabApps.setOnClickListener {
            if (workTabActive) {
                workTabActive = false
                styleTabs()
                applyFilter(resetScroll = true)
            }
        }
        tabWork.setOnClickListener {
            if (!workTabActive) {
                workTabActive = true
                styleTabs()
                applyFilter(resetScroll = true)
            }
        }
        findViewById<Button>(R.id.drawer_turn_off_work).setOnClickListener { onTurnOffWorkApps?.invoke() }
        searchActionsView = findViewById(R.id.drawer_search_actions)
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

    /** Selected tab carries the surface color; the other stays plain. */
    private fun styleTabs() {
        tabApps.setTextColor(
            context.getColor(
                if (!workTabActive) R.color.home_control_on_surface else R.color.vesna_on_surface_variant,
            ),
        )
        tabWork.setTextColor(
            context.getColor(
                if (workTabActive) R.color.home_control_on_surface else R.color.vesna_on_surface_variant,
            ),
        )
        tabApps.isSelected = !workTabActive
        tabWork.isSelected = workTabActive
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
        var filtered = search?.filter(query).orEmpty()
        // The Work tab lists only managed-profile apps; Apps shows everything else.
        filtered = WorkTabSlice.apply(filtered, workTabEnabled, workProfileUserSerial, workTabActive)
        val showPages = layoutMode == DrawerLayoutMode.PAGED && query.isEmpty()
        pager.isVisible = showPages
        list.isVisible = !showPages
        // ListAdapter may still be applying its diff when Enter is pressed.
        quickLaunchResult = if (query.isNotBlank()) filtered.singleOrNull() else null
        if (showPages) {
            // Preserve the browsing page across temporary search results.
            ++focusSubmission
            resetScrollOnCommit = false
            refreshAnchor = null
            syncPagedApps(filtered)
        } else if (search != null && (pendingAppFocus != null || (pendingViewport != null && filtered.isNotEmpty()))) {
            val submission = ++focusSubmission
            adapter.submitList(filtered) {
                resetScrollAfterFilter()
                restoreCommittedViewport()
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
        turnOffRow.isVisible = workTabActive && !workAppsPaused && onTurnOffWorkApps != null
        clearSearchButton.isVisible = query.isNotEmpty()
        noResultsView.isVisible = query.isNotBlank() && filtered.isEmpty()
        updateSearchActions(query, show = externalSearchActions && query.isNotBlank() && filtered.isEmpty())
        syncFastScrollViewport()
        if (hasFastScrollGutter != query.isEmpty()) requestLayout()
    }

    /** Providers shown on an empty search; each hands the query to another app on an explicit tap. */
    private fun searchActionIntents(query: String): List<Pair<Int, Intent>> {
        val encoded = Uri.encode(query)
        return listOf(
            R.string.drawer_search_action_web to
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query),
            R.string.drawer_search_action_maps to
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")),
            R.string.drawer_search_action_store to
                Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded&c=apps")),
            R.string.drawer_search_action_videos to
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")),
        )
    }

    private fun updateSearchActions(query: String, show: Boolean) {
        searchActionsView.removeAllViews()
        if (show) {
            val packageManager = context.packageManager
            for ((label, intent) in searchActionIntents(query)) {
                if (packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) continue
                searchActionsView.addView(actionRow(label, intent))
            }
        }
        searchActionsView.isVisible = searchActionsView.childCount > 0
    }

    private fun actionRow(@StringRes label: Int, intent: Intent): TextView {
        val row = TextView(context)
        row.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (56 * density).toInt())
        row.gravity = Gravity.CENTER_VERTICAL
        // The themed borderless ripple gives rows the press, focus and hover state layers.
        context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).run {
            row.background = getDrawable(0)
            recycle()
        }
        row.setPadding((24 * density).toInt(), 0, (24 * density).toInt(), 0)
        row.setText(label)
        row.textSize = 16f
        row.typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
        row.setTextColor(context.vesnaPaletteColor(R.attr.vesna_on_surface_variant))
        row.setOnClickListener {
            onSearchAction?.invoke(intent)
            if (closeOnLaunch()) close()
        }
        return row
    }

    internal fun focusedAppKey(): AppKey? {
        if (!isOpen) return null
        if (pager.isVisible) return pager.focusedAppKey()
        val tile = list.focusedChild ?: return null
        return adapter.currentList.getOrNull(list.getChildAdapterPosition(tile))?.key
    }

    internal fun saveResultViewport(state: Bundle) {
        if (!isOpen) return
        if (layoutMode == DrawerLayoutMode.PAGED) {
            val position = pager.viewportAppPosition()
            val viewport = pendingPageViewport ?: Viewport(pager.appKeyAt(position), position, 0)
            state.putBundle(PAGE_VIEWPORT, Bundle().apply { saveViewport(viewport) })
        }
        if (pager.isVisible || resetScrollOnCommit) return
        val viewport = currentListViewport() ?: return
        state.saveViewport(viewport)
    }

    private fun currentListViewport(): Viewport? {
        val position = grid.findFirstVisibleItemPosition()
        val tile = grid.findViewByPosition(position)
        return if (tile != null) {
            val app = adapter.currentList.getOrNull(list.getChildAdapterPosition(tile))?.key
            Viewport(app, position, grid.getDecoratedTop(tile) - list.paddingTop)
        } else {
            pendingViewport
        }
    }

    private fun Bundle.saveViewport(viewport: Viewport) {
        putInt(SCROLL_POSITION, viewport.position)
        putInt(SCROLL_OFFSET, viewport.offset)
        viewport.app?.let { app ->
            putString(SCROLL_PACKAGE, app.packageName)
            putString(SCROLL_CLASS, app.className)
            putLong(SCROLL_USER, app.userSerial)
        }
    }

    internal fun restoreResults(state: Bundle, focusedApp: AppKey?) {
        if (!isOpen) return
        pendingPageViewport = if (layoutMode == DrawerLayoutMode.PAGED) {
            state.getBundle(PAGE_VIEWPORT)?.readViewport()
        } else null
        pendingViewport = state.readViewport()
        // The saved query arrives through the text watcher, but it isn't a user edit.
        if (pendingViewport != null) resetScrollOnCommit = false
        pendingAppFocus = focusedApp
        val search = appSearch ?: return
        if (pendingPageViewport != null && !pager.isVisible) syncPagedApps(search.filter(null))
        if (pendingViewport != null || pendingPageViewport != null || focusedApp != null) applyFilter(resetScroll = false)
    }

    private fun Bundle.readViewport(): Viewport? {
        if (!containsKey(SCROLL_POSITION)) return null
        val pkg = getString(SCROLL_PACKAGE)
        val cls = getString(SCROLL_CLASS)
        val app = if (pkg != null && cls != null) AppKey(pkg, cls, getLong(SCROLL_USER)) else null
        return Viewport(app, getInt(SCROLL_POSITION), getInt(SCROLL_OFFSET))
    }

    private fun syncPagedApps(apps: List<LauncherApp>) {
        pager.submitApps(apps)
        if (apps.isEmpty()) return
        pendingPageViewport?.let { viewport ->
            val anchor = viewport.app?.let(pager::positionOf) ?: RecyclerView.NO_POSITION
            pager.scrollToApp(if (anchor >= 0) anchor else viewport.position.coerceIn(apps.indices))
            pendingPageViewport = null
        }
        if (pager.isVisible) {
            pendingAppFocus?.let { if (!isInTouchMode) pager.restoreAppFocus(it) }
            pendingAppFocus = null
        }
    }

    private fun restoreCommittedViewport() {
        val viewport = pendingViewport ?: return
        val apps = adapter.currentList
        if (apps.isEmpty()) return
        pendingViewport = null
        val anchor = viewport.app?.let { key -> apps.indexOfFirst { it.key == key } } ?: RecyclerView.NO_POSITION
        // An uninstalled or no-longer-matching app leaves the nearest saved position.
        val position = if (anchor >= 0) anchor else viewport.position.coerceAtMost(apps.lastIndex)
        refreshAnchor = apps[position].key
        grid.scrollToPositionWithOffset(position, viewport.offset)
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
        if (!resetScrollOnCommit) return
        resetScrollOnCommit = false
        refreshAnchor = null
        list.scrollToPosition(0)
        syncFastScroller()
    }

    private fun keepVisibleRowAfterRefresh() {
        if (!isOpen || pager.isVisible || resetScrollOnCommit || !list.canScrollVertically(-1)) {
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

    private fun syncFastScroller() {
        if (!pager.isVisible && !resetScrollOnCommit && searchField.text.isNullOrEmpty()) {
            fastScroller.index(adapter.currentList)
        }
        syncFastScrollViewport()
    }

    private fun syncFastScrollViewport(scrolled: Boolean = false) {
        val browsing = isOpen && !pager.isVisible && hasFastScrollGutter && fastScroller.hasSections &&
            !resetScrollOnCommit && searchField.text.isNullOrEmpty()
        val canScrollDown = browsing && list.canScrollVertically(1)
        val canScrollUp = browsing && list.canScrollVertically(-1)
        val available = (canScrollDown || canScrollUp) && list.height >= 2 * fastScroller.width
        if (isOpen && !available && fastScroller.hasFocus()) searchField.requestFocus()
        fastScroller.syncViewport(available, if (available) grid.findFirstVisibleItemPosition() else 0, !canScrollDown, scrolled)
        list.isVerticalScrollBarEnabled = !fastScroller.isVisible
    }

    private fun launchApp(app: LauncherApp) {
        if (onAppClick?.invoke(app) != true) return
        if (closeOnLaunch()) {
            val viewport = rememberableListViewport()
            close()
            autoCloseViewport = viewport
        } else {
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(searchField.windowToken, 0)
    }

    private fun raiseKeyboard() {
        // The drawer is still sliding in; wait for the field to own the window's focus.
        searchField.post {
            // The host activity owns the window; any other host keeps its own keyboard state.
            val window = (context as? Activity)?.window ?: return@post
            // The drawer may have closed, or the field lost focus, while this was queued.
            if (isOpen && searchField.hasFocus()) {
                WindowCompat.getInsetsController(window, searchField).show(WindowInsetsCompat.Type.ime())
            }
        }
    }

    internal fun submitApps(apps: List<LauncherApp>, hiddenApps: Set<AppKey> = emptySet()) {
        val search = AppSearch(apps, hiddenApps)
        appSearch = search
        if (layoutMode == DrawerLayoutMode.PAGED && !searchField.text.isNullOrEmpty()) {
            syncPagedApps(search.filter(null))
        }
        applyFilter(resetScroll = false)
    }

    /** Keeps drawer controls and results within the visible area, including above the IME. */
    internal fun applyContentInsets(insets: Insets) {
        getChildAt(0).setPadding(insets.left, insets.top, insets.right, insets.bottom)
    }

    /** A restored drawer is already open, so only a user opening raises the keyboard. */
    fun open(showKeyboard: Boolean = keyboardOnOpen) {
        if (isOpen) return
        discardUnconfirmedAutoCloseViewport()
        animate().cancel()
        isOpen = true
        if (visibility != VISIBLE) translationY = height.toFloat()
        visibility = VISIBLE
        updateDrawerFocusability()
        homeReturnViewport?.let { viewport ->
            homeReturnViewport = null
            pendingViewport = viewport
            resetScrollOnCommit = false
            applyFilter(resetScroll = false)
        }
        syncFastScrollViewport()
        if (height > 0) animate().translationY(0f).start()
        onOpenedChanged?.invoke(true)
        if (showKeyboard && searchField.requestFocus()) raiseKeyboard() else closeButton.requestFocus()
    }

    internal fun closeForHome() {
        if (!isOpen) {
            autoCloseViewport?.let { homeReturnViewport = it }
            discardUnconfirmedAutoCloseViewport()
            return
        }
        homeReturnViewport = rememberableListViewport()
        close()
    }

    internal fun discardUnconfirmedAutoCloseViewport() {
        autoCloseViewport = null
    }

    private fun rememberableListViewport(): Viewport? = if (
        isOpen && rememberPosition && layoutMode == DrawerLayoutMode.LIST &&
        searchField.text.isNullOrEmpty() && !resetScrollOnCommit
    ) currentListViewport() else null

    fun close() {
        discardUnconfirmedAutoCloseViewport()
        if (!isOpen) return
        animate().cancel()
        isOpen = false
        pendingAppFocus = null
        pendingPageViewport = null
        if (layoutMode == DrawerLayoutMode.PAGED) {
            pager.stopScroll()
            pager.scrollToApp(0)
        }
        updateDrawerFocusability()
        syncFastScrollViewport()
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
    private fun installDrawerFlingToClose(results: RecyclerView) {
        results.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var startedAtTop = false
            private var downX = 0f
            private var downY = 0f
            private var downElapsedMs = 0L

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startedAtTop = !rv.canScrollVertically(-1)
                        downX = e.x
                        downY = e.y
                        downElapsedMs = SystemClock.uptimeMillis()
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> startedAtTop = false
                    MotionEvent.ACTION_UP -> {
                        // A swipe that scrolls back to the top is still a scroll.
                        val dismiss = startedAtTop &&
                            SystemClock.uptimeMillis() - downElapsedMs in 1..MAX_FLING_DURATION_MS &&
                            e.y - downY > minFlingDistancePx &&
                            (rv != pager || e.y - downY > abs(e.x - downX)) &&
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
        val resultsWidth = width - content.paddingLeft - content.paddingRight - list.paddingLeft - list.paddingRight
        val columns = appGridColumnCount(resultsWidth, density)
        val gutter = fastScroller.layoutParams.width
        // Keep the same grid and search header when the rail appears or disappears.
        val gridColumns = if (layoutMode == DrawerLayoutMode.LIST) 1 else columns
        hasFastScrollGutter = layoutMode != DrawerLayoutMode.PAGED && searchField.text.isNullOrEmpty() &&
            resultsWidth - gutter >= (64 * density).toInt() &&
            (gridColumns == 1 || appGridColumnCount(resultsWidth - gutter, density) == gridColumns)
        val endMargin = if (hasFastScrollGutter) gutter else 0
        if ((list.layoutParams as MarginLayoutParams).marginEnd != endMargin) {
            list.updateLayoutParams<MarginLayoutParams> { marginEnd = endMargin }
        }
        val compact = columns < MAX_APP_GRID_COLUMNS
        if (compact != compactHeader) {
            compactHeader = compact
            searchField.setHint(if (compact) R.string.drawer_search_hint_compact else R.string.drawer_search_hint)
            val sideMargin = ((if (compact) 8 else 16) * density + 0.5f).toInt()
            searchBar.updateLayoutParams<MarginLayoutParams> {
                leftMargin = sideMargin
                rightMargin = sideMargin
            }
        }
        grid.spanCount = gridColumns
        pager.setColumns(columns)
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
        const val SCROLL_PACKAGE = "drawer_scroll_package"
        const val SCROLL_CLASS = "drawer_scroll_class"
        const val SCROLL_USER = "drawer_scroll_user"
        const val PAGE_VIEWPORT = "drawer_page_viewport"
        const val ANIMATION_MS = 250L
        const val MAX_FLING_DURATION_MS = 400L
    }
}
