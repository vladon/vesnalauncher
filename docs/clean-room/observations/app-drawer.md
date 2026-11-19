# Observations — App Drawer (ENV-1, Nova 8.9.9: free era rounds 1-2, Prime era rounds 4+)

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

## OBS-DRAWER-111 — Swipe-down closes the drawer (verified); Tab bar reverted

- Date / Env: 2026-11-18 / ENV-1 (Prime now active)
- Tab bar revert: Tab bar placement set back to **None** (the pending action
  from OBS-DRAWER-110); the drawer subsequently shows NO tab bar. The
  dropdown's live preview also showed the tab bar rendering "Apps" + "Work"
  tabs while Top was set (the Work tab is the work-profile tab).
- Swipe-down close: with the keyboard VISIBLE, a mid-grid swipe-down did
  nothing (the keyboard consumes the gesture). After dismissing the
  keyboard (BACK), a mid-grid swipe-down (300 ms) CLOSED the drawer and
  returned to the workspace.
- Prime delta: with Prime active the drawer's ad banner is gone; the slot
  shows only a divider between predictions and the grid.
- Evidence: EVI-DRAWER-002 (tabs-reverted, tabbar-dropdown2 — preview with
  Apps/Work tabs), EVI-DRAWER-001 (swipe-down-close[2/3])
- Confidence: Observed

## OBS-DRAWER-112 — Drawer groups split verdict: TABS work end-to-end (create → bar → switch); SMART FOLDERS ship per-folder DISABLED — ⋮ ▸ Enable activates them

- Date / Env: 2026-11-19 1:24–2:06 / ENV-1 (Nova Prime)
- Settings ▸ App drawer ▸ **Drawer groups** ("Customize tabs and folders
  in the drawer") opens a full management page: a **Tabs** section
  (default "Apps — All apps, excluding hidden" tab with ⋮ + drag handle,
  plus a "+" add button) and a **Folders** section with AUTO-POPULATED
  smart folders (Entertainment [YouTube, YouTube Music], Finance [Google
  Pay], Food [Empty], Games [Empty], Shopping [Empty], Social [Messages,
  Phone], Travel [Maps]) — sparkle icons, ⋮ menus, "+" add button. The
  management UI is fully functional with Prime.
- Related toggles verified: **Smart Folders = ON** ("Automatically
  organize my apps into folders"), **Tab bar placement = None** (custom
  tabs would need the bar; only the default tab exists anyway),
  **Folders first** checkbox (unchecked pre-test).
- RENDER TEST (all negative): the drawer grid — checked top-to-bottom
  after fresh opens, after a Nova force-stop restart, and with
  "Folders first" CHECKED — never shows any folder icon: the grid stays
  a flat alphabetical app list (Exclave/F-Droid, Gmail/Google, Keep
  Notes/Maps, Now Playing/Phone, Safety/Settings, YouTube/YT Music —
  the smart-folder MEMBER apps render raw, not inside folders). The
  divider slot above the grid stays empty regardless.
- CONCLUSION (REVISED by the tab exercise, 1:56–2:06): the feature splits.
  **TABS WORK end-to-end**: created "TestTab" via Tabs ▸ + (name dialog:
  type name → ADD; first attempt failed because the keyboard shifted the
  ADD button — stale coordinates; verified by the tab VANISHING from the
  list after navigation), placed the **Tab bar = Top** (dropdown: None/
  Top/Bottom), and the drawer then rendered the tab bar with BOTH tabs
  (Apps grey | TestTab active-dark); tapping TestTab switched to it
  (empty content area — a new tab has no assignment rules yet). Full
  revert verified: Tab bar placement → None, TestTab deleted via its ⋮ ▸
  Delete (confirmation dialog), Tabs list back to Apps-only.
  **SMART FOLDERS remain render-inert**: no folder icon in ANY state
  (fresh opens, Nova restart, Folders first checked) — the grid stays a
  flat alphabetical app list (member apps render raw), and the divider
  slot above the grid stays empty. ROOT CAUSE (found 3:44): the folders
  ship PER-FOLDER DISABLED — greyed rows + faded icons in the
  management UI (vs the Apps tab's full-color row), and each folder's
  ⋮ menu offers "Enable". Enabling Entertainment rendered it in the
  drawer grid immediately (folder icon + label).
  Pattern across the build: Nova's unfinished surfaces are the
  AUTO/decorative ones (auto-located weather, auto-expiring conditions,
  auto-organized folders), while explicit user-created structures
  (tabs) function.
  **SMART FOLDERS — ROOT CAUSE FOUND (2026-11-19 3:44–3:48)**: the
  auto-created folders ship **PER-FOLDER DISABLED**. Each folder's ⋮
  menu carries "Enable"/"Disable" — the master Smart Folders toggle is
  insufficient. PROOF: Entertainment's ⋮ ▸ Enable → the folder
  IMMEDIATELY rendered in the drawer grid (alphabetical position, with
  its member apps) on a fresh drawer open — no restart needed. Disable
  restored the pre-test state. So the feature is functional but
  dark-by-default: Nova auto-categorizes, writes the folders, and hides
  every one of them until the user discovers the per-folder Enable —
  the render failure was a DISCOVERABILITY failure, not dead code
  (unlike the conditions system and the manual-location promise).
- **ALL-SEVEN RENDER VERIFIED (2026-11-19 15:18–15:26, next session)**:
  enabled every folder (Entertainment pre-enabled + Finance, Games,
  Social via ⋮ menus with screenshot-anchored Enable taps; Food,
  Shopping, Travel enabled mid-loop) — ALL SEVEN render in the drawer
  grid, alphabetically, live. Sub-quirk: EMPTY folders (Food, Games,
  Shopping) render as BARE UNLABELED icons; populated ones show labels.
  All folders re-disabled after the test (pre-test state). Settings-app
  trap: the page re-themed (red header) mid-session and the keyboard
  swallowed grid swipes as keypresses ("ft", "by" typed into the drawer
  search field) — dismiss the keyboard before grid scrolling.
- **ENABLE PERSISTENCE CONFIRMED (02:14–02:17, next day)**: after a Nova
  force-stop restart, ALL SEVEN enabled folders still render in the
  drawer grid (Entertainment/Finance/Food/Games visible; Shopping/
  Social/Travel after one scroll) — the per-folder Enable state
  persists across restarts.
- Vesna lesson: a groups/tabs feature must render its structures or not
  ship the management UI — dead config surfaces erode trust. AND: any
  auto-generated structure that starts disabled needs an affordance
  that surfaces its own enablement (a banner/badge), not a buried
  per-item menu.
- Evidence: EVI-DRAWER-001 (groups-page1, groups-page2, groups-page3,
  groups-page-open, drawer-folders-render, smartfolders-toggle,
  drawer-folders-scroll, drawer-folders-restart, foldersfirst-find,
  foldersfirst-row, foldersfirst-flipped, foldersfirst-on,
  drawer-foldersfirst, foldersfirst-restored; tab exercise:
  groups-tabs-page, tab-add-dialog, tab-named, tab-named2, tab-added2,
  tabbar-find, tabbar-row2 through tabbar-row10, tabbar-dropdown,
  tabbar-top-set, tabbar-render, tab-switched, revert-nav,
  revert-tabbar-row, revert-dropdown, revert-none-set, groups-recheck,
  groups-with-testtab, testtab-menu2, testtab-menu3, testtab-deleted,
  testtab-gone; folder enable test: entertainment-find,
  entertainment-editor, entertainment-menu, entertainment-menu2,
  folder-enabled-render, entertainment-restored)
- Confidence: Observed
