# VESNA-QA-003F — disabled-app pin retention

**Verdict:** Pass for Android 15/API 35, personal profile. Disabling the test app hid it without deleting any pins. Re-enabling restored all placements, and uninstalling still removed them.

**Build:** local 0.4.2 candidate (version code 7), based on `a5eaedc`. Debug APK SHA-256: `f2038fccca0d3f1ba05eaaed30b6447ef539e490a80df941b6a2bc89aae0104f`.

**Environment:** fresh `Vesna_Release_042_QA` Android 15 AVD (`emulator-5572`), 1080 × 2160 at 420 dpi. ADB commands were serial-pinned and the AVD name was checked before use. No shared QA device or physical phone was modified.

On the disposable AVD, a layout fixture contained the installed test app at workspace cell 6, dock order 1, and position 0 in a two-member folder at workspace cell 14. The SQLite checks compared pin IDs, containers, positions, profile serials, and folder IDs. Android's UI hierarchy was inspected on the launcher, drawer, and folder dialog.

| Scenario | Observed result |
| --- | --- |
| Disable the fixture with `pm disable-user --user 0` while Vesna is running | Workspace pin, dock pin, folder member, and drawer entry all hidden. All three pin rows and the folder row were unchanged. |
| Force-stop and relaunch Vesna with the fixture still disabled | Both top-level icons and the folder member remained hidden; the pins and folder were unchanged. The cold relaunch was checked in dark system mode at 1.3× font scale; the live-disable case used light mode at 1.0×. |
| Re-enable the fixture with `pm enable --user 0` | Both top-level pins, the folder member, and the drawer entry reappeared. The pin rows were still unchanged. |
| Uninstall the fixture while Vesna is stopped, then relaunch | All fixture rows were removed. The folder dissolved and its surviving member moved into workspace cell 14. |

`WorkspaceRepositoryPackageLifecycleTest` also checks that an uninstalled instance of the same package in a different profile does not erase the retained pins, and that cleanup after uninstall dissolves a one-member folder.

This run did not cover disabling a managed-profile app or a device-specific suspension flow. Earlier managed-profile discovery and uninstall evidence remains in [VESNA-QA-003C](VESNA-QA-003C.md). No widgets, theme controls, layout redesign, or adaptive icons were introduced in this change; comprehensive Material 3, keyboard and TalkBack acceptance remains outside this lifecycle run.
