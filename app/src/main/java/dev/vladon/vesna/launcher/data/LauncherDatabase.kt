package dev.vladon.vesna.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.workspace.Container
import dev.vladon.vesna.launcher.workspace.WorkspaceItem

@Database(entities = [WorkspaceItemEntity::class], version = 1, exportSchema = false)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun workspaceItemDao(): WorkspaceItemDao

    companion object {
        fun build(appContext: Context): LauncherDatabase =
            Room.databaseBuilder(appContext, LauncherDatabase::class.java, "vesna-launcher.db")
                .build()
    }
}

/** Entity to domain mapping. Unknown container values fall back to WORKSPACE. */
fun WorkspaceItemEntity.toDomain(): WorkspaceItem =
    WorkspaceItem(
        id = id,
        key = AppKey(packageName, className),
        container = Container.entries.firstOrNull { it.name == container } ?: Container.WORKSPACE,
        cellIndex = cellIndex,
    )

/** Domain to entity mapping (used for inserts; `id = 0` lets Room autogenerate). */
fun WorkspaceItem.toEntity(): WorkspaceItemEntity =
    WorkspaceItemEntity(
        id = id,
        packageName = key.packageName,
        className = key.className,
        container = container.name,
        cellIndex = cellIndex,
    )
