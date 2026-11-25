# Observations — System Integration (ENV-1)

Raw observations. Evidence IDs reference the restricted local store. Nothing
in this file is copied Nova UI text verbatim; short labels are quoted only to
identify controls.

## OBS-SYS-100 — Play Store install queue behavior (environment, not Nova)

- Date / Env: 2026-11-17 / ENV-1
- Navigation path: Play Store ▸ app listing (Nova Launcher) ▸ Install
- Initial state: Play 53.5.48 signed in; Pixel 9a connected Wi-Fi (validated).
- Action: Open `market://details?id=com.teslacoilsw.launcher`, tap Install.
- Observed:
  - Install starts but stalls at "Pending..." indefinitely (5+ minutes).
  - Cancelling and re-tapping Install reproduces the stall.
  - Play logcat (Finsky) shows a stuck Google Play mainline system update
    (`com.google.mainline.adservices`, ~7 KB, `bytes_downloaded=0`, repeated
    `retry` and `reviveDownload` cycles, `onRedirectReceived` loop) while
    `runningDownloadsCount: 2, queuedCount: 1`.
  - The account has Nova installed on several other devices (OnePlus phones,
    Ulefone); the listing offers per-device Install/Uninstall entries.
  - A Google Play Store ANR dialog appeared once when first opening the
    listing; tapping Wait dismissed it and the UI recovered.
- Variations: force-stop Play, reopen listing (no change); clear-cache via pm
  (unsupported command); retry after cancel (no change).
- Persistence: n/a (environment behavior).
- Dependencies: Google account, Play queue, mainline module updates.
- Evidence: EVI-SYS-100 (screenshots play-pending.png, play-manage.png,
  play-after-restart.png; logcat excerpts).
- Confidence: Observed
- Open questions: does device reboot clear the stuck mainline download?
  (tested next)

## OBS-SYS-102 — Cross-version comparison: Android 17 (ENV-1) vs Android 16 (ENV-2)

- Date: 2026-11-17. Same Nova build (8.9.9/89900, free) on both.

| Behavior | ENV-1 (Android 17, Pixel 9a) | ENV-2 (Android 16, AVD) | Verdict |
|---|---|---|---|
| Setup wizard flow/defaults | Welcome ▸ dialog ▸ paywall ▸ defaults pages | Same flow; Icon-style page not shown this pass | Match (minor nav variance) |
| HOME-role dialog | 2 launchers | 3 launchers (Vesna installed) | Environment-dependent list |
| Subscription page | Full prices ($4.99/$99.99/$14.99) | Blank price amounts | Billing-dependent rendering |
| Default home layout | Nova Settings shortcut + Gmail/Photos/Play Store + 5-icon dock + search bar | Identical | Match |
| Drawer grid "6 ✕ 5" | Renders 5 columns | Renders 5 columns | Match — label reads rows × columns |
| Drawer empty state | Ads + predictions + contact bar | "No Frequent Apps" (fresh), no ads (no billing), no contact bar | Match structurally; ad/account content env-dependent |
| HOME key on workspace | Opens Nova Search drawer (REPLICATED ×2 controlled presses on settled workspace) | Stays on workspace (REPLICATED ×2 same protocol) | **VERSION DIFFERENCE — replicated on both sides** |
| Swipe down (mid-screen) | Notification shade expands | Notification shade expands | Match |
| Swipe up (mid-screen) | Drawer opens | Drawer opens | Match |
| Fast scrollbar | Visible thumb slider | Visible thumb slider | Match |

- Caveats: HOME-key difference replicated with controlled presses (settled
  workspace, 3 s gaps, two presses per side — EVI-SYS-001 replicate1/2
  captures both devices). ENV-2 has no Google account, which explains
  ad/pricing deltas. AVD wallpaper image also differs from its earlier
  capture — environment-side wallpaper change on the AVD as well; not
  launcher-attributed.
- Evidence: EVI-ENV2-001 (all captures), EVI-SYS-001 (home-from-app,
  avd-home-replicate1/2, pixel-home-replicate1/2), EVI-HOME-003,
  EVI-DRAWER-001
