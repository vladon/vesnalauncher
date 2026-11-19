# VESNA-QA-005 — declared minimum SDK (API 26) smoke verification

**Verdict:** PASS — the app builds, installs, cold-starts, and passes its full
androidTest suite on the declared minimum OS. One layout defect observed
(see "Observations"), and one test-runner quirk documented for future runs.
Run recorded 2026-11-19.

**Item:** VESNA-QA-005, minimum SDK smoke verification.
**Code under test:** commit `bb8c10d` (`bb8c10db2ebf334a9839aca1c1ad0c6c0380fbf5`,
"fix: show the drawer item menu (#18)"), latest `main` at dispatch, isolated
worktree `qa-min-sdk-26`.
**Package under test:** `dev.vladon.vesna.launcher`, `versionName` 0.4.1,
`versionCode` 6, declared `minSdk = 26` (`app/build.gradle.kts`).
**Fixture:** `D:/orca/scratch/vesna-qa-003/fixture-v1.apk` (user-owned,
package `dev.vladon.qa.lifecyclefixture`, reused from VESNA-QA-003).

## Environment

| Property | Value | Source |
|---|---|---|
| Dedicated AVD | `Vesna_QA_MinSdk_API_26`, adb serial `emulator-5562` | `adb -s emulator-5562 emu avd name` |
| OS / API | Android 8.0.0, SDK 26 | `getprop ro.build.version.release` / `ro.build.version.sdk` |
| Build fingerprint | `Android/sdk_gphone_x86_64/generic_x86_64:8.0.0/OSR1.180418.026/6741039:userdebug/dev-keys` | `getprop ro.build.fingerprint` |
| ABI | x86_64 | `getprop ro.product.cpu.abi` |
| Screen | 1080×1920 | `wm size` |
| User | 0 (`am get-current-user`) | no other user exercised |
| Boot | `sys.boot_completed=1` on first poll (~5 s after attach; bounded wait 180 s) | `getprop sys.boot_completed` |
| Image | google_apis x86_64, android-26 (parent-created and started) | parent dispatch |

Every adb command pinned `-s emulator-5562`; no other device, AVD, or branch
was touched; no `adb root`, no default-HOME change, no user change, no
manifest/Gradle/toolchain edits, no commits.

## Build

```
gradlew.bat assembleDebug assembleDebugAndroidTest   → BUILD SUCCESSFUL
```

Artifacts (SHA-256 in evidence `build-artifacts-sha256.txt`):

| Artifact | Size |
|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 12,263,089 B |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | 408,677 B |

## HOME registration (discoverability only)

`cmd package query-activities -a android.intent.action.MAIN
-c android.intent.category.HOME`:

| State | HOME activities |
|---|---|
| Before Vesna install | 2 — `com.google.android.apps.nexuslauncher.NexusLauncherActivity` (**default**, `isDefault=true`), `com.android.settings.FallbackHome` (priority −1000) |
| After Vesna install | 3 — **Vesna listed**: `dev.vladon.vesna.launcher.ui.HomeActivity`, priority 0, package resolves `VesnaApplication` |

The system default stayed `com.google.android.apps.nexuslauncher` throughout;
`cmd package set-home-activity` / role APIs were never invoked.

## Explicit cold start (workspace render)

```
adb -s emulator-5562 shell am start -W -n
    dev.vladon.vesna.launcher/.ui.HomeActivity
→ Status: ok  ThisTime/TotalTime: 967 ms  WaitTime: 977 ms  Complete
```

- `mCurrentFocus` = `dev.vladon.vesna.launcher/dev.vladon.vesna.launcher.ui.HomeActivity`; process alive (pid 5099).
- **Workspace renders**: uiautomator dump `qa005-01-workspace.xml` — 49 nodes,
  all `package=dev.vladon.vesna.launcher`; 20 workspace cells
  (`app_icon`/`app_label`: Clock, Contacts, Custom Locale, Dev Tools, Drive,
  Duo, Files, Gmail, Google, Maps, Messages, Phone, Photos, Play Movies & TV,
  Play Music, Settings, Voice Search, Wallpapers, WebView Browser Tester,
  YouTube) + dock (Calculator, Calendar, Camera, Chrome) + banner
  `banner_default_home` "Set Vesna as your default launcher" (correct:
  Vesna is not the default HOME). Matches `SeedPlanner` fresh-install seeding
  (first 4 apps alphabetical → dock, next 20 → workspace page 1).
