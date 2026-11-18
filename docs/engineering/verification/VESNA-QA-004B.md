# VESNA-QA-004B — workspace persistence across process death and full guest reboot

**Verdict:** PASS — layout persistence holds across force-stop and a full guest
reboot, and an intentionally emptied workspace is **not** re-seeded (process
death and reboot). Run recorded 2026-11-19.

**Item:** [VESNA-QA-004 in BACKLOG.md](../BACKLOG.md#vesna-qa-004-migration-and-restart-verification-done)
(P0, done) — the two persistence bullets: "Process death: force-stop and
relaunch; layout identical" and "Reboot: restart the device; layout identical,
no re-seed of an emptied workspace". The Room-migration bullet is part A and is
**not covered here** (QA-004B = persistence only); it is device-verified
separately by `RoomMigrationQaTest`
([PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`)
on a dedicated API 35 AVD. Part A and this record are distinct experiments; no
single migration-then-reboot run is claimed. No product change was made and
none is required: the tested behavior passed as-is.

**Code under test:** commit `0c7269c` (`0c7269c1672158ba8b95bf829958da5c293ae86b`,
"ci: compile Android instrumentation tests (#12)"), branch
`vladon/qa-workspace-reboot` (isolated worktree `qa-workspace-reboot`) — the
worktree's branch tip; `main` has since advanced past the branch point
(`1f138ab`, #19), so `0c7269c` is **not** current main.
APK `app-debug.apk` built from that checkout, sha256 `94277eb4…`.
**Package under test:** `dev.vladon.vesna.launcher`, `versionName` 0.4.1,
`versionCode` 6. This consolidated copy of the record lives in the
`qa004-verification-record` worktree at `1f138ab`; the tested build identities
are unchanged.

**Restoration-only second revision:** the drawer-drop insert bug (fixed in
PR #15, merged `40cb3ee`) exists in `0c7269c`, so restoring the seeded layout
via drawer drags was impossible on the tested build. For the restore step only
the debug APK from the `fix-drawer-drop-routing` worktree (production tree of
merged `40cb3ee`, same `versionName` 0.4.1 / `versionCode` 6 / schema /
debug signer, sha256 `89d6dde2…`) was installed with `install -r`. All
persistence evidence above was captured **before** that install, on `0c7269c`.
After restoration the original `0c7269c` build was reinstalled with
`install -r` (data preserved) and the baseline equality re-verified on it, so
the AVD's end-state revision matches its pre-run revision.

## Environment

| Property | Value | Source |
| --- | --- | --- |
| Device | `emulator-5558` = **Vesna_QA_Persistence_API_35** (re-resolved via `emu avd name` before every phase, incl. after each reboot) | adb |
| OS | Android 15 (SDK 35), userdebug | `ro.build.version.sdk` |
| Build fingerprint | `google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys` (identical before and after reboot) | `ro.build.fingerprint` |
| Screen | 1080×2400 @ 420 dpi | `wm size` / `wm density` |
| Locale | en-US | `ro.product.locale` |
| Accounts | 0 (no user data on the AVD) | `dumpsys account` |
| HOME role holder | **none** (baseline; matches VESNA-QA-002) | `cmd role get-role-holders android.app.role.HOME` |
| Pre-run foreground | system **"Select a Home app"** `ResolverActivity` (leftover picker over an empty no-home backdrop) | `dumpsys window` + screenshot |

Every adb command pinned `-s emulator-5558`. QA-003's `emulator-5556`, the
research AVD and both phones were never touched.

## Pre-run baseline (captured before any change)

- DB `vesna-launcher.db` (Room v3): 18 rows in `workspace_items`, 0 in
  `folders`. Dock: Calendar(0), Camera(1), Chrome(2), Clock(3). Grid cells
  0–13: Contacts, Drive, Files, Gmail, Google, Maps, Messages, Phone, Photos,
  Safety, Settings, Voice Search, YouTube, YT Music; cells 14–19 free.
- `shared_prefs/vesna_launcher.xml`: `workspace_seeded=true`.
- Baseline files archived read-only: `00-db-baseline-items.txt`,
  `00-prefs.txt`, `db-archive-prerun/` (db+wal+shm copies; WAL-mode caveat
  noted below).

## Build and install

`./gradlew :app:assembleDebug` (wrapper `gradlew-9.6.0`; the checked-in
`gradlew` shell script has CRLF endings — run via an LF-normalized copy or
`gradlew.bat`), then `adb install -r` **only** — no data clear at any point.
Post-install: `versionCode=6`, `versionName=0.4.1`, `installerPackageName=null`,
item count still 18 (in-place replace preserved data).

## Procedure and observations

### 1. Distinctive UI moves (evidence `01`–`02`)

Two reversible drags via `adb shell input draganddrop x1 y1 x2 y2 2500`:

| Move | From | To | DB row after |
| --- | --- | --- | --- |
| Safety | WORKSPACE cell 9 (row 2, col 1) | cell 15 (row 3, col 3) | `WORKSPACE\|15` |
| Clock | DOCK slot 3 | cell 14 (row 3, col 2) | `WORKSPACE\|14` (container change) |

Both verified in the DB (`sqlite3 -readonly` through `run-as`) and in
uiautomator dumps (icon bounds moved to the new cells; dock shows 3 items,
cell 9 empty). No folder was created (drops landed on empty cells only).

### 2. Process death: force-stop and relaunch (evidence `03`–`05`)

- `pidof` before: **4643** → `am force-stop` → `pidof`: **empty**.
- Relaunch (`am start -n dev.vladon.vesna.launcher/.ui.HomeActivity`): new pid
  **4806**.
- DB after relaunch: identical to the post-move snapshot (Safety `WORKSPACE|15`,
  Clock `WORKSPACE|14`, dock 3). UI dump: all 18 icons at identical bounds.
- Note: `ps -A` also lists `dev.vladon.vesna.launcher.test` (the
  instrumentation APK from an earlier session, pid 2567) — a *separate*
  process; `force-stop` of the main package does not kill it and `pidof` on
  the main package is the authoritative exit proof.

### 3. Full guest reboot (evidence `06`–`10`)

- Boot id before: `a1bffea1-b1e3-40c1-802d-c1d1b39a5a73`;
  `adb reboot` → `sys.boot_completed=1` after ~30 s.
- Identity re-verified post-reboot: `emu avd name` =
  `Vesna_QA_Persistence_API_35`, fingerprint unchanged, Vesna still installed.
- Boot id after: `a9d6da…` (`a9d6a7da-0500-4adb-9d3a-d5517eebaa54`) — new boot
  proven. Keyguard: `deviceLocked=0` (no unlock needed on this AVD).
- Relaunch (new pid 3028): DB identical to post-move snapshot; uiautomator
  dump with **identical XML bounds** to the pre-reboot dump (18 icons, same
  bounds). The screenshot
  confirms placement (Clock and Safety in row 3, dock 3 items).

### 4. Restore of the moved layout via UI (evidence `11`–`12`)

Reverse drags (Safety 15→9, Clock 14→dock slot 3) on `0c7269c`. Normalized DB
diff against the baseline: **IDENTICAL** (`base.norm2` vs `rest.norm2`).

### 5. No re-seed of an intentionally emptied workspace (evidence `13`–`18`)

Assessed against the "reversible via UI, no user data" gate: the layout holds
only seeded stock apps (0 accounts), and the drawer offers UI paths to re-add
apps — reversible. Proceeded on the dedicated AVD.

- All 18 items removed **via UI only**: per item, long-press without movement
  (`input motionevent DOWN` → hold ~1 s → `input motionevent UP` on the icon)
  opens the context menu → tap "Remove from workspace" / "Remove from dock".
  18/18 removed, DB: items 0, folders 0; `workspace_seeded` **stays true**;
  screen shows only the "Long-press here for options" empty hint (`16`).
- Force-stop → relaunch: items still **0** (pid before 3028, after stop empty,
  relaunch 7849) — no re-seed after process death.
- Second full reboot: boot id `a9d6a7da…` → `154b6e02-f716-4f39-9e65-3521db1858fa`,
  `emu avd name` re-verified, keyguard unlocked, relaunch: items **still 0**,
  folders 0, flag still true, empty workspace on screen (`18`) — **no re-seed
  after reboot**.

Code corroboration: seeding is gated by the `workspace_seeded` SharedPreferences
flag, not DB emptiness (`WorkspaceRepository.isSeeded()` / `KEY_SEEDED`;
`LauncherController.refresh()` seeds only `!isSeeded() && isEmpty()`), so a
post-seed emptied workspace can never re-seed. This run verified that
empirically at both process-death and reboot levels.

### 6. Restore of the seeded layout via drawer drags (evidence `19`–`22`)

On `0c7269c`, drawer drags commit nothing (drawer closes mid-gesture, DB count
stays 0 — the pre-#15 drawer-drop insert bug, confirmed live and by the
coordinator). With the restoration-only build installed, 16/18 items restored
by drawer drags (`draganddrop` → empty grid cells in ascending cellIndex order
and dock targets). Two lookup races (Calendar, YT Music) and four misplaced
dock drops were repaired manually — details in "Operational findings". Final
DB diff vs baseline: **IDENTICAL** (`27-final-db.txt` / `final2.norm`).
The original `0c7269c` build was then reinstalled (`install -r`, data
preserved) and the equality re-verified: 18 items, **STILL == BASELINE**.

### End state (cleanup)

- Workspace layout == pre-run baseline (exact container/cellIndex/package set;
  Room ids are regenerated and not part of the invariant).
- Installed revision = `0c7269c` build (as found), 0.4.1/6, no data ever
  cleared, no direct DB writes at any point.
- HOME role holder: **none** (never changed during the run; re-verified).
- Foreground: HOME key pressed → the pre-run **"Select a Home app"**
  `ResolverActivity` is back in focus (`29-final-foreground.png`).
- No system app removed, no other device change; QA-003's AVD untouched.

## Verdict per acceptance bullet

| # | Assertion | Result | Evidence |
| --- | --- | --- | --- |
| 1 | Force-stop → relaunch: layout identical (normalized DB set + XML bounds) | **PASS** | `03`–`05` |
| 2 | Reboot → layout identical (identity, new boot id re-verified; normalized DB set + XML bounds) | **PASS** | `06`–`10` |
| 3 | Emptied workspace → no auto re-seed (process death + reboot) | **PASS** | `13`–`18` + code path |
| — | (Restore of the test layout itself) | done on the fix build; see revisions note | `19`–`27` |

## Operational findings

1. **Drawer long-press menu renders invisible (0c7269c) — fixed in merged
   PR #18.** Long-press without movement on a drawer item creates the popup
   menu window but it measures **zero height** and nothing is visible
   (`PopupMenu(this, drawer)` anchored to the full-height drawer; logcat
   `AccessibilityNodeInfoDumper: Skipping invisible child … Rect(0, 2337 -
   515, 2337)`; degenerate 425-byte dumps `21-drawer-menu-*.xml`).
   Workspace-icon menus anchored to small icon views work (the remove loop
   used them 18×). Superseded for drops by the #15 fix build. On merged trunk
   the menu half of this finding is **fixed** by
   [PR #18](https://github.com/vladon/vesnalauncher/pull/18) (`bb8c10d`,
   "fix: show the drawer item menu"): re-verified on an independent API 35
   AVD, the drawer long-press menu now measures **378 px** tall (was 0 px)
   and its workspace actions are tappable. This finding therefore describes
   the `0c7269c` build only.
2. **Dock/workspace touch overlap on this AVD (scripted-input observation).**
   On `Vesna_QA_Persistence_API_35` (1080×2400 @ 420 dpi), scripted dock drops
   at y=2222 (dock-icon centers) silently map to grid row 4 (cells 16–19 —
   the row the dock overlays); workspace→dock and drawer→dock drags need a
   drop point at y≈2320+. This is an observation of this AVD's screen
   geometry with scripted input coordinates (y values recorded so future
   scripted QA on the same AVD doesn't misplace dock items); it is not a
   claim of a build-to-build behavioral difference and says nothing about
   physical-device behavior.
3. **BACK exits the launcher when no HOME default is set.** BACK with the
   drawer closed finishes `HomeActivity`; with no HOME role holder the system
   then summons the "Select a Home app" resolver. A QA loop that presses BACK
   on the launcher loses the surface (cost three loop runs here).
4. **Gesture mechanics (API 35 emulator).** `input draganddrop` works for icon
   drags (includes the long-press dwell). A zero-length `input swipe x y x y
   900` triggers *nothing* (no pickup, no menu, no click). Menu flows need
   `input motionevent DOWN` → hold ≥1 s → `input motionevent UP` (the
   moved=false path shows the context menu).

## Notes and limitations

- Scripted `adb input` run on one AVD, one user (serial 0), one profile; no
  manual finger. Row `id` values are regenerated by the re-add path; the
  verified invariant is the `(container, cellIndex, packageName)` set
  (folders empty throughout).
- DB reads went through `run-as … sqlite3 -readonly` (device `sqlite3`
  present). File copies in `db-archive-prerun/` were taken while the app was
  stopped (Vesna was not running), but WAL implies they are a best-effort
  forensic archive, not a transactional snapshot; the `-readonly` queries are
  the authoritative layout evidence.
- The `workspace_seeded` flag and 0-account state make the empty-workspace
  sub-check user-data-free; it was run only on this dedicated, owned AVD and
  was UI-reversible (drawer re-add), per the backlog gate.
- Windows/MSYS tooling notes for reproducibility: `gradlew` is CRLF (use an
  LF copy placed in the repo root so the wrapper resolves, or `gradlew.bat`);
  `sh script.sh` path-converts `/sdcard/...` (set `MSYS_NO_PATHCONV=1`);
  Windows `python` emits `\r\n` on pipes (strip before `$`-anchored greps) and
  rejects MSYS-style paths (pass `cygpath -w` output).

## Evidence inventory (outside Git: `D:/orca/workspaces/vesnalauncher/qa-evidence/qa004b/`)

| File(s) | Proves |
| --- | --- |
| `00-prerun-probes.txt`, `00-prerun-foreground.*`, `00-prerun-behind.*` | pre-run HOME/foreground fixture, identity, resolver picker |
| `00-db-baseline-items.txt`, `00-prefs.txt`, `db-archive-prerun/` | baseline layout, seeded flag |
| `01-movesrc.*`, `02-movedst.*` | UI moves (before/after dumps + screenshots) |
| `03-forcestop-proof.txt`, `04-post-forcestop-db.txt`, `05-after-forcestop.xml` | process exit (pids) and identical layout after relaunch |
| `06-reboot-proof.txt`, `08-postreboot-db.txt`, `09-postreboot.xml`, `10-postreboot-workspace.png` | reboot identity, new boot id, layout equality (normalized DB set + XML bounds) |
| `11-restored-db.txt`, `base.norm2`, `12-restored-workspace.png` | baseline restoration after moves |
| `13-empty-workspace-log.txt`, `16-empty-workspace.png`, `17-empty-state-proof.txt`, `18-empty-after-reboot.png` | UI emptying, flag state, no re-seed after force-stop and reboot |
| `19-restore-drawer-log.txt`, `20-drawer-debug.*`, `21-drawer-menu-*.png/xml`, `23-drawer-*.png` | drawer-menu invisible-popup finding; drawer-drop no-op on 0c7269c |
| `22-restored-drag-db.txt`, `24`–`27`, `final*.norm` | fix-build restoration, per-drop counts, final == baseline |
| `28-restored-workspace.png`, `29-final-foreground.png` | end state: baseline layout, resolver foreground back |