- Confidence: Observed per cell; HOME-key row replicated

## OBS-SYS-101 — Reboot to clear Play queue

- Date / Env: 2026-11-17 / ENV-1
- Action: `adb reboot`, wait for boot completion, unlock.
- Observed: pending install resumed but still stalled (mainline retry loop
  persisted; later Nova APK download restarted from 0 and its streamed
  install session failed with Play internal status 6581; downloads repeatedly
  failed with 0 bytes while API traffic worked).
- Follow-up: `pm clear com.android.vending` did not unblock it.
- Root cause (verified from host): the LAN blocks Google Play CDN edge hosts.
  API hosts (`play.googleapis.com`, `redirector.gvt1.com`) resolve and
  respond, but the download edges returned after redirect are unreachable
  (host curl following the redirect also failed). Evidence: device ping tests,
  host curl tests, Finsky logs (`onFailed`, `onRedirectReceived`, 0-byte
  progress).
- Resolution: device-wide HTTP proxy through the host's local proxy —
  `adb reverse tcp:2080 tcp:2080` then
  `adb shell settings put global http_proxy 127.0.0.1:2080`. Play content
  downloads then completed and Nova 8.9.9 installed from the Play listing.
- Persistence: proxy mapping was **removed after installation completed**
  (`settings put global http_proxy :0`, `adb reverse --remove tcp:2080`;
  device connectivity verified afterwards). Re-enable temporarily for future
  Play content downloads per the checklist in TEST_ENVIRONMENTS.md. App
  launching/UI unaffected either way.
- Evidence: EVI-SYS-100, EVI-SYS-101 (logcat excerpts, curl outputs,
  screenshots play-after-reboot.png, play-reset1.png, nova-first-run.png)
- Confidence: Observed

## OBS-SYS-110 — Tapping a Prime-locked row opens the subscription page

- Date / Env: 2026-11-17 / ENV-1
- Action: tapped the Prime-locked "Notification badges" row in main settings.
- Observed: opens the full-screen "Manage Subscription — Choose your plan"
  page (back-arrow page, not a dialog): Nova Prime card ($4.99/year;
  ad-free, custom gestures & swipe actions, hide apps for privacy,
  themes/icons/animations) and Nova Plus card ($99.99/year "Save 44%" or
  $14.99/month radios; adds Personalized AI insights daily + AI assistant on
  your home screen), each with "Try 3 days free"; close (X) top-right.
- The same page opened when tapping Settings ▸ Nova AI (earlier
  OBS-SETT-009, dark themed there) and when tapping "Quick start" (see
  OBS-BACKUP-110). On ENV-2 the same page rendered with BLANK price amounts
  (no Play billing availability).
- Resolves OPEN_QUESTIONS #5.
- Evidence: EVI-SYS-002 (prime-row-tap)
- Confidence: Observed

## OBS-SYS-111 — Prime license activation (edition change) + badge rendering blocked

- Date / Env: 2026-11-18 / ENV-1
- Change: the user installed **Nova Prime** (lifetime, `com.teslacoilsw.launcher.prime`
  versionName 2023, the classic Pro Key unlock app). After a Nova restart:
  - Settings ▸ Manage Subscription subtitle: "Nova Prime · Activated by Pro
    Key APK";
  - the version row re-brands to "Nova Prime 89900 (8.9.9)";
  - previously Prime-greyed rows (Notification badges, gestures, Hide apps,
    Drawer groups) are now dark/normal;
  - the drawer's advertisement banner is GONE (only the divider slot
    remains) — matching Prime's "ad-free" benefit;
  - the drawer now also lists the Prime license app icon with a "PRIME"
    ribbon badge.
- Badge rendering test could NOT complete at the time: the OS reported
  Nova's notifications as blocked (`importance=NONE userSet=false`) and Nova
  was NOT in the enabled notification listeners. RESOLVED LATER (see
  OBS-SYS-112/113): the user granted Notification access and rebooted — the
  listener bound, notifications were allowed via App info, and the badge
  root cause was found (Nova's test button posts a group summary, which
  Android never badges).
