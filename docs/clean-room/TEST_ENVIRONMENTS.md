# Vesna Launcher — Test Environments

All observations cite one of the environments below via an environment ID
(`ENV-1`, …). Facts are recorded only after verification on the device.

## ENV-1 — Physical Pixel 9a (primary)

| Property | Value (verified) |
|---|---|
| Type | Physical device (USB debugging) |
| Model / codename | Pixel 9a / `tegu` |
| Manufacturer | Google |
| Android version | 17 |
| API level | 37 |
| Build fingerprint | `google/tegu/tegu:17/CP4A.261105.040.A1/16581829:user/release-keys` |
| Screen resolution | 1080 × 2424 |
| Density | 420 dpi |
| Navigation mode | Gesture navigation |
| System language | English (US) |
| Google Play | Installed, version 53.5.48 |
| Google account | Signed in |
| Default launcher at session start | Pixel Launcher (`com.google.android.apps.nexuslauncher`) |
| First seen | 2026-11-17 |

### Host / tooling

| Property | Value |
|---|---|
| Host OS | Windows Server (x64) |
| ADB | Android platform-tools (winget install) |
| Orchestration | Orca 1.4.261 with `orca-emulator-android` skill |
| Coding agent | OMP 18.22.0 |
| Device control | `orca emulator` verbs over adb + raw adb |
| Evidence store | Local restricted store outside the repository |

### Control capabilities verified on ENV-1

- Screenshot capture (`adb exec-out screencap -p`).
- Accessibility tree dump (`uiautomator dump`).
- Tap, swipe/gesture, text input (US-ASCII), hardware buttons
  (back/home/recents), wake.
- App install via Play Store UI; package presence checks via `pm list packages`.
- Launch arbitrary packages (`am start` / `monkey`).

### Known limitations

- Long-press must be synthesized as a zero-distance swipe with a long
  duration (`input swipe x y x y <ms>`).
- Multi-touch pinch is not directly synthesizable; approximations only.
- `orca emulator` coordinates are normalized 0..1; raw adb uses pixels.

## Nova Launcher installations

| Env | Version | Edition | Install source | Install date | Notes |
|---|---|---|---|---|---|
| ENV-1 | (recorded after install) | TBD | Google Play | 2026-11-17 | Listing: "Nova Launcher: Home Screen", developer Instabridge Sweden AB, 29 MB, contains ads / in-app purchases |

## Rotation / secondary environments

Available AVDs on the host (unused so far in this session):

| AVD | API level | State |
|---|---|---|
| Medium_Phone_API_36.1 | 36.1 | shutdown |
| SetkaTest | 35 | shutdown |
| arm14 | 34 | shutdown |

These may be booted later for Android-version-difference research.
