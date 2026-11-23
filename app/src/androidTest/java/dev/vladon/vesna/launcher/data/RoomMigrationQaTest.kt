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
 * unique disposable test file and migrated to version 4 (1→2→3→4, 2→3→4,
 * 3→4), and a fresh install exercises Room's on-create path. Migration and
 * Room's own post-migration schema + identity validation run inside Room; any
 * failure throws uncaught and fails the test.
 *
 * Historical sources (authentic Room-generated DDL of the era):
 * - v1: tag `v0.1.0` — `app/src/main/java/dev/vladon/vesna/launcher/data/LauncherDatabase.kt`
 *   (version 1, no migrations) + `WorkspaceItemEntity.kt`.
 * - v2: tag `v0.2.0` — same files, adds `userSerial INTEGER NOT NULL` inline.
 * - v3: tag `v0.4.2` — same files at version 3 (`MIGRATION_1_2`/`MIGRATION_2_3`):
 *   `folderId` on `workspace_items`, `folders` table without pages.
 *
 * Fixture matrix: v1/v2 layout rows (page assignment derived), a full v3
 * workspace/dock/folder-member mix, an empty v3 database, a malformed v3
 * database (unknown container, orphan `folderId`, negative and duplicate
 * `cellIndex`), and a fresh install (default-page seed callback).
 *
 * Kept checks (not duplicated by Room): isolation fails fast before Room opens
 * (seeded file absent, production path/dir distinct, opened file IS the seeded
 * one), `PRAGMA user_version` = 5, surviving `(packageName, className)` index
 * columns plus the 3→4 `pageId` indices, row identity/container/cell/
 * userSerial/folderId, derived `pageId` (default page for top-level non-DOCK
 * rows, null for dock rows and folder members), folders on the default page,
 * exactly one page row (the default page, rank 0), data intact after
 * close/reopen. Table_info, identity-hash equality and the enforced page
 * foreign keys (child column, parent table, `ON DELETE RESTRICT` action) are
 * deliberately not re-asserted: Room validates all of them itself on open via
 * its schema comparison. Process-death/reboot durability is a separate task;
 * the production database is never touched.
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
            WorkspaceItemEntity(id = 1, packageName = "dev.vladon.qa.fixture1", className = ".AlphaActivity", container = "WORKSPACE", cellIndex = 5, userSerial = 0, folderId = null, pageId = 1),
            WorkspaceItemEntity(id = 2, packageName = "dev.vladon.qa.fixture2", className = ".BetaActivity", container = "DOCK", cellIndex = 2, userSerial = 0, folderId = null, pageId = null),
            WorkspaceItemEntity(id = 3, packageName = "dev.vladon.qa.fixture3", className = ".GammaActivity", container = "WORKSPACE", cellIndex = 9, userSerial = 0, folderId = null, pageId = 1),
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
            WorkspaceItemEntity(id = 1, packageName = "dev.vladon.qa.fixture1", className = ".AlphaActivity", container = "WORKSPACE", cellIndex = 5, userSerial = 0, folderId = null, pageId = 1),
            WorkspaceItemEntity(id = 2, packageName = "dev.vladon.qa.fixture2", className = ".BetaActivity", container = "DOCK", cellIndex = 2, userSerial = 11, folderId = null, pageId = null),
        ),
    )

    @Test
    fun migrationFromV3AssignsDefaultPageAndKeepsMembership() = migrateAndVerify(
        version = 3,
        createItemsSql = SQL_CREATE_ITEMS_V3,
        rows = listOf(
            FixtureRow(id = 1, pkg = "dev.vladon.qa.fixture1", cls = ".AlphaActivity", container = "WORKSPACE", cell = 5),
            FixtureRow(id = 2, pkg = "dev.vladon.qa.fixture2", cls = ".BetaActivity", container = "DOCK", cell = 2),
            FixtureRow(id = 3, pkg = "dev.vladon.qa.fixture3", cls = ".GammaActivity", container = "WORKSPACE", cell = 6, folderId = 1),
            FixtureRow(id = 4, pkg = "dev.vladon.qa.fixture4", cls = ".DeltaActivity", container = "DOCK", cell = 0, folderId = 1),
        ),
        expectedItems = listOf(
            WorkspaceItemEntity(id = 1, packageName = "dev.vladon.qa.fixture1", className = ".AlphaActivity", container = "WORKSPACE", cellIndex = 5, userSerial = 0, folderId = null, pageId = 1),
            WorkspaceItemEntity(id = 2, packageName = "dev.vladon.qa.fixture2", className = ".BetaActivity", container = "DOCK", cellIndex = 2, userSerial = 0, folderId = null, pageId = null),
            WorkspaceItemEntity(id = 3, packageName = "dev.vladon.qa.fixture3", className = ".GammaActivity", container = "WORKSPACE", cellIndex = 6, userSerial = 0, folderId = 1, pageId = null),
            WorkspaceItemEntity(id = 4, packageName = "dev.vladon.qa.fixture4", className = ".DeltaActivity", container = "DOCK", cellIndex = 0, userSerial = 0, folderId = 1, pageId = null),
        ),
        folders = listOf(
            FolderRow(id = 1, title = "Work", cellIndex = 9),
            FolderRow(id = 2, title = "Play", cellIndex = 10),
        ),
        expectedFolders = listOf(
            FolderEntity(id = 1, title = "Work", cellIndex = 9, pageId = 1),
            FolderEntity(id = 2, title = "Play", cellIndex = 10, pageId = 1),
        ),
    )

    @Test
    fun migrationFromV3EmptyDatabaseStillSeedsDefaultPage() = migrateAndVerify(
        version = 3,
        createItemsSql = SQL_CREATE_ITEMS_V3,
        rows = emptyList(),
        expectedItems = emptyList(),
    )

    @Test
    fun migrationFromV3MalformedRowsFallBackToDefaultPage() = migrateAndVerify(
        version = 3,
        createItemsSql = SQL_CREATE_ITEMS_V3,
        rows = listOf(
            // Unknown legacy container value: the migration treats it like any
            // non-DOCK row (the domain fallback resolves it to WORKSPACE, so
            // the row must end up on the default page to stay consistent).
            FixtureRow(id = 1, pkg = "dev.vladon.qa.fixture1", cls = ".EpsilonActivity", container = "WIDGET", cell = 4),
            FixtureRow(id = 2, pkg = "dev.vladon.qa.fixture2", cls = ".ZetaActivity", container = "DOCK", cell = 1),
            // Orphan folder membership (no matching folder row) must neither
            // crash the migration nor gain a page; a negative and a duplicate
            // cellIndex must be preserved verbatim.
            FixtureRow(id = 3, pkg = "dev.vladon.qa.fixture3", cls = ".EtaActivity", container = "WORKSPACE", cell = -3, folderId = 99),
            FixtureRow(id = 4, pkg = "dev.vladon.qa.fixture4", cls = ".ThetaActivity", container = "WORKSPACE", cell = 4),
        ),
        expectedItems = listOf(
            WorkspaceItemEntity(id = 1, packageName = "dev.vladon.qa.fixture1", className = ".EpsilonActivity", container = "WIDGET", cellIndex = 4, userSerial = 0, folderId = null, pageId = 1),
            WorkspaceItemEntity(id = 2, packageName = "dev.vladon.qa.fixture2", className = ".ZetaActivity", container = "DOCK", cellIndex = 1, userSerial = 0, folderId = null, pageId = null),
            WorkspaceItemEntity(id = 3, packageName = "dev.vladon.qa.fixture3", className = ".EtaActivity", container = "WORKSPACE", cellIndex = -3, userSerial = 0, folderId = 99, pageId = null),
            WorkspaceItemEntity(id = 4, packageName = "dev.vladon.qa.fixture4", className = ".ThetaActivity", container = "WORKSPACE", cellIndex = 4, userSerial = 0, folderId = null, pageId = 1),
        ),
        // folderId 99 has no matching folder row; the migration must not invent one.
        folders = emptyList(),
        expectedFolders = emptyList(),
    )

    @Test
    fun freshInstallSeedsSingleDefaultPage() {
        assertIsolatedTarget()

        withOpenedDatabase { db ->
            assertEquals(seededDbFile.canonicalPath, File(db.openHelper.writableDatabase.path).canonicalPath)
            assertEquals(5, pragmaUserVersion(db))
            assertEquals(emptyList<WorkspaceItemEntity>(), readItems(db))
            assertEquals(emptyList<FolderEntity>(), readFolders(db))
            assertEquals(listOf(WorkspacePageEntity(id = 1, rank = 0)), readPages(db))
        }
    }

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

    /** Room-generated DDL for the `workspace_items` entity at tag `v0.4.2` (v3): adds `folderId` (nullable). */
    private val SQL_CREATE_ITEMS_V3 =
        "CREATE TABLE IF NOT EXISTS `workspace_items` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`packageName` TEXT NOT NULL, " +
            "`className` TEXT NOT NULL, " +
            "`container` TEXT NOT NULL, " +
            "`cellIndex` INTEGER NOT NULL, " +
            "`userSerial` INTEGER NOT NULL, " +
            "`folderId` INTEGER)"

    /** Room-generated DDL for the `folders` entity at tag `v0.4.2` (v3): no pages yet. */
    private val SQL_CREATE_FOLDERS_V3 =
        "CREATE TABLE IF NOT EXISTS `folders` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`title` TEXT NOT NULL, " +
            "`cellIndex` INTEGER NOT NULL)"

    /** Same index declaration on the entity at both historical tags. */
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
        val folderId: Long? = null,
    )

    private data class FolderRow(val id: Long, val title: String, val cellIndex: Int)

    private data class Seed(
        val createSql: String,
        val folderCreateSql: String?,
        val inserts: List<Pair<String, Array<Any?>>>,
    )

    /** Fixture rows inserted with parameter binding (no interpolated SQL). */
    private fun buildSeed(version: Int, createSql: String, rows: List<FixtureRow>, folders: List<FolderRow> = emptyList()): Seed {
        val itemInsertSql = when (version) {
            1 -> "INSERT INTO workspace_items (id, packageName, className, container, cellIndex) VALUES (?, ?, ?, ?, ?)"
            2 -> "INSERT INTO workspace_items (id, packageName, className, container, cellIndex, userSerial) VALUES (?, ?, ?, ?, ?, ?)"
            else -> "INSERT INTO workspace_items (id, packageName, className, container, cellIndex, userSerial, folderId) VALUES (?, ?, ?, ?, ?, ?, ?)"
        }
        val itemInserts = rows.map { row ->
            itemInsertSql to when (version) {
                1 -> arrayOf<Any?>(row.id, row.pkg, row.cls, row.container, row.cell)
                2 -> arrayOf<Any?>(row.id, row.pkg, row.cls, row.container, row.cell, row.userSerial)
                else -> arrayOf<Any?>(row.id, row.pkg, row.cls, row.container, row.cell, row.userSerial, row.folderId)
            }
        }
        val folderInserts = folders.map { folder ->
            "INSERT INTO folders (id, title, cellIndex) VALUES (?, ?, ?)" to
                arrayOf<Any?>(folder.id, folder.title, folder.cellIndex)
        }
        return Seed(
            createSql = createSql,
            folderCreateSql = if (version >= 3) SQL_CREATE_FOLDERS_V3 else null,
            inserts = itemInserts + folderInserts,
        )
    }

    private fun seedHistoricalDatabase(version: Int, seed: Seed) {
        SQLiteDatabase.openOrCreateDatabase(seededDbFile, null).use { raw ->
            raw.execSQL(seed.createSql)
            raw.execSQL(SQL_CREATE_ITEMS_INDEX)
            seed.folderCreateSql?.let(raw::execSQL)
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
    private fun migrateAndVerify(
        version: Int,
        createItemsSql: String,
        rows: List<FixtureRow>,
        expectedItems: List<WorkspaceItemEntity>,
        folders: List<FolderRow> = emptyList(),
        expectedFolders: List<FolderEntity> = emptyList(),
        expectedPages: List<WorkspacePageEntity> = listOf(WorkspacePageEntity(id = 1, rank = 0)),
    ) {
        assertIsolatedTarget()

        seedHistoricalDatabase(version, buildSeed(version, createItemsSql, rows, folders))

        withOpenedDatabase { db ->
            assertEquals(seededDbFile.canonicalPath, File(db.openHelper.writableDatabase.path).canonicalPath)
            assertEquals(5, pragmaUserVersion(db))
            assertEquals(listOf("packageName", "className"), pragmaIndexColumns(db, INDEX_ITEMS_LEGACY))
            assertEquals(listOf("pageId"), pragmaIndexColumns(db, INDEX_ITEMS_PAGE))
            assertEquals(listOf("pageId"), pragmaIndexColumns(db, INDEX_FOLDERS_PAGE))
            assertEquals(expectedItems, readItems(db))
            assertEquals(expectedFolders, readFolders(db))
            assertEquals(expectedPages, readPages(db))
        }

        // Close/reopen: plain open makes Room re-validate the written identity.
        withOpenedDatabase { db ->
            assertEquals(seededDbFile.canonicalPath, File(db.openHelper.writableDatabase.path).canonicalPath)
            assertEquals(expectedItems, readItems(db))
            assertEquals(expectedPages, readPages(db))
        }
    }

    /** Isolation asserts fail fast before anything touches Room. */
    private fun assertIsolatedTarget() {
        // Deterministic path proof; the production file's mtime is racy because
        // the app may open it itself.
        val productionPath = appContext.getDatabasePath(PRODUCTION_DB_NAME).canonicalPath
        assertFalse("seeded file must not pre-exist", seededDbFile.exists())
        assertNotEquals("test database must not resolve to the production file", productionPath, seededDbFile.canonicalPath)
        assertNotEquals("test database must not share the production directory", File(productionPath).parent, seededDbFile.parent)
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

    private fun readPages(db: LauncherDatabase): List<WorkspacePageEntity> =
        runBlocking { db.workspacePageDao().getAll() }

    private fun pragmaUserVersion(db: LauncherDatabase): Int =
        db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA user_version")).use { c ->
            check(c.moveToFirst()) { "PRAGMA user_version returned no row" }
            c.getInt(0)
        }

    private fun pragmaIndexColumns(db: LauncherDatabase, indexName: String): List<String> =
        db.openHelper.writableDatabase
            .query(SimpleSQLiteQuery("PRAGMA index_info('$indexName')")).use { c ->
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

        const val INDEX_ITEMS_LEGACY = "index_workspace_items_packageName_className"
        const val INDEX_ITEMS_PAGE = "index_workspace_items_pageId"
        const val INDEX_FOLDERS_PAGE = "index_folders_pageId"

        const val IDENTITY_PLACEHOLDER = "qa-historical-seed"
    }
}
