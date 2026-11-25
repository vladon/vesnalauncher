package dev.vladon.vesna.launcher.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.FolderSnapshot
import dev.vladon.vesna.launcher.data.DockPinSnapshot
import dev.vladon.vesna.launcher.data.LayoutSnapshotException
import dev.vladon.vesna.launcher.data.LayoutSnapshotV1
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.PinSnapshot
import dev.vladon.vesna.launcher.workspace.LayoutRestoreAttempt
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * User-controlled layout export and restore (ADR-BACKUP-1 part 1, #116):
 * the codec round trip, the destination validation, the all-or-nothing
 * replace, and the pending-package protection against stale-item cleanup.
 * Runs against an isolated in-memory Room database and its own preference file.
 */
@RunWith(AndroidJUnit4::class)
class LayoutTransferTest {

    private lateinit var db: LauncherDatabase
    private lateinit var repo: WorkspaceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .allowMainThreadQueries()
            .build()
        val prefs = context.getSharedPreferences(
            "layout-transfer-qa-${java.util.UUID.randomUUID()}",
            Context.MODE_PRIVATE,
        )
        repo = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun snapshot(
        columns: Int = 4,
        rows: Int = 5,
        pages: List<Int> = listOf(0),
        dock: List<DockPinSnapshot> = listOf(DockPinSnapshot("qa.dock", "qa.dock.Main", null, null, null)),
        folders: List<FolderSnapshot> = emptyList(),
        pins: List<PinSnapshot> = listOf(PinSnapshot("qa.app", "qa.app.Main", 0, 2, null, null, null, null)),
        seeded: Boolean = true,
    ) = LayoutSnapshotV1(columns, rows, seeded, pages, dock, folders, pins)

    @Test
    fun codecRoundTripKeepsEveryField() {
        val snapshot = snapshot(
            pages = listOf(0, 1),
            folders = listOf(FolderSnapshot("Tools", 0, 9)),
            pins = listOf(
                PinSnapshot("qa.app", "qa.app.Main", 0, 2, null, null, "Custom", null),
                PinSnapshot("qa.member", "qa.member.Main", 0, 0, 0, null, null, null),
            ),
            dock = listOf(
                DockPinSnapshot("qa.dock", "qa.dock.Main", null, null, null),
                DockPinSnapshot("qa.dock2", "qa.dock2.Main", "qa.shortcut", null, null),
            ),
        )
        assertEquals(snapshot, LayoutSnapshotV1.fromJson(snapshot.toJson()))
    }

    @Test(expected = LayoutSnapshotException::class)
    fun codecRejectsUnknownVersion() {
        LayoutSnapshotV1.fromJson(snapshot().toJson().replace("\"version\":1", "\"version\":99"))
    }

    @Test(expected = LayoutSnapshotException::class)
    fun codecRejectsCellOutsideGrid() {
        LayoutSnapshotV1.fromJson(snapshot(pins = listOf(PinSnapshot("qa.app", "qa.app.Main", 0, 999, null, null, null, null))).toJson())
    }

    @Test(expected = LayoutSnapshotException::class)
    fun codecRejectsUnknownPageReference() {
        LayoutSnapshotV1.fromJson(snapshot(pins = listOf(PinSnapshot("qa.app", "qa.app.Main", 5, 0, null, null, null, null))).toJson())
    }

    @Test(expected = LayoutSnapshotException::class)
    fun codecRejectsUnknownFolderReference() {
        LayoutSnapshotV1.fromJson(
            snapshot(
                pins = listOf(PinSnapshot("qa.app", "qa.app.Main", 0, 0, 3, null, null, null)),
            ).toJson(),
        )
    }

    @Test(expected = LayoutSnapshotException::class)
    fun codecRejectsTwoPinsOnOneCell() {
        LayoutSnapshotV1.fromJson(
            snapshot(
                pins = listOf(
                    PinSnapshot("qa.a", "qa.a.Main", 0, 4, null, null, null, null),
                    PinSnapshot("qa.b", "qa.b.Main", 0, 4, null, null, null, null),
                ),
            ).toJson(),
        )
    }

    @Test
    fun restoreReplacesTheWholeLayout() = runBlocking {
        // A pre-existing layout the restore must fully replace.
        assertTrue(repo.addToWorkspace(app("qa.old")))
        assertTrue(repo.addToDock(app("qa.old.dock")))
        val before = repo.exportLayout()
        assertTrue(before.pins.isNotEmpty())

        val incoming = snapshot(pins = listOf(PinSnapshot("qa.new", "qa.new.Main", 0, 6, null, null, null, null)))
        repo.applyRestore(incoming) { true }

        val exported = repo.exportLayout()
        assertEquals(incoming, exported)
        assertTrue("The previous pins are gone", exported.pins.none { it.packageName.startsWith("qa.old") })
        assertTrue("The previous dock pins are gone", exported.dock.none { it.packageName.startsWith("qa.old") })
    }

    @Test
    fun restorePreviewListsUnavailablePackages() = runBlocking {
        val preview = repo.restorePreview(
            snapshot(
                dock = listOf(
                    DockPinSnapshot("qa.here", "qa.here.Main", null, null, null),
                    DockPinSnapshot("qa.gone", "qa.gone.Main", null, null, null),
                ),
                pins = listOf(PinSnapshot("qa.absent", "qa.absent.Main", 0, 1, null, null, null, null)),
            ),
        ) { it != "qa.gone" && it != "qa.absent" }
        assertEquals(listOf("qa.absent", "qa.gone"), preview.unavailablePackages)
        assertEquals(2, preview.dockPinCount)
    }

    @Test
    fun previewAndApplyRejectADockOverDestinationCapacity() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val spaciousPrefs = context.getSharedPreferences(
            "layout-transfer-capacity-big-${java.util.UUID.randomUUID()}",
            Context.MODE_PRIVATE,
        )
        // A destination whose dock holds 16 slots accepts the 9-pin file.
        spaciousPrefs.edit().putInt("dock_capacity", 8).commit()
        val spacious = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), spaciousPrefs)
        spacious.applyRestore(
            snapshot(
                dock = List(9) { DockPinSnapshot("qa.over$it", "qa.over$it.Main", null, null, null) },
                pins = emptyList(),
            ),
        ) { true }
        // A destination whose dock holds 8 slots refuses the 9-pin file wholesale.
        val restrictedPrefs = context.getSharedPreferences(
            "layout-transfer-capacity-small-${java.util.UUID.randomUUID()}",
            Context.MODE_PRIVATE,
        )
        restrictedPrefs.edit().putInt("dock_capacity", 4).commit()
        val restricted = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), restrictedPrefs)
        try {
            restricted.restorePreview(spacious.exportLayout()) { true }
            throw AssertionError("A 9-pin dock must not preview on a capacity-4 dock")
        } catch (expected: LayoutSnapshotException) {
            assertTrue(expected.message!!.contains("dock capacity"))
        }
    }

    @Test
    fun restoredPinsSurviveCleanupUntilTheirPackageArrives() = runBlocking {
        val installed = mutableSetOf("qa.present")
        repo.applyRestore(
            snapshot(
                dock = listOf(DockPinSnapshot("qa.present", "qa.present.Main", null, null, null)),
                pins = listOf(PinSnapshot("qa.pending", "qa.pending.Main", 0, 3, null, null, null, null)),
            ),
        ) { it in installed }
        // The absent package's pin survives the first cleanup, the present one stays.
        repo.pruneUninstalled(emptySet()) { pkg, _ -> pkg in installed }
        var exported = repo.exportLayout()
        assertTrue(exported.pins.any { it.packageName == "qa.pending" })
        assertTrue(exported.dock.any { it.packageName == "qa.present" })

        // The package arrives: the protection ends and the pin follows normal rules.
        installed.add("qa.pending")
        repo.pruneUninstalled(emptySet()) { pkg, _ -> pkg in installed }
        exported = repo.exportLayout()
        assertTrue(exported.pins.any { it.packageName == "qa.pending" })

        // A non-launchable installed package prunes like any other once unprotected.
        repo.pruneUninstalled(emptySet()) { _, _ -> false }
        exported = repo.exportLayout()
        assertTrue(exported.pins.none { it.packageName == "qa.pending" })
    }

    private fun app(pkg: String) = LauncherApp(AppKey(pkg, "$pkg.Activity"), pkg)
}
