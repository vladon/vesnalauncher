package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import kotlin.math.ceil

/** Each RecyclerView item is a viewport-sized page; the app catalogue stays flat. */
internal class DrawerPager(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    private val pages = LinearLayoutManager(context, HORIZONTAL, false)
    private val snap = PagerSnapHelper()
    private val pageAdapter = PageAdapter()
    private var apps = emptyList<LauncherApp>()
    private var requestedColumns = MAX_APP_GRID_COLUMNS
    private var columns = requestedColumns
    private var rows = 1
    private var viewportAnchor: Int? = null
    private val capacity get() = columns * rows

    var bindTile: ((AppIconView, LauncherApp) -> Unit)? = null
    var showAppNames = true
        set(value) {
            if (field == value) return
            field = value
            for (index in 0 until childCount) (getChildAt(index) as Page).showAppNames(value)
        }

    init {
        layoutManager = pages
        adapter = pageAdapter
        itemAnimator = null
        snap.attachToRecyclerView(this)
    }

    fun setColumns(columns: Int) {
        if (requestedColumns == columns) return
        requestedColumns = columns
        requestLayout()
    }

    fun submitApps(apps: List<LauncherApp>) {
        if (this.apps === apps) return
        val oldPosition = viewportAppPosition()
        val oldKey = appKeyAt(oldPosition)
        this.apps = apps
        pageAdapter.notifyDataSetChanged()
        val anchor = oldKey?.let(::positionOf) ?: NO_POSITION
        scrollToApp(if (anchor >= 0) anchor else oldPosition)
    }

    fun viewportAppPosition(): Int {
        viewportAnchor?.let { return it }
        val page = snap.findSnapView(pages)?.let(pages::getPosition) ?: pages.findFirstVisibleItemPosition()
        return (page.coerceAtLeast(0) * capacity).coerceAtMost(apps.lastIndex.coerceAtLeast(0))
    }

    fun appKeyAt(position: Int): AppKey? = apps.getOrNull(position)?.key

    fun positionOf(key: AppKey): Int = apps.indexOfFirst { it.key == key }

    fun scrollToApp(position: Int) {
        val target = position.coerceIn(0, apps.lastIndex.coerceAtLeast(0))
        viewportAnchor = target
        pages.scrollToPositionWithOffset(target / capacity, 0)
    }

    fun focusedAppKey(): AppKey? {
        val page = focusedChild as? Page ?: return null
        val pagePosition = getChildAdapterPosition(page)
        val tile = page.focusedChild ?: return null
        return if (pagePosition == NO_POSITION) null else appKeyAt(pagePosition * capacity + page.indexOfChild(tile))
    }

    fun restoreAppFocus(key: AppKey): Boolean {
        val position = positionOf(key)
        if (position == NO_POSITION) return false
        if (requestAppFocus(position)) return true
        scrollToApp(position)
        doOnPreDraw {
            if (isShown && !isInTouchMode) requestAppFocus(positionOf(key))
        }
        return true
    }

    private fun requestAppFocus(position: Int): Boolean {
        if (position < 0) return false
        val page = pages.findViewByPosition(position / capacity) as? Page ?: return false
        return page.getChildAt(position % capacity)?.requestFocus() == true
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val height = MeasureSpec.getSize(heightSpec) - paddingTop - paddingBottom
        if (height > 0) {
            val metrics = resources.displayMetrics
            // 48 dp artwork, 12 dp tile padding, 4 dp gap, and a scaled label line.
            val rowHeight = ceil(64f * metrics.density +
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, metrics)).toInt()
            val rowCount = (height / rowHeight).coerceAtLeast(1)
            if (rows != rowCount || columns != requestedColumns) {
                val anchor = viewportAppPosition()
                rows = rowCount
                columns = requestedColumns
                pageAdapter.notifyDataSetChanged()
                scrollToApp(anchor)
            }
        }
        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        // A reflow may temporarily place the same app on a different page
        // (for example, clearing search with the keyboard still visible).
        // Use the original anchor until the user actually scrolls again.
        if (dx != 0) viewportAnchor = null
    }

    private inner class PageAdapter : Adapter<PageHolder>() {
        override fun getItemCount(): Int = (apps.size + capacity - 1) / capacity

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder =
            PageHolder(Page(parent.context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            })

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val start = position * capacity
            val count = minOf(capacity, apps.size - start)
            val page = holder.page
            page.setGrid(columns, rows)
            while (page.childCount < count) {
                page.addView(AppIconView(page.context).apply { useSurfaceLabel() })
            }
            for (index in 0 until page.childCount) {
                val tile = page.getChildAt(index) as AppIconView
                tile.visibility = if (index < count) VISIBLE else GONE
                if (index < count) bindTile?.invoke(tile, apps[start + index])
            }
            page.showAppNames(showAppNames)
        }
    }

    private class PageHolder(val page: Page) : ViewHolder(page)

    private class Page(context: Context) : ViewGroup(context) {
        private var columns = 1
        private var rows = 1

        init {
            descendantFocusability = FOCUS_AFTER_DESCENDANTS
        }

        fun setGrid(columns: Int, rows: Int) {
            if (this.columns == columns && this.rows == rows) return
            this.columns = columns
            this.rows = rows
            requestLayout()
        }

        fun showAppNames(show: Boolean) {
            for (index in 0 until childCount) (getChildAt(index) as AppIconView).setAppNameVisible(show)
        }

        override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        override fun onMeasure(widthSpec: Int, heightSpec: Int) {
            val width = MeasureSpec.getSize(widthSpec)
            val height = MeasureSpec.getSize(heightSpec)
            setMeasuredDimension(width, height)
            for (index in 0 until childCount) {
                val tile = getChildAt(index)
                if (tile.visibility == GONE) continue
                val col = index % columns
                val row = index / columns
                val w = (col + 1) * width / columns - col * width / columns
                val h = (row + 1) * height / rows - row * height / rows
                tile.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
            }
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
            for (index in 0 until childCount) {
                val tile = getChildAt(index)
                if (tile.visibility == GONE) continue
                val col = index % columns
                val y = (index / columns) * height / rows
                val x = if (rtl) width - (col + 1) * width / columns else col * width / columns
                tile.layout(x, y, x + tile.measuredWidth, y + tile.measuredHeight)
            }
        }
    }
}
