package dev.vladon.vesna.launcher.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.apps.LauncherApp

/** Grid adapter for the contents of an open folder. */
class FolderContentsAdapter(
    private val onItemClick: (LauncherApp) -> Unit,
    private val onItemLongClick: (LauncherApp) -> Boolean,
) : ListAdapter<LauncherApp, FolderContentsAdapter.Holder>(DIFF) {

    class Holder(val view: AppIconView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(AppIconView(parent.context))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = getItem(position)
        holder.view.bind(app)
        holder.view.setOnClickListener { onItemClick(app) }
        holder.view.setOnLongClickListener { onItemLongClick(app) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LauncherApp>() {
            override fun areItemsTheSame(oldItem: LauncherApp, newItem: LauncherApp): Boolean =
                oldItem.key == newItem.key

            // Comparing ConstantState by identity is intentional: drawables loaded
            // from the same unchanged resource share one ConstantState instance.
            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: LauncherApp, newItem: LauncherApp): Boolean =
                oldItem.label == newItem.label &&
                    oldItem.icon?.constantState == newItem.icon?.constantState
        }
    }
}
