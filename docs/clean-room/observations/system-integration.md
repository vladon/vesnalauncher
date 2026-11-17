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
