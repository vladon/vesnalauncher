package dev.vladon.vesna.launcher.workspace

/**
 * A workspace folder: occupies a grid cell and contains workspace items
 * ([WorkspaceItem.folderId]). Folders dissolve automatically when fewer than two
 * members remain (the last member returns to the folder's cell).
 */
data class Folder(
    val id: Long,
    val title: String,
    val cellIndex: Int,
    val pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
)
