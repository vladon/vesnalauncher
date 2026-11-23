package dev.vladon.vesna.launcher.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.apps.LauncherApp

/** Adapter and shared tile binding for drawer surfaces. */
class DrawerAppsAdapter(
    private val onItemClick: (LauncherApp) -> Unit,
    private val onItemOptions: (LauncherApp, View) -> Unit,
    private val onItemTouchHold: (LauncherApp, View) -> Unit,
    private val onAppsCommitted: () -> Unit,
) : ListAdapter<LauncherApp, DrawerAppsAdapter.Holder>(DIFF) {

    class Holder(val view: AppIconView) : RecyclerView.ViewHolder(view)

    var singleColumn = false
        set(value) {
            if (value == field) return
            field = value
            notifyDataSetChanged()
        }

    /** Scales only the artwork of every tile; cells and labels keep their size. */
    var iconScale = 1f
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int = if (singleColumn) ROW else TILE

    override fun onCurrentListChanged(previousList: List<LauncherApp>, currentList: List<LauncherApp>) {
        onAppsCommitted()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(AppIconView(parent.context).apply {
            useSurfaceLabel()
            if (viewType == ROW) useDrawerListRow() else keepDefaultTileHeight()
            artworkScale = iconScale
        })

    override fun onBindViewHolder(holder: Holder, position: Int) = bindTile(holder.view, getItem(position))

    fun bindTile(tile: AppIconView, app: LauncherApp) {
        // A recycled tile may keep the scale from an earlier bind.
        tile.artworkScale = iconScale
        tile.bind(app)
        tile.setOnClickListener { onItemClick(app) }
        tile.bindOptions(
            onOptions = { anchor -> onItemOptions(app, anchor) },
            onTouchHold = { anchor -> onItemTouchHold(app, anchor) },
        )
    }

    private companion object {
        const val TILE = 0
        const val ROW = 1

        val DIFF = object : DiffUtil.ItemCallback<LauncherApp>() {
            override fun areItemsTheSame(oldItem: LauncherApp, newItem: LauncherApp): Boolean =
                oldItem.key == newItem.key

            // Comparing ConstantState by identity is intentional: drawables loaded
            // from the same unchanged app resource share one ConstantState instance,
            // while an app update produces a new one. Lint cannot see that intent.
            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: LauncherApp, newItem: LauncherApp): Boolean =
                oldItem.label == newItem.label &&
                    oldItem.accessibilityLabel == newItem.accessibilityLabel &&
                    oldItem.icon?.constantState == newItem.icon?.constantState
        }
    }
}
