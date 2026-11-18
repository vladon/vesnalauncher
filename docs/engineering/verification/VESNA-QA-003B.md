# VESNA-QA-003B — placed-item package lifecycle verification

**Result:** The API 35 run verified drawer-to-workspace and drawer-to-dock
pinning, placement retention across an update, and folder cleanup on uninstall.
Disabling the fixture deleted both top-level placements. Re-enabling it restored
drawer discovery only. The run restored the original layout. Run recorded
2026-11-19.

**Backlog:** [VESNA-QA-003](../BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial).

**Scope:** QA of existing behavior only (install → pin → move → upgrade →
disable/enable → folder → uninstall against a disposable fixture package); no
product change, no research approval claimed, no shared status edited.
**Code under test:** commit `40cb3ee` (`40cb3eed8f5507f7b6065cd2939120ee80d7412e`,
"fix: insert apps dropped from the drawer (#15)"), checked out in the isolated
`qa-placed-app-lifecycle` worktree. The APK was built from this checkout.
**Package under test:** `dev.vladon.vesna.launcher`, `versionName` 0.4.1,
`versionCode` 6.

## Environment

| Property | Value | Source |
|---|---|---|
| Dedicated device | AVD `Vesna_QA_API_35`, adb serial `emulator-5556` | `adb -s emulator-5556 emu avd name` → `Vesna_QA_API_35` |
| OS / API | Android 15, API 35 | `getprop ro.build.version.release/sdk` |
| Build fingerprint | `google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys` | `getprop ro.build.fingerprint` |
| Screen | 1080×2400 | `wm size` |
| Foreground at session start | `com.google.android.apps.nexuslauncher/.NexusLauncherActivity` (window `5f7cc5e`) | `dumpsys window` → restored at the end |
| User | 0 only; no managed profile exercised | every command ran on the default user |

Every adb command in this run pinned the serial (`-s emulator-5556`); no other
device, AVD, or research branch was touched.

## Fixture validation (before any install)

Reusable fixtures from `D:/orca/scratch/vesna-qa-003/` (same package, same
signer, built by the previous session):

| Property | fixture-v1.apk | fixture-v2.apk |
|---|---|---|
| SHA-256 | `f6fb89981616580d8485a728449020fc8236b1386c90901119ec47755eefcebf` | `ce286df06f70b9f5e213a7d7a8032d68df3848520c9e51717f82c13b48303f83` |
| Package | `dev.vladon.qa.lifecyclefixture` | `dev.vladon.qa.lifecyclefixture` |
| versionCode / versionName | 1 / 0.1 | 2 / 0.2 |
| Launchable activity | `.MainActivity` | `.MainActivity` |
| apksigner verify | silent exit 0 (verified) | silent exit 0 (verified) |
| Signer cert SHA-256 | `582314987823bd90e8e66af56e5fa32d95528a368c85d3cbe0095949a80a82e4` (Android Debug) | identical |

Label as discovered by the launcher: **"QA Fixture"**.

## Build and install

```bash
ANDROID_HOME=… MSYS_NO_PATHCONV=1 cmd /c "gradlew.bat assembleDebug --console=plain"
# BUILD SUCCESSFUL in 6s
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk   # Success
adb -s emulator-5556 install -r /d/orca/scratch/vesna-qa-003/fixture-v1.apk # Success
```

Vesna observation enabler used throughout:
`am start -W -n dev.vladon.vesna.launcher/.ui.HomeActivity` (Status ok).

## Initial persisted state (recorded before any change)

`run-as dev.vladon.vesna.launcher sqlite3 -readonly databases/vesna-launcher.db`:

- `workspace_items`: 18 rows — DOCK cells 0–3
  (`calendar`, `camera2`, `chrome`, **`deskclock` at dock slot 3**) and
  WORKSPACE cells 0–13 (the 14 stock apps); **cells 14 and 15 free**; all
  `folderId` NULL; all `userSerial` 0.
- `folders`: empty. Fixture package absent (`pm list packages` confirmed).

## Drag technique (verified)

Drawer/dock/workspace drags were driven as **one continuous injected gesture**:
`input motionevent DOWN <item>` → wait at least 0.9 s for the long-press →
intermediate `MOVE` steps → `UP <target>`. The intercepted movement closes
the drawer. The UP coordinates select the drop cell or slot.

The run used live `uiautomator dump` XML to locate items and predetermined
coordinates for the empty workspace cells. Text bounds and icon touch
coordinates refer to different parts of an item:

| Target | Node bounds | Center used |
|---|---|---|
| QA Fixture in drawer | `[285,1087][525,1130]` | (405,1108) |
| Clock in dock slot 3 | `[820,2269][1055,2312]` | (937,2222) |
| Cell 14 | given by dispatch | (668,1720) |
| Cell 15 | given by dispatch | (927,1720) |
| Clock in cell 15 | `[823,1625][1032,1668]` | (927,1646) |

## Case 1 — drawer drag onto free cell 14

Fixture visible and unpinned in the drawer; continuous drag (405,1108) →
(668,1720). Drawer closed mid-gesture; drop committed on release.

- Persisted: `21|dev.vladon.qa.lifecyclefixture|WORKSPACE|14|` (id, package,
  container, cellIndex, folderId).
- Visual: fixture label node on workspace at `[564,1625][773,1668]` (cell 14);
  no "Search apps" node (drawer closed).
- **PASS** — the #15 fix inserts drawer-dropped apps into the grid.

## Case 2 — Clock dock→cell 15, fixture drawer→dock slot 3

1. Continuous drag Clock (937,2222) → cell 15 (927,1720):
   `com.google.android.deskclock|WORKSPACE|15`; fixture row unchanged
   (WORKSPACE 14).
