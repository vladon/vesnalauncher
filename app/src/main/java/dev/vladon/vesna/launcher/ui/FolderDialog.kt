package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.settings.ThemeMode
import dev.vladon.vesna.launcher.settings.VesnaTheme
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.LauncherUiState

/**
 * Opens a folder: shows its member apps in a grid. Members launch on tap; a
 * long-press offers removal back to the workspace. The folder can be renamed or
 * removed entirely. [update] is called by the owner whenever launcher state emits.
 */
internal class FolderDialog(
    context: Context,
    private val folder: Folder,
    private val onLaunch: (LauncherApp) -> Unit,
    private val onRemoveFromFolder: (LauncherApp) -> Unit,
    private val onRename: () -> Unit,
    private val onRemoveFolder: () -> Unit,
    private val onDismissed: () -> Unit,
) {

    private val host = ComposeView(context)
    private val list = LayoutInflater.from(context).inflate(R.layout.dialog_folder, null) as FolderGridView
    private val appNames = GridAppNames(list)
    private val adapter = FolderContentsAdapter(
        onItemClick = onLaunch,
        onItemOptions = ::showItemMenu,
    )
    private var currentTitle by mutableStateOf(folder.title)

    internal var showAppNames: Boolean
        get() = appNames.visible
        set(value) { appNames.visible = value }

    init {
        list.adapter = adapter
    }

    fun show(parent: ViewGroup, themeMode: ThemeMode) {
        host.setContent {
            VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme())) {
                AlertDialog(
                    onDismissRequest = ::dismiss,
                    title = { Text(currentTitle, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    text = { AndroidView(factory = { list }, modifier = Modifier.fillMaxWidth()) },
                    confirmButton = {
                        TextButton(onClick = onRename) { Text(stringResource(R.string.folder_rename)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            onRemoveFolder()
                            dismiss()
                        }) { Text(stringResource(R.string.folder_remove)) }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
        }
        parent.addView(host, 0, 0)
    }

    /** Closes the dialog (for example when the folder dissolved). */
    fun dismiss() {
        val parent = host.parent as? ViewGroup ?: return
        parent.removeView(host)
        onDismissed()
    }

    fun update(state: LauncherUiState) {
        val folder = state.folders[folder.id] ?: return dismiss()
        currentTitle = folder.title
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

    private companion object {
        const val MENU_REMOVE_FROM_FOLDER = 1
        const val MENU_APP_INFO = 2
    }
}
