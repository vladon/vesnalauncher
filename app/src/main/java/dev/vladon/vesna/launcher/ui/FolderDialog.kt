package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
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
 *
 * A drag stays in the folder while pointing at the panel. Leaving the panel
 * reveals Home for the rest of that gesture, using the same attached window.
 */
internal class FolderDialog(
    context: Context,
    private val folder: Folder,
    private var pendingMemberFocus: Long?,
    private val onLaunch: (LauncherApp) -> Unit,
    private val onAppInfo: (LauncherApp) -> Unit,
    private val onRemoveFromFolder: (Long) -> Unit,
    private val onRename: () -> Unit,
    private val onRemoveFolder: () -> Unit,
    private val onShowPopup: (VesnaPopupMenu) -> Unit,
    private val onDismissed: () -> Unit,
    private val onMemberDragStarted: (FolderPin) -> Boolean,
    private val onMemberDragMoved: (Float, Float) -> Unit,
    private val onMemberDragFinished: (Float, Float, Boolean) -> Boolean,
    private val onMemberReordered: (Long) -> Boolean,
) {

    private val host = ComposeView(context)
    private val snackbar = SnackbarHostState()
    private val list = LayoutInflater.from(context).inflate(R.layout.dialog_folder, null) as FolderGridView
    private val appNames = GridAppNames(list)
    private val adapter = FolderContentsAdapter(
        onItemClick = onLaunch,
        onItemOptions = ::showItemMenu,
        onItemHold = { pin, anchor -> list.onMemberHold(pin, anchor) },
    )
    private var currentTitle by mutableStateOf(folder.title)
    private val layoutLockedState = mutableStateOf(false)

    /** The Compose dialog's own window, found lazily from the grid's parents. */
    private var memberWindow: Window? = null
    private val panelLocation = IntArray(2)
    private var memberSurfaceDim = 0f
    private var memberSurfaceHidden = false
    private var awaitingMemberDrop = false

    var layoutLocked: Boolean
        get() = layoutLockedState.value
        set(value) {
            if (layoutLockedState.value == value) return
            layoutLockedState.value = value
            if (value) list.cancelMemberDrag()
        }

    internal var showAppNames: Boolean
        get() = appNames.visible
        set(value) { appNames.visible = value }

    init {
        list.adapter = adapter
        list.isMemberDragAllowed = { !layoutLocked }
        list.isMemberInputLocked = { awaitingMemberDrop }
        list.onMemberHoldOptions = ::showItemMenu
        list.onMemberDragStarted = onMemberDragStarted
        list.onMemberDragMoved = { x, y ->
            if (!memberSurfaceHidden && isOverFolder(x, y)) {
                list.previewReorder(x, y)
            } else {
                list.clearReorderPreview()
                hideMemberSurface()
                onMemberDragMoved(x, y)
            }
        }
        list.onMemberDragFinished = { x, y, cancelled ->
            list.clearReorderPreview()
            if (!cancelled && !memberSurfaceHidden && isOverFolder(x, y)) {
                val targetId = list.reorderTargetAt(x, y)
                awaitingMemberDrop = if (targetId != null) onMemberReordered(targetId)
                    else onMemberDragFinished(x, y, true)
            } else {
                awaitingMemberDrop = onMemberDragFinished(x, y, cancelled)
                if (awaitingMemberDrop) {
                    hideMemberSurface()
                    memberWindow?.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                } else {
                    restoreMemberSurface()
                }
            }
        }
    }

    fun show(parent: ViewGroup, themeMode: ThemeMode) {
        host.setContent {
            VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme())) {
                AlertDialog(
                    onDismissRequest = { if (!list.cancelMemberDrag()) dismiss() },
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
                    confirmButton = { if (!layoutLocked) FolderActions(onRename, onRemoveFolder) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
        }
        parent.addView(host, 0, 0)
    }

    /** Closes the dialog (for example when the folder dissolved). */
    fun dismiss() {
        // End member motion silently: callbacks must not outlive the window,
        // and the owner tears its own armed drag down through [onDismissed].
        awaitingMemberDrop = false
        list.clearMemberDrag()
        val parent = host.parent as? ViewGroup ?: return
        parent.removeView(host)
        onDismissed()
    }

    /**
     * Resolves a queued member drop: [placed] closes the dialog on the accepted
     * placement; false restores the hidden surface, the same window and focus.
     * No-op once the dialog is gone.
     */
    fun finishMemberDrop(placed: Boolean) {
        if (!awaitingMemberDrop) return
        if (placed) {
            dismiss()
            return
        }
        if (host.parent == null) {
            // Dismissed while the drop resolved; the window no longer exists.
            awaitingMemberDrop = false
            return
        }
        restoreMemberSurface()
    }

    fun finishMemberReorder() {
        awaitingMemberDrop = false
    }

    /** Cancels an armed or active member drag (for example the owner's onStop). */
    fun cancelMemberDrag() {
        list.cancelMemberDrag()
    }

    suspend fun showSnackbar(message: String) {
        snackbar.showSnackbar(message)
    }

    fun update(state: LauncherUiState) {
        val folder = state.folders[folder.id] ?: return dismiss()
        currentTitle = folder.title
        val members = state.folderContents[folder.id].orEmpty()
        if (pendingMemberFocus == null) {
            adapter.submitList(members)
        } else {
            // A newer submission may skip this callback; keep the target until a list commits.
            adapter.submitList(members) { list.doOnLayout { restoreMemberFocus() } }
        }
    }

    fun focusedMemberId(): Long? {
        val focused = list.focusedChild ?: return null
        return adapter.currentList.getOrNull(list.getChildAdapterPosition(focused))?.id
    }

    private fun restoreMemberFocus() {
        val memberId = pendingMemberFocus ?: return
        pendingMemberFocus = null
        val position = adapter.currentList.indexOfFirst { it.id == memberId }
        if (position < 0) return
        val tile = list.findViewHolderForAdapterPosition(position)?.itemView
        if (tile != null) {
            tile.requestFocus()
        } else {
            list.scrollToPosition(position)
            list.doOnNextLayout {
                val currentPosition = adapter.currentList.indexOfFirst { it.id == memberId }
                if (currentPosition >= 0) {
                    list.findViewHolderForAdapterPosition(currentPosition)?.itemView?.requestFocus()
                }
            }
        }
    }

    private fun showItemMenu(pin: FolderPin, anchor: View) {
        val popup = VesnaPopupMenu(anchor)
        if (!layoutLocked) popup.add(R.string.folder_remove_from_folder) { onRemoveFromFolder(pin.id) }
        popup.add(R.string.menu_app_info) { onAppInfo(pin.app) }
        onShowPopup(popup)
    }

    private fun isOverFolder(rawX: Float, rawY: Float): Boolean {
        val window = memberWindow ?: findMemberWindow(list)?.also { memberWindow = it } ?: return false
        val decor = window.decorView
        decor.getLocationOnScreen(panelLocation)
        val x = rawX - panelLocation[0]
        val y = rawY - panelLocation[1]
        return x >= 0 && x < decor.width && y >= 0 && y < decor.height
    }

    /**
     * Hides the dialog surface for a real drag: the decor content turns fully
     * transparent (alpha, not visibility) and the dim drops to zero, but the
     * same window stays attached and focused so the original pointer stream
     * keeps arriving as the finger travels outside the dialog bounds.
     */
    private fun hideMemberSurface() {
        if (memberSurfaceHidden) return
        val window = memberWindow ?: findMemberWindow(list)?.also { memberWindow = it } ?: return
        memberSurfaceHidden = true
        memberSurfaceDim = window.attributes.dimAmount
        window.decorView.alpha = 0f
        window.attributes = window.attributes.apply { dimAmount = 0f }
    }

    /** Restores the hidden surface: same window, same focus, original dim. */
    private fun restoreMemberSurface() {
        awaitingMemberDrop = false
        if (!memberSurfaceHidden) return
        val window = memberWindow ?: return
        memberSurfaceHidden = false
        window.decorView.alpha = 1f
        window.attributes = window.attributes.apply {
            dimAmount = memberSurfaceDim
            flags = flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }
    }

    private fun findMemberWindow(view: View): Window? {
        var candidate: View? = view
        while (candidate != null) {
            if (candidate is DialogWindowProvider) return candidate.window
            candidate = candidate.parent as? View
        }
        return null
    }
}

@Composable
private fun FolderActions(onRename: () -> Unit, onRemove: () -> Unit) {
    val rename = stringResource(R.string.folder_rename)
    val remove = stringResource(R.string.folder_ungroup)
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
