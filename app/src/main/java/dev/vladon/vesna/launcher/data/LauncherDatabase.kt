package dev.vladon.vesna.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.workspace.Container
import dev.vladon.vesna.launcher.workspace.DEFAULT_WORKSPACE_PAGE_ID
import dev.vladon.vesna.launcher.workspace.WorkspaceItem

@Database(
    entities = [WorkspaceItemEntity::class, FolderEntity::class, WorkspacePageEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun workspaceItemDao(): WorkspaceItemDao

    abstract fun folderDao(): FolderDao

    abstract fun workspacePageDao(): WorkspacePageDao

    companion object {
        /** Room DDL for `workspace_pages` (fresh installs get it from the entity). */
        private const val SQL_CREATE_PAGES =
            "CREATE TABLE IF NOT EXISTS `workspace_pages` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`rank` INTEGER NOT NULL)"

        /** The single default page row. */
        private const val SQL_SEED_DEFAULT_PAGE =
            "INSERT INTO `workspace_pages` (`id`, `rank`) VALUES ($DEFAULT_WORKSPACE_PAGE_ID, 0)"

        /**
         * Seeds the default page at schema creation. The production builder and
         * every in-memory test builder add this same callback so both start
         * with page [DEFAULT_WORKSPACE_PAGE_ID]; upgraded installs get the row
         * from [MIGRATION_3_4] instead (onCreate never runs for existing files).
         */
        val DEFAULT_PAGE_SEED_CALLBACK: RoomDatabase.Callback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_SEED_DEFAULT_PAGE)
            }
        }

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

        /**
         * 3 → 4: added workspace pages. Creates `workspace_pages`, seeds the
         * default page, then adds the nullable `pageId` foreign-key columns and
         * their indices. `ALTER TABLE ADD COLUMN` accepts a `REFERENCES` clause
         * when the added column's default is NULL (ours declares none), so
         * existing installs get the same enforced `ON DELETE RESTRICT` page
         * reference that fresh installs are created with. Every top-level
         * non-DOCK row lands on the default page — including rows with unknown
         * legacy container values, which the domain mapping falls back to
         * WORKSPACE — while dock rows and folder members keep no page (NULL
         * passes the foreign key). All folders move onto the default page; the
         * parent row is inserted before any child references it.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_CREATE_PAGES)
                db.execSQL(SQL_SEED_DEFAULT_PAGE)
                db.execSQL(
                    "ALTER TABLE workspace_items ADD COLUMN pageId INTEGER " +
                        "REFERENCES workspace_pages(id) ON DELETE RESTRICT",
                )
                db.execSQL(
                    "UPDATE workspace_items SET pageId = $DEFAULT_WORKSPACE_PAGE_ID " +
                        "WHERE folderId IS NULL AND container != 'DOCK'",
                )
                db.execSQL(
                    "ALTER TABLE folders ADD COLUMN pageId INTEGER " +
                        "REFERENCES workspace_pages(id) ON DELETE RESTRICT",
                )
                db.execSQL("UPDATE folders SET pageId = $DEFAULT_WORKSPACE_PAGE_ID")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_items_pageId` ON `workspace_items` (`pageId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_pageId` ON `folders` (`pageId`)")
            }
        }

        fun build(appContext: Context): LauncherDatabase =
            Room.databaseBuilder(appContext, LauncherDatabase::class.java, "vesna-launcher.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(DEFAULT_PAGE_SEED_CALLBACK)
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
        pageId = pageId,
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
        pageId = pageId,
    )
