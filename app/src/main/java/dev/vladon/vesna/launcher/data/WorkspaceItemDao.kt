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

    /**
     * Deletes all matched top-level rows for a key+container+profile on one
     * page (an app can hold several top-level pins across pages). Rows that
     * live inside a folder (`folderId` non-null) share the key and container —
     * for example a dock item dragged onto a workspace folder — and are
     * deliberately preserved, as are identical pins on other pages: a
     * menu-driven unpin removes only the top-level placements of the page it is
     * invoked on, never folder membership or other pages. `pageId IS :pageId`
     * matches the dock (`pageId` null) and each workspace page alike. Bulk
     * uninstall cleanup uses [deleteByPackage] instead.
     */
    @Query(
        "DELETE FROM workspace_items WHERE packageName = :packageName " +
            "AND className = :className AND container = :container AND userSerial = :userSerial " +
            "AND folderId IS NULL AND pageId IS :pageId",
    )
    suspend fun deleteTopLevelByKey(
        packageName: String,
        className: String,
        container: String,
        userSerial: Long,
        pageId: Long?,
    )

    @Query("DELETE FROM workspace_items WHERE packageName = :packageName AND userSerial = :userSerial")
    suspend fun deleteByPackage(packageName: String, userSerial: Long)

    @Query("DELETE FROM workspace_items")
    suspend fun deleteAll()
}
