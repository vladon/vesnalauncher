package dev.vladon.vesna.launcher.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.FolderPin

/** Grid adapter for the contents of an open folder. */
internal class FolderContentsAdapter(
    private val onItemClick: (LauncherApp) -> Unit,
    private val onItemOptions: (FolderPin, View) -> Unit,
) : ListAdapter<FolderPin, FolderContentsAdapter.Holder>(DIFF) {

    class Holder(val view: AppIconView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(AppIconView(parent.context).apply { useSurfaceLabel() })

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val pin = getItem(position)
        holder.view.bind(pin.app)
        holder.view.setOnClickListener { onItemClick(pin.app) }
        holder.view.bindOptions(onOptions = { anchor -> onItemOptions(pin, anchor) })
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<FolderPin>() {
            override fun areItemsTheSame(oldItem: FolderPin, newItem: FolderPin): Boolean =
                oldItem.id == newItem.id

            // Comparing ConstantState by identity is intentional: drawables loaded
            // from the same unchanged resource share one ConstantState instance.
            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: FolderPin, newItem: FolderPin): Boolean =
                oldItem.app.label == newItem.app.label &&
                    oldItem.app.accessibilityLabel == newItem.app.accessibilityLabel &&
                    oldItem.app.icon?.constantState == newItem.app.icon?.constantState
        }
    }
}
