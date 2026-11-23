package dev.vladon.vesna.launcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkspacePageDao {

    /** All pages, ordered by rank then id (stable order for equal ranks). */
    @Query("SELECT * FROM workspace_pages ORDER BY rank, id")
    suspend fun getAll(): List<WorkspacePageEntity>

    @Query("SELECT * FROM workspace_pages WHERE id = :id")
    suspend fun getById(id: Long): WorkspacePageEntity?

    @Insert
    suspend fun insert(page: WorkspacePageEntity): Long

    /** A page with saved content is never deleted, even if its icons aren't visible. */
    @Query(
        "DELETE FROM workspace_pages WHERE id = :pageId " +
            "AND NOT EXISTS (SELECT 1 FROM workspace_items WHERE pageId = :pageId) " +
            "AND NOT EXISTS (SELECT 1 FROM folders WHERE pageId = :pageId)"
    )
    suspend fun deleteIfEmpty(pageId: Long): Int

    @Query("UPDATE workspace_pages SET rank = :rank WHERE id = :id")
    suspend fun updateRank(id: Long, rank: Int)
}
