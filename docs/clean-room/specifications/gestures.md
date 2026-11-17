# Specifications — Gestures and Navigation

Derived from `observations/gestures.md` and the gesture catalog inventory
(OBS-SETT-008).

## GESTURE-100 — Preset gesture bindings and customization gating

- Description: the launcher ships preset gesture bindings that work out of
  the box; customizing them is a paid-tier feature in Nova (free edition:
  rows visible but locked with current bindings displayed).
- Observed catalog (bindings shown in free edition): Swipe up → App drawer;
  Swipe down → Expand notifications; Swipe left/right, Double tap, Double
  tap + swipe up/down, Two-finger swipe up/down, Pinch in/out, Two-finger
  rotation (counterclockwise/clockwise) → None; Button actions: Home button
  → Nova Search ("Only on main home page" checked).
- Expected behavior (Vesna model, implementation-independent):
  1. Preset bindings function without configuration (verified: swipe up,
     swipe down, long-press empty area, home-button action).
  2. The full assignment catalog is present in settings with each gesture's
     current binding visible.
  3. Mid-screen swipes (not starting at system-reserved edges) are claimed
     by the launcher; system gestures (shade pull from top, nav-bar zones)
     keep priority.
- Boundary conditions: gestures must not fire from system-reserved zones
  (top edge shade pull, bottom nav area).
- Evidence: OBS-GESTURE-001, OBS-GESTURE-002 (EVI-GESTURE-001,
  EVI-DRAWER-001, EVI-HOME-001, EVI-HOME-003)
- Confidence: Partially Observed (verified: swipe up/down, long-press,
  home-button; multi-touch gestures Not Tested)

### Vesna requirements

- R-GESTURE-100.1: Provide preset bindings: swipe up → drawer, swipe down →
  expand notifications, long-press empty → customization; all functional
  out of the box.
- R-GESTURE-100.2: Provide a gesture assignment catalog listing every
  supported gesture with its current binding.
- R-GESTURE-100.3: Support custom assignment (Vesna offers this free —
  independent design decision, recorded as a deviation from Nova's
  paywall).
- R-GESTURE-100.4: Launcher gestures must yield to system-reserved zones.
- R-GESTURE-100.5: Provide a home-button action binding with an "only on
  main home page" scope option (Vesna's default remains plain HOME to
  workspace — independent design decision; Nova's search-binding default
  recorded as reference).
