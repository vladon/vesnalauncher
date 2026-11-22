package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.settings.ThemeMode
import dev.vladon.vesna.launcher.settings.VesnaTheme
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.FolderPin
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
    private val onAppInfo: (LauncherApp) -> Unit,
    private val onRemoveFromFolder: (Long) -> Unit,
    private val onRename: () -> Unit,
    private val onRemoveFolder: () -> Unit,
    private val onShowPopup: (PopupMenu) -> Unit,
    private val onDismissed: () -> Unit,
) {

    private val host = ComposeView(context)
    private val snackbar = SnackbarHostState()
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
                    text = {
                        Column {
                            AndroidView(
                                factory = { list },
                                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).clipToBounds(),
                            )
                            SnackbarHost(snackbar)
                        }
                    },
                    confirmButton = { FolderActions(onRename, onRemoveFolder) },
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

    suspend fun showSnackbar(message: String) {
        snackbar.showSnackbar(message)
    }

    fun update(state: LauncherUiState) {
        val folder = state.folders[folder.id] ?: return dismiss()
        currentTitle = folder.title
        adapter.submitList(state.folderContents[folder.id].orEmpty())
    }

    private fun showItemMenu(pin: FolderPin, anchor: View) {
        val popup = vesnaPopupMenu(anchor)
        popup.menu.add(0, MENU_REMOVE_FROM_FOLDER, 0, R.string.folder_remove_from_folder)
        popup.menu.add(0, MENU_APP_INFO, 1, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_REMOVE_FROM_FOLDER -> onRemoveFromFolder(pin.id)
                MENU_APP_INFO -> onAppInfo(pin.app)
            }
            true
        }
        onShowPopup(popup)
    }

    private companion object {
        const val MENU_REMOVE_FROM_FOLDER = 1
        const val MENU_APP_INFO = 2
    }
}

@Composable
private fun FolderActions(onRename: () -> Unit, onRemove: () -> Unit) {
    val rename = stringResource(R.string.folder_rename)
    val remove = stringResource(R.string.folder_remove)
    val label = MaterialTheme.typography.labelLarge
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val padding = ButtonDefaults.TextButtonContentPadding
    val horizontalPadding = padding.calculateLeftPadding(direction) + padding.calculateRightPadding(direction)
    val gap = 8.dp

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val labelsFit = with(density) {
            val renameWidth = measurer.measure(rename, label, maxLines = 1).size.width.toDp() + horizontalPadding
            val removeWidth = measurer.measure(remove, label, maxLines = 1).size.width.toDp() + horizontalPadding
            maxOf(renameWidth, ButtonDefaults.MinWidth) + maxOf(removeWidth, ButtonDefaults.MinWidth) + gap <= maxWidth
        }
        // Keep the footer on one line so it cannot consume the member viewport.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.End)) {
            if (labelsFit) {
                TextButton(onClick = onRemove) { Text(remove, maxLines = 1) }
                TextButton(onClick = onRename) { Text(rename, maxLines = 1) }
            } else {
                CompactFolderAction(R.drawable.ic_folder_delete, remove, onRemove)
                CompactFolderAction(R.drawable.ic_folder_edit, rename, onRename)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactFolderAction(icon: Int, label: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp).semantics { contentDescription = label },
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null)
        }
    }
}
