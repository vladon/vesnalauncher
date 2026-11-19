package dev.vladon.vesna.launcher.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.vladon.vesna.launcher.workspace.DEFAULT_WORKSPACE_PAGE_ID

/**
 * A workspace folder occupying a grid cell. `pageId` is the workspace page the
 * folder sits on: an enforced foreign key to [WorkspacePageEntity]
 * (`ON DELETE RESTRICT` keeps a page from being dropped while folders reference
 * it). SQLite's `ALTER TABLE ADD COLUMN` accepts a `REFERENCES` clause when the
 * added column defaults to NULL, which is exactly how the 3→4 migration
 * attaches it to existing installs. The default places legacy construction
 * (tests) on the default page.
 */
@Entity(
    tableName = "folders",
    indices = [Index("pageId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkspacePageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val cellIndex: Int,
    val pageId: Long? = DEFAULT_WORKSPACE_PAGE_ID,
)
