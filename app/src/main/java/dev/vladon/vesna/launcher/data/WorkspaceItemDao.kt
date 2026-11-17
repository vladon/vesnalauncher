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

    @Query(
        "UPDATE workspace_items SET container = :container, cellIndex = :cellIndex WHERE id = :id",
    )
    suspend fun updatePlacement(id: Long, container: String, cellIndex: Int)

    @Query("UPDATE workspace_items SET folderId = :folderId WHERE id = :id")
    suspend fun updateFolder(id: Long, folderId: Long?)

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "DELETE FROM workspace_items WHERE packageName = :packageName " +
            "AND className = :className AND container = :container AND userSerial = :userSerial",
    )
    suspend fun deleteByKey(packageName: String, className: String, container: String, userSerial: Long)

    @Query("DELETE FROM workspace_items WHERE packageName = :packageName AND userSerial = :userSerial")
    suspend fun deleteByPackage(packageName: String, userSerial: Long)

    @Query("DELETE FROM workspace_items")
    suspend fun deleteAll()
}
