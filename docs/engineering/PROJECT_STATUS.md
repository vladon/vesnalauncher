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
| Dock | Four slots aligned with workspace columns, ordering and reorder rules in `DockOrdering` | `DockOrderingTest` (9 tests) |
| First-run seeding | `SeedPlanner` fills dock and first page once; a persisted "seeded" flag prevents re-seeding; user-emptied workspaces are never re-seeded | `SeedPlannerTest` (4 tests) |
| App discovery | `LauncherApps`-based `AppDiscovery` across the main and managed profiles, sorted by `label.lowercase(Locale.ROOT)`, single `<queries>` manifest entry (no `QUERY_ALL_PACKAGES`) | Source only; discovery needs system services and is deliberately not exercised by tests |
| Drawer search | Case-insensitive substring filter (`AppSearch`), empty state, clear restores full list; drawer never re-orders | `AppSearchTest` (5 JVM tests); on-device `DrawerSearchTest` |
| Drag-and-drop | Cross-container drag (workspace ⇄ dock ⇄ drawer-to-workspace) routed by `LauncherDragLayer`; drop-target highlights | Source only |
| Folders | `Folder` grid cell, open/rename/remove in `FolderDialog`, dissolution below two members (`removeFromFolder` returns members to free cells); drag-into-folder via drops onto folder cells (`addToFolder`, two `HomeActivity` paths) | Model only: `WorkspaceResolverFolderTest` (7 tests) covers the resolver projection (member exclusion from top-level cells, folder cell with member count, invalid cell skipped, no override of an app in the target cell, in-folder ordering, stale members dropped, empty without folders). Create/rename/dissolution flows, the dialog UI, and the drag-into-folder UI flow are **not** covered |
| Package events | `PackageChangeMonitor` via `LauncherApps.registerPackageCallback` (install/update/uninstall, all profiles, `userSerial` tagged); stale layout entries pruned | **Drawer-side device-verified** by the [VESNA-QA-003 run](verification/VESNA-QA-003.md) (2026-11-19, API 35 AVD), accepted for review as VESNA-QA-003A: install→drawer, same-package update, disable/re-enable, uninstall + launcher restart all clean. The run did not cover pinned fixture entries. [PR #15](https://github.com/vladon/vesnalauncher/pull/15) restored drawer drops and was verified on API 35. [VESNA-QA-003C](verification/VESNA-QA-003C.md) verified managed-profile discovery, a work-profile pin carrying the profile's `userSerial`, and profile-scoped uninstall (work-only removal left the personal package and pin, stable across a launcher restart); [VESNA-QA-003D](verification/VESNA-QA-003D.md) verified uninstall with simultaneous workspace, dock, and folder placements reconciling after a cold relaunch (both API 35, 2026-11-19) |
| Persistence | Room `vesna-launcher.db`, schema version 3; migrations 1→2 (per-profile `userSerial`) and 2→3 (folders, `folderId`) | **Device-verified at API 35** in two separate experiments: migrations by `RoomMigrationQaTest` ([PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`; v1→2→3 and v2→3 through the production `LauncherDatabase.build`), and process death / full guest reboot / no re-seed by [VESNA-QA-004B](verification/VESNA-QA-004B.md) (2026-11-19, tested build `0c7269c`) |
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
  smoke-verified through the system RoleManager dialog.
- The test preserves the order of a pre-sorted fixture (sorted with the production
  comparator) and asserts the drawer filters without re-ordering. The baseline
  audit does not exercise: discovery sorting, match highlighting (not
  implemented), the HOME gesture, Room migrations, reboot persistence, or
  managed-profile behavior. Behaviors outside that list are not claimed tested
  here either — see the audit-scope note below.

**No performance measurements or benchmarks exist** for the launcher. Startup,
scroll, and drag behavior have not been measured; any such claim would be
unfounded.

## Audit status discipline

Statements in this file and in [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md)
are either (a) verified against the source at the pinned commit, or (b) marked
as covered/not covered by the recorded audit runs. "Source-verified" means the
code implements the behavior as described; it is not a claim of on-device QA.
The audit ran the checks listed above and no others; absence of a check from the
list means it was not performed, nothing more.

## Known gaps and risks

- Platform coverage comes from dedicated API 26, API 35, and API 36.1
  emulators. See the instrumentation record for the tested builds and
  device configurations.
- CI does not run instrumentation; `DrawerSearchTest` depends on a local AVD and
  a manual runner.
- Managed-profile behavior (badged work apps, profile-aware removal) is
  implemented via `LauncherApps` and `userSerial` and is outside the audit;
  profile-scoped discovery, pinning, and removal were device-verified
  afterwards by [VESNA-QA-003C](verification/VESNA-QA-003C.md) (2026-11-19,
  API 35 AVD).
- The repository README still labels the status `0.3.0`; `app/build.gradle.kts`
  (0.4.1, code 6) is authoritative.

## Where to go next

- Completed since the baseline: [VESNA-QA-002](verification/VESNA-QA-002.md) —
  HOME registration/selection and launch-return device-verified on the
  dedicated API 35 AVD (a Vesna platform baseline item; no approved research ID).
- Accepted for review as **VESNA-QA-003A**:
  [VESNA-QA-003](verification/VESNA-QA-003.md) — package lifecycle, drawer
  side (install/update/disable/uninstall/restart) on the API 35 AVD. The
  [VESNA-QA-003B](verification/VESNA-QA-003B.md) verified upgrade retention
  for workspace/dock placements and folder cleanup on uninstall. Disabling
  the fixture dropped its pins, and re-enabling it restored drawer discovery.
  The parent QA-003 item stays **in-progress** for the disable-time placement
  retention product decision only; the remaining device cases are verified —
  uninstall across all three containers by
  [VESNA-QA-003D](verification/VESNA-QA-003D.md) (simultaneous
  workspace/dock/folder removal, reconciled after a cold relaunch) and the
  managed-profile case by [VESNA-QA-003C](verification/VESNA-QA-003C.md)
  (work-only uninstall left the personal package and pin, stable across a
  launcher restart).
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
  dedicated API 35 AVD as two separate experiments. QA-003 sub-items remain
  open ([VESNA-QA-003 in BACKLOG.md](BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial)).
- Prioritized plan: [ROADMAP.md](ROADMAP.md).
- Requirement-level pointers: [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).
