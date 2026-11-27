package dev.vladon.vesna.launcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WidgetDao {

    @Query("SELECT * FROM widgets")
    suspend fun all(): List<WidgetEntity>

    @Query("SELECT * FROM widgets WHERE pageId = :pageId")
    suspend fun forPage(pageId: Long): List<WidgetEntity>

    @Insert
    suspend fun insert(widget: WidgetEntity): Long

    @Query("DELETE FROM widgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM widgets")
    suspend fun count(): Int
}
