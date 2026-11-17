package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.LauncherApp

/** What a workspace grid cell renders: a single app or a folder. */
sealed interface CellContent {
    data class App(val app: LauncherApp) : CellContent

    data class FolderCell(val folder: Folder, val memberCount: Int) : CellContent
}
