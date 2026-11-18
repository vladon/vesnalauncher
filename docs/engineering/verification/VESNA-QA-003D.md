# VESNA-QA-003D — simultaneous personal-profile uninstall verification

**Result:** The fixture had three simultaneous placements in one workspace
cell, one dock slot, and a two-member folder with Clock. Within three seconds
of uninstall, all three fixture rows were gone, the folder dissolved, and
Clock occupied the former folder cell. A cold relaunch showed no ghost
entries. The run restored the original layout. Recorded 2026-11-19.

**Backlog:** [VESNA-QA-003](../BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial).

**Scope:** QA of existing behavior only (uninstall with simultaneous
multi-placement in the personal profile, user 0); no product change, no
research approval claimed, no shared status edited. The procedure follows the
verified [VESNA-QA-003B](VESNA-QA-003B.md) run (drag technique, DB probes,
restore semantics).
**Code under test:** `main` at commit `bb8c10d`
(`bb8c10db2ebf334a9839aca1c1ad0c6c0380fbf5`, "fix: show the drawer item menu
(#18)"). The APK was built from an unmodified `git archive` export of
this commit.
**Package under test:** `dev.vladon.vesna.launcher`, `versionName` 0.4.1,
`versionCode` 6; installed APK SHA-256
`9d54d0f900c1966a415a6d2432bd5bd9e48ba6f86fcb3009d48279251d8fec75`.

## Environment

| Property | Value | Source |
|---|---|---|
| Dedicated device | AVD `Vesna_QA_API_35`, adb serial `emulator-5556` | `adb -s emulator-5556 emu avd name` |
| Boot / user | `sys.boot_completed=1`; user 0 only, no managed profile exercised | `getprop`, `am get-current-user` → `0` |
| OS / API | Android 15, API 35 | `getprop ro.build.version.release/sdk` |
| Screen | 1080×2400 | `wm size` |
| Foreground at session start | `com.google.android.apps.nexuslauncher/.NexusLauncherActivity` (window `5f7cc5e`) | `dumpsys window` → restored at the end |
| HOME binding at session start | role manager reports **no explicit HOME holder**; HOME intent resolves to `android/com.android.internal.app.ResolverActivity` (Vesna observed via `am start -W …/.ui.HomeActivity`, as in QA-003B) | `cmd role get-role-holders --user 0 android.app.role.HOME` (empty, rc 0); `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME` |

Fixture v1 pre-checks before any change: package absent
(`pm list packages` grep = 0), `sys.boot_completed=1`, current user `0`.
Raw evidence (dumps, screenshots, DB captures) lives outside Git under
`D:/orca/workspaces/vesnalauncher/qa-evidence/qa003d/`.

Every adb command in this run pinned the serial (`-s emulator-5556`); no other
device, AVD, or branch was touched.

## Fixture validation

`D:/orca/scratch/vesna-qa-003/fixture-v1.apk`, SHA-256
`f6fb89981616580d8485a728449020fc8236b1386c90901119ec47755eefcebf` — identical
to the value recorded in VESNA-QA-003B (package
`dev.vladon.qa.lifecyclefixture`, versionCode 1, versionName 0.1, launchable
`.MainActivity`, label **"QA Fixture"**). v2 was not used in this run.

## Build and install

```bash
git archive bb8c10d | tar -x -C /d/orca/scratch/vesna-qa-003d-build
ANDROID_HOME=… MSYS_NO_PATHCONV=1 cmd /c "gradlew.bat assembleDebug --console=plain"
# BUILD SUCCESSFUL in 7s
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk   # Success
adb -s emulator-5556 install -r <fixture-v1.apk>                            # Success
```

Baseline persisted state before the fixture install (18 rows — DOCK
calendar 0 / camera2 1 / chrome 2 / **deskclock 3**, WORKSPACE cells 0–13,
**14/15 free**, folders empty) was still intact after the launcher
`install -r` (Replaced → `refresh()` → `pruneStale` kept all stock rows:
COUNT 18 / folders 0).

Vesna observation enabler used throughout:
`am start -W -n dev.vladon.vesna.launcher/.ui.HomeActivity` (Status ok).

## Setup — three simultaneous fixture placements

All four drags used the QA-003B continuous-gesture recipe (`DOWN` → ≥0.9 s →
`MOVE` steps → dwell → `UP`, coordinates from live `uiautomator dump`); each
committed on the first attempt.

**1. Drawer → cell 14.** Fixture drawer bounds `[285,1087][525,1130]`
(center 405,1108) → UP (668,1720):
`24|dev.vladon.qa.lifecyclefixture|MainActivity|WORKSPACE|14|`.

**2. Drawer → cell 15 (second workspace pin).** Reopened drawer, same drag to
UP (927,1720): new row
`25|…|WORKSPACE|15|` — **two simultaneous top-level workspace pins**, labels
at `[564,1625][773,1668]` and `[823,1625][1032,1668]`.

**3. Clock (dock slot 3) → onto the cell-14 fixture pin → folder.** UP on the
occupied cell label center (668,1646). A dock→occupied-cell drop **does create
a folder**:

- `folders`: `2|Folder|14` (default title, cellIndex = drop cell).
- `24|…lifecyclefixture|WORKSPACE|14|folderId=2` (member, kept its cell).
- `4|com.google.android.deskclock|DeskClock|**DOCK**|3|folderId=2` — the
  Clock row **kept container `DOCK` and cellIndex 3** and only gained
  `folderId=2` (QA-003B's folder members had both been WORKSPACE rows). The UI
  renders it correctly as a folder member: Clock disappears from the dock
  (dock labels Calendar/Camera/Chrome only) and appears inside the opened
  folder dialog next to QA Fixture.
- The cell-15 pin stayed top-level (`25|…|WORKSPACE|15|folderId=NULL`) —
  the preserved second pin.

**4. Fixture drawer → dock slot 3.** UP (937,2222): row
`26|…|DOCK|3|folderId=NULL`; dock label `QA Fixture` at
`[820,2269][1055,2312]` (Clock's original slot bounds).

## Record taken immediately before uninstall

`run-as dev.vladon.vesna.launcher sqlite3 -readonly databases/vesna-launcher.db`:

| Where | Row(s) |
|---|---|
| Folder id 2 "Folder" @ WORKSPACE cell 14 | `lifecyclefixture WORKSPACE 14 folderId=2` + `deskclock DOCK 3 folderId=2` |
| Top-level workspace pin | `lifecyclefixture WORKSPACE 15 folderId=NULL` |
| Dock slot 3 | `lifecyclefixture DOCK 3 folderId=NULL` |

UI confirmation: Folder label @ cell 14 `[564,1625][773,1668]`; QA Fixture @
cell 15 `[823,1625][1032,1668]`; QA Fixture @ dock `[820,2269][1055,2312]`;
opened folder dialog shows **two members** (Clock + QA Fixture).
**Three fixture rows confirmed in both DB and UI. PASS.**

## Uninstall — simultaneous removal

```bash
adb -s emulator-5556 shell "pm uninstall dev.vladon.qa.lifecyclefixture"  # Success
```

Observed ~3 s after the uninstall (PackageRemoved → `removeAllForPackage` →
`refresh()` → `pruneStale` → folder normalization):

- **All three fixture rows deleted** (COUNT 0). The post-uninstall snapshot
  contained zero fixture rows across the workspace, dock, and folder.
- **Folder dissolved**: `folders` COUNT 0; sole remaining member Clock returned
  to the folder's cell as a top-level item —
  `4|com.google.android.deskclock|DeskClock|**WORKSPACE**|14|folderId=NULL`.
  Note the container changed `DOCK → WORKSPACE` and cellIndex `3 → 14` (the
  folder's cellIndex) during normalization, resolving the DOCK-container
  data shape from setup step 3.
- Dock collapsed to calendar 0 / camera2 1 / chrome 2; UI shows no QA Fixture
  or Folder labels; Clock label at cell 14 `[564,1625][773,1668]`.

Force-stop + relaunch persistence:

```bash
adb -s emulator-5556 shell "am force-stop dev.vladon.vesna.launcher; sleep 1; \
am start -W -n dev.vladon.vesna.launcher/.ui.HomeActivity"   # Status ok, COLD
```

After the COLD restart: fixture COUNT 0, folders COUNT 0, 18 rows total,
0 "QA Fixture" UI nodes, dock Calendar/Camera/Chrome, Clock at cell 14 —
**no ghost entries**. **PASS.**

## Restoration

Continuous drag Clock (cell 14, (668,1646)) → dock slot 3 (937,2222) restored
`4|com.google.android.deskclock|DeskClock|DOCK|3|folderId=NULL` with the Clock
label back at `[820,2269][1055,2312]`. Foreground restored via
`am start -n com.google.android.apps.nexuslauncher/.NexusLauncherActivity`
(`mCurrentFocus` window `5f7cc5e`, same as session start). Comparing the
restored placement data (id column excluded) with the pre-run baseline:
**identical** (18 rows, folders empty, fixture package uninstalled).

## Gaps and limitations

- **Folder-member rows can keep a `DOCK` container.** Dropping a dock item
  onto an occupied workspace cell creates the folder but leaves the member row
  as `DOCK|3|folderId=2` until normalization. Rendering is correct (no dock
  duplicate), and uninstall normalization rewrote the row to
  `WORKSPACE|14|folderId=NULL`, but the stored container/cellIndex of a
  folder member is not canonical while the folder exists. Recorded as
  observed data shape only; no product change claimed.
- **No explicit HOME role holder is bound on this AVD** (role manager empty;
  HOME intent resolves to `ResolverActivity`). Vesna was driven as the
  foreground activity via `am start`, matching QA-003B; default-home prompts
  were left untouched.
- Claims are bounded to this recorded case: one fixture package, user 0,
  personal profile only, single device `emulator-5556`, build `bb8c10d`.

## Hygiene

No DB edits (all queries `run-as … sqlite3 -readonly`), no app data clears, no
SDK/system changes, no production source files touched, no root, no work
profile, no GitHub writes, no commits. Transient UI dumps were pulled into the
out-of-Git evidence directory (`qa-evidence/qa003d/`) and removed from the
device and scratch build tree; the worktree contains only this document.
