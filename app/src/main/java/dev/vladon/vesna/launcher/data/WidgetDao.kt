package dev.vladon.vesna.launcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {

    @Query("SELECT * FROM widgets")
    suspend fun all(): List<WidgetEntity>

    @Query("SELECT * FROM widgets")
    fun observeAll(): Flow<List<WidgetEntity>>

    @Query("SELECT * FROM widgets WHERE pageId = :pageId")
    suspend fun forPage(pageId: Long): List<WidgetEntity>

    @Insert
    suspend fun insert(widget: WidgetEntity): Long

    @Query("UPDATE widgets SET cellIndex = :cell WHERE id = :id")
    suspend fun updateCell(id: Long, cell: Int)

    @Query("DELETE FROM widgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM widgets")
    suspend fun count(): Int
}
