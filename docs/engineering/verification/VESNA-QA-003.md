# VESNA-QA-003 — package lifecycle verification

**Verdict:** PARTIAL — every **drawer-side** lifecycle case passed end to end
(install, same-package update, disable/re-enable, uninstall, launcher process
restart). The run did not exercise workspace, dock, or folder placement.
Two UI defects prevented pinning the fixture in this run. Recorded 2026-11-19
on the dedicated QA device.

**Item:** [VESNA-QA-003 in BACKLOG.md](../BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial)
(P0, Vesna platform baseline, no approved research ID — QA of existing behavior
only; no product change, no research approval claimed).
**Sub-item status:** the drawer-side evidence in this document is accepted for
review as **VESNA-QA-003A**; the parent VESNA-QA-003 item remains **open**
for the workspace/dock/folder placement cases and the managed-profile repeat.
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
`run-as` provided the read path. The files were copied sequentially, so these
snapshots describe observations taken while the app was idle. UI observations and DB assertions are labeled
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

Consequence for this run: the tested drawer flows did not create the first
placement, so workspace, dock, and folder lifecycle cases remain open. Per the item's own instruction ("pin … only where existing UI
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
| B | Pin to workspace/dock/folder | **NOT EXECUTABLE** — the tested pin paths failed on the baseline (defects 1–2 above) | `dumpsys window` popup frame; DB unchanged after continuous drawer drag |
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

## Regression verification recipe (handoff for the fix increment)

Status note (2026-11-19): [PR #15](https://github.com/vladon/vesnalauncher/pull/15)
fixed defect 1 at `40cb3ee`. The coordinator verified that the same fixture
could be placed in workspace cell 14 and dock slot 3 on API 35. VESNA-QA-003B
tracks the separate placed-item lifecycle run against the fixed revision.

**Fixture (retained, do not regenerate):** `D:/orca/scratch/vesna-qa-003/`

| File | Package | Activity | versionCode/Name | SHA-256 |
|---|---|---|---|---|
| `fixture-v1.apk` | `dev.vladon.qa.lifecyclefixture` | `dev.vladon.qa.lifecyclefixture.MainActivity` | 1 / 0.1 | `f6fb89981616580d8485a728449020fc8236b1386c90901119ec47755eefcebf` |
| `fixture-v2.apk` | `dev.vladon.qa.lifecyclefixture` | `dev.vladon.qa.lifecyclefixture.MainActivity` | 2 / 0.2 | `ce286df06f70b9f5e213a7d7a8032d68df3848520c9e51717f82c13b48303f83` |

Same debug keystore on both (`CN=Android Debug`, SHA-256 `58231498…`), no
permissions, drawer label "QA Fixture". The two versionCodes exercise the
in-place-update path; identical label keeps the alphabetical drawer slot
stable (between Photos and Safety on the seeded AVD).

**Defect 1 repro — drawer drag-pin is a silent no-op** (clean seeded AVD,
Vesna foreground, `adb -s <serial>` pinned throughout):

1. `adb install fixture-v1.apk`; verify `pm list packages dev.vladon.qa`.
2. Open the drawer: swipe up mid-screen (scripted `input swipe 540 1500 540 600 350`).
   **Gate every gesture on proof the drawer is open** (uiautomator dump contains
   the item, or screenshot) — a drag that starts on the workspace behind a
   closed drawer will *move whatever icon sits under the start coordinates*
   (observed during this run: Safety moved from cell 9 to 14; repaired by drag).
3. Isolate the item first to make coordinates deterministic: tap the search
   field, `input text "QA"`, dismiss the keyboard (`input keyevent KEYCODE_BACK`,
   verify `dumpsys input_method` `mInputShown=false` — an up keyboard gets
   hidden by the long-press handler and the resulting relayout cancels the
   in-flight gesture), and re-dump: the sole item renders at
   `[15,457][255,500]`; press its icon at **(135, 420)**.
4. One continuous hold-drag to a verified-empty cell (cells 14–19 are free on
   the seeded layout; cell 14 center ≈ (668, 1691)): `input draganddrop 135 420 668 1691 2000`.
5. **Fixed build expectation:** the fixture appears at that cell; a read-only
   snapshot (`adb exec-out run-as dev.vladon.vesna.launcher cat` of
   `vesna-launcher.db` + `-wal` + `-shm`, compared row-by-row) gains a
   `WORKSPACE` row `packageName='dev.vladon.qa.lifecyclefixture'` at the
   dropped `cellIndex`. **0.4.1 actual (bug):** drawer closes, no icon, no
   error, package row count still 0. Also re-verify the drawer→dock drop
   (same drag, drop over the dock zone): expected `insertIntoDock` (dock must
   have a free slot — it is full at 4/4 on the seeded layout, so free slot 3
   first via the dock item menu "Remove from dock", which works); 0.4.1
   actual is the same silent no-op.

**Defect 2 repro — drawer item menu opens as a zero-height popup:**

1. Same drawer + search-isolation + keyboard-down state as above.
2. Long-press the item: scripted `input motionevent DOWN 135 420`, sleep ≥1 s,
   `input motionevent UP 135 420` (physical long-press reproduces identically).
3. **Fixed build expectation:** a visible popup anchored at the item with
   "Add to workspace" / "Add to dock" / "App info", each tappable. **0.4.1
   actual (bug):** no visible menu; `adb shell dumpsys window windows` shows
   `PopupWindow:<hash>` owned by `dev.vladon.vesna.launcher` with
   `frame=[0,2337][515,2337]` — height 0 at the screen bottom. Root cause:
   `showDrawerItemMenu` anchors `PopupMenu(this, drawer)` to the full-screen
   `DrawerView` instead of the tapped icon view (dock/workspace item menus
   anchor to their icon and render correctly — verified contrast in this run).

**After the fix, finish the deferred placement rows of this item** on the
fixed revision: pin via menu and via drag, dock placement (free slot 3, then
"Add to dock"; re-adding Clock afterwards restores the exact original dock
order), folder (drag the pinned icon onto an occupied cell — the occupant
joins the folder; after uninstall the folder dissolves and the occupant
returns to its own cell, so full cleanup restores the baseline without DB
writes), then the placement branches of update/disable/uninstall with the
same read-only row-comparison assertions.
