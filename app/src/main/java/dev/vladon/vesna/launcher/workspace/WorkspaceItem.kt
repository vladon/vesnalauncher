package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey

/** Where a persisted launcher item lives. */
enum class Container { WORKSPACE, DOCK }

internal const val DEFAULT_WORKSPACE_PAGE_ID = 1L

/**
 * One persisted entry of the launcher layout: an app placed either in a workspace
 * grid cell or at a dock position (docks reuse [cellIndex] as the sort order).
 * [userSerial] keeps entries of different Android profiles apart. A non-null
 * [folderId] means the entry lives inside that folder ([cellIndex] is then its
 * position within the folder). [pageId] identifies the page for top-level
 * workspace items only; it is null for dock items and folder members.
 */
data class WorkspaceItem(
    val id: Long,
    val key: AppKey,
    val container: Container,
    val cellIndex: Int,
    val userSerial: Long = 0,
    val folderId: Long? = null,
    val pageId: Long? = if (container == Container.WORKSPACE && folderId == null) DEFAULT_WORKSPACE_PAGE_ID else null,
)
