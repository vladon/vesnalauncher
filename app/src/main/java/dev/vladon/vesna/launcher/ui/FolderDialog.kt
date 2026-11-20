package dev.vladon.vesna.launcher.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.LauncherUiState

/**
 * Opens a folder: shows its member apps in a grid. Members launch on tap; a
 * long-press offers removal back to the workspace. The folder can be renamed or
 * removed entirely. [update] is called by the owner whenever launcher state emits.
 */
class FolderDialog(
    context: Context,
    private val folder: Folder,
    private val onLaunch: (LauncherApp) -> Unit,
    private val onRemoveFromFolder: (LauncherApp) -> Unit,
    private val onRename: (Folder, String) -> Unit,
    private val onRemoveFolder: (Folder) -> Unit,
    private val onDismissed: () -> Unit,
) {

    private val dialog: AlertDialog
    private val adapter: FolderContentsAdapter
    private val appNames: GridAppNames
    private var currentTitle: String = folder.title

    internal var showAppNames: Boolean
        get() = appNames.visible
        set(value) { appNames.visible = value }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_folder, null)
        val list = view.findViewById<RecyclerView>(R.id.folder_list)
        appNames = GridAppNames(list)
        list.layoutManager = GridLayoutManager(context, GRID_COLUMNS)
        adapter = FolderContentsAdapter(
            onItemClick = onLaunch,
            onItemOptions = ::showItemMenu,
        )
        list.adapter = adapter

        dialog = AlertDialog.Builder(context)
            .setTitle(folder.title)
            .setView(view)
            .setPositiveButton(R.string.folder_rename) { _, _ -> showRenamePrompt(context) }
            .setNegativeButton(R.string.folder_remove) { _, _ ->
                onRemoveFolder(folder)
                onDismissed()
            }
            .setOnDismissListener { onDismissed() }
            .create()
    }

    fun show() = dialog.show()

    /** Closes the dialog (for example when the folder dissolved). */
    fun dismiss() = dialog.dismiss()

    fun update(state: LauncherUiState) {
        val folder = state.folders[folder.id] ?: return
        if (folder.title != currentTitle) {
            currentTitle = folder.title
            dialog.setTitle(currentTitle)
        }
        adapter.submitList(state.folderContents[folder.id].orEmpty())
    }

    private fun showItemMenu(app: LauncherApp, anchor: View) {
        val popup = vesnaPopupMenu(anchor)
        popup.menu.add(0, MENU_REMOVE_FROM_FOLDER, 0, R.string.folder_remove_from_folder)
        popup.menu.add(0, MENU_APP_INFO, 1, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_REMOVE_FROM_FOLDER -> onRemoveFromFolder(app)
                MENU_APP_INFO -> anchor.context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", app.packageName, null),
                    ),
                )
            }
            true
        }
        popup.show()
    }

    private fun showRenamePrompt(context: Context) {
        val input = EditText(context)
        input.setText(currentTitle)
        input.setSelection(input.text.length)
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.folder_rename))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty() && newTitle != currentTitle) {
                    onRename(folder, newTitle)
                    currentTitle = newTitle
                    dialog.setTitle(newTitle)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private companion object {
        const val GRID_COLUMNS = 4
        const val MENU_REMOVE_FROM_FOLDER = 1
        const val MENU_APP_INFO = 2
    }
}
