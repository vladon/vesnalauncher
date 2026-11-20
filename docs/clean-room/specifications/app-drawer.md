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
  search focus); **swipe-down over the grid with the keyboard dismissed**
  (Observed 2026-11-18; with the keyboard up the gesture is consumed by the
  keyboard and does not close).
- Expected behavior: drawer dismisses, restoring the workspace page and
  scroll position.
- Open behaviors (Not Tested): HOME press while drawer open, auto-close
  after app launch (setting exists, default off).
- Evidence: OBS-DRAWER-003, OBS-DRAWER-111 (EVI-DRAWER-001)
- Confidence: Partially Observed

### Vesna requirements

- R-DRAWER-102.1: BACK closes the drawer; if search text is present, BACK
  first clears it.
- R-DRAWER-102.2: After closing, the workspace shows its previous page.
- R-DRAWER-102.3: Optional "close on app launch" (default off).
- R-DRAWER-102.4: A downward swipe over the grid closes the drawer when the
  keyboard is dismissed; while the keyboard is shown, the keyboard consumes
  the gesture.

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

## DRAWER-104 — Drawer tab bar (free edition: default Apps tab)

- Description: an optional tab bar above the drawer grid, placed Top or
  Bottom; the free edition shows a single automatic "Apps" tab, while
  custom tab groups are a Prime feature.
- Configuration: Tab bar placement [None (default) / Top / Bottom].
- Expected behavior (observed): with Top, a pill-shaped "Apps" tab appears
  between the predictions area and the grid; the grid stays alphabetical.
  Custom groups ("Drawer groups") remain Prime-locked.
- Evidence: OBS-DRAWER-110 (EVI-DRAWER-002)
- Confidence: Observed

### Vesna requirements

- R-DRAWER-104.1: (Independent Vesna decision — reference recorded) Vesna
  may offer optional drawer tabs with automatic categories; Nova's free
  edition limits tabs to a single "Apps" tab (custom groups paywalled) —
  recorded as reference.
- R-DRAWER-104.2: Tab bar placement (none/top/bottom) is a user setting.

## DRAWER-105 — Nova Now cards surface

- Description: Cards (CARDS-100) render inside the drawer when their
  conditions are met; the drawer doubles as the "Nova Now" card surface
  (search + cards).
- Evidence: OBS-CARDS-110 (EVI-CARDS-001), OBS-BACKUP-005 (EVI-BACKUP-002)
- Confidence: Observed (architecture statement from the Card details page);
  rendered card behavior in drawer Not Tested.

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

## DRAWER-106 — Smart folders and drawer groups (verified)

- Research-verified on Nova 8.9.9 Prime (OBS-DRAWER-112; the complete
  enable/render/persistence cycle). Nova's management UI is fully
  functional (tabs + auto-categorized folders with members) but its render
  path shipped dark-by-default: every auto-generated folder starts
  PER-FOLDER DISABLED behind a per-item ⋮ ▸ Enable menu; the master
  "Smart Folders" toggle alone hides all of them. Once enabled, folders
  render immediately (alphabetical position, live), persist across
  restarts, and EMPTY folders render as bare unlabeled icons.

### Vesna requirements

- R-DRAWER-106.1: The master toggle is sufficient: ON = every generated
  folder renders; per-folder granularity lives only in the folder's own
  editor, never as a hidden second gate (Nova counter-example recorded:
  seven folders, master ON, zero rendered until each was individually
  enabled — a discoverability failure, not dead code).
- R-DRAWER-106.2: First enablement of auto-organization is a first-run
  choice presented once, surfacing the generated result immediately.
- R-DRAWER-106.3: Empty generated folders are not rendered (no bare
  unlabeled icons).
- R-DRAWER-106.4: Folder/tab enable state persists across restarts
  (verified pattern).
- R-DRAWER-106.5: A generated folder's editor exposes its rules and its
  enabled state in one surface.
