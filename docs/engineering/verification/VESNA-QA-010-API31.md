# VESNA-QA-010 — Android 12/API 31 emulator smoke

On 2026-11-19, all **12 instrumentation tests** passed on a dedicated
Android 12/API 31 emulator. The debug app also rendered the workspace
and drawer.

## Environment

| Item | Verified configuration |
| --- | --- |
| AVD | `Vesna_QA_API_31`, `emulator-5570`, Pixel 6 profile, user 0 |
| Image | `system-images;android-31;google_apis;x86_64`, Android 12 |
| Host | Windows build 26300, Cmdline Tools 22.0, WHPX, SwiftShader |
| Tested build | Debug 0.4.1/6, candidate `75ca3ed6818dbb2fca5094de84bd9678666fac12` for [PR #39](https://github.com/vladon/vesnalauncher/pull/39) |
| App APK SHA-256 | `b3ca210958fb205ae12be344d3876bbe3ccc3a549fc44c4f7c4cb7df12c0f0f9` |
| Test APK SHA-256 | `b0fdfe04250d6aaebc24f814849c70a3b60cf1f1d684c1d7de1cef9289b70724` |

The new AVD was created from the installed SDK image. Two Orca attach
attempts ended before the guest finished booting. A direct SDK launch
with `-no-window -no-snapshot -no-audio -gpu swiftshader_indirect` booted
the same AVD. The run verified its identity, completed boot, user 0,
core service stability, NexusLauncher focus, and Vesna package absence.

## Results

The verified app and test APKs were installed using adb pinned to
`emulator-5570`. AndroidJUnitRunner reported `OK (12 tests)`:

| Test class | Passed |
| --- | ---: |
| `RoomMigrationQaTest` | 2 |
| `DrawerSearchTest` | 1 |
| `WorkspaceRepositoryDockFolderTest` | 3 |
| `WorkspaceRepositoryFolderCreationTest` | 2 |
| `WorkspaceRepositoryFolderRemovalTest` | 2 |
| `WorkspaceRepositoryTopLevelRemovalTest` | 2 |

An explicit package launch focused Vesna's `HomeActivity` and rendered the
workspace. The drawer-opening swipe revealed app entries in the UI
hierarchy and screenshot. Android's resolved HOME remained NexusLauncher
throughout the run.

A separate registration check used the same app APK on the same AVD.
`cmd package query-activities` listed Vesna's `HomeActivity` as a HOME
candidate alongside NexusLauncher. The HOME role stayed with NexusLauncher.
The candidate entry disappeared after removing the temporary Vesna package.

## Teardown

The task-installed Vesna packages were removed after each pass. The preflight HOME
resolution and NexusLauncher foreground matched. Orca shut down only the
new AVD, and its definition remains available for subsequent verification.

The result covers the recorded build and emulator only. Runner output and
screenshots are retained in `qa-evidence/qa010-api31/`.

## Follow-up on the sixteen-test suite

The same API 31 AVD passed all 16 instrumentation tests on 2026-11-19 with
the debug candidate `1b5dc2e55b8ad2a8f440e5793af6efa7a7050e48` for
[PR #45](https://github.com/vladon/vesnalauncher/pull/45). The tested APKs
were verified before installation:

| APK | SHA-256 |
| --- | --- |
| `app-debug.apk` | `0d588b59331fc2fe94f7a8a07cede2d54dafd5d666a95327206d92768bcee1fe` |
| `app-debug-androidTest.apk` | `b8ee2c8ca9934680eb97db3a839ec4b88378502b6bb5c4a2dc223ee40f74ec6a` |

The recorded SDK boot command started the preserved AVD on one attempt.
The run verified `emulator-5570`, API 31, user 0, stable core services,
and stock HOME before serial-pinned installation and AndroidJUnitRunner
execution. Both folder rollback regressions and the selected-pin move
regression passed in the full suite. This follow-up covers instrumentation
with the latest candidate. The workspace, drawer, and HOME observations
above belong to the earlier build.

The two Vesna packages were removed and the original NexusLauncher HOME
and foreground were verified. Orca shut down the AVD and retained its
definition. Runner output and boot logs are in
`qa-evidence/qa010b-api31-suite/`.
