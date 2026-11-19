# Requirements Traceability

**Research source:** branch `vladon/vesna-research` at `ee15ed591c946cd3dc1fb623657c518e8a592424`
(pinned paths in [RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#pinned-research-paths);
approval status in [RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#approval-status--read-this-first)).
**Vesna baseline:** commit `888f557`, app 0.4.1 (see [PROJECT_STATUS.md](PROJECT_STATUS.md)).

> **What this matrix is.** Every row below is a **traceability pointer**: it
> records which Vesna code and tests correspond to a research requirement ID.
> It is **not** a claim of conformance with the research specification, and
> "Complete" in the research matrix is not repeated here. Approval status is
> tracked in [RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#approval-status--read-this-first);
> verification claims are limited to what the baseline audit actually ran
> ([PROJECT_STATUS.md](PROJECT_STATUS.md#quality-status-at-the-baseline)).

Status vocabulary:

- **Source-verified** — the implementation at the baseline commit implements the
  behavior as described; no automated/on-device test covers it.
- **Test-verified (exact scope named)** — covered by a named passing test suite
  (JVM) or a recorded on-device run; the covered behavior is named exactly.
- **Not implemented** — no corresponding Vesna code.
- **Blocked / decision needed** — work cannot proceed without a research-matrix
  fix or a product/privacy decision.
- "Outside the baseline audit" — the audit did not run it; no broader claim is
  made either way.

## Drawer

Pinned spec: [`specifications/app-drawer.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/app-drawer.md).

| Research ID | Meaning (as recorded in research/review) | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|---|
| `R-DRAWER-100.4` | Alphabetical drawer listing. The **owner-approved QA baseline covers only the order-preservation subset**: the drawer preserves a supplied order and does not re-order while filtering | Production sort lives in `LauncherAppsAppDiscovery.discover()` (`sortBy { label.lowercase(Locale.ROOT) }`); `DrawerView` filters via `AppSearch` and never re-orders | `DrawerSearchTest` verifies an unsorted supplied list and the order of multiple matches. All 8 instrumentation tests passed on the dedicated API 35 AVD (candidate `18dd2d1`). The test covers the drawer with injected input. [VESNA-QA-008, PR #34](https://github.com/vladon/vesnalauncher/pull/34) | **Test-verified (order-preservation subset only)** |
| `R-DRAWER-101.1` (local filtering subset) | Case-insensitive substring search of drawer labels | `AppSearch.filter`, `DrawerView` search field, empty state | Owner-approved QA baseline; `AppSearchTest` (5 JVM tests); `DrawerSearchTest` (mixed-case query, empty state, clear/restore) | **Test-verified (local filtering subset only)** |
| `DRAWER-102` | Back/IME key sequencing with open search | `DrawerView` search field handling | none | **Ambiguous in research** — sequencing not agreed; do not implement against a guess. Resolve in the research matrix first ([ENG-107 in BACKLOG.md](BACKLOG.md#eng-107-research-matrix-fix-ups-research-branch-not-trunk)) |
| `DRAWER-104` | (research matrix claims acceptance complete) | — | Named scenarios absent from research inventory | **Pointer only** — matrix status unsupported; no trunk action |
| `DRAWER-105` | Mistakenly reuses the ID `R-DRAWER-103.1`; its cards surface conflicts with the later `CARDS-100` | — | — | **Blocked** — ID reuse must be repaired in the research matrix; see the cards row |
| `R-DRAWER-103.1` | **Contact opt-in** | — | — | **Independent item** — kept separate from the `DRAWER-105`/`CARDS-100` conflict; no trunk work |

## Home screen and workspace pages

Pinned spec: [`specifications/home-screen.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/home-screen.md).

| Research ID | Meaning | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|---|
| `HOME-104` | **Workspace page management via drag** (not Android HOME registration) | none — single fixed 4×5 page | Named scenario absent from research inventory | **Not implemented**; matrix "complete" unsupported. Research candidate for [ENG-101 in BACKLOG.md](BACKLOG.md#eng-101-workspace-page-identities-and-schema-migration) |
| Workspace placement/ordering | — | `GridCanvas`, `WorkspaceResolver`, `WorkspaceRepository` | `GridCanvasTest` (11), `WorkspaceResolverTest` (6) | **Test-verified (grid/order model)** |

## HOME role and launching

Trunk QA items in this section are **Vesna platform baseline** work; none of
them carries an approved research ID.

| Item | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|
| HOME registration/selection, return-from-app | `HomeActivity` `CATEGORY_HOME` filter (`singleTask`, `clearTaskOnLaunch`, `excludeFromRecents`), `HomeRoleChecker`, `defaultHomeRequestIntent()` | On-device run recorded 2026-11-18: [verification/VESNA-QA-002.md](verification/VESNA-QA-002.md) — offered in system HOME selection, selected as default via system UI, drawer launch of Settings verified in real foreground, HOME return to workspace with drawer closed, previous HOME holder restored. Android 8 (API 26) follow-up appended to the same report as [VESNA-QA-002B](verification/VESNA-QA-002.md#api-26-follow-up-vesna-qa-002b) (2026-11-19, [PR #37](https://github.com/vladon/vesnalauncher/pull/37)): Settings radio chooser selected Vesna and HOME returned from Calculator to Vesna with matching workspace snapshots | **Device-verified** (VESNA-QA-002 run, API 35; QA-002B replay, API 26); at audit time **outside the baseline audit** — see [VESNA-QA-002 in BACKLOG.md](BACKLOG.md#vesna-qa-002-home-and-launch-return-verification-done) |
| HOME gesture (swipe-up drawer open) | `DrawerView` swipe-up / handle open | The baseline audit does not exercise the HOME gesture | **Outside the baseline audit**; research gesture follow-up (`ee15ed5` on research branch) records a separate drawer-open setting — binding values unvalidated in research |

## Dock

Pinned spec: [`specifications/dock.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/dock.md).

| Research ID | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|
| `DOCK-100` core ordering | `DockOrdering`, `DockView`, four slots | `DockOrderingTest` (9 JVM tests) | **Test-verified (ordering model)** |
| Dock capacity with hidden folder members (Vesna platform fix, no research ID) | `WorkspaceRepository.addToDock` counts/appends only top-level entries (`folderId == null`) | [VESNA-QA-003E](verification/VESNA-QA-003E.md) (2026-11-19, API 35): baseline `1f138ab` rejected Add to dock with three visible items; the [PR #26](https://github.com/vladon/vesnalauncher/pull/26) patch accepted it, preserved folder membership, and a four-visible-item dock still rejected; both regression cases passed and the 2-test run is in the record's evidence index | **Device-verified (focused case, API 35)** |
| `DOCK-100` advanced paging, drag-out | partial | none | **Gap** — not implemented and not specified; see [BACKLOG.md](BACKLOG.md) candidates |

## Folders

Pinned spec: [`specifications/folders.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/folders.md).

| Research ID | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|
| `FOLDER-100` core | `Folder` cell, `FolderDialog` open/rename/remove, dissolution below two members; **drag-into-folder**: drops onto a folder cell route through `LauncherController.addToFolder` (two `HomeActivity` paths) | **Exact model scope:** `WorkspaceResolverFolderTest` (7 JVM tests) covers the resolver projection only — member exclusion from top-level cells, folder cell with member count, invalid cell skipped, no override of an app in the target cell, in-folder ordering, stale members dropped, empty without folders. The audit tests covered projection. The focused internal device cases below record later checks | **Test-verified (resolver model only)**; drag-into-folder **source-verified, UI outside the baseline audit** |
| `FOLDER-100`/`FOLDER-101` pagination | none | none | **Gap** — pagination not implemented; scenarios missing in research too ([ENG-107](BACKLOG.md#eng-107-research-matrix-fix-ups-research-branch-not-trunk)) |
| Scoped folder-member removal (Vesna platform fix VESNA-BUG-007, no research ID) | `removeFromFolder` lookup scoped to the selected folder and exact app key | [PR #38](https://github.com/vladon/vesnalauncher/pull/38): UI replay on the reviewed production patch (API 36.1 AVD) removed Clock from the second of two folders and left both rows of the first intact, only the second dissolved; the duplicate-membership Room case failed as expected with the unscoped lookup. Both focused tests passed with the fix; the coordinator reran the integrated candidate `0948b81` — all 10 instrumentation tests passed on the same dedicated API 36.1 AVD | **Device-verified (candidate `0948b81`, API 36.1; UI replay on the reviewed production patch)** |
| Drawer self-drop folder guard (Vesna platform fix VESNA-BUG-008, no research ID) | drawer drop onto the app's own pin no longer creates a one-member folder; two-distinct-pin folder creation unchanged | [PR #39](https://github.com/vladon/vesnalauncher/pull/39): baseline/fix UI replays of a Contacts drawer self-drop on the API 36.1 AVD (one-member folder reproduced on baseline, pin intact and folder table empty with the guard; positive control kept two-pin folder creation); the coordinator independently repeated the baseline failure and ran the integrated candidate `75ca3ed` — all 12 instrumentation tests passed on the dedicated API 26 AVD | **Device-verified (UI on API 36.1; candidate `75ca3ed`, 12 tests on API 26)** |
| Atomic folder creation (Vesna platform fix VESNA-BUG-009, no research ID) | `WorkspaceRepository.createFolderFromDrop` inserts the folder row and both membership updates in one Room transaction; a failed membership write rolls the folder row back, the original placements survive, and no empty folder remains | [PR #42](https://github.com/vladon/vesnalauncher/pull/42): the new in-memory Room regression aborts the second membership write — the baseline APK failed at the expected assertion and left an empty folder; the three focused folder-creation tests passed on the fix on the dedicated API 36.1 AVD, and the coordinator independently repeated the baseline failure and ran all 13 instrumentation tests on candidate `846289f` on the dedicated API 26 AVD | **Device-verified (focused red/green on API 36.1; candidate `846289f`, 13 tests on API 26)** |
| Atomic folder-member removal and dissolution (Vesna platform fix VESNA-BUG-011, no research ID) | `removeFromFolder` commits the member's placement and membership updates and the sparse-folder dissolution in one Room transaction; a failed folder delete rolls the updates back and the original placement, membership, and folder row survive | [PR #45](https://github.com/vladon/vesnalauncher/pull/45) (`19228c6`): a test-only SQLite trigger aborted deletion of a two-member fixture's folder after the membership changes — the baseline left both member rows outside the folder, and the patched transaction preserved their original placement, membership, and folder row; the new case failed at the expected placement assertion on the baseline, all three focused folder-removal tests passed on the API 36.1 emulator, and the coordinator repeated the baseline failure on API 26 and ran all 16 instrumentation tests on candidate `1b5dc2e` on both dedicated API 26 and API 36.1 emulators | **Device-verified (candidate `1b5dc2e`, 16 tests on API 26 and API 36.1)** |

## Widgets

Pinned spec: [`specifications/widgets.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/widgets.md).

| Research ID | Meaning | Vesna anchors | Status |
|---|---|---|---|
| `WIDGET-100` | Placement; provider config-cancel behavior is a research gap | none | **Not implemented**; any widget work must include widget host cleanup design — [ADR-WIDGET-1 in BACKLOG.md](BACKLOG.md#adr-widget-1-widget-host-cleanup) |
| `WIDGET-102` | Removal/undo | none | **Not implemented** |
| `WIDGET-101` | Widget resize | none | **Not implemented** |

## Settings, backup, privacy

Pinned specs: [`specifications/settings.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/settings.md) · [`specifications/backup-restore.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/backup-restore.md).

| Research ID | Meaning | Vesna anchors | Status |
|---|---|---|---|
| `SETT-110` | **Settings search/deep-linking** (not reset) | none | **Not implemented**; no dedicated research scenario either ([ENG-107](BACKLOG.md#eng-107-research-matrix-fix-ups-research-branch-not-trunk)) |
| `BACKUP-101` | Reset (coverage partial in research) | none — trunk has no reset feature | **Gap on both sides**; reset work pairs with [ENG-106 in BACKLOG.md](BACKLOG.md#eng-106-backuprestore-policy) |
| `BACKUP-102` | Backup | `android:allowBackup="true"` in the manifest (platform-managed); no explicit backup/restore feature | **Not implemented (feature)**; platform backup enabled by manifest default. No dedicated research scenario. Scope decision pending — [ADR-BACKUP-1 in BACKLOG.md](BACKLOG.md#adr-backup-1-backup-and-privacy-policy) |
| `CARDS-100` | **Condition-triggered content** (cards). Location/manual-fallback privacy question open; notification indicators are a separate item | none | **Blocked** — product + privacy decision required before any planning |
| `DRAWER-105`/`R-DRAWER-103.1` | See drawer section | — | ID reuse to repair; contact opt-in independent |

## Data and platform behaviors without research IDs

| Vesna behavior | Anchors | Tests / evidence | Status |
|---|---|---|---|
| Room schema evolution 1→2→3 | `LauncherDatabase` (`MIGRATION_1_2`, `MIGRATION_2_3`), schema version 3 | none at the baseline audit; device-verified afterwards by `RoomMigrationQaTest` ([PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`) — v1→2→3 and v2→3 seeded through the production `LauncherDatabase.build`, run on the dedicated API 35 AVD | **Device-verified at API 35** ([VESNA-QA-004 part A](BACKLOG.md#vesna-qa-004-migration-and-restart-verification-done)); outside the baseline audit scope |
| Package install/update/uninstall across profiles | `PackageChangeMonitor`, `userSerial` tagging, stale-item pruning | [VESNA-QA-003A](verification/VESNA-QA-003.md) verified the drawer lifecycle and launcher restart. [VESNA-QA-003B](verification/VESNA-QA-003B.md) verified upgrade retention and folder cleanup, and observed placement loss on disable. [VESNA-QA-003C](verification/VESNA-QA-003C.md) verified profile-scoped removal: a work-only uninstall deleted exactly the work-serial placements while the personal package and pin survived, stable across a launcher restart. [VESNA-QA-003D](verification/VESNA-QA-003D.md) verified simultaneous workspace, dock, and folder fixture removal: all rows left within seconds, the folder dissolved, and a cold relaunch showed no ghosts. All runs used an API 35 AVD. | **Device-verified at API 35, personal and managed profiles.** The disable-time placement retention policy remains an open product decision in [VESNA-QA-003](BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial); no policy is assumed here. |
| Persistence across process death / reboot | Room persistence (intended behavior) | none at the baseline audit; device-verified afterwards by [VESNA-QA-004B](verification/VESNA-QA-004B.md) (2026-11-19, API 35 AVD, tested build `0c7269c`): force-stop relaunch, full guest reboot, no re-seed of an emptied workspace | **Device-verified at API 35** ([VESNA-QA-004 part B](BACKLOG.md#vesna-qa-004-migration-and-restart-verification-done)); outside the baseline audit scope |
| Managed-profile behavior | `LauncherApps` discovery/launch, profile badges | [VESNA-QA-003C](verification/VESNA-QA-003C.md) (2026-11-19, API 35 AVD, shell-provisioned managed profile): work-only fixture discovered in the drawer, drawer-drag pin persisted with the profile's `userSerial`, launching opened the fixture in the work profile via `LauncherApps.startMainActivity`, and the work-only uninstall left the personal copy and pin intact across a launcher restart | **Device-verified at API 35** (VESNA-QA-003C); at audit time **outside the baseline audit** — see [VESNA-QA-003 in BACKLOG.md](BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial) |
| Minimum-OS behavior (declared `minSdk` 26) | whole app on API 26 | [VESNA-QA-005-API26](verification/VESNA-QA-005-API26.md) (2026-11-19, dedicated API 26 AVD): build, HOME discoverability, cold start, workspace/drawer render, fixture drawer round-trip, instrumentation suite 3/3; the observed fifth-row overlap (VESNA-BUG-004) was fixed in [PR #23](https://github.com/vladon/vesnalauncher/pull/23). Follow-up appended to the same report as [VESNA-QA-005B](verification/VESNA-QA-005-API26.md#follow-up-on-the-eight-test-suite) (2026-11-19, [PR #36](https://github.com/vladon/vesnalauncher/pull/36)): all eight instrumentation tests passed on API 26 at build `4c5f0c1` — the original smoke used the initial three-test suite | **Device-verified at API 26 (narrow smoke; full eight-test suite per QA-005B)**; outside the baseline audit scope |
| Android 16 QPR2 (API 36.1) | HOME entry point, launcher surfaces and instrumented data paths | [VESNA-QA-006-API36-1](verification/VESNA-QA-006-API36-1.md) (2026-11-19, dedicated API 36.1 AVD on Windows): instrumentation 5/5 at `332fb03` and 7/7 with the integrated BUG-005 candidate at `d8d6a748`; HOME discovery, cold start, drawer, fixture launch, HOME selection via the system RoleManager dialog, and launch-return verified manually; stable AVD documented after a controlled A/B recreation | **Device-verified at API 36.1 (listed AVD configuration)**; outside the baseline audit scope |
| Android 12 (API 31) | HOME entry point, launcher surfaces and instrumented data paths | [VESNA-QA-010-API31](verification/VESNA-QA-010-API31.md) (2026-11-19, dedicated API 31 AVD on Windows, [PR #43](https://github.com/vladon/vesnalauncher/pull/43)): 12/12 instrumentation tests on the debug `75ca3ed` candidate from [PR #39](https://github.com/vladon/vesnalauncher/pull/39) — the suite as it stood before VESNA-BUG-009/BUG-010; an explicit launch rendered workspace and drawer; the HOME-registration check listed Vesna as a HOME candidate while NexusLauncher stayed resolved; boot fallback and teardown are recorded in the report | **Device-verified at API 31 (recorded build `75ca3ed` and AVD)**; outside the baseline audit scope |
| Top-level unpin preserves hidden folder membership (Vesna platform fix, no research ID) | `WorkspaceItemDao.deleteTopLevelByKey` filters by app key, container, profile, and null `folderId`; package uninstall keeps the package-level `deleteByPackage` path | VESNA-BUG-005: [PR #27](https://github.com/vladon/vesnalauncher/pull/27) (`81cea18`) — dedicated API 35 replay showed the baseline deleting both Clock rows and the fix preserving the folder member while removing the visible pin; both new isolated Room regression cases failed as expected on the baseline; the combined build passed 7/7 on separate API 26 and API 35 emulators (candidate `d8d6a748`; those device runs are scoped to the branch candidate that became PR #27) | **Device-verified (candidate `d8d6a748`, API 26 + API 35)** |
| Debug process-cold launch timing (Android 15/API 35, no research ID) | `HomeActivity` cold start via `am start -W` TotalTime | [VESNA-QA-007-COLD-START](verification/VESNA-QA-007-COLD-START.md) (2026-11-19, [PR #33](https://github.com/vladon/vesnalauncher/pull/33)): debug 0.4.1 at `81cea18` on the shared-host Android 15/API 35 emulator; 10 process-cold samples with the timings in the linked report | **Measured (debug, emulator, process-cold scope)** |
| Drag-and-drop across containers | `LauncherDragLayer`, `WorkspaceView`, `DockView`, `DrawerView` | Dock-to-workspace drag of an app with both a visible dock entry and a dock-origin folder member (VESNA-BUG-006): [PR #31](https://github.com/vladon/vesnalauncher/pull/31) (`65614d0`) — original and corrected behavior reproduced end to end on the dedicated Android 15/API 35 AVD; the new Room instrumentation test failed on baseline `81cea18` at the exact placement assertion and all 8 instrumentation tests passed on candidate `051febb` (serial-pinned adb, same AVD) | **Device-verified (focused dock-to-workspace case, API 35)** |
| Workspace pin move matches the source cell (Vesna platform fix VESNA-BUG-010, no research ID) | `WorkspaceRepository.moveWithinWorkspace` matches the drag's source cell together with the exact app key and top-level placement, so a second pin of the same app can move to an empty cell without displacing the first | [PR #44](https://github.com/vladon/vesnalauncher/pull/44): API 36.1 UI replay — the baseline dragged the Contacts pin at cell 14 but moved the original pin from cell 0, while the fixed replay moved the selected pin and preserved the other; the two new Room regressions in `WorkspaceRepositoryWorkspaceMoveTest` (exact duplicate placement, stale source cell) failed on the prior lookup and passed with source-cell matching; all 15 instrumentation tests passed on candidate `790afd3` on the dedicated API 26 AVD | **Device-verified (UI on API 36.1; candidate `790afd3`, 15 tests on API 26)** |
| Current full instrumentation suite | Seven suites covering Room migrations, drawer filtering, dock, folders, and workspace moves | All 16 tests passed on dedicated API 26, 31, 35, and 36.1 emulators with debug candidate `1b5dc2e` for [PR #45](https://github.com/vladon/vesnalauncher/pull/45). The [API 31 report](verification/VESNA-QA-010-API31.md) records the follow-up. The API 35 runner output and matching pre-run and restored 18-placement snapshots are in `qa-evidence/bug011/`. | **Device-verified at the tested build and emulator configurations** |
| Launch-state timings, cold and warm (Android 15, no research ID) | `HomeActivity` via `am start -W` ActivityManager `TotalTime`, COLD and WARM | [VESNA-QA-009-LAUNCH-STATES](verification/VESNA-QA-009-LAUNCH-STATES.md) (2026-11-19, [PR #40](https://github.com/vladon/vesnalauncher/pull/40)): paired cold and warm samples for one debug APK on the dedicated Android 15 AVD; COLD and WARM only — no HOT state was measured in this procedure; sample counts, values, and teardown evidence in the report | **Measured (debug, emulator, COLD/WARM scope)** |

## Coverage summary

| Verification level | Research IDs / behaviors |
|---|---|
| Test-verified (exact scope) | `R-DRAWER-100.4` order-preservation subset, `R-DRAWER-101.1` local-filtering subset, dock ordering model, workspace grid/order model, folder resolver model, BUG-003/BUG-005/BUG-006/BUG-007/BUG-008/BUG-009/BUG-010/BUG-011 focused device cases (exact scope per row above) |
| Source-verified at the audit baseline | Initial HOME, profiles, seeding, package-event and settings wiring. See the platform rows above for the subsequent device runs. |
| Measured (debug, emulator scope) | QA-007 process-cold timing (API 35 shared-host emulator), QA-009 paired cold/warm timings (dedicated Android 15 AVD; no HOT measured) |
| Outside the baseline audit | HOME gesture, migrations, reboot persistence, managed profiles, package removal/unavailability, app-to-HOME return, folder/dock UI flows |
| Not implemented | Widgets, cards, settings search/deep-linking, settings reset, explicit backup/restore, icon packs, themes, fuzzy search, workspace page management |
| Blocked / decision needed | `DRAWER-105` ID reuse, `CARDS-100` (condition-triggered content; location/manual-fallback privacy), `DRAWER-102` ambiguity, `BACKUP-102` scenario |

New engineering work must add or update a row here in the same change that adds
the code or test, so the matrix never silently rots. Work items live in
[BACKLOG.md](BACKLOG.md); sequencing is [ROADMAP.md](ROADMAP.md).
