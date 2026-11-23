package dev.vladon.vesna.launcher.workspace

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.data.FolderEntity
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemDao
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.UUID

/**
 * Regression for VESNA-BUG-007: the same app key may be a member of several
 * folders (a state the UI can produce by re-pinning an app and filing it again),
 * so "remove from folder" must target the membership of the folder the user
 * tapped, not the first matching row across all folders. Removing the tapped
 * member from the selected folder must keep every other folder's placements
 * exactly equal, return the tapped member to the first free grid cell, and
 * dissolve only the selected folder, returning its remaining member to the
 * folder's cell. Runs against an isolated in-memory Room database; the
 * production database is never touched.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryFolderRemovalTest {

    private lateinit var db: LauncherDatabase
    private lateinit var dao: WorkspaceItemDao
    private lateinit var prefs: SharedPreferences
    private lateinit var repo: WorkspaceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .build()
        prefs = context.getSharedPreferences(
            "folder-removal-qa-${UUID.randomUUID()}",
            Context.MODE_PRIVATE,
        )
        dao = db.workspaceItemDao()
        repo = WorkspaceRepository(dao, db, db.folderDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Seeds a WORKSPACE folder member; [cellIndex] is its stored placement. */
    private suspend fun seedMember(pkg: String, folderId: Long, cellIndex: Int): Long =
        dao.insert(
            WorkspaceItemEntity(
                packageName = pkg,
                className = "$pkg.Activity",
                container = "WORKSPACE",
                cellIndex = cellIndex,
                folderId = folderId,
            ),
        )

    /**
     * Both folders hold two members and contain the same app: folder A has the
     * duplicate plus calendar, folder B the duplicate plus camera. Removing the
     * duplicate from the selected folder B must free only B's duplicate row, send
     * B's other member back to B's cell when B dissolves, and leave folder A with
     * both of its placements untouched. With the legacy lookup that ignores the
     * folder scope, the first matching membership wins — A's duplicate row is
     * seeded first, so the pre-fix behavior moved A's row, sent A's other member
     * home and dissolved folder A instead of B.
     */
    @Test
    fun removalFromSelectedFolderMovesOnlyItsMemberAndDissolvesOnlyIt() = runBlocking {
        val folderA = db.folderDao().insert(FolderEntity(title = "Alpha", cellIndex = 10))
        val folderB = db.folderDao().insert(FolderEntity(title = "Beta", cellIndex = 11))
        // Folder A's duplicate is seeded FIRST so the legacy cross-folder
        // first-match lookup picks it instead of the tapped folder B's row.
        seedMember("com.google.android.deskclock", folderA, cellIndex = 1)
        seedMember("com.google.android.calendar", folderA, cellIndex = 2)
        seedMember("com.google.android.deskclock", folderB, cellIndex = 3)
        seedMember("com.android.camera2", folderB, cellIndex = 4)
        val rowsBefore = dao.getAll()
        val rowADup = rowsBefore.single { it.folderId == folderA && it.packageName == "com.google.android.deskclock" }
        val rowACal = rowsBefore.single { it.folderId == folderA && it.packageName == "com.google.android.calendar" }
        val rowBDup = rowsBefore.single { it.folderId == folderB && it.packageName == "com.google.android.deskclock" }
        val rowBCam = rowsBefore.single { it.folderId == folderB && it.packageName == "com.android.camera2" }

        assertEquals(FolderRemovalResult.REMOVED, repo.removeFromFolder(rowBDup.id, folderB))

        val rowsAfter = dao.getAll()
        assertEquals(
            "VESNA-BUG-007: removing B's member must keep A's duplicate membership pinned",
            folderA,
            rowsAfter.single { it.id == rowADup.id }.folderId,
        )
        assertEquals("folder A keeps its first placement", rowADup, rowsAfter.single { it.id == rowADup.id })
        assertEquals("folder A keeps its second placement", rowACal, rowsAfter.single { it.id == rowACal.id })
        assertNotNull("folder A survives removing folder B's member", db.folderDao().getById(folderA))

        val freed = rowsAfter.single { it.id == rowBDup.id }
        assertEquals("the tapped member returns to the workspace", "WORKSPACE", freed.container)
        assertNull("the tapped member leaves folder B", freed.folderId)
        assertEquals("the tapped member lands in the first free grid cell", 0, freed.cellIndex)

        assertNull("folder B dissolves when left with one member", db.folderDao().getById(folderB))
        val returned = rowsAfter.single { it.id == rowBCam.id }
        assertEquals("B's remaining member returns to the workspace", "WORKSPACE", returned.container)
        assertNull(returned.folderId)
        assertEquals("B's remaining member returns to B's cell", 11, returned.cellIndex)
    }

    @Test
    fun removingTheLaterCopyKeepsTheEarlierCopyAndIgnoresAStaleAction() = runBlocking {
        val folder = FolderEntity(title = "Duplicates", cellIndex = 10)
        val folderId = db.folderDao().insert(folder)
        val firstCopy = seedMember("com.google.android.deskclock", folderId, cellIndex = 1)
        seedMember("com.android.camera2", folderId, cellIndex = 2)
        val selectedCopy = seedMember("com.google.android.deskclock", folderId, cellIndex = 3)
        val before = dao.getAll().associateBy { it.id }

        assertEquals(FolderRemovalResult.REMOVED, repo.removeFromFolder(selectedCopy, folderId))
        assertEquals(FolderRemovalResult.UNAVAILABLE, repo.removeFromFolder(selectedCopy, folderId))

        val after = dao.getAll().associateBy { it.id }
        assertEquals("The earlier copy stays in the folder", before.getValue(firstCopy), after.getValue(firstCopy))
        assertEquals(
            before + (selectedCopy to before.getValue(selectedCopy).copy(
                cellIndex = 0, folderId = null, pageId = DEFAULT_WORKSPACE_PAGE_ID,
            )),
            after,
        )
        assertEquals(folder.copy(id = folderId), db.folderDao().getById(folderId))
    }

    /**
     * A folder action cannot move a pin that belongs to a different folder.
     */
    @Test
    fun removalFromWrongFolderReturnsFalseWithoutChanges() = runBlocking {
        val folderA = db.folderDao().insert(FolderEntity(title = "Alpha", cellIndex = 10))
        val folderB = db.folderDao().insert(FolderEntity(title = "Beta", cellIndex = 11))
        val pinId = seedMember("com.google.android.deskclock", folderA, cellIndex = 1)
        seedMember("com.google.android.calendar", folderA, cellIndex = 2)
        seedMember("com.google.android.deskclock", folderB, cellIndex = 1)
        seedMember("com.android.camera2", folderB, cellIndex = 2)
        val rowsBefore = dao.getAll()
        val foldersBefore = db.folderDao().getAll()

        assertEquals(FolderRemovalResult.UNAVAILABLE, repo.removeFromFolder(pinId, folderB))

        assertEquals("no row changed", rowsBefore, dao.getAll())
        assertEquals(foldersBefore, db.folderDao().getAll())
    }

    @Test
    fun fullFolderPagePreservesEveryPlacementAndBothActionsWorkAfterMakingRoom() = runBlocking {
        val pageId = requireNotNull(repo.createPage())
        val folder = FolderEntity(title = "Apps", cellIndex = 10, pageId = pageId)
        val folderId = db.folderDao().insert(folder)
        val first = seedMember("qa.first", folderId, cellIndex = 0)
        val second = seedMember("qa.second", folderId, cellIndex = 1)
        val third = seedMember("qa.third", folderId, cellIndex = 2)
        for (cell in 0 until GridCanvas.DEFAULT.capacity) {
            if (cell == folder.cellIndex) continue
            dao.insert(
                WorkspaceItemEntity(
                    packageName = "qa.other", className = "Main", container = "WORKSPACE",
                    cellIndex = cell, pageId = pageId,
                ),
            )
        }
        val before = dao.getAll().associateBy { it.id }

        assertEquals(FolderRemovalResult.NO_ROOM, repo.removeFromFolder(third, folderId))
        assertEquals(FolderRemovalResult.NO_ROOM, repo.removeFolder(folderId))
        assertEquals(before, dao.getAll().associateBy { it.id })
        assertEquals(folder.copy(id = folderId), db.folderDao().getById(folderId))

        val freedForMember = before.values.single { it.pageId == pageId && it.cellIndex == 4 }
        dao.deleteById(freedForMember.id)
        assertEquals(FolderRemovalResult.REMOVED, repo.removeFromFolder(third, folderId))
        val movedMember = before.getValue(third).copy(cellIndex = 4, folderId = null, pageId = pageId)
        assertEquals(before - freedForMember.id + (third to movedMember), dao.getAll().associateBy { it.id })

        val freedForFolder = before.values.single { it.pageId == pageId && it.cellIndex == 6 }
        dao.deleteById(freedForFolder.id)
        assertEquals(FolderRemovalResult.REMOVED, repo.removeFolder(folderId))
        assertEquals(
            before - freedForMember.id - freedForFolder.id + mapOf(
                first to before.getValue(first).copy(cellIndex = 6, folderId = null, pageId = pageId),
                second to before.getValue(second).copy(cellIndex = 10, folderId = null, pageId = pageId),
                third to movedMember,
            ),
            dao.getAll().associateBy { it.id },
        )
        assertNull(db.folderDao().getById(folderId))
        assertEquals(FolderRemovalResult.UNAVAILABLE, repo.removeFolder(folderId))
    }

    @Test
    fun fullTwoByTwoPageCanUnpinAndRestoreExactFolderWithoutMovingOtherPins() = runBlocking {
        assertEquals(GridResizeResult.APPLIED, repo.resizeGrid(GridCanvas(columns = 2, rows = 2)))
        val pageId = requireNotNull(repo.createPage())
        val folders = db.folderDao()
        val folder = FolderEntity(title = "Tools", cellIndex = 3, pageId = pageId)
        val folderId = folders.insert(folder)
        val target = Folder(folderId, folder.title, folder.cellIndex, pageId)
        for (cell in 0..2) {
            dao.insert(
                WorkspaceItemEntity(
                    packageName = "qa.same", className = "Main", container = "WORKSPACE", cellIndex = cell, pageId = pageId,
                ),
            )
        }
        dao.insert(WorkspaceItemEntity(packageName = "qa.same", className = "Main", container = "DOCK", cellIndex = 0))
        seedMember("qa.same", folderId, cellIndex = 2)
        seedMember("qa.other", folderId, cellIndex = 5)
        dao.insert(
            WorkspaceItemEntity(
                packageName = "qa.actions", className = "Main", container = "WORKSPACE",
                cellIndex = 9, folderId = folderId, shortcutId = "compose",
                customLabel = "My action", shortcutLabel = "Compose",
            ),
        )
        val original = dao.getAll().sortedBy { it.id }
        val members = dao.folderMembersInOrder(folderId)

        assertEquals(FolderRemovalResult.NO_ROOM, repo.removeFolder(folderId))
        assertNull(repo.unpinFolder(target.copy(cellIndex = 2)))
        WorkspacePreferences(prefs).layoutLocked = true
        assertNull(repo.unpinFolder(target))
        assertEquals(original, dao.getAll().sortedBy { it.id })
        WorkspacePreferences(prefs).layoutLocked = false

        val removed = requireNotNull(repo.unpinFolder(target))
        assertNull(folders.getById(folderId))
        val remaining = original.filterNot { it.folderId == folderId }
        assertEquals(remaining, dao.getAll().sortedBy { it.id })
        assertEquals(emptySet<String>(), repo.shortcutIds("qa.actions", 0))
        assertEquals(listOf(DEFAULT_WORKSPACE_PAGE_ID, pageId), repo.savedLayout.first().pageIds)

        // Other insertions during the Undo window must not collide with the removed IDs.
        val addedFolderId = folders.insert(FolderEntity(title = "Later", cellIndex = 1))
        val addedPinId = seedMember("qa.later", addedFolderId, cellIndex = 0)
        val addedPin = dao.getAll().single { it.id == addedPinId }
        WorkspacePreferences(prefs).layoutLocked = true
        assertEquals(FolderRestoreResult.LOCKED, repo.restoreFolder(removed))
        assertEquals(remaining + addedPin, dao.getAll().sortedBy { it.id })
        WorkspacePreferences(prefs).layoutLocked = false

        assertEquals(FolderRestoreResult.RESTORED, repo.restoreFolder(removed))
        assertEquals(folder.copy(id = folderId), folders.getById(folderId))
        assertEquals(members, dao.folderMembersInOrder(folderId))
        assertEquals(original + addedPin, dao.getAll().sortedBy { it.id })
        assertEquals(setOf("compose"), repo.shortcutIds("qa.actions", 0))
    }

    @Test
    fun undoCannotOverwriteAnOccupiedFolderCell() = runBlocking {
        val folders = db.folderDao()
        val folderId = folders.insert(FolderEntity(title = "Tools", cellIndex = 4))
        val target = Folder(folderId, "Tools", 4, DEFAULT_WORKSPACE_PAGE_ID)
        seedMember("qa.one", folderId, cellIndex = 0)
        seedMember("qa.two", folderId, cellIndex = 1)
        val removed = requireNotNull(repo.unpinFolder(target))
        dao.insert(WorkspaceItemEntity(packageName = "qa.replacement", className = "Main", container = "WORKSPACE", cellIndex = 4))
        val remaining = dao.getAll()

        assertEquals(FolderRestoreResult.NO_ROOM, repo.restoreFolder(removed))
        assertEquals(remaining, dao.getAll())
        assertNull(folders.getById(folderId))
    }

    /** VESNA-BUG-011: abort folder deletion and verify both memberships roll back. */
    @Test
    fun failedFolderDissolveRollsBackTheWholeRemovalAtomically() = runBlocking {
        val folderB = db.folderDao().insert(FolderEntity(title = "Beta", cellIndex = 10))
        val pinId = seedMember("com.google.android.deskclock", folderB, cellIndex = 1)
        seedMember("com.google.android.calendar", folderB, cellIndex = 2)
        val rowsBefore = dao.getAll()
        val foldersBefore = db.folderDao().getAll()

        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER vesna_bug011_failpoint " +
                "BEFORE DELETE ON folders WHEN OLD.id = $folderB " +
                "BEGIN SELECT RAISE(ABORT, 'VESNA-BUG-011 failpoint: sparse folder deletion'); END",
        )

        val thrown: SQLiteConstraintException = try {
            repo.removeFromFolder(pinId, folderB)
            error("VESNA-BUG-011: the aborted folder deletion must surface to the caller")
        } catch (expected: SQLiteConstraintException) {
            expected
        }
        assertTrue(
            "VESNA-BUG-011: the failure must come from the installed failpoint trigger, got: $thrown",
            thrown.message.orEmpty().contains("VESNA-BUG-011 failpoint"),
        )

        assertEquals(
            "VESNA-BUG-011: the rolled-back removal must leave every placement untouched",
            rowsBefore,
            dao.getAll(),
        )
        assertEquals(
            "VESNA-BUG-011: the fixture folder must survive the failed dissolution",
            foldersBefore,
            db.folderDao().getAll(),
        )
    }

    /**
     * An exact drop lands on the cell the user chose, never on the first free
     * cell the "remove from folder" menu path would pick. With cells 0-4 taken,
     * the first vacancy is 5, but the drop names 7: only 7 may change, the row
     * keeps its stable ID and cleared membership, and a folder left with two
     * members survives untouched.
     */
    @Test
    fun memberDropLandsOnTheChosenNonFirstVacancyAndKeepsTheFolder() = runBlocking {
        val folder = FolderEntity(title = "Apps", cellIndex = 10)
        val folderId = db.folderDao().insert(folder)
        seedMember("qa.first", folderId, cellIndex = 0)
        val moving = seedMember("qa.moving", folderId, cellIndex = 1)
        seedMember("qa.third", folderId, cellIndex = 2)
        for (cell in 0..4) {
            dao.insert(
                WorkspaceItemEntity(
                    packageName = "qa.top", className = "Main", container = "WORKSPACE", cellIndex = cell,
                ),
            )
        }
        val before = dao.getAll().associateBy { it.id }
        val foldersBefore = db.folderDao().getAll()

        assertTrue(repo.moveFolderMemberToCell(moving, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 7))

        val after = dao.getAll().associateBy { it.id }
        assertEquals(
            "the chosen cell wins over the first vacancy (5), with the row ID kept and membership cleared",
            before.getValue(moving).copy(cellIndex = 7, folderId = null, pageId = DEFAULT_WORKSPACE_PAGE_ID),
            after.getValue(moving),
        )
        assertEquals("every other placement is untouched", before - moving + (moving to after.getValue(moving)), after)
        assertEquals("the folder survives with two members left", foldersBefore, db.folderDao().getAll())
    }

    /**
     * Dropping one of exactly two members dissolves the folder: the dropped row
     * lands on its chosen cell, and the remaining member keeps its exact row ID,
     * profile and both labels while taking over the folder's cell.
     */
    @Test
    fun movingOneOfTwoMembersDissolvesTheFolderIntoItsExactCellWithLabelsKept() = runBlocking {
        val folderCell = 11
        val folderId = db.folderDao().insert(FolderEntity(title = "Beta", cellIndex = folderCell))
        val leaving = dao.insert(
            WorkspaceItemEntity(
                packageName = "qa.leaving", className = "qa.leaving.Activity", container = "WORKSPACE",
                cellIndex = 0, folderId = folderId,
            ),
        )
        val remaining = dao.insert(
            WorkspaceItemEntity(
                packageName = "qa.remaining", className = "qa.remaining.Activity", container = "WORKSPACE",
                cellIndex = 1, folderId = folderId, userSerial = 7,
                customLabel = "Kept", shortcutLabel = "Publisher",
            ),
        )

        assertTrue(repo.moveFolderMemberToCell(leaving, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 5))

        assertNull("the folder dissolves once one member is left", db.folderDao().getById(folderId))
        val home = dao.getAll().associateBy { it.id }
        assertEquals(
            "the dropped member sits on the chosen cell with membership cleared",
            WorkspaceItemEntity(
                id = leaving, packageName = "qa.leaving", className = "qa.leaving.Activity",
                container = "WORKSPACE", cellIndex = 5, folderId = null,
                pageId = DEFAULT_WORKSPACE_PAGE_ID,
            ),
            home.getValue(leaving),
        )
        assertEquals(
            "the remaining member keeps its exact row ID, profile and labels at the folder cell",
            WorkspaceItemEntity(
                id = remaining, packageName = "qa.remaining", className = "qa.remaining.Activity",
                container = "WORKSPACE", cellIndex = folderCell, userSerial = 7, folderId = null,
                pageId = DEFAULT_WORKSPACE_PAGE_ID, customLabel = "Kept", shortcutLabel = "Publisher",
            ),
            home.getValue(remaining),
        )
    }

    /**
     * Every rejected drop — occupied cell, the folder's reserved cell, an
     * out-of-range cell, another page, an unknown folder — and a locked layout
     * change nothing at all: no placement, no membership, no folder row.
     */
    @Test
    fun invalidOrLockedDropsPreserveEveryPlacementAndTheFolder() = runBlocking {
        val folderCell = 10
        val folderId = db.folderDao().insert(FolderEntity(title = "Apps", cellIndex = folderCell))
        val member = seedMember("qa.member", folderId, cellIndex = 0)
        seedMember("qa.other", folderId, cellIndex = 1)
        dao.insert(
            WorkspaceItemEntity(packageName = "qa.top", className = "Main", container = "WORKSPACE", cellIndex = 3),
        )
        val otherPage = requireNotNull(repo.createPage())
        val before = dao.getAll().associateBy { it.id }
        val foldersBefore = db.folderDao().getAll()

        assertFalse("an occupied cell is rejected", repo.moveFolderMemberToCell(member, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 3))
        assertFalse("the folder cell stays reserved", repo.moveFolderMemberToCell(member, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = folderCell))
        assertFalse("an out-of-range cell is rejected", repo.moveFolderMemberToCell(member, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = GridCanvas.DEFAULT.capacity))
        assertFalse("a drop naming another page is rejected", repo.moveFolderMemberToCell(member, folderId, otherPage, cell = 5))
        assertFalse("a stale folder ID is rejected", repo.moveFolderMemberToCell(member, folderId + 100, DEFAULT_WORKSPACE_PAGE_ID, cell = 5))

        assertEquals("every failed drop keeps the layout exactly", before, dao.getAll().associateBy { it.id })
        assertEquals(foldersBefore, db.folderDao().getAll())

        WorkspacePreferences(prefs).layoutLocked = true
        assertFalse("a locked layout rejects the drop", repo.moveFolderMemberToCell(member, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 5))
        assertEquals("the lock changed nothing either", before, dao.getAll().associateBy { it.id })
        assertEquals(foldersBefore, db.folderDao().getAll())
    }

    @Test
    fun reorderingInsertsAcrossTiedAndSparseRanksWithoutChangingOtherFolderRows() = runBlocking {
        val folders = db.folderDao()
        val folder = folders.insert(FolderEntity(title = "Editing", cellIndex = 8))
        val other = folders.insert(FolderEntity(title = "Unchanged", cellIndex = 9))
        seedMember("qa.copy", other, cellIndex = 3)
        seedMember("qa.sibling", other, cellIndex = 10)
        val first = seedMember("qa.first", folder, cellIndex = 5)
        val second = seedMember("qa.second", folder, cellIndex = 5)
        val third = dao.insert(WorkspaceItemEntity(
            packageName = "qa.copy", className = "qa.copy.Activity", container = "WORKSPACE",
            cellIndex = 20, userSerial = 7, folderId = folder,
            shortcutId = "open", customLabel = "Custom", shortcutLabel = "Published",
        ))
        val before = dao.getAll().associateBy { it.id }
        val originalFolders = folders.getAll()
        assertTrue(repo.reorderFolderMember(folder, DEFAULT_WORKSPACE_PAGE_ID, third, first))
        val reordered = before + mapOf(
            third to before.getValue(third).copy(cellIndex = 0),
            first to before.getValue(first).copy(cellIndex = 1),
            second to before.getValue(second).copy(cellIndex = 2),
        )
        assertEquals(reordered, dao.getAll().associateBy { it.id })
        assertTrue(repo.reorderFolderMember(folder, DEFAULT_WORKSPACE_PAGE_ID, third, second))
        val restored = before + mapOf(
            first to before.getValue(first).copy(cellIndex = 0),
            second to before.getValue(second).copy(cellIndex = 1),
            third to before.getValue(third).copy(cellIndex = 2),
        )
        assertEquals(restored, dao.getAll().associateBy { it.id })
        assertEquals(originalFolders, folders.getAll())
    }

    @Test
    fun lockedInvalidAndCrossFolderReordersLeaveEveryRowUnchanged() = runBlocking {
        val folders = db.folderDao()
        val folder = folders.insert(FolderEntity(title = "Selected", cellIndex = 8))
        val other = folders.insert(FolderEntity(title = "Other", cellIndex = 9))
        val first = seedMember("qa.first", folder, cellIndex = 10)
        val second = seedMember("qa.second", folder, cellIndex = 12)
        val foreign = seedMember("qa.second", other, cellIndex = 12)
        val anotherPage = requireNotNull(repo.createPage())
        val before = dao.getAll().associateBy { it.id }
        val originalFolders = folders.getAll()

        assertFalse(repo.reorderFolderMember(folder, DEFAULT_WORKSPACE_PAGE_ID, first, foreign))
        assertFalse(repo.reorderFolderMember(folder, DEFAULT_WORKSPACE_PAGE_ID, foreign, second))
        assertFalse(repo.reorderFolderMember(folder, anotherPage, first, second))
        assertFalse(repo.reorderFolderMember(folder, DEFAULT_WORKSPACE_PAGE_ID, first, first))
        WorkspacePreferences(prefs).layoutLocked = true
        assertFalse(repo.reorderFolderMember(folder, DEFAULT_WORKSPACE_PAGE_ID, first, second))

        assertEquals(before, dao.getAll().associateBy { it.id })
        assertEquals(originalFolders, folders.getAll())
    }

    @Test
    fun sortingUsesTheVisibleLabelsAndKeepsUnavailableAndEqualLabelsInOrder() = runBlocking {
        val folders = db.folderDao()
        val folder = folders.insert(FolderEntity(title = "Sort", cellIndex = 8))
        val other = folders.insert(FolderEntity(title = "Other", cellIndex = 9))
        seedMember("qa.same", other, cellIndex = 3)
        seedMember("qa.other", other, cellIndex = 4)
        val aRing = dao.insert(WorkspaceItemEntity(
            packageName = "qa.same", className = "Main", container = "WORKSPACE", cellIndex = 0,
            userSerial = 7, folderId = folder, shortcutId = "open", customLabel = "Åland", shortcutLabel = "Action",
        ))
        val unavailable = seedMember("qa.unavailable", folder, cellIndex = 1)
        val zulu = seedMember("qa.earlier.package", folder, cellIndex = 2)
        val alpha = seedMember("qa.later.package", folder, cellIndex = 3)
        val duplicate = seedMember("qa.later.package", folder, cellIndex = 4)
        val before = dao.getAll().associateBy { it.id }
        val originalFolders = folders.getAll()
        val labels = mapOf(aRing to "Åland", zulu to "Zulu", alpha to "alpha", duplicate to "ALPHA")

        assertTrue(repo.sortFolderMembers(folder, DEFAULT_WORKSPACE_PAGE_ID, labels, Locale.forLanguageTag("sv-SE")))

        val expected = before + listOf(alpha, unavailable, duplicate, zulu, aRing)
            .mapIndexed { rank, id -> id to before.getValue(id).copy(cellIndex = rank) }
        assertEquals(expected, dao.getAll().associateBy { it.id })
        assertEquals(originalFolders, folders.getAll())
    }

    @Test
    fun staleOrLockedFolderSortDoesNotChangeMembershipOrRanks() = runBlocking {
        val folders = db.folderDao()
        val folder = folders.insert(FolderEntity(title = "Selected", cellIndex = 8))
        val other = folders.insert(FolderEntity(title = "Other", cellIndex = 9))
        val first = seedMember("qa.first", folder, cellIndex = 5)
        val second = seedMember("qa.second", folder, cellIndex = 8)
        val foreign = seedMember("qa.foreign", other, cellIndex = 0)
        val page = requireNotNull(repo.createPage())
        val before = dao.getAll().associateBy { it.id }
        val originalFolders = folders.getAll()
        val labels = mapOf(first to "Zulu", second to "Alpha")

        assertFalse(repo.sortFolderMembers(folder, page, labels, Locale.US))
        assertFalse(repo.sortFolderMembers(folder, DEFAULT_WORKSPACE_PAGE_ID, mapOf(first to "Zulu", foreign to "Alpha"), Locale.US))
        WorkspacePreferences(prefs).layoutLocked = true
        assertFalse(repo.sortFolderMembers(folder, DEFAULT_WORKSPACE_PAGE_ID, labels, Locale.US))

        assertEquals(before, dao.getAll().associateBy { it.id })
        assertEquals(originalFolders, folders.getAll())
    }

    /**
     * A member dropped at an exact cell of an adjacent page (edge preview)
     * takes over that cell with its stable row ID and membership cleared while
     * the folder — left with one member — dissolves in the same transaction:
     * the remaining member keeps its exact row ID, profile and both labels and
     * returns to the folder cell on the folder's own (source) page, not on the
     * destination page. Nothing else moves.
     */
    @Test
    fun crossPageMemberDropLandsOnExactDestinationAndDissolvesSparseFolderOnItsOwnPage() = runBlocking {
        val folderCell = 11
        val folderId = db.folderDao().insert(FolderEntity(title = "Beta", cellIndex = folderCell))
        val leaving = dao.insert(
            WorkspaceItemEntity(
                packageName = "qa.leaving", className = "qa.leaving.Activity", container = "WORKSPACE",
                cellIndex = 0, folderId = folderId,
            ),
        )
        val remaining = dao.insert(
            WorkspaceItemEntity(
                packageName = "qa.remaining", className = "qa.remaining.Activity", container = "WORKSPACE",
                cellIndex = 1, folderId = folderId, userSerial = 7,
                customLabel = "Kept", shortcutLabel = "Publisher",
            ),
        )
        val second = requireNotNull(repo.createPage())
        dao.insert(
            WorkspaceItemEntity(
                packageName = "qa.resident", className = "qa.resident.Activity", container = "WORKSPACE",
                cellIndex = 0, pageId = second,
            ),
        )
        val before = dao.getAll().associateBy { it.id }
        val foldersBefore = db.folderDao().getAll()

        assertTrue(
            repo.moveFolderMemberToCell(leaving, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 7, destinationPageId = second),
        )

        assertNull("the folder dissolves once one member is left", db.folderDao().getById(folderId))
        val after = dao.getAll().associateBy { it.id }
        assertEquals(
            "the dropped member sits at the exact destination cell on the destination page, ID and labels kept",
            before.getValue(leaving).copy(cellIndex = 7, folderId = null, pageId = second),
            after.getValue(leaving),
        )
        assertEquals(
            "the remaining member returns to the folder cell on the folder's own source page",
            before.getValue(remaining).copy(cellIndex = folderCell, folderId = null, pageId = DEFAULT_WORKSPACE_PAGE_ID),
            after.getValue(remaining),
        )
        assertEquals(
            "every other row is untouched",
            before - leaving - remaining + mapOf(
                leaving to after.getValue(leaving),
                remaining to after.getValue(remaining),
            ),
            after,
        )
        assertEquals("only the folder row disappears", emptyList<FolderEntity>(), db.folderDao().getAll())
    }

    /**
     * Every refused cross-page member drop — a pin that is not a member, a
     * call naming a page the folder no longer sits on, a missing destination
     * page, an occupied or out-of-range destination cell, and a locked layout
     * — changes nothing at all: no placement, no membership, no folder row.
     */
    @Test
    fun crossPageMemberDropRejectsStaleSourcesOccupiedCellsAndLockWithoutChanges() = runBlocking {
        val folderId = db.folderDao().insert(FolderEntity(title = "Apps", cellIndex = 10))
        val member = seedMember("qa.member", folderId, cellIndex = 0)
        seedMember("qa.other", folderId, cellIndex = 1)
        val second = requireNotNull(repo.createPage())
        dao.insert(
            WorkspaceItemEntity(
                packageName = "qa.resident", className = "Main", container = "WORKSPACE",
                cellIndex = 0, pageId = second,
            ),
        )
        val foreign = dao.insert(
            WorkspaceItemEntity(packageName = "qa.top", className = "Main", container = "WORKSPACE", cellIndex = 3),
        )
        val before = dao.getAll().associateBy { it.id }
        val foldersBefore = db.folderDao().getAll()

        assertFalse(
            "a pin that is not a member of the folder is rejected",
            repo.moveFolderMemberToCell(foreign, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 7, destinationPageId = second),
        )
        assertFalse(
            "a call naming a page the folder is not on is rejected",
            repo.moveFolderMemberToCell(member, folderId, second, cell = 7, destinationPageId = second),
        )
        assertFalse(
            "a missing destination page is rejected",
            repo.moveFolderMemberToCell(member, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 7, destinationPageId = 424242L),
        )
        assertFalse(
            "an occupied destination cell is rejected",
            repo.moveFolderMemberToCell(member, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 0, destinationPageId = second),
        )
        assertFalse(
            "an out-of-range destination cell is rejected",
            repo.moveFolderMemberToCell(member, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = GridCanvas.DEFAULT.capacity, destinationPageId = second),
        )

        assertEquals("every rejected drop keeps the layout exactly", before, dao.getAll().associateBy { it.id })
        assertEquals(foldersBefore, db.folderDao().getAll())

        WorkspacePreferences(prefs).layoutLocked = true
        assertFalse(
            "a locked layout rejects the drop",
            repo.moveFolderMemberToCell(member, folderId, DEFAULT_WORKSPACE_PAGE_ID, cell = 7, destinationPageId = second),
        )
        assertEquals("the lock changed nothing either", before, dao.getAll().associateBy { it.id })
        assertEquals(foldersBefore, db.folderDao().getAll())
    }
}
