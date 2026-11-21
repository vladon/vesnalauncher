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
  main home page" scope option whose semantics are: first press returns to
  the main home page, a second press executes the bound action (Nova's
  observed model — recorded as reference; Vesna's default remains plain
  HOME).
- R-GESTURE-100.6: The drawer-open gesture is a SEPARATE configuration from
  the gesture assignments (Nova: App drawer ▸ "Open App Drawer" with its
  own gesture type + gear; the gestures-section "Swipe up" binding is
  independent and does not gate drawer opening).

## GESTURE-112 — Gesture routing precedence and assignment safety (verified)

- Research-verified on Nova 8.9.9 Prime (OBS-GESTURE-112; the complete
  binding/precedence cycle). Nova's workspace swipe-up routing is
  BINDING-DEPENDENT: an "Expand quick settings" binding FIRES and preempts
  the drawer-open gesture from BOTH swipe zones, while an "Expand
  notifications" binding fires from neither. Assignment dialogs AUTO-APPLY
  on tap with the dialog identity only in the title — a misread title
  silently mutates the wrong gesture.

### Vesna requirements

- R-GESTURE-112.1: ONE owner per gesture stream: every workspace gesture
  has exactly one bound action, resolved identically regardless of the
  swipe's start zone (Nova counter-example: the same binding fired from one
  zone pattern but not another).
- R-GESTURE-112.2: System-panel actions (quick settings, notifications)
  route through the SAME dispatcher as launcher gestures — they never
  preempt or bypass a configured launcher gesture.
- R-GESTURE-112.3: Gesture assignment dialogs confirm or offer undo —
  silent auto-apply on the wrong gesture must be impossible, and the dialog
  names its target gesture prominently (not only in its title).
- R-GESTURE-112.4: Every gesture target has a deterministic non-gesture
  affordance (Nova: the search pill's grid icon opens the drawer when the
  swipe route is ambiguous) — no behavior is reachable ONLY through a
  gesture whose routing can conflict.
- R-GESTURE-112.5: Recognize workspace swipe-up using both travel distance
  and speed. A sufficiently long, vertical swipe must be able to open the
  drawer even when it is too slow for the short-swipe path. Use
  density-independent distance and velocity criteria, not a fixed
  duration cutoff or raw screen pixels. Verify using matched trajectories
  from different workspace start zones and varying duration and distance
  independently; the drawer starts closed for each trial.
  Once movement exceeds the platform touch slop, cancel pending
  workspace long-press handling so a slow swipe does not open the
  workspace menu.

### Distance and speed evidence

- On the research Pixel, an 800 px swipe at 800 ms did not open the
  drawer in either launcher. A 1100 px swipe at the same duration opened
  it in both. Each result held for the mid-screen and lower start paths
  tested. These device-specific samples are regression evidence, not
  production thresholds (OBS-VESNA-003).
- Repeated 800 px trials on Vesna 0.4.8 opened at 660 ms (3/3) but
  not at 670 ms (0/3). At 1100 px from the same start, Vesna initially
  opened at 900 ms, but the repeat showed the workspace menu in 3/3
  trials at both 900 and 925 ms. Nova still opened at 20000 ms on
  that path. The menu outcome is a regression case, not a rule to copy
  (OBS-VESNA-003; AT-GESTURE-112.5).
