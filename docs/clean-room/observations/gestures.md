# Observations — Gestures and Navigation (ENV-1, Nova 8.9.9: free era rounds 1-2, Prime era rounds 4+)

Evidence under `EVI-GESTURE-001`. Gesture catalog inventory:
observations/settings.md OBS-SETT-008.

## OBS-GESTURE-001 — Default bindings function; customization is Prime-gated

- Date / Env: 2026-11-17 / ENV-1
- Initial state: workspace, gesture navigation active, single page.
- Action: swipe downward starting mid-screen (y≈700) over the workspace
  (300 ms), NOT from the status bar area.
- Observed: the system notification shade expanded. A shade pull via system
  gesture requires starting at the top edge, so this is attributed to Nova's
  "Swipe down → Expand notifications" default binding.
- Conclusion: in the free edition, the preset default gesture bindings are
  ACTIVE and function; Prime gating removes the ability to customize them
  (rows greyed with Prime ribbon in Gestures & inputs).
- Related verified gesture behaviors (elsewhere in this research):
  - Swipe up (mid-screen) opens the app drawer (OBS-DRAWER-001).
  - Long-press empty workspace opens customization (OBS-HOME-001).
  - HOME key default binding → Nova Search when on the workspace
    (OBS-HOME-006).
- Evidence: EVI-GESTURE-001 (swipe-down-home)
- Confidence: Observed (swipe down, swipe up, long-press, home button);
  two-finger/pinch/rotation Not Tested (not synthesizable reliably via adb).

## OBS-GESTURE-002 — Gesture nav boundary

- Observation: system gesture navigation (back/home/recents zones) coexists
  with Nova's gestures; Nova's drawer-open swipe uses a mid-screen start
  zone and the system shade uses the top edge. No conflict observed in
  tested cases.
- Evidence: EVI-DRAWER-001, EVI-GESTURE-001
- Confidence: Partially Observed

## OBS-GESTURE-112 — Prime-unlocked gesture assignment catalog (verified)

- Date / Env: 2026-11-18 / ENV-1 (Nova Prime)
- Action: Gestures & inputs ▸ tapped "Swipe up".
- Observed: with Prime, the assignment dialogs are unlocked and reveal the
  full action catalog as a tabbed list: tabs **NOVA / APPS / SHORTCUTS**
  (APPS = launch any installed app; SHORTCUTS = launch any app shortcut).
  The NOVA tab (scrolled) exposes at least: None, App drawer, Expand
  notifications, Expand quick settings, Recent apps, Screen lock, Nova
  Search, Nova Now, Assistant, Toggle notification bar, Toggle dock,
  Default page, Jump to page, Desktop previews, Nova Settings.
- Prime-era binding rows observed on the section page: Home button →
  Nova Search with an "Only on main home page" checkbox below it; and the
  "Swipe up" row's value line read "Expand quick settings" (round 1, free
  edition, read "App drawer") — see the caveat below.
- Caveat: the Swipe-up value reading needs a controlled re-check — a
  swipe-up at 00:33 (after the Prime restart) still opened the drawer, and
  swipe-up also opened the drawer during later tests, so either the binding
  changed mid-session (e.g. via the Prime profile import) or the subtitle
  mapping differs when rows are expanded. Do not treat either reading as
  final without a fresh controlled observation.
- Evidence: EVI-GESTURE-002 (gestures-prime, swipeup-actions,
  swipeup-actions-scrolled, apps-tab, gestures-find)
- Confidence: Observed (catalog); binding values need re-check