- Evidence: EVI-SYS-002 (prime-active-main, settings-after-prime,
  badges-page/page2, badge-test-workspace, badge-test2, settings-check),
  EVI-DRAWER-002 (tabs-reverted — no ad banner)
- Confidence: Observed (activation effects); badge rendering Blocked
  (environment grants). SUPERSEDED by OBS-SYS-112/113 (2026-11-18 later
  session: enablement completed, root cause found).

## OBS-SYS-112 — Badge pipeline fully enabled; Nova self-badge does not render

- Date / Env: 2026-11-18 / ENV-1 (Nova Prime)
- Enablement chain (all verified):
  1. After the user's reboot, Nova's notification listener IS BOUND
     (`com.teslacoilsw.launcher/.notificationlistener.NotificationListener`
     in the active listener list with a live binder proxy).
  2. Via system App info ▸ Notifications, "All Nova Launcher notifications"
     toggled ON (`AppSettings … importance=DEFAULT userSet=true`; before:
     `importance=NONE userSet=false` — "Notifications: Off").
  3. The built-in "Test notification badge" button then posted a LIVE
     notification (importance=3): "This is a test notification, check the
     Nova Settings icon for its badge".
- RESULT: **no badge renders on any Nova Settings icon instance** — the
  workspace shortcut, the drawer frequently-used icon, and the drawer
  prediction chip were all zoom-verified (4× nearest-neighbor crops) with
  the notification live: no corner dot, no content circle.
- CROSS-EVIDENCE that the badge pipeline itself works: a Play Store
  notification rendered a dynamic badge (avatar content) on the Play Store
  icon minutes earlier, and cleared when the notification cleared.
- Bonus finding: `am start -n com.teslacoilsw.launcher/.NovaSettings` fails
  with "Error type 3 — Activity does not exist": **the `.NovaSettings`
  activity is not in the manifest**; earlier am-start attempts silently
  failed (stderr was suppressed). Reliable settings entries are the
  workspace "Nova Settings" shortcut and the drawer prediction chip.
- Evidence: EVI-SYS-002 (post-reboot-state, badges-nav-r3*, badges-page-r3*,
  badge-test-r3/final/success, drawer-badge-check, badge-zoom, zoom-chip,
  zoom-frequent, amstart-result)
- Confidence: Observed (enablement chain + absence); root cause resolved in
  OBS-SYS-113

## OBS-SYS-113 — Numeric-style re-test and the badge root cause

- Date / Env: 2026-11-18 / ENV-1 (Nova Prime)
- Action: switched badge style to "Numeric Dots" ("Displays a badge with the
  number of notifications"), re-posted the Test notification, zoom-verified
  the Nova Settings drawer icon (4×), then restored the style to "Dynamic
  badges".
- Result: still no badge — BUT the decisive evidence was in the dumpsys
  record: the Test notification carries flags
  **`LOCAL_ONLY|GROUP_SUMMARY|AUTOGROUP_SUMMARY`** — it is a GROUP SUMMARY.
  Android never renders group summaries as badges (they are the invisible
  parents of notification groups); only real child notifications count.
- Root cause (style-independent): Nova's own "Test notification badge"
  button posts a group-summary notification, which the badge counter
  correctly ignores. Other apps' real notifications DO badge (Play Store
  dynamic badge observed). The badge feature itself works; the self-test
  button cannot demonstrate it by construction.
- Vesna spec takeaway (implementation-independent, public platform
  semantics): badge counting must exclude GROUP_SUMMARY notifications
  (Android platform behavior); a launcher's "test badge" affordance must
  post a normal, non-summary notification to be visible.
