package dev.vladon.vesna.launcher.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.vladon.vesna.launcher.workspace.Container
import dev.vladon.vesna.launcher.workspace.DEFAULT_WORKSPACE_PAGE_ID

/**
 * Persisted launcher layout entry. `container` holds the name of
 * [dev.vladon.vesna.launcher.workspace.Container]; for WORKSPACE rows `cellIndex`
 * is the grid cell, for DOCK rows it is the position order. `userSerial` is the
 * serial number of the owning Android profile (`0` = main profile). `folderId`
 * links the entry to a folder: a non-null value means the entry lives inside that
 * folder and its `cellIndex` is the position within the folder. `pageId` is the
 * workspace page a top-level entry sits on: non-null only for rows outside
 * folders whose container is not DOCK, and an enforced foreign key to
 * [WorkspacePageEntity] (`ON DELETE RESTRICT` keeps a page from being dropped
 * while rows reference it). SQLite's `ALTER TABLE ADD COLUMN` accepts a
 * `REFERENCES` clause when the added column defaults to NULL, which is exactly
 * how the 3→4 migration attaches it to existing installs. The default places
 * legacy construction (tests) on the default page.
 */
@Entity(
    tableName = "workspace_items",
    indices = [Index("packageName", "className"), Index("pageId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkspacePageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val className: String,
    val container: String,
    val cellIndex: Int,
    val userSerial: Long = 0,
    val folderId: Long? = null,
    val pageId: Long? =
        if (folderId == null && container == Container.WORKSPACE.name) DEFAULT_WORKSPACE_PAGE_ID else null,
)
