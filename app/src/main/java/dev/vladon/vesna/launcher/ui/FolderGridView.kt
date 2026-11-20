package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

internal class FolderGridView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    private val grid = GridLayoutManager(context, MAX_APP_GRID_COLUMNS)

    init {
        layoutManager = grid
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            grid.spanCount = appGridColumnCount(availableWidth, resources.displayMetrics.density)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