- Style options captured: Dynamic badges ("Uses notification content to
  create a custom badge"), Dots ("Android 8.0+ style badges"), Numeric Dots
  ("Displays a badge with the number of notifications"). Style restored to
  Dynamic badges after the test.
- Evidence: EVI-SYS-002 (style-row, style-options, style-numeric,
  numeric-drawer-check, numeric-zoom, style-restored), dumpsys excerpt with
  notification flags
- Confidence: Observed (flags + absence across two styles)

## OBS-SYS-114 — Third-party badge matrix attempts (inconclusive)

- Date / Env: 2026-11-18 / ENV-1 (Nova Prime)
- Goal: badge a THIRD-PARTY app icon (Nova's self-test cannot badge —
  OBS-SYS-113) and then exercise the popup-menu notification content.
- Attempt A — Calendar event + reminder (content provider insert; event
  1120 "BadgeTest", reminder minute 0): the event fired at 02:04:19 but NO
  Calendar notification record ever appeared in `dumpsys notification`
  (adb-inserted events without full reminder sync may not schedule). Event
  deleted after the test.
- Attempt B — Clock SET_ALARM intent (02:10 "BadgeTest", SKIP_UI):
  inconclusive — no deskclock notification record appeared at/after the
  fire time; whether the alarm was created could not be confirmed.
- Live records at test time (for the record): 2× android, com.android.vending
  (imp=2, silent), com.github.dyhkwong.sagernet (imp=2, persistent proxy
  service), 3× com.teslacoilsw.launcher (imp=3, group summaries).
- Popup-menu notification content: NOT TESTED — it requires an app whose
  notification actually BADGES; no such third-party notification could be
  generated in this session. Next session: trigger a real notification from
  an installed messaging/play app (e.g. ask Play to update an app, or send a
  test message), then long-press its icon and capture the popup.
- Evidence: EVI-SYS-002 (calendar-badge-check, calendar-zoom,
  playstore-popup-notif, clock-badge-check/2), dumpsys summaries
- Confidence: Observed (attempts + absence); feature status Not Tested

## OBS-SYS-115 — Chrome route design-suppressed (badge matrix final entry)

- Date / Env: 2026-11-18 22:15–22:21 / ENV-1 (Nova Prime)
- Attempt: triggered a real Chrome download from the device (small .zip
  served by a local HTTP server on the host LAN, then via `adb reverse` +
  127.0.0.1 loopback after the LAN route hit ERR_TOO_MANY_RETRIES — the
  download completed, visible in Chrome's Downloads screen).
- Result: NO download-complete notification record — Chrome holds
  `android.permission.DOWNLOAD_WITHOUT_NOTIFICATION: granted=true`, which
  suppresses notifications for user-initiated downloads by design.
- Third-party badge matrix FINAL STATE: every adb-triggerable route is
  suppressed either by Nova's own design (test button = GROUP_SUMMARY),
  by Chrome's design (DOWNLOAD_WITHOUT_NOTIFICATION), or by the Calendar
  provider (adb-inserted events don't schedule reminders). A REAL user-
  triggered notification (message, mail) would complete the popup-menu
  content test in seconds when the user is next at the phone.
- Evidence: EVI-SYS-002 (chrome-download, chrome-proceed2, chrome-retry,
  chrome-loopback, drawer-chrome-badge), dumpsys package chrome grant
  lines, Downloads screenshot
- Confidence: Observed

## OBS-SYS-116 — Deterministic badge-compat probe; popup content absent on Work-tab row

- Date / Env: 2026-11-25 / Nova_Reboot_API35 (emulator-5588, cold boot from userdata, work profile user 11 RUNNING_UNLOCKED; Nova 9.0.1 Prime foreground) — the ROADMAP P3.3 deterministic re-run promised by OBS-SYS-114/115, enabled by the new work-profile QA inventory.
- Probe: `com.vesnaresearch.enumdiag` v2 (raw-SDK build; manifest `<queries>` MAIN/LAUNCHER identical to Vesna's; exported receiver actions POST/CANCEL). POST creates channel `badge` (IMPORTANCE_HIGH, `mShowBadge=true` confirmed in `dumpsys notification`) and posts notification id=42 "BadgeTest / Deterministic badge-compat probe notification" — no group, flags=0. POST_NOTIFICATIONS granted headlessly (`pm grant`) for user 0 and user 11.
- Active record verified: `key=0|com.vesnaresearch.enumdiag|42|null|10213` importance=4 channel=badge flags=0 (user 0); later `key=11|…|1110213` (user 11). This closes OBS-SYS-114's blocker — a deterministic, adb-triggerable, badgeable third-party notification exists.
- **Vesna v0.4.84:** drawer tiles (personal + work) render with NO notification badge and the work briefcase badge intact — live confirmation of the structural conclusion (no NotificationListenerService in the APK; #200 blocked on privacy).
- **Nova:** its NotificationListenerService is grantable headlessly (`cmd notification allow_listener …notificationlistener.NotificationListener`; binding recorded in `dumpsys notification`) — OBS-SYS-159's manual disallow/restore step is now scripted. With the listener connected and the notification active, Nova's **Work-tab** long-press popup on the EnumDiag row shows only Edit / Hide / App info / Uninstall — **no notification content section and no badge observed** on the plain-list row. The personal-side grid tile could not be exercised: Nova's drawer groups collapse app rows into collapsed categories (Entertainment/Finance/…), leaving no direct grid tile for a newly installed app without further Nova settings surgery. Popup-menu notification content therefore remains UNCONFIRMED on Nova's own surfaces — now bounded: the Work-tab row path shows no content even with a live badgeable notification.
- Cleanup: CANCEL action removes both records (records count 0 verified); `cmd notification disallow_listener` restored the listener set to AiAi + Pixel Launcher exactly as before.
- Evidence: EVI-SYS-003 (vesna-drawer-badge, vesna-enumdiag-tiles, nova-hold2, nova-popup2), `dumpsys notification` key lines quoted above.
- Confidence: Observed (attempts + records); Nova badge rendering and popup content Not Tested further on Nova's own surfaces.

## OBS-SYS-117 — AVD wedge: multi-instance race corrupts adb + package manager

- Date / Env: 2026-11-25 / Nova_Reboot_API35 — operational (recovery recorded for future cycles).
- A second `emulator -avd Nova_Reboot_API35` launch against a running instance wedged the AVD: adb flapped `offline`, `pm install` streamed installs hung >300 s, `dumpsys package --user 11 <pkg>` timed out, and a stale `qemu-system-x86_64.exe` child (PID 45600) survived its dead `emulator.exe` parent holding the AVD locks.
- Recovery: `powershell Stop-Process` on the zombie **qemu child** (killing `emulator.exe` alone is insufficient), delete `hardware-qemu.ini.lock` + `multiinstance.lock`, cold boot `-no-snapshot`, then re-grant POST_NOTIFICATIONS. Full userdata image backup (`userdata-qemu.img.qcow2` 6.3 GB + base `userdata-qemu.img`) taken BEFORE the kill; `qemu-img check` clean (1 leaked cluster, no errors). Work profile user 11 intact after restart (232 packages).
- Never launch a second emulator for the same AVD; if wedged, back up userdata → kill qemu child → clear locks → cold boot.
- Confidence: Observed (recovery verified end to end).


## OBS-SYS-118 — Post-reboot AVD: launcher a11y instrumentation flakes (operational)

- Date / Env: 2026-11-25 / Nova_Reboot_API35 (emulator-5588) + Vesna_QA_Menu_API_35 (emulator-5560), after full AVD reboots.
- After an AVD reboot, keyboard/accessibility-dependent instrumentation tests (drawer holds, folder-rename dialogs, settings search Tab flows) fail in batch runs regardless of the launcher code under test — verified by A/B: the unmodified v0.4.84 baseline build reproduces the same failures (15/24 in three classes) on the same session. The same suites were green on the long-uptime system before the reboots. Isolated reruns of the same tests pass.
- Operational rule: treat post-reboot batch a11y/keyboard failures as environmental unless the same tests pass on a warmed session; warm the AVD (open drawers, exercise dialogs) before judging a code change by batch instrumentation.
- Confidence: Observed (two AVDs, A/B on code).
