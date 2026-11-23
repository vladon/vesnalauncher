package dev.vladon.vesna.launcher.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The optional row stores the grid selected by the user; absence means the default. */
@Entity(tableName = "workspace_grid")
internal data class WorkspaceGridEntity(
    @PrimaryKey val id: Int = 1,
    val columnCount: Int,
    val rowCount: Int,
)

@Dao
internal interface WorkspaceGridDao {
    @Query("SELECT * FROM workspace_grid WHERE id = 1")
    suspend fun get(): WorkspaceGridEntity?

    @Query("SELECT * FROM workspace_grid WHERE id = 1")
    fun observe(): Flow<WorkspaceGridEntity?>

    @Upsert
    suspend fun set(grid: WorkspaceGridEntity)
}
