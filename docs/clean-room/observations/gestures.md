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
- Prime-era binding rows observed on the section page (crystal capture,
  2026-11-18 22:23): Home button → Nova Search with CHECKED "Only on main
  home page" (checkbox help text: "Return to the main home page with the
  first tap, execute the action with a second tap"); Swipe up → Expand
  quick settings; Swipe down → Expand notifications; Swipe left/right,
  Double tap, Double tap + swipe up/down → None.
- RESOLUTION of the earlier binding ambiguity: the free-era "Swipe up →
  App drawer" reading and the Prime-era "Expand quick settings" reading are
  BOTH correct — they are DIFFERENT systems. The drawer opens via the
  SEPARATE App drawer setting ("Open App Drawer: Swipe + gear", in
  Settings ▸ App drawer), independent of the Gestures section bindings.
  Live verification: swipe-up opened the drawer post-Prime (00:33, 21:59)
  while the Gestures section showed "Expand quick settings" — the drawer
  gesture is not the gestures-section binding.
- The Home-button semantics also fully explain every observed HOME
  behavior: HOME from another app → workspace; HOME while on the main home
  page → Nova Search drawer; HOME while the launcher process is dead →
  cold-start into the search drawer (the binding fires on the incoming
  HOME intent).
- LIVE BINDING EXPERIMENT (2026-11-18 23:25, state restored after): bound
  "Swipe up" to "Expand notifications" (safe probe action), exited to the
  workspace, performed a mid-screen swipe-up — **the drawer opened, NOT the
  notification shade**. The gestures-section "Swipe up" binding is
  NON-AUTHORITATIVE on the workspace: the separate App-drawer opening
  gesture ("Open App Drawer: Swipe") takes precedence. Meanwhile "Swipe
  down → Expand notifications" DOES fire (verified earlier — the shade
  expands). Binding restored to "Expand quick settings" after the test
  (verified via the section row).
- Vesna takeaway: if Vesna implements both a drawer-open swipe and
  assignable swipe-up actions, the drawer-open gesture must take precedence
  over (or consume) the swipe-up action on the workspace — Nova's effective
  behavior matches that model.
- Evidence: EVI-GESTURE-002 (gestures-prime, swipeup-actions,
  swipeup-actions-scrolled, apps-tab, gestures-find, gestures-r6-open,
  notif-assigned, swipeup-live-test/final, binding-restored)
- Confidence: Observed (catalog, live binding experiment, binding
  resolution); the only open item is whether the "Expand quick settings"
  swipe-up binding EVER fires on the workspace (both observed swipe-ups
  opened the drawer instead)
