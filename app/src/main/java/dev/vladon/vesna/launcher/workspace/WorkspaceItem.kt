package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey

/** Where a persisted launcher item lives. */
enum class Container { WORKSPACE, DOCK }

/**
 * One persisted entry of the launcher layout: an app placed either in a workspace
 * grid cell or at a dock position (docks reuse [cellIndex] as the sort order).
 */
data class WorkspaceItem(
    val id: Long,
    val key: AppKey,
    val container: Container,
    val cellIndex: Int,
)