- No crash-buffer entries for `dev.vladon` during the whole run
  (`logcat -b crash` grep count 0; the only buffer entry predates the session
  and is a boot-time `system_server` event).

## App drawer render

Upward fling on the workspace (`input swipe 540 1300 540 750 150`, matches
`WorkspaceView.onSwipeUp` → fling threshold 1200 px/s, ≤100 dp drift) opens
the `DrawerView` slide-up panel:

- `qa005-02-drawer-open.png` / `qa005-02-drawer.xml` — search bar
  ("Search apps") + all 24 discovered apps over the dimmed workspace
  (99 a11y nodes: 24 drawer + 24 workspace + containers).

## Fixture round-trip (`dev.vladon.qa.lifecyclefixture`)

| Step | Command | Result |
|---|---|---|
| Install | `adb install -r fixture-v1.apk` | Success |
| Drawer discovery | re-fling, dump `qa005-03-drawer-fixture.xml` | **"QA Fixture" listed** alphabetically between Play Music and Settings (drawer updated live; bounds `[825,1286][1065,1329]`) |
| Tap-to-launch | `input tap 945 1307` | `mCurrentFocus` = `dev.vladon.qa.lifecyclefixture/dev.vladon.qa.lifecyclefixture.MainActivity`; `qa005-04-fixture-launched.png` shows "QA Fixture / QA Lifecycle Fixture 0.1" |
| Uninstall | `adb uninstall dev.vladon.qa.lifecyclefixture` | Success |
| Drawer removal | re-fling, dump `qa005-05-drawer-after-uninstall.xml` | **"QA Fixture" gone**; 24 apps remain; Vesna process unaffected, no crash |

Only the fixture was uninstalled at that point; Vesna stayed installed until
the test runner's own teardown (below).

## androidTest suite (`connectedDebugAndroidTest`)

Attempt 1 **failed to run tests**: the Android Test Engine installs the app
APK without `-r`, and the manually pre-installed same-version build
triggered `INSTALL_FAILED_ALREADY_EXISTS` while Gradle still printed
"BUILD SUCCESSFUL" (engine failure surfaced only in the log;
`qa005-connected-test-attempt1-failed-install.log`). **Bounded recovery
(one retry)**: the engine had already removed the app, so the device was
clean (`pm list packages dev.vladon` empty); attempt 2 ran on that state.

Attempt 2 — `ANDROID_SERIAL=emulator-5562 gradlew.bat connectedDebugAndroidTest`
→ "Running tests on devices: Vesna_QA_MinSdk_API_26(AVD) - 8.0.0",
BUILD SUCCESSFUL in 24 s, exit code 0. Per-case (from
`TEST-Vesna_QA_MinSdk_API_26-AVD-8.0.0.xml`; per-case logcat files copied to
evidence):

| Case | Result | Time |
|---|---|---|
| `RoomMigrationQaTest.migrationFromV1PreservesLayoutAndDerivesDefaults` | PASS | 0.201 s |
| `RoomMigrationQaTest.migrationFromV2PreservesUserSerialsAndAddsFolders` | PASS | 0.237 s |
| `DrawerSearchTest.searchNarrowsShowsEmptyStateAndRestore` | PASS | 5.103 s |

**3/3 passed, 0 failures, 0 errors, 0 skipped** — the full existing suite
passes on the declared minimum OS (Room migration chain and drawer search
both work on API 26). The runner uninstalled both APKs after the run
(final `pm list packages dev.vladon` empty).

## Observations

1. **Workspace grid occluded by the dock on 1080×1920 (layout defect).** `GridCanvas` is a fixed 4×5 grid (capacity 20) plus a 4-slot
   dock; on the shorter API 26 screen the 5th row (Voice Search, Wallpapers,
   WebView Browser Tester, YouTube) is drawn behind the dock — its icons are
   hidden and its labels bleed above the dock scrim
   (`qa005-01-workspace-coldstart.png`). Those four seeded cells are visually
   occluded in the captured image. The smoke run left last-row tap behavior
   unverified. VESNA-BUG-004 tracks this overlap.
