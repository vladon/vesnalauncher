package dev.vladon.vesna.launcher.data

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Room migration verification through the unmodified production
 * [LauncherDatabase.build]: authentic historical schemas are seeded into a
 * unique disposable test file and migrated 1→2→3 / 2→3. Migration and
 * Room's own post-migration schema + identity validation run inside Room;
 * any failure throws uncaught and fails the test.
 *
 * Historical sources (authentic Room-generated DDL of the era):
 * - v1: tag `v0.1.0` — `app/src/main/java/dev/vladon/vesna/launcher/data/LauncherDatabase.kt`
 *   (version 1, no migrations) + `WorkspaceItemEntity.kt`.
 * - v2: tag `v0.2.0` — same files, adds `userSerial INTEGER NOT NULL` inline.
 *
 * Kept checks (not duplicated by Room): isolation fails fast before Room opens
 * (seeded file absent, production path/dir distinct, opened file IS the seeded
 * one), `PRAGMA user_version` = 3, surviving `(packageName, className)` index
 * columns, row identity/container/cell/userSerial/folderId (null legacy
 * linkage), empty `folders`, data intact after close/reopen. Table_info and
 * identity-hash equality are deliberately not re-asserted: Room validates both
 * itself on open. Process-death/reboot durability is a separate task; the
 * production database is never touched.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationQaTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    /** Unique app-private directory per test instance (JUnit4: one instance per test). */
    private val isolatedDir: File =
        appContext.getDir("qa-room-migration-${UUID.randomUUID()}", Context.MODE_PRIVATE)

    /** The seeded file, named exactly what the production builder passes to Room. */
    private val seededDbFile: File = File(isolatedDir, BUILDER_DB_NAME)

    @Before
    fun ensureCleanTarget() {
        isolatedDir.listFiles()?.forEach { it.delete() }
        assertFalse("isolated test database must not pre-exist", seededDbFile.exists())
    }

    @After
    fun deleteIsolatedDirectory() {
        // ContextImpl.getDir prefixes "app_"; guard against deleting anything else.
        check(isolatedDir.name.contains("qa-room-migration-")) { "unexpected isolated dir: ${isolatedDir.name}" }
        isolatedDir.deleteRecursively()
    }

    // ------------------------------------------------------------ test cases

    @Test
    fun migrationFromV1PreservesLayoutAndDerivesDefaults() = migrateAndVerify(
        version = 1,
        createItemsSql = SQL_CREATE_ITEMS_V1,
        rows = listOf(
            FixtureRow(id = 1, pkg = "dev.vladon.qa.fixture1", cls = ".AlphaActivity", container = "WORKSPACE", cell = 5),
            FixtureRow(id = 2, pkg = "dev.vladon.qa.fixture2", cls = ".BetaActivity", container = "DOCK", cell = 2),
            FixtureRow(id = 3, pkg = "dev.vladon.qa.fixture3", cls = ".GammaActivity", container = "WORKSPACE", cell = 9),
        ),
        expectedItems = listOf(
            WorkspaceItemEntity(id = 1, packageName = "dev.vladon.qa.fixture1", className = ".AlphaActivity", container = "WORKSPACE", cellIndex = 5, userSerial = 0, folderId = null),
            WorkspaceItemEntity(id = 2, packageName = "dev.vladon.qa.fixture2", className = ".BetaActivity", container = "DOCK", cellIndex = 2, userSerial = 0, folderId = null),
            WorkspaceItemEntity(id = 3, packageName = "dev.vladon.qa.fixture3", className = ".GammaActivity", container = "WORKSPACE", cellIndex = 9, userSerial = 0, folderId = null),
        ),
    )

    @Test
    fun migrationFromV2PreservesUserSerialsAndAddsFolders() = migrateAndVerify(
        version = 2,
        createItemsSql = SQL_CREATE_ITEMS_V2,
        rows = listOf(
            FixtureRow(id = 1, pkg = "dev.vladon.qa.fixture1", cls = ".AlphaActivity", container = "WORKSPACE", cell = 5, userSerial = 0),
            FixtureRow(id = 2, pkg = "dev.vladon.qa.fixture2", cls = ".BetaActivity", container = "DOCK", cell = 2, userSerial = 11),
        ),
        expectedItems = listOf(
            WorkspaceItemEntity(id = 1, packageName = "dev.vladon.qa.fixture1", className = ".AlphaActivity", container = "WORKSPACE", cellIndex = 5, userSerial = 0, folderId = null),
            WorkspaceItemEntity(id = 2, packageName = "dev.vladon.qa.fixture2", className = ".BetaActivity", container = "DOCK", cellIndex = 2, userSerial = 11, folderId = null),
        ),
    )

    // ------------------------------------------------- historical schema seeds

    /** Room-generated DDL for the `workspace_items` entity at tag `v0.1.0` (v1). */
    private val SQL_CREATE_ITEMS_V1 =
        "CREATE TABLE IF NOT EXISTS `workspace_items` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`packageName` TEXT NOT NULL, " +
            "`className` TEXT NOT NULL, " +
            "`container` TEXT NOT NULL, " +
            "`cellIndex` INTEGER NOT NULL)"

    /** Room-generated DDL for the `workspace_items` entity at tag `v0.2.0` (v2): adds `userSerial` inline (no SQL DEFAULT). */
    private val SQL_CREATE_ITEMS_V2 =
        "CREATE TABLE IF NOT EXISTS `workspace_items` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`packageName` TEXT NOT NULL, " +
            "`className` TEXT NOT NULL, " +
            "`container` TEXT NOT NULL, " +
            "`cellIndex` INTEGER NOT NULL, " +
            "`userSerial` INTEGER NOT NULL)"

    /** Same index declaration on the entity at both tags. */
    private val SQL_CREATE_ITEMS_INDEX =
        "CREATE INDEX IF NOT EXISTS `index_workspace_items_packageName_className` " +
            "ON `workspace_items` (`packageName`, `className`)"

    /** Room's own master-table DDL (RoomMasterTable.CREATE_QUERY). */
    private val SQL_CREATE_ROOM_MASTER =
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"

    private data class FixtureRow(
        val id: Long,
        val pkg: String,
        val cls: String,
        val container: String,
        val cell: Int,
        val userSerial: Long = 0,
    )

    private data class Seed(val createSql: String, val inserts: List<Pair<String, Array<Any?>>>)

    /** Fixture rows inserted with parameter binding (no interpolated SQL). */
    private fun buildSeed(version: Int, createSql: String, rows: List<FixtureRow>): Seed {
        val insertSql = if (version == 1) {
            "INSERT INTO workspace_items (id, packageName, className, container, cellIndex) VALUES (?, ?, ?, ?, ?)"
        } else {
            "INSERT INTO workspace_items (id, packageName, className, container, cellIndex, userSerial) VALUES (?, ?, ?, ?, ?, ?)"
        }
        return Seed(
            createSql = createSql,
            inserts = rows.map { row ->
                val args = if (version == 1) {
                    arrayOf<Any?>(row.id, row.pkg, row.cls, row.container, row.cell)
                } else {
                    arrayOf<Any?>(row.id, row.pkg, row.cls, row.container, row.cell, row.userSerial)
                }
                insertSql to args
            },
        )
    }

    private fun seedHistoricalDatabase(version: Int, seed: Seed) {
        SQLiteDatabase.openOrCreateDatabase(seededDbFile, null).use { raw ->
            raw.execSQL(seed.createSql)
            raw.execSQL(SQL_CREATE_ITEMS_INDEX)
            seed.inserts.forEach { (sql, args) -> raw.execSQL(sql, args) }
            raw.version = version
            raw.execSQL(SQL_CREATE_ROOM_MASTER)
            // Placeholder identity: Room overwrites it with the current hash
            // while running the migrations, then re-validates it on reopen.
            raw.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, '$IDENTITY_PLACEHOLDER')",
            )
        }
    }

    // ----------------------------------------------- single migration check

    /**
     * The one migration check: isolation asserts fail fast before Room opens,
     * then the production builder migrates the seeded file (failures
     * propagate), metadata/rows are verified, and a close/reopen verifies the
     * data survived. Both Room instances are closed on every path.
     */
    private fun migrateAndVerify(version: Int, createItemsSql: String, rows: List<FixtureRow>, expectedItems: List<WorkspaceItemEntity>) {
        // Isolation before anything touches Room (deterministic path proof; the
        // production file's mtime is racy because the app may open it itself).
        val productionPath = appContext.getDatabasePath(PRODUCTION_DB_NAME).canonicalPath
        assertFalse("seeded file must not pre-exist", seededDbFile.exists())
        assertNotEquals("test database must not resolve to the production file", productionPath, seededDbFile.canonicalPath)
        assertNotEquals("test database must not share the production directory", File(productionPath).parent, seededDbFile.parent)

        seedHistoricalDatabase(version, buildSeed(version, createItemsSql, rows))

        withOpenedDatabase { db ->
            assertEquals(seededDbFile.canonicalPath, File(db.openHelper.writableDatabase.path).canonicalPath)
            assertEquals(3, pragmaUserVersion(db))
            assertEquals(listOf("packageName", "className"), pragmaIndexColumns(db))
            assertEquals(expectedItems, readItems(db))
            assertEquals("migration must not invent folder rows", emptyList<FolderEntity>(), readFolders(db))
        }

        // Close/reopen: plain open makes Room re-validate the written identity.
        withOpenedDatabase { db ->
            assertEquals(seededDbFile.canonicalPath, File(db.openHelper.writableDatabase.path).canonicalPath)
            assertEquals(expectedItems, readItems(db))
        }
    }

    private inline fun <T> withOpenedDatabase(block: (LauncherDatabase) -> T): T {
        val db = LauncherDatabase.build(IsolatedDbContext(appContext, seededDbFile))
        try {
            runBlocking { db.workspaceItemDao().getAll() } // forces the open + migration
            return block(db)
        } finally {
            runCatching { db.close() }
        }
    }

    // --------------------------------------------------------- small helpers

    private fun readItems(db: LauncherDatabase): List<WorkspaceItemEntity> =
        runBlocking { db.workspaceItemDao().getAll().sortedBy { it.id } }

    private fun readFolders(db: LauncherDatabase): List<FolderEntity> =
        runBlocking { db.folderDao().getAll() }

    private fun pragmaUserVersion(db: LauncherDatabase): Int =
        db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA user_version")).use { c ->
            check(c.moveToFirst()) { "PRAGMA user_version returned no row" }
            c.getInt(0)
        }

    private fun pragmaIndexColumns(db: LauncherDatabase): List<String> =
        db.openHelper.writableDatabase
            .query(SimpleSQLiteQuery("PRAGMA index_info('index_workspace_items_packageName_className')")).use { c ->
                buildList {
                    while (c.moveToNext()) add(c.getString(2)) // seqno, cid, name
                }
            }

    // --------------------------------------------------------- isolation shim

    /**
     * Routes every database name the production builder can pass (it hardcodes
     * `"vesna-launcher.db"`) to the exact seeded file — the only paths Room
     * 2.8.5 / sqlite-framework 2.6.2 use (verified in bytecode: no
     * `applicationContext` normalization). `deleteDatabase` is overridden so a
     * corruption-recovery path cannot delete the real production file.
     */
    private class IsolatedDbContext(base: Context, private val seededFile: File) : ContextWrapper(base) {
        override fun getDatabasePath(name: String): File = seededFile

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?,
        ): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(seededFile.path, factory, errorHandler)

        override fun deleteDatabase(name: String): Boolean = seededFile.exists() && seededFile.delete()
    }

    private companion object {
        /** The database name hardcoded in [LauncherDatabase.build]. */
        const val BUILDER_DB_NAME = "vesna-launcher.db"

        /** The real production database name, only ever used for path-distinct assertions. */
        const val PRODUCTION_DB_NAME = "vesna-launcher.db"

        const val IDENTITY_PLACEHOLDER = "qa-historical-seed"
    }
}
