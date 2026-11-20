package dev.vladon.vesna.launcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceItemDao {

    @Query("SELECT * FROM workspace_items")
    fun observeAll(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items")
    suspend fun getAll(): List<WorkspaceItemEntity>

    @Insert
    suspend fun insert(item: WorkspaceItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<WorkspaceItemEntity>)

    @Query("UPDATE workspace_items SET cellIndex = :cellIndex WHERE id = :id")
    suspend fun updateCellIndex(id: Long, cellIndex: Int)

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

    /** Deletes the selected workspace pin without touching copies in other cells. */
    @Query(
        "DELETE FROM workspace_items WHERE id = (" +
            "SELECT id FROM workspace_items WHERE packageName = :packageName AND className = :className " +
            "AND userSerial = :userSerial AND pageId = :pageId AND cellIndex = :cell " +
            "AND container != 'DOCK' AND folderId IS NULL ORDER BY id LIMIT 1)",
    )
    suspend fun deleteWorkspacePin(packageName: String, className: String, userSerial: Long, pageId: Long, cell: Int)

    @Query("DELETE FROM workspace_items WHERE id = :id AND container = 'DOCK' AND folderId IS NULL")
    suspend fun deleteDockPin(id: Long)

    @Query("DELETE FROM workspace_items WHERE packageName = :packageName AND userSerial = :userSerial")
    suspend fun deleteByPackage(packageName: String, userSerial: Long)
}
