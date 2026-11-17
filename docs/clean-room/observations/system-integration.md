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
- Persistence: proxy mapping active for the research session; required for
  further Play downloads on this network. App launching/UI unaffected.
- Evidence: EVI-SYS-100, EVI-SYS-101 (logcat excerpts, curl outputs,
  screenshots play-after-reboot.png, play-reset1.png, nova-first-run.png)
- Confidence: Observed
