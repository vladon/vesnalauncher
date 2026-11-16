package dev.vladon.vesna.launcher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.GridCanvas

/**
 * Full-height app drawer that slides up over the workspace. Opened by a swipe-up
 * gesture on the workspace or by tapping the handle; closed by tapping the handle,
 * a downward fling, or the back button.
 */
class DrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    var onAppClick: ((LauncherApp) -> Unit)? = null
    var onAppLongClick: ((LauncherApp) -> Boolean)? = null
    var onOpenedChanged: ((Boolean) -> Unit)? = null

    private val list: RecyclerView
    private val handle: ViewGroup
    private val adapter: DrawerAppsAdapter
    private val density = resources.displayMetrics.density

    val isOpen: Boolean get() = visibility == VISIBLE

    init {
        inflate(context, R.layout.view_drawer, this)
        background = ContextCompat.getDrawable(context, R.color.drawer_background)
        elevation = 8 * density

        adapter = DrawerAppsAdapter(
            onItemClick = { onAppClick?.invoke(it) },
            onItemLongClick = { onAppLongClick?.invoke(it) ?: true },
        )
        list = findViewById(R.id.drawer_list)
        list.layoutManager = GridLayoutManager(context, GridCanvas.COLUMNS)
        list.adapter = adapter
        installDrawerFlingToClose()

        handle = findViewById(R.id.drawer_handle)
        handle.setOnClickListener { if (isOpen) close() else open() }

        visibility = GONE
        translationY = height.toFloat()
    }

    fun submitApps(apps: List<LauncherApp>) {
        adapter.submitList(apps)
    }

    /** Pads the drawer content so handle and list clear the system bars. */
    fun applySystemBarInsets(top: Int, bottom: Int) {
        findViewById<ViewGroup>(R.id.drawer_content).setPadding(0, top, 0, 0)
        list.setPadding(0, 0, 0, bottom)
    }

    fun open() {
        if (isOpen) return
        visibility = VISIBLE
        animate().translationY(0f).setDuration(ANIMATION_MS).start()
        onOpenedChanged?.invoke(true)
    }

    fun close() {
        if (!isOpen) return
        animate().translationY(height.toFloat())
            .setDuration(ANIMATION_MS)
            .withEndAction { visibility = GONE }
            .start()
        onOpenedChanged?.invoke(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Keep the drawer off-screen below the parent when closed.
        if (!isOpen && animation == null) translationY = h.toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun installDrawerFlingToClose() {
        val detector = android.view.GestureDetector(
            context,
            object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    if (velocityY > FLING_VELOCITY) {
                        close()
                        return true
                    }
                    return false
                }
            },
        )
        list.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean =
                detector.onTouchEvent(e)
        })
    }

    // The drawer fills its parent; the inflated content uses the same bounds.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
        val content = getChildAt(0)
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
        const val FLING_VELOCITY = 1600f
    }
}