2. **Test-engine clean-install behavior.** In this run, the pre-installed
   APK triggered `INSTALL_FAILED_ALREADY_EXISTS` while Gradle exited 0.
   The retry succeeded with Vesna absent from the disposable AVD. Use the
   per-case test XML and engine output to verify suite execution.

## Exercised scope and limits

Exercised: build (debug + androidTest), HOME discoverability, explicit cold
start, workspace + drawer render, fixture drawer discovery + tap-to-launch +
uninstall, full androidTest suite, on user 0 of the dedicated AVD only.
Not exercised (by design): changing the default HOME or user, `adb root`,
deep drawer interaction (search UI beyond the suite), workspace editing on
API 26 (QA-003B covered it on API 35), the occluded-row launch path.

## Restoration (verified end state)

| Item | End state |
|---|---|
| Fixture `dev.vladon.qa.lifecyclefixture` | **uninstalled** (only the fixture; before the suite) |
| Vesna app | absent — removed by the test runner's own post-suite teardown |
| Default HOME | `com.google.android.apps.nexuslauncher/.NexusLauncherActivity` (untouched) |
| Foreground | stock: `mCurrentFocus` = `NexusLauncherActivity` |
| AVD residue | `/sdcard/window_dump.xml` removed |
| Worktree | Smoke report only |
| Other devices | untouched (all commands pinned `-s emulator-5562`) |

## Evidence index (`D:/orca/workspaces/vesnalauncher/qa-evidence/qa005-api26/`)

- `build-artifacts-sha256.txt` — APK SHA-256 + git SHA
- `qa005-01-workspace-coldstart.png` / `qa005-01-workspace.xml` — cold start, workspace
- `qa005-02-drawer-open.png` / `qa005-02-drawer.xml` — drawer render
- `qa005-03-drawer-fixture.png` / `qa005-03-drawer-fixture.xml` — fixture discovered in drawer
- `qa005-04-fixture-launched.png` — fixture foreground
- `qa005-05-drawer-after-uninstall.xml` — fixture removed from drawer
- `qa005-connected-test-attempt1-failed-install.log`, `qa005-connected-test-stdout.log` — runner stdout (exact)
- `TEST-Vesna_QA_MinSdk_API_26-AVD-8.0.0.xml` — per-case JUnit XML
- `logcat-*.txt` — per-case device logcat captures
- `test-result-exit-code.txt` — runner exit code (0)

## Follow-up on the eight-test suite

On 2026-11-19, the same Android 8.0/API 26 AVD ran the full instrumentation
suite at commit `4c5f0c19b652610aa40fe290c26f06074490d33f` (PR #34). User 0
was active, core services were stable, and both Vesna packages were absent
at preflight. NexusLauncher was the resolved HOME and foreground.

Debug APK assembly, instrumentation APK assembly, and all 42 JVM tests
passed. Android lint reported 0 errors and 18 warnings. The build artifacts
were recorded with these SHA-256 hashes:

| APK | SHA-256 |
| --- | --- |
| `app-debug.apk` | `beeeff4a797d9f88b189ae0d32295e6d4c2de91b34d8e9675516b3673147aaf7` |
| `app-debug-androidTest.apk` | `a49d3ec967f28efa4f7f5519d55282404cdf59b8e087d5305b074528e7b93337` |

Serial-pinned `adb` installation and AndroidJUnitRunner execution on
`emulator-5562` passed **8/8**:

| Test class | Passed |
| --- | ---: |
| `RoomMigrationQaTest` | 2 |
| `DrawerSearchTest` | 1 |
| `WorkspaceRepositoryDockFolderTest` | 3 |
| `WorkspaceRepositoryTopLevelRemovalTest` | 2 |

An explicit launch opened `HomeActivity` and rendered workspace icons.
Teardown removed the two packages installed for the run and confirmed
NexusLauncher as the resolved HOME and foreground. The previous HOME default
was preserved throughout.

Detached evidence is in `qa-evidence/qa005b-api26-current/`, including the
runner log, JVM test XMLs, artifact checksums, and workspace hierarchy.
