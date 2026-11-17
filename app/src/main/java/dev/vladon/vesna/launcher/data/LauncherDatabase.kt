package dev.vladon.vesna.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.workspace.Container
import dev.vladon.vesna.launcher.workspace.WorkspaceItem

@Database(entities = [WorkspaceItemEntity::class, FolderEntity::class], version = 3, exportSchema = false)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun workspaceItemDao(): WorkspaceItemDao

    abstract fun folderDao(): FolderDao

    companion object {
        /** 1 → 2: added per-profile ownership of layout entries. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace_items ADD COLUMN userSerial INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** 2 → 3: added folders and per-entry folder membership. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, `cellIndex` INTEGER NOT NULL)",
                )
                db.execSQL("ALTER TABLE workspace_items ADD COLUMN folderId INTEGER")
            }
        }

        fun build(appContext: Context): LauncherDatabase =
            Room.databaseBuilder(appContext, LauncherDatabase::class.java, "vesna-launcher.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}

/** Entity to domain mapping. Unknown container values fall back to WORKSPACE. */
fun WorkspaceItemEntity.toDomain(): WorkspaceItem =
    WorkspaceItem(
        id = id,
        key = AppKey(packageName, className, userSerial),
        container = Container.entries.firstOrNull { it.name == container } ?: Container.WORKSPACE,
        cellIndex = cellIndex,
        userSerial = userSerial,
        folderId = folderId,
    )

/** Domain to entity mapping (used for inserts; `id = 0` lets Room autogenerate). */
fun WorkspaceItem.toEntity(): WorkspaceItemEntity =
    WorkspaceItemEntity(
        id = id,
        packageName = key.packageName,
        className = key.className,
        container = container.name,
        cellIndex = cellIndex,
        userSerial = key.userSerial,
        folderId = folderId,
    )
