# Specifications — App Drawer

Derived from `observations/app-drawer.md` and settings observations. Free
edition on ENV-1; ads and Prime gating recorded as Nova-specific reference
behavior, not Vesna requirements (Vesna is ad-free by product definition).

## DRAWER-100 — Drawer opening gesture and initial state

- Description: the app drawer opens over the workspace via an upward swipe;
  with "show keyboard on open" enabled, search is focused immediately.
- Preconditions: launcher on workspace; drawer-open gesture set to Swipe
  (default).
- Trigger: swipe up from the lower portion of the screen.
- Expected behavior:
  1. Drawer covers the workspace.
  2. Search bar at top receives focus; the soft keyboard is presented when
     the show-keyboard option is enabled.
  3. A frequently-used predictions section appears above the app list.
  4. The app grid lists launchable apps alphabetically.
- State transitions: workspace → drawer (with search focused).
- Configuration: drawer style (Vertical default), grid size, search bar
  placement (Top default), show keyboard (on default), frequently used apps
  (on default).
- Boundary conditions: swipe must not conflict with gesture navigation
  (upward swipe from the very bottom edge belongs to the system).
- Evidence: OBS-DRAWER-001 (EVI-DRAWER-001)
- Confidence: Observed

### Vesna requirements

- R-DRAWER-100.1: Swipe up opens the drawer.
- R-DRAWER-100.2: On open, drawer search is focusable; keyboard shown iff
  the show-keyboard setting is enabled.
- R-DRAWER-100.3: Frequently-used predictions shown when enabled.
- R-DRAWER-100.4: Apps listed alphabetically (default sort).

## DRAWER-101 — Drawer search filtering and suggestions

- Description: typing in drawer search filters apps live and offers extended
  results.
- Expected behavior:
  1. App list narrows to matches; matched text prefix is visually
     emphasized.
  2. Provider row offers web/store search hand-offs (configurable set).
  3. Web suggestions list with per-item open affordances.
  4. On-device shortcut search participates when enabled (local indexing).
- Configuration: Limit apps to one row (default on) caps visible app matches.
- Evidence: OBS-DRAWER-002 (EVI-DRAWER-001)
- Confidence: Observed (apps/providers/web suggestions); shortcut search
  results Not Tested.

### Vesna requirements

- R-DRAWER-101.1: Typing filters apps case-insensitively with emphasized
  match prefix.
- R-DRAWER-101.2: Optional provider row for external search hand-off.
- R-DRAWER-101.3: Optional web suggestion rows; each opens the external
  service.
- R-DRAWER-101.4: "Limit apps to one row" caps app result rows when enabled.

## DRAWER-102 — Drawer closing

- Description: the drawer closes and returns to the workspace.
- Trigger (verified): BACK (with search text present, first BACK clears
  search focus).
- Expected behavior: drawer dismisses, restoring the workspace page and
  scroll position.
- Open behaviors (Not Tested): swipe-down close, HOME press while drawer
  open, auto-close after app launch (setting exists, default off).
- Evidence: OBS-DRAWER-003 (EVI-DRAWER-001)
- Confidence: Partially Observed

### Vesna requirements

- R-DRAWER-102.1: BACK closes the drawer; if search text is present, BACK
  first clears it.
- R-DRAWER-102.2: After closing, the workspace shows its previous page.
- R-DRAWER-102.3: Optional "close on app launch" (default off).

## DRAWER-103 — Contact suggestions bar (permission-gated)

- Description: a bar in the drawer offers enabling contact suggestions.
- Expected behavior: shows an enable affordance; enabling requests the
  Contacts permission through the standard system dialog; a dismiss control
  hides the offer.
- Evidence: OBS-DRAWER-001 (EVI-DRAWER-001)
- Confidence: Observed (presence); enable flow Not Tested.

### Vesna requirements

- R-DRAWER-103.1: Contact search is opt-in: the drawer presents a discrete
  enable offer, and enabling routes through the standard runtime-permission
  dialog.

## Note on free-edition ads (reference only)

Nova's free edition displays an advertisement banner inside the drawer
(between predictions and grid; persists during search). Vesna's product
policy is ad-free; no Vesna requirement is derived from this behavior. It is
recorded because it explains layout space usage in Nova's drawer that Vesna
will reclaim for content.
