# Engineering Project Status

**Baseline commit:** `888f557` — `test: cover drawer search on Android (#9)` (QA PR #9,
squash merge). Post-merge CI run 44385854347 is green.
**App version:** 0.4.1 (`versionName`), `versionCode` 6, application ID
`dev.vladon.vesna.launcher`.
**Date of baseline:** 2026-11-18.

This document is the engineering handoff baseline: what exists in the code today,
what is verified, and what is not. Requirement-level traceability lives in
[REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md); the research input
is summarized in [RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md).

## Product shape

Single Gradle module `:app`, Kotlin. The launcher hot path (workspace, dock,
drawer) is Android Views; the About/settings surface is Jetpack Compose with
Material 3. `minSdk` 26, `compileSdk`/`targetSdk` 37. See
[../architecture/OVERVIEW.md](../architecture/OVERVIEW.md) for package layout,
key decisions, and data flow.

## Implemented capabilities (source-verified)

Every item below was verified by reading the implementation at the baseline
commit. The "Verification" column names exactly what the baseline audit
exercised; everything else is source-verified only.

| Capability | Implementation anchors | Verification |
|---|---|---|
| HOME role | `HomeActivity` declares `CATEGORY_HOME` (`singleTask`, `excludeFromRecents`); `HomeRoleChecker` (RoleManager on API 29+, `resolveActivity` fallback) and the system "choose home app" intent | Source only at the audit baseline; **device-verified** afterwards by the [VESNA-QA-002 run](verification/VESNA-QA-002.md) (2026-11-18, API 35 AVD) |
| Workspace | Fixed 4×5 grid (`GridCanvas`), placement via `WorkspaceResolver` + `WorkspaceRepository`, Room-backed; persistence across process death/reboot is intended behavior | 17 JVM tests in `GridCanvasTest`, `WorkspaceResolverTest` |
| Workspace pages (unreleased) | Long-press empty Home space → **Add home page**. Up to eight 4×5 pages; swipe horizontally or use the arrows above the shared dock. Vesna restores the last selected page. | Android 8 and 15 emulator verification; see [Workspace page controls](#workspace-page-controls). |
| Dock | Four slots aligned with workspace columns, ordering and reorder rules in `DockOrdering` | `DockOrderingTest` (9 tests) |
| First-run seeding | `SeedPlanner` fills dock and first page once; a persisted "seeded" flag prevents re-seeding; user-emptied workspaces are never re-seeded | `SeedPlannerTest` (4 tests) |
| App discovery | `LauncherApps`-based `AppDiscovery` across the main and managed profiles, sorted by `label.lowercase(Locale.ROOT)`, single `<queries>` manifest entry (no `QUERY_ALL_PACKAGES`) | Source only; discovery needs system services and is deliberately not exercised by tests |
| Drawer search | Case-insensitive substring filter (`AppSearch`), empty state, clear restores full list; drawer never re-orders | `AppSearchTest` (5 JVM tests); on-device `DrawerSearchTest` |
| Drag-and-drop | Workspace, dock, and drawer routes through `LauncherDragLayer` and the controller | The recorded device cases include dock-to-workspace moves and workspace moves to an empty cell. [Requirements traceability](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids) |
| Folders | `Folder` cell and `FolderDialog` for opening, renaming, and removing members | `WorkspaceResolverFolderTest` covers the projection. Device and Room tests cover selected-member removal, creation and removal rollback, and uninstall-driven dissolution. [Folder verification](REQUIREMENTS_TRACEABILITY.md#folders) |
| Package events | `PackageChangeMonitor` uses profile-scoped `LauncherApps` callbacks; disabled apps retain their pins | [VESNA-QA-003A](verification/VESNA-QA-003.md) verified drawer changes after install, update, disable/re-enable, and uninstall. [VESNA-QA-003C](verification/VESNA-QA-003C.md) verified work-profile pinning and uninstall. [VESNA-QA-003D](verification/VESNA-QA-003D.md) verified uninstall across all three containers. [VESNA-QA-003F](verification/VESNA-QA-003F.md) verified disable/re-enable retains placements across a launcher restart. All four records use API 35. |
| Persistence | Room `vesna-launcher.db`, schema version 4. The 3→4 migration adds stable workspace page IDs without changing the dock or existing cells. | Production Room migrated a copy of the signed 0.4.2 database on API 35 with all 18 placements, the folder, and the seeding flag intact. See [ADR-PAGE-1](../adr/0001-workspace-page-identity.md). Historical API 35 migration and reboot evidence is in [VESNA-QA-004](BACKLOG.md#vesna-qa-004-migration-and-restart-verification-done). |
| Settings / About | `SettingsActivity` (Compose + Material 3) with app and version info | Source only |

**Explicitly not implemented:** widgets, icon packs, themes, fuzzy search, drawer
search-result match highlighting, search beyond substring. The Android manifest
sets `android:allowBackup="true"` (platform default behavior), but a dedicated
backup/restore feature does not exist; the backup/privacy ADR candidate in
[BACKLOG.md](BACKLOG.md#adr-backup-1-backup-and-privacy-policy) must decide
whether platform backup stays enabled and what its scope is. There is no
monetization of any kind — no ads, subscriptions, paid features, in-app
purchases, or analytics/tracking SDKs.

## Quality status at the baseline

| Check | Result |
|---|---|
| `assembleDebug` | Success |
| `:app:assembleDebugAndroidTest` | Success |
| JVM unit tests | 42 tests, 6 suites, all passing |
| Android Lint | 0 errors, 18 warnings (3 of them are test-dependency notices) |
| Post-merge CI | Run 44385854347 green (build, JVM tests, lint; no instrumentation in CI) |

JVM suites: `DockOrderingTest`, `GridCanvasTest`, `SeedPlannerTest`,
`WorkspaceResolverTest`, `WorkspaceResolverFolderTest` (workspace package) and
`AppSearchTest` (apps package). At the baseline one instrumentation suite
existed: `DrawerSearchTest` (single test covering mixed-case query narrowing,
empty state, and clear/restore), run locally, not in CI; `RoomMigrationQaTest`
was added afterwards by [PR #13](https://github.com/vladon/vesnalauncher/pull/13)
(recorded in the instrumentation verification record below).

### Instrumentation verification record

- `DrawerSearchTest` passed 3 consecutive runs on the dedicated API 35 AVD.
- `RoomMigrationQaTest` (introduced by
  [PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`):
  both cases (`migrationFromV1PreservesLayoutAndDerivesDefaults`,
  `migrationFromV2PreservesUserSerialsAndAddsFolders`) plus `DrawerSearchTest`
  were executed by the coordinator on the dedicated API 35 AVD (2026-11-19).
  The test seeds authentic v1/v2 schemas into isolated on-device databases and
  migrates through the production `LauncherDatabase.build`; its own doc states
  process-death/reboot durability is a separate task — covered by
  [VESNA-QA-004B](verification/VESNA-QA-004B.md).
- The full existing instrumentation suite passed on the declared minimum OS:
  [VESNA-QA-005-API26](verification/VESNA-QA-005-API26.md) (2026-11-19,
  dedicated API 26 AVD) ran `RoomMigrationQaTest` (both cases) and
  `DrawerSearchTest` — 3/3 passed, 0 failures/errors/skipped; the per-case
  JUnit XML (`TEST-Vesna_QA_MinSdk_API_26-AVD-8.0.0.xml`) is listed in the
  record's evidence index. Narrow smoke scope: build, HOME discoverability,
  cold start, workspace/drawer render, one fixture drawer round-trip. The
  same run exposed a fifth-row dock overlap (VESNA-BUG-004), fixed in
  [PR #23](https://github.com/vladon/vesnalauncher/pull/23) and replayed on API 26 and API 35.
- At audit time, an API 36.1 AVD was provisioned but did not boot, so API 36.1
  was not covered by the audit. That gap was closed afterwards by
  [VESNA-QA-006-API36-1](verification/VESNA-QA-006-API36-1.md)
  (2026-11-19, dedicated API 36.1 AVD on Windows): the full existing
  instrumentation suite passed 5/5, with HOME selection and launch-return
  smoke-verified through the system RoleManager dialog. Run 2 on the same AVD
  exercised the BUG-005 candidate `d8d6a748` with the added
  `WorkspaceRepositoryTopLevelRemovalTest`: 7/7 passed; these device runs are
  scoped to that branch candidate, which merged as
  [PR #27](https://github.com/vladon/vesnalauncher/pull/27) (`81cea18`).
- On Android 12/API 31, the 12-test candidate `75ca3ed` passed. The run
  verified workspace and drawer rendering and HOME registration while
  NexusLauncher remained default.
  [VESNA-QA-010](verification/VESNA-QA-010-API31.md)
- The full 16-test instrumentation suite passed on dedicated API 26, 31,
  35, and 36.1 emulators using the same reviewed debug candidate
  `1b5dc2e`. Each run used the serial-pinned runner and restored its
  fixture. [Requirements traceability](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids)
- Dock capacity, top-level unpin, and dock-to-workspace drag regressions were
  verified on dedicated emulators. The verification scope for each fix is
  recorded in [Requirements traceability](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids).
- [VESNA-QA-008](https://github.com/vladon/vesnalauncher/pull/34) strengthened
  the approved order-preservation and local-filtering coverage. The test
  supplies an unsorted fixture and verifies the order of two mixed-case
  substring matches. The full suite passed 8/8 on the dedicated API 35 AVD
  with candidate `18dd2d1`.
- The eight-test suite passed on API 26 at build `4c5f0c1`.
  [VESNA-QA-005B](verification/VESNA-QA-005-API26.md#follow-up-on-the-eight-test-suite)
- The legacy HOME chooser and return from Calculator were verified on API 26.
  [VESNA-QA-002B](verification/VESNA-QA-002.md#api-26-follow-up-vesna-qa-002b)
- Focused regression records cover the two folder transactions and the
  selected-pin workspace move.
  [Requirements traceability](REQUIREMENTS_TRACEABILITY.md#folders)

At audit commit `888f557`, startup, scroll, and drag had no baseline
measurements. [VESNA-QA-007](verification/VESNA-QA-007-COLD-START.md) now
records a process-cold debug ActivityManager timing series on an Android 15
shared-host emulator, and
[VESNA-QA-009-LAUNCH-STATES](verification/VESNA-QA-009-LAUNCH-STATES.md)
adds paired cold and warm timings on the dedicated Android 15 AVD (COLD and
WARM only; no HOT state was measured). Drawer scrolling, drag latency, time to
interactivity, release builds, and real devices remain open.

## Audit status discipline

Statements in this file and in [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md)
are either (a) verified against the source at the pinned commit, or (b) marked
as covered/not covered by the recorded audit runs. "Source-verified" means the
code implements the behavior as described; it is not a claim of on-device QA.
The audit ran the checks listed above and no others; absence of a check from the
list means it was not performed, nothing more.

## Known gaps and risks

- Platform coverage comes from dedicated API 26, API 31, API 35, and API 36.1
  emulators. See the instrumentation record for the tested builds and
  device configurations.
- CI does not run instrumentation; `DrawerSearchTest` depends on a local AVD and
  a manual runner.
- Managed-profile behavior (badged work apps, profile-aware removal) is
  implemented via `LauncherApps` and `userSerial` and is outside the audit;
  profile-scoped discovery, pinning, and removal were device-verified
  afterwards by [VESNA-QA-003C](verification/VESNA-QA-003C.md) (2026-11-19,
  API 35 AVD).
- The last published release is `0.4.2` (version code 7, Room schema 3).
  The signed [0.4.1 → 0.4.2 upgrade](verification/VESNA-QA-0.4.2-UPGRADE.md)
  preserved all workspace, dock, and folder data on Android 15.
- The local `0.4.3` candidate (version code 8, Room schema 4) passed 41 JVM
  tests and all 29 instrumentation tests on each of two dedicated emulators:
  Android 8/API 26 (`Vesna_Release_043_API26_QA`, fresh install) and Android
  15/API 35 (`Vesna_Pages_V4_QA`). Home rendered on both. On API 35, the debug
  candidate migrated a copy of the signed 0.4.2 database, preserving all 18
  placements, the folder and its members, and the seeding flag. Lint reported
  0 errors and 20 warnings. The minified release APK was built unsigned;
  its package, version, and ZIP alignment were checked locally.

### Workspace page controls

The local `workspace-page-controls` branch builds on the 0.4.3 storage work; it
does not change the Room schema or the last published release. New pages are
empty, and adding or moving an app acts on the displayed page. Long-press an
empty extra page and choose **Remove empty page** to free a slot and return to
the first Home page. The first page and pages with saved shortcuts or folders
cannot be removed, even if a saved shortcut is not currently visible. Page
reordering and dragging between pages are not available.

- All 41 JVM tests and 34 instrumentation tests passed; the instrumentation
  suite ran on both Android 8 (API 26) and Android 15 (API 35). Debug lint had no
  errors.
- On Android 15, a drawer drag and a drawer-menu pin landed on page two. A
  force-stop and relaunch restored that page and both pins. The original
  18 placement rows, the folder, and the shared dock were unchanged. The Home
  menu stopped offering creation at eight pages.
- The page controls were checked at 320, 411, and 610 dp widths, with gesture
  and three-button navigation, and in Light and Dark at up to 1.6× font scale.
  Verified 48 dp targets, color contrast, visible keyboard focus, English and
  Russian labels, accessibility position/count, and mirrored RTL gestures.
  Spoken TalkBack announcements and physical-device behavior were not verified.
- The eight focused page tests passed on both emulators for empty-page removal.
  The Home menu restored **Add home page** after a removal at the cap, retained
  the first page across restart, and preserved all 20 saved placements and the
  folder on Android 15. Android 8 hid the pager after removing the only extra
  page. The new menu row fit at 320 dp and 1.6× type in English and Russian;
  touch selection and visible D-pad focus and activation were verified.

## Where to go next

- Completed since the baseline: [VESNA-QA-002](verification/VESNA-QA-002.md) —
  HOME registration/selection and launch-return device-verified on the
  dedicated API 35 AVD (a Vesna platform baseline item; no approved research ID).
- Completed since the baseline: [VESNA-QA-003](BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial)
  — installed app lifecycle verified at API 35, including a work-only
  uninstall and retention of workspace, dock, and folder pins while
  a personal-profile app is disabled.
- Completed since the baseline: [VESNA-QA-005](verification/VESNA-QA-005-API26.md)
  — minimum SDK (API 26) smoke on the dedicated API 26 AVD: build, HOME
  discoverability, cold start, workspace/drawer render, one fixture drawer
  round-trip, and the full instrumentation suite 3/3 (per-case JUnit XML in
  the record's evidence index). The layout fix for VESNA-BUG-004 is recorded
  in [PR #23](https://github.com/vladon/vesnalauncher/pull/23).
- Completed since the baseline: [VESNA-QA-004](BACKLOG.md#vesna-qa-004-migration-and-restart-verification-done)
  — Room migrations (`RoomMigrationQaTest`, PR #13, merge `14ec712`) and
  persistence (process death, full guest reboot, no re-seed;
  [VESNA-QA-004B](verification/VESNA-QA-004B.md)) device-verified on the
  dedicated API 35 AVD as two separate experiments.
- Prioritized plan: [ROADMAP.md](ROADMAP.md).
- Requirement-level pointers: [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).
