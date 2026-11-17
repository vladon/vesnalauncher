# Open Questions

Unresolved items from research. Each entry names the owner area and what
would resolve it. Triaged items are removed once answered in observations.

| # | Area | Question | Raised | Status |
|---|---|---|---|---|
| 1 | Install | Play install of Nova stuck "Pending..." behind a stuck Google Play mainline module download (`com.google.mainline.adservices`, 0 bytes, retry loop, `reviveDownload` cycles). Survived force-stop and device reboot. | 2026-11-17 | RESOLVED: LAN blocked Google Play CDN edge hosts; device-wide HTTP proxy via `adb reverse tcp:2080` + `settings put global http_proxy 127.0.0.1:2080` restored downloads; Nova 8.9.9 installed 2026-11-17 |
| 2 | Environment | Nova compatibility with Android 17 (API 37) — any deprecation warnings at first run? | 2026-11-17 | open — install/launch/setup completed without visible errors on API 37 |
| 3 | Settings | Full settings hierarchy unknown until first navigation | 2026-11-17 | RESOLVED: full inventory captured (see observations/settings.md); behavioral testing pending |
| 4 | Settings | Main settings "Search" field scope and behavior | 2026-11-17 | open |
| 5 | Edition | Does tapping a Prime-locked row open the subscription page or show a trial dialog? | 2026-11-17 | open |
| 6 | Search | "Nova Now" surface — where does it appear in the UI and what does it do? | 2026-11-17 | RESOLVED (entry found): full-screen overlay reachable by a LEFT-edge horizontal swipe on the workspace (capture copy: EVI-BACKUP-002/novanow-discovery.png); hosts search + Nova AI enable/disable + Cards onboarding ("Welcome to Nova Now!"). Exact trigger zone and Cards internals still open (see OBS-BACKUP-005, HOME-105) |
| 7 | Settings | Home screen "Lock layout" default state (visual read ambiguous, Compose toggles not exposed to uiautomator) | 2026-11-17 | open |
| 8 | Wallpaper | Workspace wallpaper image changed once mid-session with no launcher wallpaper action | 2026-11-17 | RESOLVED as external: `dumpsys wallpaper` = platform ImageWallpaper; no Nova wallpaper write observed; stock-style image also default on the Android 16 AVD. OS-level trigger unidentified — outside clean-room scope (OBS-WALL-001) |
| 9 | Gestures | HOME-key → Nova Search on workspace: opens on Android 17, did NOT on Android 16 AVD — re-verify both sides with controlled repeats | 2026-11-17 | RESOLVED as replicated difference: controlled protocol (settled workspace, two presses, 3 s gaps) run on both sides — Android 17 opens Nova Search, Android 16 stays on workspace (OBS-SYS-102, EVI-SYS-001 replicate captures). Root cause (OS input delivery vs Nova version gating) unknown — Vesna spec unaffected (Vesna default = plain HOME) |
