# Observations — App Drawer (ENV-1, Nova 8.9.9 free)

Evidence under `EVI-DRAWER-001`. Environment: drawer style Vertical, grid
6×5, search bar Top, Show Keyboard on, Frequently used apps on, Fast
scrollbar on.

## OBS-DRAWER-001 — Drawer opens with swipe up and focuses search

- Initial state: home screen page 1.
- Action: swipe up from the lower part of the screen (300 ms).
- Observed:
  - drawer opens covering the workspace;
  - search bar at the top is focused with keyboard immediately visible
    (matches "Show Keyboard" default on);
  - "Frequently used" predictions section under the search bar (an app
    prediction chip row + app icon);
  - alphabetical app grid below;
  - a contact-suggestions bar ("Tap to enable contact suggestions") with
    enable and dismiss controls;
  - an advertisement banner between the predictions area and the app grid
    (free edition; ad content rotates).
- Evidence: EVI-DRAWER-001 (drawer-open)
- Confidence: Observed

## OBS-DRAWER-002 — Drawer search filtering

- Action: type "ca" into the focused drawer search field.
- Observed:
  - app results filtered, matched prefix rendered bold (Calculator, Calendar,
    Camera);
  - the ad banner remains between results area and provider row;
  - "Search with" provider row (search engines/services);
  - web suggestions list below, each row with a magnifier icon and an
    open-externally icon;
  - keyboard suggestion strip visible.
- Note: with "Limit apps to one row" checked, only the first row of app
  matches shows (consistent with setting; single-row result observed).
- Evidence: EVI-DRAWER-001 (drawer-search-ca)
- Confidence: Observed

## OBS-DRAWER-003 — Drawer closes with BACK

- Action: BACK (one or two presses; first clears search focus when text
  present), from the open drawer.
- Observed: drawer closes; home screen restored.
- Open questions: exact behavior of single BACK with/without focused search;
  swipe-down close; HOME-button close.
- Evidence: EVI-DRAWER-001 (drawer-close-back)
- Confidence: Observed (BACK path only)

## OBS-DRAWER-004 — Free-edition ads in the drawer

- Observed: an advertisement banner is shown inside the drawer (both in the
  all-apps view and during search). This is a free-edition monetization
  behavior. Vesna (free, no ads) records this as reference-only: the
  *placement slot* exists in Nova's drawer layout between the
  predictions/search area and the app grid.
- Evidence: EVI-DRAWER-001 (drawer-open, drawer-search-ca)
- Confidence: Observed

## OBS-DRAWER-110 — Drawer tabs in the free edition

- Date / Env: 2026-11-17 / ENV-1
- Action: App drawer settings ▸ Tab bar placement [None] → selected "Top";
  opened the drawer.
- Observed: options are None / Top / Bottom. With Top, the drawer shows a
  tab bar under the search area containing a single default tab "Apps"
  (pill-highlighted). Custom tab groups remain Prime ("Drawer groups"
  greyed) — the free edition gets the tab UI with the automatic Apps tab.
- State note: the research session was suspended while the device came
  under active human use (a personal Calendar app was in the foreground);
  **Tab bar placement was left at "Top" — the revert to None is PENDING**
  and must be completed at the next session start before other tests.
- Evidence: EVI-DRAWER-002 (tab-options, tabs-open)
- Confidence: Observed
