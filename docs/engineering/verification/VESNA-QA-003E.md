# VESNA-QA-003E — Add to dock ignores hidden dock folder members (VESNA-BUG-003)

## Verdict

**PASS (fix verified).** The bug reproduced exactly as reported on baseline `1f138ab`:
with Clock persisted as `DOCK|3` + `folderId` set (member of a workspace folder), the
dock rendered three icons (Calendar/Camera/Chrome) yet the drawer context-menu
**Add to dock** was rejected with "The dock is full". After the one-line
`WorkspaceRepository.addToDock` fix the same replay accepts the request, appends a
top-level row at slot 3, keeps Clock inside the folder, and a genuinely full
four-slot dock still rejects. The final placement data matches the stock
layout captured before the run.

## Item

VESNA-BUG-003 — "a dock-origin folder member is still counted by the context-menu
Add to dock path" (QA-003D observation: Clock persisted as `DOCK|3` with folderId
set while only Calendar/Camera/Chrome were visible).

## Code under test

| Revision | SHA / artifact | SHA-256 |
|---|---|---|
| Baseline build | `1f138ab` (main tip after PR #19), clean tree | `9d54d0f900c1966a415a6d2432bd5bd9e48ba6f86fcb3009d48279251d8fec75` |
| Fixed build | branch `vladon/fix-dock-folder-capacity` = `1f138ab` + `WorkspaceRepository.addToDock` patch (+ new androidTest) | `c34ec92b97ffc926c6c9952edb133471bd2c469b6883552a8827ca83632e8e87` |

Both `versionName=0.4.1, versionCode=6`. Fixture: `dev.vladon.qa.lifecyclefixture`
v1, SHA-256 `f6fb89981616580d8485a728449020fc8236b1386c90901119ec47755eefcebf`.

## Environment

- `emulator-5556`, AVD `Vesna_QA_API_35`, API 35, 1080x2400, user 0 only.
- Start state: 18 stock rows (dock calendar0/camera1/chrome2/deskclock3; workspace
  cells 0–13; 14/15 free), fixture absent, HOME role without holders (recorded, not
  "fixed"), foreground restored to NexusLauncher at the end.

## Baseline reproduction (attempt 1 of 2, succeeded)

1. Install baseline APK (`install -r`), install fixture v1 → DB stays 18 rows.
2. Drawer drag QA Fixture (405,1108) → cell 14 (668,1720): `WORKSPACE|14|lifecyclefixture`.
3. Drag Clock from dock slot 3 (937,2290) onto the fixture icon (668,1720):
   folder `id=3 "Folder" @14`; Clock row `DOCK|3 folderId=3`; fixture row
   `WORKSPACE|14 folderId=3`. UI dock renders **Calendar/Camera/Chrome only**.
4. Drawer long-press QA Fixture → **Add to dock** (527,1333):
   - Toast **"The dock is full"** captured on screen
     (`05-screen-after-addtodock.png`).
   - DB (`05-db-after-addtodock-rejected.txt`): dock rows unchanged
     (calendar0/camera1/chrome2 + Clock `DOCK|3 folderId=3`); **no new top-level
     row**; folder intact.

| Assertion | Result |
|---|---|
| Apparent free dock slot visible (3 rendered of capacity 4) | ✅ |
| Clock persisted as `DOCK|3` with folderId set | ✅ |
| Add to dock rejected despite free slot (toast + no new row) | ✅ |

## Patch

`app/src/main/java/.../workspace/WorkspaceRepository.kt` — `addToDock` now filters
`&& it.folderId == null` for **both** capacity and append order, matching
`insertIntoDock`, `moveWorkspaceToDock` and `WorkspaceResolver.dockApps`
(rendering). Folder rows untouched; append semantics (max top-level cellIndex + 1)
and mutation mutex preserved. Diff: +8/−2 lines, one function.

Regression `app/src/androidTest/.../workspace/WorkspaceRepositoryDockFolderTest.kt`
(isolated in-memory Room DB, unique SharedPreferences per run; no dependency
changes): (a) 3 top-level + 1 folder-member DOCK row → `addToDock` returns true and
appends top-level at cellIndex 3 while the member row is untouched; (b) 4 top-level
entries → false, row count unchanged.

## Builds

`assembleDebug`, `testDebugUnitTest`, `lint`, `compileDebugAndroidTestSources` —
**BUILD SUCCESSFUL** (Gradle 9.6.0 wrapper, `cmd /c gradlew.bat`). Targeted
instrumentation run on emulator-5556 only (`ANDROID_SERIAL=emulator-5556` +
`-Pandroid.testInstrumentationRunnerArguments.class=…WorkspaceRepositoryDockFolderTest`):
**2 tests, 0 failures, 0 errors** (`12-instrumentation-result.xml`).

## Fixed replay (independent, same emulator, restored-stock start)

1. Fixed APK `install -r`, fixture v1 reinstalled (18 stock rows confirmed), full
   replay: pin → cell 14 → Clock drag → folder `id=4` (Clock `DOCK|3 folderId=4`,
   fixture `WORKSPACE|14 folderId=4`), dock renders 3 icons.
2. Drawer long-press QA Fixture → **Add to dock**:
   - **Accepted.** New row `29|DOCK|3|lifecyclefixture|` (folderId **null**);
     Clock member row untouched; folder intact (`08-fixed-db-after-addtodock.txt`).
   - Dock renders **Calendar/Camera/Chrome/QA Fixture** (slot 3 visible,
     `08-fixed-ui-dock4.xml`); no rejection toast on the capture frame.

| Assertion | Result |
|---|---|
| Top-level dock slot 3 appears (`DOCK|3`, folderId null, rendered) | ✅ |
| Clock still in folder (`DOCK|3 folderId=4`, folder contents intact) | ✅ |
| Genuinely full dock (4 top-level) rejects: tap Add to dock on Clock → toast "The dock is full", 20 rows unchanged | ✅ (`09-full-dock-rejection-toast.png`, `09-full-dock-db.txt`) |

## Restore

- Uninstall fixture only → folder dissolves, Clock returns to `WORKSPACE|14`
  (container rewritten DOCK→WORKSPACE by normalization, as documented in QA-003B).
- Drag Clock (668,1646) → dock slot 3 (937,2290): `DOCK|3`.
- Final DB sorted-set diff vs baseline: **identical**
  (`11-db-baseline-sorted.txt` vs `11-db-final-sorted.txt`); 18 rows, 0 folders.
- Fixture uninstalled (`pm list` clean); HOME role still has no holders
  (`11-role-home.txt`); foreground returned to NexusLauncherActivity
  (`11-foreground-final.txt`).
- The test runner later removed the Vesna package from this AVD. The
  coordinator reinstalled the baseline APK, compared all 18 seeded placement
  rows to the pre-run snapshot, and restored the empty HOME holder and
  NexusLauncher foreground (`13-after-runner-restoration.txt`).

## Operational findings

1. **BACK on Vesna home exits the activity** (known QA-003 trap, recurred): after
   the first fixture drop a BACK press sent the Pixel launcher foreground; the pin
   itself had committed. Recovered with `am start -W …/.ui.HomeActivity`; no crash
   (`03-logcat-exit-check.txt`).
2. **`install -r` of the launcher puts it in the stopped state**: manifest
   broadcasts (PACKAGE_REMOVED) are not delivered until the next explicit launch.
   Fixture uninstall cleanup therefore ran on relaunch (`pruneStale` +
   folder normalization), not at uninstall time. QA loops should `am start` the
   launcher after any `install -r` before asserting DB state.
3. **Correction, 2026-11-19.** An unscoped `connectedDebugAndroidTest`
   invocation selected six connected targets. Later package-manager logs
   confirm that it installed and removed the Vesna app and test APKs on two
   off-target development AVDs, `emulator-5558` and `emulator-5560`. Their
   logs include app-data clearing events. The coordinator reinstalled the
   prior debug APKs and restored the documented placement layouts (18 and
   19 rows, respectively) and HOME states by comparing them with the
   pre-run snapshots. Recovery of other removed app data remains unverified.
   The host run also listed a phone and the separate research emulator as
   targets. Available host logs do not establish what ran on either.
4. The retry selected only `emulator-5556` with `ANDROID_SERIAL` and
   produced the two-passing test report. In a separate planned run on
   `emulator-5560`, the coordinator used serial-pinned adb instrumentation.
   One test failed as expected on the baseline, and both cases passed on
   the patch. For execution on a shared host, follow the serial-pinned
   installation and test procedure in [Development](../../DEVELOPMENT.md#ci-quality-gates).

## Limitations

- Scope limited to the context-menu **Add to dock** path per the task; no
  other dock actions were outside the regression scope. Teardown used
  `moveWorkspaceToDock` to restore Clock.
- The persisted non-canonical `DOCK`+folderId member row itself (QA-003D's
  normalization gap) is existing behavior and was deliberately preserved.
- JVM/lint/instrumentation results cover the patch; no performance or multi-user
  (managed profile) dimension was tested.

## Evidence inventory (qa-evidence/qa003e/)

`00-db-baseline.txt`, `00-fixture-sha.txt`, `02-drawer.xml`,
`04-db-folder-created.txt`, `04-ui-folder-state.xml`, `04-screen-dock-3icons.png`,
`05-drawer-with-folder-state.xml`, `05-menu.xml`,
`05-screen-after-addtodock.png`, `05-db-after-addtodock-rejected.txt`,
`03-logcat-exit-check.txt`, `06-db-after-relaunch.txt`, `06-db-stock-restored.txt`,
`06-ui-stock-dock.xml`, `07-fixed-*` (drawer/home/folder DB+UI),
`08-fixed-drawer.xml`, `08-fixed-menu.xml`, `08-fixed-screen-after-addtodock.png`,
`08-fixed-db-after-addtodock.txt`, `08-fixed-ui-dock4.xml`,
`09-fulltest-drawer.xml`, `09-fulltest-menu*.xml`,
`09-full-dock-rejection*.png`, `09-full-dock-db.txt`,
`10-db-after-final-uninstall.txt`, `10-ui-clock-workspace-final.xml`,
`10-db-final-restored.txt`, `11-ui-final.xml`, `11-screen-final-restored.png`,
`11-db-baseline-sorted.txt`, `11-db-final-sorted.txt`, `11-role-home.txt`,
`11-foreground-final.txt`, `12-instrumentation-result.xml`, `apk-sha256.txt`,
`vesna-launcher-1f138ab-baseline.apk`,
`vesna-launcher-fix-dock-folder-capacity-fixed.apk`.
