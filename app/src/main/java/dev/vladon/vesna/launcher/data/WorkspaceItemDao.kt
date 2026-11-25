package dev.vladon.vesna.launcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkspaceItemDao {

    @Query("SELECT * FROM workspace_items")
    suspend fun getAll(): List<WorkspaceItemEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM workspace_items WHERE id = :id AND pageId = :pageId " +
            "AND cellIndex = :cell AND container != 'DOCK' AND folderId IS NULL AND shortcutId IS NULL)",
    )
    suspend fun hasRegularWorkspacePin(id: Long, pageId: Long, cell: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM workspace_items WHERE id = :id AND folderId = :folderId)")
    suspend fun hasFolderMember(id: Long, folderId: Long): Boolean

    @Query("SELECT * FROM workspace_items WHERE folderId = :folderId LIMIT 2")
    suspend fun firstTwoFolderMembers(folderId: Long): List<WorkspaceItemEntity>

    @Query("SELECT * FROM workspace_items WHERE folderId = :folderId ORDER BY cellIndex, id")
    suspend fun folderMembersInOrder(folderId: Long): List<WorkspaceItemEntity>

    @Insert
    suspend fun insert(item: WorkspaceItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<WorkspaceItemEntity>)

    @Query("UPDATE workspace_items SET cellIndex = :cellIndex WHERE id = :id")
    suspend fun updateCellIndex(id: Long, cellIndex: Int)

    @Query("UPDATE workspace_items SET cellIndex = :cell, pageId = :pageId WHERE id = :id")
    suspend fun updateWorkspacePosition(id: Long, cell: Int, pageId: Long)

    /**
     * Places the entry at top level atomically: container, grid/dock cell and
     * page are written in a single statement, and folder membership is cleared
     * (`folderId = NULL`), so no intermediate state (e.g. a page set while
     * still a folder member) is ever visible or persisted on crash. Callers
     * pass [pageId] only for workspace placements (null for the dock).
     */
    @Query(
        "UPDATE workspace_items SET container = :container, cellIndex = :cellIndex, " +
            "pageId = :pageId, folderId = NULL WHERE id = :id",
    )
    suspend fun updatePlacement(id: Long, container: String, cellIndex: Int, pageId: Long?)

    /** Moves the entry into a folder; folder members carry no page (`pageId` cleared). */
    @Query("UPDATE workspace_items SET folderId = :folderId, pageId = NULL WHERE id = :id")
    suspend fun updateFolder(id: Long, folderId: Long)

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Clears every pin; only the restore transaction may wipe the table. */
    @Query("DELETE FROM workspace_items")
    suspend fun deleteAll()


    @Query("DELETE FROM workspace_items WHERE folderId = :folderId")
    suspend fun deleteFolderMembers(folderId: Long)

    /** Deletes the selected workspace pin without touching copies in other cells. */
    @Query(
        "DELETE FROM workspace_items WHERE id = (" +
            "SELECT id FROM workspace_items WHERE packageName = :packageName AND className = :className " +
            "AND userSerial = :userSerial AND shortcutId IS :shortcutId " +
            "AND pageId = :pageId AND cellIndex = :cell " +
            "AND container != 'DOCK' AND folderId IS NULL ORDER BY id LIMIT 1)",
    )
    suspend fun deleteWorkspacePin(
        packageName: String,
        className: String,
        userSerial: Long,
        shortcutId: String?,
        pageId: Long,
        cell: Int,
    ): Int

    @Query("DELETE FROM workspace_items WHERE id = :id AND container = 'DOCK' AND folderId IS NULL")
    suspend fun deleteDockPin(id: Long)

    @Query(
        "SELECT DISTINCT folderId FROM workspace_items " +
            "WHERE packageName = :packageName AND userSerial = :userSerial AND folderId IS NOT NULL",
    )
    suspend fun folderIdsForPackage(packageName: String, userSerial: Long): List<Long>

    @Query("DELETE FROM workspace_items WHERE packageName = :packageName AND userSerial = :userSerial")
    suspend fun deleteByPackage(packageName: String, userSerial: Long)

    /** Real snapshot of the pinned action IDs of one publisher package in one profile. */
    @Query(
        "SELECT DISTINCT shortcutId FROM workspace_items " +
            "WHERE packageName = :packageName AND userSerial = :userSerial AND shortcutId IS NOT NULL",
    )
    suspend fun shortcutIds(packageName: String, userSerial: Long): List<String>

    /** Per-pin rename; `label = NULL` clears the override. Returns the row count written. */
    @Query("UPDATE workspace_items SET customLabel = :label WHERE id = :id")
    suspend fun updateCustomLabel(id: Long, label: String?): Int
}
