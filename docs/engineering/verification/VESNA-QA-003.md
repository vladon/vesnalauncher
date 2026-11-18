# VESNA-QA-003 — package lifecycle verification

**Verdict:** PARTIAL — every **drawer-side** lifecycle case passed end to end
(install, same-package update, disable/re-enable, uninstall, launcher process
restart). The **workspace/dock/folder placement cases are not executable on
0.4.1**: the build offers no working UI path to pin an app that is not already
pinned, due to two recorded UI defects (below). Run recorded 2026-11-19 on the
dedicated QA device.

**Item:** [VESNA-QA-003 in BACKLOG.md](../BACKLOG.md#vesna-qa-003-package-lifecycle-verification-done)
(P0, Vesna platform baseline, no approved research ID — QA of existing behavior
only; no product change, no research approval claimed).
**Code under test:** commit `1a2590a` ("docs: record HOME selection and
launch-return verification (#11)"), branch `vladon/qa-package-lifecycle`
(isolated worktree `qa-package-lifecycle`); launcher under test
`dev.vladon.vesna.launcher` 0.4.1 / versionCode 6 (installed build matches the
trunk baseline; it was **not** rebuilt or replaced in this run).

## Fixture design (bounded, repeatable)

Built **outside the repository** in `D:/orca/scratch/vesna-qa-003/` (no tracked
file touched):

- Package `dev.vladon.qa.lifecyclefixture`, single activity
  `dev.vladon.qa.lifecyclefixture.MainActivity` with `MAIN`/`LAUNCHER`; label
  "QA Fixture" (literal, stable across versions so drawer order is stable).
- No permissions, no network, no services/receivers; `minSdk 26`,
  `targetSdk 35`.
- Two APKs built from identical sources with SDK build-tools 36.1.0
  (`aapt2 link` → `d8` → `zipalign` → `apksigner sign`), signed with the local
  Android debug keystore (`CN=Android Debug`, SHA-256 `58231498…` — identical
  certificate on both): `fixture-v1.apk` versionCode 1 (0.1) and
  `fixture-v2.apk` versionCode 2 (0.2). Same package, same component, same
  signature — exactly the in-place-update shape the `PackageEvent.Replaced`
  path covers.
- Rebuild: re-run the four build-tools steps over the manifest + one `.java`
  file; no Gradle, no repo state involved.

## Environment

| Property | Value | Source |
|---|---|---|
| Dedicated device | AVD `Vesna_QA_API_35`, adb serial `emulator-5556` | `adb -s emulator-5556 emu avd name` before **every** execution phase |
| OS / API | Android 15, API 35 | `getprop` |
| Screen | 1080×2400 @ 420 dpi | `wm size`, `wm density` |
| Users | user 0 only — **no managed profile exists on this AVD; that row is untested** | `pm list users` |
| HOME holder at start and end | **none** (`cmd role get-role-holders android.app.role.HOME` empty; Pixel Launcher visible only as fallback, same task `t7` / record `314b1a0` at start and end) | role probe, `dumpsys activity activities` |

Every device command pinned `-s emulator-5556`. No other device, worktree,
Nova install, production module, or system package was touched. No app data
was cleared; no system-wide configuration was changed (adbd was temporarily
switched to root solely for raw `/dev/input` gesture injection during UI
automation, and restored with `adb unroot` before closing — no device setting
was modified).

## Initial state (recorded before the fixture existed)

- `workspace_items`: 18 rows (dock: Calendar, Camera2, Chrome, DeskClock at
  slots 0–3; workspace cells 0–13), `folders`: 0, all `userSerial` 0 —
  row-for-row identical to the layout QA-002 left behind.
- `shared_prefs/vesna_launcher.xml`: `workspace_seeded=true`.
- Drawer: 18 apps, alphabetical.

### Read-only DB inspection method (used at every phase)

`adb exec-out run-as dev.vladon.vesna.launcher cat` of `vesna-launcher.db`,
`-wal`, and `-shm` as a trio while the launcher was idle, then a **local**
SQLite read of the copied set. Assertions in this doc are **row-by-row
comparisons of `workspace_items` and `folders`** (id, package, class,
container, cellIndex, userSerial, folderId) against the baseline snapshot —
they are *not* claims of byte-identical database files (WAL checkpointing
legitimately rewrites pages). The production database on the device was never
written, checkpointed, or cleared by this run; the launcher is debuggable, so
`run-as` provided the read path. UI observations and DB assertions are labeled
separately throughout.

## Suspected product defects found (recorded only; fix left to a separate increment)

Both defects block the *initial* pin of an unpinned app from the drawer; both
were reproduced on 0.4.1 (`1a2590a` build installed on the AVD).

1. **Drawer drag-to-pin is a silent no-op.** `HomeActivity` builds every drawer
   drag source as `DragSource(app, Container.WORKSPACE, fromCell = -1,
   fromDrawer = true)`, so `commitGridDrop`'s empty-cell branch routes to
   `controller.moveWithinWorkspace` — which requires an existing top-level
   workspace row and returns false for an unpinned app. The
   `else -> controller.insertWorkspaceAt(...)` branch is unreachable (source
   container can only be `WORKSPACE` or `DOCK`), and the same applies to dock
   drops (`moveWorkspaceToDock`) and occupied/folder drops
   (`createFolderFromDrop`/`addToFolder` require a top-level row of the dragged
   app). Observed on device: with the drawer open and a search filter isolating
   the fixture, one continuous `input draganddrop` from the fixture icon to a
   verified-empty cell produced **no layout row** (18 items before and after;
   fixture rows 0) and no error surface — drawer simply closed.
2. **Drawer item long-press menu opens as a zero-height popup.** The drawer
   item menu is constructed as `PopupMenu(this, drawer)` — anchored to the
   full-screen `DrawerView` instead of the tapped icon. On device the long-press
   *does* open a popup owned by `dev.vladon.vesna.launcher`, but its window
   frame is `[0,2337][515,2337]` — **0 px tall** at the bottom edge
   (`dumpsys window windows`, `Window #7 PopupWindow:…`): items are invisible
   and untappable ("Add to workspace" / "Add to dock" are effectively
   unreachable). Contrast: the dock item menu (`PopupMenu(this, anchor)`,
   icon-sized anchor) renders and taps normally — verified "Remove from dock" /
   "App info" visible on screen in this run.

Consequence for this item: workspace, dock, and folder **placement** rows of
the backlog scenario could not be exercised, because no UI path can create the
first placement. Per the item's own instruction ("pin … only where existing UI
supports it") those sub-cases are reported **NOT EXECUTABLE** rather than
simulated.

## Procedure and per-case results

### A. Install → drawer visibility — **PASS**

`adb install` of `fixture-v1.apk` (versionCode 1) succeeded. The drawer
(swipe-up, "Search apps" grid) listed **QA Fixture** at the alphabetical slot
between Photos and Safety (bounds `[285,1087][525,1130]`), label resolved from
the manifest. Layout DB unchanged (18 rows, 0 fixture rows — install only adds
a discovery entry, as designed). Evidence: `01_drawer_with_fixture_v1.png`.

### B. Pin into workspace / dock / folder — **NOT EXECUTABLE (defects 1 and 2)**

Attempted through every supported UI route: drawer item long-press menu
(defect 2), drawer drag to empty cell / dock (defect 1, run once cleanly with
a single continuous gesture per the coordinator's instruction), drawer drag to
occupied cell / folder (no-op — requires a top-level row that cannot exist yet).
No placement row was ever created. Dock placement additionally requires
freeing a slot (dock is full at 4/4) via "Remove from dock" — reachable, but
meaningless without a pinnable fixture. Consequently the folder/dock/workspace
branches of cases C–E could not be exercised either; their **drawer-side**
aspects below were fully verified instead.

### C. Same-package update (v1 → v2) — **PASS (drawer side; layout stable)**

`adb install -r fixture-v2.apk`: versionCode 1→2 in place, same signature, no
data clear. Drawer kept the identical entry at the identical alphabetical slot
(bounds unchanged `[285,1087][525,1130]`) — `PackageEvent.Replaced` path
refreshes discovery without touching the layout. DB row-identical to baseline.
Evidence: `03_drawer_after_update_v2.png`.

### D. Disable (`pm disable-user`) → re-enable — **PASS (drawer side)**

- Disable: fixture left the drawer immediately (`LauncherApps` reports the
  package unavailable → discovery excludes it); drawer dump shows 18 apps, no
  QA Fixture. DB row-identical (with no fixture rows, the
  `removeAllForPackage`/`pruneStale` wipe had nothing to delete — the
  **placement-loss on temporary disable** that the source code predicts
  (`onPackagesUnavailable` is mapped to `PackageEvent.Removed`) was therefore
  **not observable** here and is documented as source-verified expectation
  only). Evidence: `04_drawer_after_disable.png`.
- Re-enable (`pm enable`): drawer entry **recovered at the identical slot**
  `[285,1087][525,1130]`; DB row-identical. Note for a future placement run:
  source analysis says stored placements would *not* come back (rows are
  hard-deleted at disable time) — untested here, no product-policy claim made.
  Evidence: `05_drawer_after_reenable.png`.

### E. Uninstall → no ghosts, incl. launcher process restart — **PASS (drawer side)**

- `adb uninstall` while the launcher was running: drawer entry gone
  immediately (live `PackageEvent.Removed` path); DB row-identical; 18-app
  drawer renders with no empty cell, dock untouched. Evidence:
  `06_drawer_after_uninstall.png`.
- **Launcher process restart** (`am force-stop` + cold start, `LaunchState:
  COLD`): workspace and drawer render from the DB with **no ghost fixture
  anywhere**; DB row-identical. Evidence: `07_after_launcher_restart.png`.
- **Uninstall while the launcher was dead** (startup-prune path): v2
  reinstalled (drawer shows it), launcher force-stopped, package uninstalled,
  launcher cold-started — first discovery pass lists 18 apps, no fixture, DB
  row-identical. With no placements the prune had nothing to remove; the path
  itself executed cleanly (no re-seed, no layout mutation). Evidence:
  `08_startup_after_dead_uninstall.png`.

### Managed profile — **UNTESTED**

`pm list users` shows only user 0; no managed profile was provisioned (the
task forbids provisioning without reporting separately). The cross-profile
`userSerial` tagging of `PackageChangeMonitor` remains untested on this AVD.

## Cleanup / restore (verified after all cases)

- Fixture uninstalled; `pm list packages -3` = `dev.vladon.vesna.launcher` +
  its test package only (the pre-run set). Temporary `/data/local/tmp`
  automation scripts removed.
- Vesna untouched: 0.4.1 / versionCode 6, `workspace_seeded=true`, database
  row-identical to the pre-run snapshot (18 rows, 0 folders, 0 fixture rows).
- HOME holder: none — identical to session start (the run never touched the
  HOME selection); Pixel Launcher re-foregrounded via `am start` and verified
  on the **same task `t7` / ActivityRecord `314b1a0`** as at session start.
  Evidence: `09_final_restored_workspace.png`.
- adbd restored to non-root (`adb unroot`), device identity re-confirmed
  (`emu avd name` → `Vesna_QA_API_35`).

## Verdict per observed case

| # | Case | Result | Evidence |
|---|---|---|---|
| A | Install → drawer visibility | **PASS** | `01`, drawer dump, DB unchanged |
| B | Pin to workspace/dock/folder | **NOT EXECUTABLE** — no working pin path on 0.4.1 (defects 1–2 above) | `dumpsys window` popup frame; DB unchanged after continuous drawer drag |
| C | Same-package update preserves drawer entry + layout | **PASS** | `03`, identical bounds, DB row-identical |
| D | Disable → drawer removal; re-enable → recovery | **PASS** (drawer side) | `04`, `05`, DB row-identical both steps |
| E | Uninstall → no ghosts live, after restart, after dead-uninstall | **PASS** (drawer side) | `06`, `07`, `08`, DB row-identical |
| — | Placement persistence/wipe (update, disable, uninstall) | **NOT EXECUTABLE** — depends on B | defects 1–2 |
| — | Managed-profile repeat | **UNTESTED** — AVD has no profile | `pm list users` |

## Notes and limitations

- Manual scripted run (adb input, `run-as` probes, screenshots) on one device,
  one user; not an automated suite. Drawer-side automation required raw
  gesture injection (`sendevent` under temporary adb root) because stock
  `input` primitives cannot produce a hold-without-move gesture; all `input`
  attempts that did move also produced the drag paths documented above, so the
  defect evidence does not depend on the raw-injection method.
- DB assertions are row-by-row snapshot comparisons (see method above), not
  byte-identical-file claims; snapshots were taken only while the launcher was
  idle, and the on-device database was never written by this run.
- The two defects are **recorded findings**; behavior, policy, and fixes belong
  to a separate increment. In particular no claim is made here about what the
  disable-time placement wipe *should* do — only what the code predicts and
  what could be observed.
- Evidence captures (PNG + uiautomator XML + DB snapshots, device-attributed
  to `emulator-5556` / `Vesna_QA_API_35`) live **outside the repository** at
  `/d/orca/workspaces/vesnalauncher/evidence/qa-package-lifecycle/`
  (`00_baseline_workspace.png` … `09_final_restored_workspace.png`, plus the
  `*.db` snapshot trios and `*.xml` dumps per case).
