package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.WidgetEntity

/** What a workspace grid cell renders: a single app, a folder, or a widget. */
sealed interface CellContent {
    data class App(val app: LauncherApp) : CellContent

    data class FolderCell(val folder: Folder, val memberCount: Int) : CellContent

    /** A committed widget placement; [WidgetEntity.cellIndex] is the anchor cell. */
    data class WidgetCell(val widget: WidgetEntity) : CellContent
}
