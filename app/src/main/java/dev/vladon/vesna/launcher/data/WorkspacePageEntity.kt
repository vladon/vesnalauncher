package dev.vladon.vesna.launcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A workspace page. Pages are ordered by [rank] (ties broken by [id]). The
 * default page ([dev.vladon.vesna.launcher.workspace.DEFAULT_WORKSPACE_PAGE_ID])
 * always exists: seeded at schema creation by
 * [LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK] and by the 3→4 migration for
 * upgraded installs.
 */
@Entity(tableName = "workspace_pages")
data class WorkspacePageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rank: Int,
)
