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

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "DELETE FROM workspace_items WHERE packageName = :packageName " +
            "AND className = :className AND container = :container",
    )
    suspend fun deleteByKey(packageName: String, className: String, container: String)

    @Query("DELETE FROM workspace_items WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM workspace_items")
    suspend fun deleteAll()
}
