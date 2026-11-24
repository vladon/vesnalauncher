# VESNA-QA-HOME-ORIENTATION — Home screen orientation choice (#189, v0.4.82)

Device: Android 15 emulator (API 35, emulator-5560), debug build 0.4.82 (87), gesture navigation, over the 0.4.68 baseline data (19 launcher tiles, four dock pins). Rotation forced with `settings put system accelerometer_rotation 0` plus `settings put system user_rotation 1` (the OBS-VESNA-131 method), restored to `accelerometer_rotation 1` afterwards.

| Check | Evidence | Result |
|---|---|---|
| Setting found: Home screen → **Home screen orientation** ("Rotate the workspace, dock and page controls with the device, or keep Home portrait. Apps launched from Home keep the device orientation."), radios **Follow device** (selected by default — today's behavior) and **Portrait** | Capture `qa-o3-orientation-landscape-default` | PASS |
| Follow device unchanged: with the device forced to landscape, Home rotates to 2400 × 1080 with the dock as a bottom row and the page controls centred — the landscape layout OBS-VESNA-131 recorded | `qa-o1-follow-landscape`; Settings itself also follows (`qa-o2-settings-landscape`) | PASS |
| Portrait lock applies live: selecting **Portrait** while the device is forced landscape brings the restarted Home back to 1080 × 2400 with all 19 tiles in their cells | `qa-o5-home-portrait-while-landscape-forced` (node dump: 19 tiles) | PASS |
| The choice survives restart: force-stop and relaunch under the same forced landscape keeps Home portrait | `qa-o5` (captured after a force-stop/relaunch) | PASS |
| Launched apps keep the device orientation: with Home portrait-locked, Settings launched from Home opens 2400 × 1080 | `qa-o6b-launched-settings-landscape`; returning to Home is portrait again (`qa-o7-home-portrait-after-app-return`) | PASS |
| Switching back releases the lock: **Follow device** returns Home to 2400 × 1080 under the forced landscape | `qa-o8-follow-device-live-landscape` | PASS |
| The lock re-applies on resume without a relaunch (the path that picks up a Settings change) | `HomeOrientationTest.defaultFollowsTheDeviceAndPortraitLockApplies` on-device | PASS |
| Russian: **«Ориентация главного экрана»**, **«Следовать устройству»**, **«Портретная»** | `qa-o10-russian-orientation`, `qa-o10-russian-orientation-radios` | PASS |
| Dark: the section and radios render with the Vesna dark scheme | `qa-o11-dark-orientation-radios` | PASS |
| Tests: JVM `HomeOrientationTest` pins the stable names and the follow-device default; `ui.HomeOrientationTest` covers launch application, portrait configuration, the switch-back, and the resume re-read on the running launcher instance | 64 JVM tests, instrumentation 179/179 (workspace 90, ui 74, settings 15), lintDebug | PASS |

**System behavior recorded (not Vesna code):** on this Android 15 image, while a fixed-orientation **HOME** activity is top, the platform rewrites `Settings.System.USER_ROTATION` to the home orientation (verified: with auto-rotate off and `user_rotation 1`, returning to the portrait-locked Home flips the setting to 0; foregrounding Settings or returning the follow-device mode leaves it alone). This is inherent to any launcher locking its window on Android 15 — it is not specific to this implementation. Apps keep following whatever the device rotation is at launch time, so the acceptance "apps launched from Home keep the device orientation" holds; only the user's manual rotation *override value* is settled by the system while the lock is on. With auto-rotate on (the common configuration) no rotation settings are touched at all.

Notes and limits:

- The lock covers the whole Home window: the drawer is a layer inside `HomeActivity`, so it stays portrait with Home; Settings keeps following the device.
- `requestedOrientation` is re-applied in `onCreate` and `onResume`; Home handles configuration changes in place (manifest `configChanges`), so switching re-lays the workspace, dock and page controls live without a recreation.
- Not exercised: split-screen and desktop-windowing modes (the launcher is a full-screen HOME surface on this device), and foldables' posture changes.
- Screenshots: `D:/orca/scratch/arcticons/` (`qa-o1` … `qa-o11`). The device was restored to the 0.4.68 baseline data, auto-rotate on, Light, English afterwards.
