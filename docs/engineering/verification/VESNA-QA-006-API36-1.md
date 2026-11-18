# VESNA-QA-006 — Android 16 / API 36.1 smoke verification (AVD Vesna_QA_API_36_1_CLI22)

- Date: 2026-11-19
- Device: `emulator-5564`, AVD `Vesna_QA_API_36_1_CLI22` (serial-pinned throughout; no other device touched)
- Base: `origin/main` @ `332fb03` (`332fb0392b5dcd39c971c7579d3e2ea4f5b41358`, includes #26), branch `vladon/qa-api36-1`, clean tree
- App: Vesna Launcher `0.4.1` debug + androidTest APKs built from the exact checkout (`gradlew.bat assembleDebug assembleDebugAndroidTest`)
- Evidence: `qa-evidence/qa006-api36/` (A/B AVD setup artifacts from parent in the same directory, timestamps 02:15–02:23)

## Controlled AVD recreation (A/B, Windows host)

The preceding API 36.1 AVD (`Vesna_QA_Google_APIs_36_1`, created with SDK
Command-line Tools 19.0, `target=android-0` in its
`incorrect-target.ini`)
booted but crashed repeatedly: `surfaceflinger` SIGABRT in the `RegionSampling`
thread via `mapper.ranchu.so`, abort message
`Assertion failed: !rcEnc->featureInfo()->hasReadColorBufferDma`
(`first-renderer-crash.txt`,
`fallback-renderer-crash.txt` —
recurrent: three SIGABRTs under fresh `surfaceflinger` PIDs at 23:19:53, 23:20:33
and 23:21:10; both captured startup logs show the same gfxstream backend with
SwiftShader GLES/Vulkan).

Recreation, same Windows host and the same system image revision
(`system-images;android-36.1;google_apis;x86_64`, revision 4):

1. The CLI-19.0-created AVD was powered off (its ini carried `target=android-0`);
   on that AVD the stock system compositor (`surfaceflinger`) had restarted
   repeatedly after each RegionSampling SIGABRT.
2. A side-by-side AVD was created with SDK Command-line Tools **22.0**:
   `Vesna_QA_API_36_1_CLI22`, `target=android-36.1`
   (`corrected-target.ini`).
3. Launch flags disabled SwiftShader GLDMA/GLDMA2 exactly as in the preceding
   failed comparator
   (`corrected.stdout.log`:
   `Feature 'GLDMA' (51) is overridden to 'disabled'`,
   `Feature 'GLDMA2' (52) is overridden to 'disabled'`, Emulator 37.2.13).
4. Readiness was accepted only after boot completed and system services were
   stable: 9 samples over ~100 seconds with unchanged SurfaceFlinger (PID 618)
   and system_server (PID 780) and the package service responsive
   (`correct-target-stability.txt`).
5. Stock HOME (Pixel Launcher) was ready and remained the HOME role before any
   Vesna install.

The corrected AVD ran on this Windows host. A separate A/B report documents
a comparable assertion and target-metadata problem on another configuration.
[Runner images issue #14484](https://github.com/actions/runner-images/issues/14484)

## Environment verification

| Check | Expected | Observed |
|---|---|---|
| AVD name | `Vesna_QA_API_36_1_CLI22` | ✔ (`adb emu avd name`) |
| `ro.build.version.sdk_full` | `36.1` | ✔ (`36.1`, `ro.build.version.sdk=36`) |
| Users | user 0 only | ✔ (`UserInfo{0:null:4c13} running`) |
| Baseline HOME role | stock Pixel Launcher (`com.google.android.apps.nexuslauncher`) | ✔ |
| surfaceflinger PID | 618 | ✔ (unchanged across the entire session, incl. after cleanup) |
| system_server PID | 780 | ✔ (unchanged across the entire session) |
| RegionSampling / `mapper.ranchu` abort | none | ✔ none observed; no new tombstones |
| Tombstones | pre-existing only | ✔ single `tombstone_00` from 2026-11-18 23:33, `com.google.android.bluetooth` SIGABRT, predates this run (archived as `tombstone_00-pre-existing-bluetooth.txt`) |

The new AVD (SDK Command-line Tools 22.0, target `android-36.1`, Emulator 37.2.13) booted clean and stayed stable; none of the old AVD's RegionSampling crashes reproduced.

## Runs and revisions

Two instrumentation runs are recorded, each against a different revision of the
same branch; the revision under test is stated per run.

### Run 1 — baseline `332fb03` (existing 5-test suite)

Run: `adb -s emulator-5564 install -r` (app), `install -t` (test APK), then
`adb -s emulator-5564 shell am instrument -w dev.vladon.vesna.launcher.test/androidx.test.runner.AndroidJUnitRunner`

Revision: `332fb0392b5dcd39c971c7579d3e2ea4f5b41358` (`origin/main` @ `332fb03`,
includes #26). APKs built from that exact checkout
(`gradlew.bat assembleDebug assembleDebugAndroidTest`).

Result: **OK (5 tests), 9.357 s, no failures, no runtime errors**
(`instrument-run.txt`).

| Class | Method | Result |
|---|---|---|
| `dev.vladon.vesna.launcher.data.RoomMigrationQaTest` | `migrationFromV1PreservesLayoutAndDerivesDefaults` | pass |
| `dev.vladon.vesna.launcher.data.RoomMigrationQaTest` | `migrationFromV2PreservesUserSerialsAndAddsFolders` | pass |
| `dev.vladon.vesna.launcher.ui.DrawerSearchTest` | `searchNarrowsShowsEmptyStateAndRestore` | pass |
| `dev.vladon.vesna.launcher.workspace.WorkspaceRepositoryDockFolderTest` | `folderMemberDoesNotConsumeCapacityOrAppendOrder` | pass |
| `dev.vladon.vesna.launcher.workspace.WorkspaceRepositoryDockFolderTest` | `fullDockStillRejectsAddToDock` | pass |

Exact method names cross-checked against `@Test` sources (5 total — no other
androidTest methods exist at `332fb03`); see
`qa-evidence/qa006-api36/test-method-names.txt`.

### Run 2 — integrated BUG-005 candidate `d8d6a748` (7 tests)

A separate run by the coordinator, after `d8d6a748152418511a940e0626a349410ba88ae0`
("fix: preserve folder membership on top-level unpin") added
`WorkspaceRepositoryTopLevelRemovalTest` on top of the 5-test suite:

Result: **OK (7 tests), 5.73 s, no failures, no runtime errors**
(`integrated-unpin-instrument.txt`).
All 7 tests passed; stock HOME role, third-party package list, and
SurfaceFlinger/system_server PIDs were verified restored afterwards. This run
exercised the BUG-005 candidate fix — that fix's own verification status is the
coordinator's record, not claimed further here.

## Manual smoke (normal interactions only)

Manual smoke was performed during the Run 1 session (build `332fb03`).

1. **HOME eligibility** — `cmd package query-activities` for `MAIN`/`HOME` lists `dev.vladon.vesna.launcher.ui.HomeActivity` alongside stock launcher (`home-eligible-before.txt`).
2. **Cold start** — `am force-stop` then `am start` of `.ui.HomeActivity`; workspace renders with dock, app grid and the "Set Vesna as your default launcher" banner (`01-cold-start.png`, `02-workspace.png`).
3. **Drawer** — swipe up opens the drawer with search field and all apps including the installed disposable fixture "QA Fixture" (`05-drawer-open.png`, `05-drawer.xml`, fixture bounds `[270,997][540,1207]`). Note: a slow swipe on the workspace is interpreted as long-press and opens the Vesna context menu (`04-context-menu.png`) — the menu itself works.
4. **Tap-to-launch** — tapping "QA Fixture" in the drawer launches `dev.vladon.qa.lifecyclefixture/.MainActivity` (verified `topResumedActivity`, `06-fixture-launched.png`).
5. **HOME selection** — Vesna context menu ▸ "Set as default launcher" opens the system RoleManager dialog; selecting "Vesna Launcher" then "Set as default" sets the HOME role (`07-role-dialog.png`, `07-role.xml`, `08-vesna-as-home.png`). `cmd role get-role-holders android.app.role.HOME` = `dev.vladon.vesna.launcher`.
6. **Return to Vesna** — after launching the fixture again as foreground, pressing the HOME key returns to `dev.vladon.vesna.launcher/.ui.HomeActivity` (`09-home-returns-vesna.png`).

## Inset / source-screen observation (extent observed only)

The captured portrait screens showed a visible status bar, a banner below that bar, and the dock above the gesture area. The run used visual checks.

## Cleanup and restored state

- HOME role restored: `cmd role remove-role-holder` → holder back to `com.google.android.apps.nexuslauncher` (exact baseline).
- Uninstalled, in order: `dev.vladon.vesna.launcher.test`, `dev.vladon.qa.lifecyclefixture` (job-installed fixture), `dev.vladon.vesna.launcher` — all `Success`.
- `pm list packages -3` empty; user 0 only; surfaceflinger PID 618 / system_server PID 780 unchanged; no new tombstones or dropbox crash entries.
- Nothing committed, pushed, or modified outside this report and `qa-evidence/qa006-api36/`.

## Limits

- No Gradle `connected*` task used (shared-workstation multi-device safety); all installs/instrumentation serial-pinned.
- HOME-role change exercised via the system RoleManager dialog (normal user path) rather than `adb root`/settings changes.
- Inset behavior checked visually only, per scope.
- Both runs cover the listed Windows emulator, image revision, and portrait
  configuration. Run 2 exercised the BUG-005 candidate that subsequently
  merged as [PR #27](https://github.com/vladon/vesnalauncher/pull/27).

## Evidence index

The raw evidence is kept outside Git at
`D:/orca/workspaces/vesnalauncher/qa-evidence/qa006-api36/`.

- `instrument-run.txt` — Run 1 runner output, exact (`OK (5 tests)`, 9.357 s)
- `integrated-unpin-instrument.txt` — Run 2 runner output, exact (`OK (7 tests)`, 5.73 s)
- `test-method-names.txt` — `@Test` method cross-check
- `incorrect-target.ini` / `corrected-target.ini` — AVD `target` before/after (CLI 19.0 vs 22.0)
- `first-renderer-crash.txt` / `fallback-renderer-crash.txt` — RegionSampling SIGABRT captures from the failed AVD
- `corrected.stdout.log` / `corrected.stderr.log` — corrected AVD emulator logs (GLDMA/GLDMA2 disabled)
- `correct-target-stability.txt` — 9 stability samples over ~100 s (PIDs, package service)
- `home-eligible-before.txt` — HOME `query-activities` before role change
- `01`–`09` PNG/XML captures — cold start, workspace, drawer, context menu, fixture launch, role dialog, Vesna as HOME, HOME return
- `tombstone_00-pre-existing-bluetooth.txt` — pre-existing tombstone, predates this run
- `emulator.stdout.log` / `emulator.stderr.log` — failed-AVD emulator logs
