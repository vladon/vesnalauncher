# Observations — Gestures and Navigation (ENV-1, Nova 8.9.9 free)

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
