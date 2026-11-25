package dev.vladon.vesna.launcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("UPDATE folders SET cellIndex = :cell, pageId = :pageId WHERE id = :id")
    suspend fun updateWorkspacePosition(id: Long, cell: Int, pageId: Long)

    @Query(
        "UPDATE folders SET pageId = :pageId, cellIndex = :cell " +
            "WHERE id = :id AND pageId = :sourcePage AND cellIndex = :sourceCell " +
            "AND EXISTS(SELECT 1 FROM workspace_pages WHERE id = :pageId) " +
            "AND NOT EXISTS(SELECT 1 FROM folders WHERE pageId = :pageId AND cellIndex = :cell) " +
            "AND NOT EXISTS(SELECT 1 FROM workspace_items WHERE pageId = :pageId AND cellIndex = :cell " +
            "AND folderId IS NULL AND container != 'DOCK')",
    )
    suspend fun moveToEmptyCell(id: Long, sourcePage: Long, sourceCell: Int, pageId: Long, cell: Int): Int

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Clears every folder; only the restore transaction may wipe the table. */
    @Query("DELETE FROM folders")
    suspend fun deleteAll()
}
