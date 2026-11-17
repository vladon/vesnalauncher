package dev.vladon.vesna.launcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A workspace folder occupying a grid cell. */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val cellIndex: Int,
)