2. Continuous drag fixture drawer (405,1108) → dock (937,2222): new row
   `dev.vladon.qa.lifecyclefixture|DOCK|3` while the workspace placement
   **stayed** (`WORKSPACE|14`) — a drawer drag pins a second placement.

Final dock order: `calendar 0, camera2 1, chrome 2, lifecyclefixture 3`.
Visual: dock label bounds `[820,2269][1055,2312]` = QA Fixture; Clock label at
cell 15 `[823,1625][1032,1668]`. **PASS** — both rows verified.

## Case 3 — in-place upgrade to v2 (`install -r`)

```bash
adb -s emulator-5556 install -r /d/orca/scratch/vesna-qa-003/fixture-v2.apk  # Success
dumpsys package dev.vladon.qa.lifecyclefixture   # versionCode=2 versionName=0.2
```

Diff/metadata: same package + same launchable component (`MainActivity`),
versionCode 1→2, versionName 0.1→0.2, identical signer.

Persisted placements **preserved exactly**:
`MainActivity|WORKSPACE|14` and `MainActivity|DOCK|3`. Drawer still lists
"QA Fixture" (the `onPackageChanged` → Replaced → `refresh()` pass re-discovers
v2). **PASS**.

## Case 4 — disable and re-enable (fixture only)

`pm disable dev.vladon.qa.lifecyclefixture` on API 35 fails for shell with
`SecurityException: Shell cannot change component state for null to 2`; the
run used `pm disable-user --user 0`:

```bash
adb -s emulator-5556 shell "pm disable-user --user 0 dev.vladon.qa.lifecyclefixture"
# Package dev.vladon.qa.lifecyclefixture new state: disabled-user (enabled=3)
```

Observed after disable (2.5 s settle): **drawer 0 "QA Fixture" nodes; BOTH
placement rows deleted** (`COUNT(*) = 0`; dock collapsed to calendar/camera/
chrome). Mechanism: `LauncherApps` excludes the disabled activity from
discovery, the Replaced event triggers `refresh()`, and `pruneStale` removes
the rows.

After `pm enable` (enabled=1): drawer lists "QA Fixture" again (1 node), but
**rows remain COUNT = 0**.

**Observed behavior:** Re-enabling the app restored drawer discovery. The
workspace and dock placements had to be added again. The desired retention
policy remains open.

## Case 5 — two-member folder with a stock icon

Fixture repinned to cell 14 via the Case-1 drawer drag
(`lifecyclefixture|WORKSPACE|14`), then a **top-level workspace drag** moved
Clock (cell 15, (927,1646)) onto the fixture (cell 14, (668,1646)). Drop onto
an occupant created a folder:

- `folders`: `1|Folder|14` (default title "Folder", cellIndex 14).
- `workspace_items`: `deskclock|WORKSPACE|15|folderId=1` and
  `lifecyclefixture|WORKSPACE|14|folderId=1`.
- Visual: "Folder" label node at cell 14 `[564,1625][773,1668]`.

**Record taken immediately before uninstall:**

| Dock | Slot | Folder id 1 "Folder" @ cell 14 |
|---|---|---|
| calendar | 0 | `com.google.android.deskclock` |
| camera2 | 1 | `dev.vladon.qa.lifecyclefixture` |
| chrome | 2 | |

**PASS** — two members (stock icon + fixture) in one folder.

## Teardown — uninstall, stale-data clearing, restoration

```bash
adb -s emulator-5556 shell "pm uninstall dev.vladon.qa.lifecyclefixture"  # Success
```

Observed ~3 s after the uninstall (PackageRemoved → `removeAllForPackage` →
`refresh()` → `pruneStale` → folder normalization):

- Fixture rows: **0**; folders: **0** — the singleton folder **dissolved** and
  its sole remaining member returned to the folder's cell:
  `deskclock|WORKSPACE|14|folderId=NULL`. Dock unchanged (3 apps).
- Drawer: no fixture (package uninstalled).

Force-stop + relaunch persistence:

```bash
adb -s emulator-5556 shell "am force-stop dev.vladon.vesna.launcher; sleep 1; \
am start -W -n dev.vladon.vesna.launcher/.ui.HomeActivity"   # Status ok
```

Rows stable after relaunch: fixture COUNT 0, `deskclock|WORKSPACE|14`.

Final restoration drag Clock (cell 14, (668,1646)) → dock slot 3 (937,2222),
then the persisted placement data matches the initial baseline:

- DOCK `calendar 0, camera2 1, chrome 2, deskclock 3` (Clock label back at
  `[820,2269][1055,2312]`); WORKSPACE cells 0–13 = the original 14 stock apps;
  cells 14/15 free; `folders` empty; no fixture rows. (Row ids differ from the
  baseline because of AUTOINCREMENT; placement rows — the comparison target —
  are identical.)
- Original foreground restored: `mCurrentFocus=NexusLauncherActivity`
  (window `5f7cc5e`, as at session start). The run issued no HOME role changes.

## Gaps and limitations

- **Plain `pm disable` is not available to shell on API 35** (SecurityException
  quoted above). Case 4 covers `pm disable-user --user 0`.
- This uninstall removed the fixture from a folder after disable had removed
  its top-level pins. Uninstall with simultaneous workspace, dock, and folder
  placements remains unverified in this run.
- Work-profile behavior (`LauncherApps` multi-profile paths) was not exercised:
  only user 0 exists on this AVD, per dispatch.
- `getprop ro.product.avd.name` is empty on this image; device identity was
  established with `adb emu avd name` instead.

## Hygiene

No DB edits (all queries `run-as … sqlite3 -readonly`), no app data clears, no
SDK/system changes, no production source files touched, no raw evidence store
access, no GitHub writes. Transient UI dumps were deleted from the worktree.
