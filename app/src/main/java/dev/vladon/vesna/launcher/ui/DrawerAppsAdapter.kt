package dev.vladon.vesna.launcher.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.apps.LauncherApp

/** Grid adapter of the app drawer. */
class DrawerAppsAdapter(
    private val onItemClick: (LauncherApp) -> Unit,
    private val onItemOptions: (LauncherApp, View) -> Unit,
    private val onItemTouchHold: (LauncherApp, View) -> Unit,
) : ListAdapter<LauncherApp, DrawerAppsAdapter.Holder>(DIFF) {

    class Holder(val view: AppIconView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(AppIconView(parent.context).apply { useSurfaceLabel() })

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = getItem(position)
        holder.view.bind(app)
        holder.view.setOnClickListener { onItemClick(app) }
        holder.view.bindOptions(
            onOptions = { anchor -> onItemOptions(app, anchor) },
            onTouchHold = { anchor -> onItemTouchHold(app, anchor) },
        )
    }

    private companion object {
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
