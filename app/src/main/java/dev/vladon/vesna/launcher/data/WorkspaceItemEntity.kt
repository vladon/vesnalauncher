package dev.vladon.vesna.launcher.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted launcher layout entry. `container` holds the name of
 * [dev.vladon.vesna.launcher.workspace.Container]; for WORKSPACE rows `cellIndex`
 * is the grid cell, for DOCK rows it is the position order.
 */
@Entity(
    tableName = "workspace_items",
    indices = [Index("packageName", "className")],
)
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val className: String,
    val container: String,
    val cellIndex: Int,
)
