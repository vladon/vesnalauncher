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

| Env | Version | Version code | Edition | Install source | Install date | minSdk / targetSdk |
|---|---|---|---|---|---|---|
| ENV-1 | 8.9.9 | 89900 | **Prime** from 2026-11-18 (lifetime; `com.teslacoilsw.launcher.prime` v2023 "Pro Key APK" unlock installed by the user; before that: Free with subscription upsell Prime USD 4.99/yr, Plus USD 99.99/yr or 14.99/mo) | Google Play (launcher + prime unlock) | 2026-11-17 / prime 2026-11-18 | 26 / 36 |
| ENV-2 (Medium_Phone_API_36.1 AVD) | 8.9.9 | 89900 | Free | APK copied from ENV-1 via `adb pull` + `adb install` (same authorized Play-installed copy moved between the project's own devices; no decompilation, no third-party download; no Google account on the AVD, so Play sign-in was unavailable — auth blocker documented) | 2026-11-17 | 26 / 36 |

### ENV-1 device-config changes made for research (2026-11-18)

- Badge-rendering enablement: added
  `com.teslacoilsw.launcher/.notificationlistener.NotificationListener` to
  `enabled_notification_listeners` via `settings put secure`, set the
  `POST_NOTIFICATION` appop to allow, and toggled "All Nova Launcher
  notifications" ON via the system App-info UI. The user then granted
  Notification access and rebooted — the listener is BOUND and badge
  rendering verified working for third-party notifications (see
  OBS-SYS-112/113). Grants remain in place (standard launcher setup Nova
  itself would request).

### ENV-2 notes

- Medium_Phone_API_36.1 AVD: Android 16 (API 36), 1080×2400, gesture
  navigation. Play Store app present but NOT signed in (no account) — Play
  installs impossible without credentials (reported, not guessed).
- Nova's setup wizard ran identically to ENV-1 (Welcome ▸ Get started ▸
  system default-home dialog ▸ subscription page ▸ App drawer defaults ▸ All
  done). Differences: the HOME-role dialog listed THREE launchers (Nova,
  Pixel Launcher, Vesna Launcher as current default — the implementation
  team's AVD); the subscription page rendered with BLANK price amounts
  ("/year", "/month" without numbers — billing/price fetch unavailable
  without Play sign-in); the Icon-style wizard page did not appear in this
  pass.
- Vesna Launcher was restored as the AVD default after the comparison
  (`cmd package set-home-activity`), leaving the implementation team's
  environment intact.

### Install notes (ENV-1, 2026-11-17)

- Play install initially stalled ("Pending...") behind a stuck Google Play
  mainline module download; Play content downloads fail on this LAN (CDN edge
  hosts unreachable; API endpoints fine). A device-wide HTTP proxy
  (`adb reverse tcp:2080` + `settings put global http_proxy 127.0.0.1:2080`
  pointing at the host's local proxy) restored Play content downloads and the
  install completed. **The proxy mapping was torn down after installation**
  (see the session-close checklist below); it must be re-enabled temporarily
  whenever Play content downloads are needed again.
- Setup wizard flow observed (free path): Welcome ▸ Get started ▸ system
  default-home-app dialog (radio list: Nova Launcher / Pixel Launcher
  "Current default"; confirm button disabled until a radio is selected) ▸
  subscription page (closeable via X) ▸ Search defaults ▸ App drawer defaults ▸
  Icon style ▸ All done ▸ Go home.
- Wizard defaults observed (ENV-1, free edition): search Mode "Immersive";
  Shortcut Search ON; Contact Search OFF; home search bar placement
  "Dock (Below icons)"; drawer search bar placement "Top"; app drawer style
  "Vertical"; Show Keyboard ON; Frequently used apps ON; Frequently used
  shortcuts ON; Smart Folders ON; icon size 125%; icon shape "Round (Default)".
- After setup, Nova places a "Nova Settings" shortcut on the workspace and a
  default set of icons (Gmail, Photos, Play Store on the page; Phone, Messages,
  Maps, Chrome, Camera in the dock).

## Rotation / secondary environments

### Research AVD — `emulator-5588` (`Nova_Reboot_API35`), persistent expansion (2026-11-25)

| Property | Value (verified) |
|---|---|
| Type | AVD (`Nova_Reboot_API35`), serial `emulator-5588` |
| Android / API | 15 / 35 (`sdk_gphone64_x86_64`) |
| Users | 0 Owner (running), 11 Work (running) |
| Launcher | Vesna v0.4.82 (87) + Nova 9.0.1 Prime (90100) |

The work profile is a **persistent, owner-approved expansion**
(OBS-VESNA-156): created with `pm create-user --profileOf 0
--managed Work` (user 11), started with `am start-user -w 11`,
profile owner set to the debug-signed research probe
`com.vesnaresearch.iconprobe` 1.0 (installed for user 0 and 11;
custom magenta icon no pack covers). The profile yields 17 work
entries in the drawers (16 system apps + the probe). Do not remove
the probe or the profile between cycles — later observations reuse
them. Arcticons Black 15.4.7 (`com.donnnno.arcticons.light`) was
installed for the OBS-156 fallback test and **uninstalled**
afterwards; the Vesna Icon pack was restored to System.

If a pre-expansion snapshot is ever restored, the profile and probe
are lost with it: recreate with the same three commands (create,
start, install for both users) and re-verify OBS-156's evidence
before citing the environment again.

| AVD | API level | State |
|---|---|---|
| Medium_Phone_API_36.1 | 36.1 | shutdown |
| SetkaTest | 35 | shutdown |
| arm14 | 34 | shutdown |

These may be booted later for Android-version-difference research.

## Session close / open checklist (ENV-1)

The device is the user's personal phone. Any host-side bridging set up for
research MUST be reverted at session end — a device-wide proxy that points at
a dead `adb reverse` port silently kills the phone's internet.

**At session close (mandatory):**

```bash
adb -s 55021JEBF03248 shell settings put global http_proxy :0
adb -s 55021JEBF03248 reverse --remove tcp:2080   # ignore error if absent
adb -s 55021JEBF03248 reverse --list               # must be empty
```

Verify: `settings get global http_proxy` returns `:0` and the device still
reaches the network (e.g. `ping -c1 play.googleapis.com` from the device).

**When Play content downloads are needed again (temporary):**

```bash
adb -s 55021JEBF03248 reverse tcp:2080 tcp:2080
adb -s 55021JEBF03248 shell settings put global http_proxy 127.0.0.1:2080
# ... perform downloads ...
# then re-run the session-close teardown immediately after.
```

Notes:
- `adb reverse` is registered per transport; always target the USB serial
  `55021JEBF03248` explicitly. A WiFi-adb transport (`…:5555`) may
  auto-reconnect after reboots and would not carry the reverse mapping.
- Pin `-s 55021JEBF03248` in every adb command while a second transport
  (`emulator-5554`) is attached.
