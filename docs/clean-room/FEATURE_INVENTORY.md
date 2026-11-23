# Vesna Launcher — Feature Inventory (Clean-Room)

Hierarchical inventory of feature areas under investigation. This file lists
**feature areas to investigate**; it does not claim Nova has a feature until an
observation file records it with evidence. Status lives in
[COVERAGE_MATRIX.md](COVERAGE_MATRIX.md).

ID ranges are pre-allocated per area. IDs are stable: once assigned to a
behavior, an ID is never reassigned to a different behavior.

## CARDS — Nova Now cards (Prime)

| ID | Feature | Discovery status |
|---|---|---|
| CARDS-100 | Condition-gated cards | Nova 8.9.9: conditions did not gate search or surface cards in the plain drawer (OBS-CARDS-117). Nova 9.0.1: inactive Day/time and unmatched Wi-Fi did not gate name search; an active time window did not surface the test card in the plain drawer (OBS-VESNA-009/011). |
| CARDS-101 | Custom card builder (Add card: name, featured content, up to 6 pinned actions, condition set) | discovered (overlay ⋮ ▸ Add card and Cards-page "+" FAB; OBS-CARDS-116) |
| CARDS-102 | Overlay card affordances (card long-press = provider deep link; card gear = Card details w/ Save; search-bar ⋮ = Add card/API Integrations/Settings; Fahrenheit rendering; no manual location) | discovered (OBS-CARDS-116) |
| CARDS-103 | Calendar card variant (permission empty-state + New event chip; in-app "Connect to Calendar" gate — adb grant insufficient; real events post-connect) | discovered (OBS-CARDS-118) |
| CARDS-104 | Custom card builder save flow | Nova 8.9.9 baseline (OBS-CARDS-119); 9.0.1: name-only Save did not commit, action and inactive Day/time condition persisted, and drawer search matched full name, prefix and suffix (OBS-VESNA-009/013/014). |
| CARDS-105 | Map locations built-in card (sixth card, below Screenshots): config = geocoded location editor (two-stage: text → resolve → display name + map preview; unresolvable text leaves OK disabled) | discovered (OBS-CARDS-119; card's own search render untested) |

## DRAWER — App drawer

| ID | Feature | Discovery status |
|---|---|---|
| DRAWER-106 | Drawer groups (tabs + smart folders) | Nova 8.9.9: tabs render and switch, generated folders require per-item Enable (OBS-DRAWER-112). Nova 9.0.1: all seven enabled folders survive process restart, but toggling the master OFF→ON resets them (OBS-VESNA-008/010/012/014). |

## HOME — Home screen / workspace

| ID | Feature | Discovery status |
|---|---|---|
| HOME-001 | Home screen page management (add/remove/reorder pages) | pending |
| HOME-002 | Grid sizing (rows/columns) | Matched five-item geometry: Nova 9.0.1 overflowed a shortcut and restored its original layout on re-expansion; signed Vesna v0.4.53 overflowed the folder and kept both pages without losing members (OBS-VESNA-043/045; #106). |
| HOME-003 | Icon placement: add app/shortcut to workspace | pending |
| HOME-004 | Drag-and-drop rearrangement on workspace | discovered (drag requires hold-then-move) |
| HOME-005 | Drag side-by-side push/placement behavior on occupied cells | pending |
| HOME-006 | Long-press context menu on workspace items | Nova 9.0.1 and signed Vesna v0.4.54 open and pin the native Clock timer action. Vesna can rename it without losing its launch target; layout lock retains Open but blocks edits and pinning. Nova additionally exposes widgets, artwork and per-pin swipe editing (OBS-VESNA-021/046/049/051; #114). |
| HOME-007 | Wallpaper interactions (pick, scroll behavior) | discovered (Wallpapers button; wallpaper scrolling setting) |
| HOME-008 | Icon labels (show/hide/edit) | discovered (Label toggle + sub-options) |
| HOME-009 | Page transition effects and scrolling behavior | discovered (Transition effect, Infinite scroll, Page indicator settings) |
| HOME-010 | Workspace search bar / persistent search | discovered (Search bar placement: Dock Below icons) |
| HOME-011 | Empty cell behavior and drop targeting | pending |
| HOME-012 | Widget placement on workspace (see WIDGET area) | pending |
| HOME-013 | Lock workspace editing without blocking app launch | Verified in Nova 9.0.1, Vesna main and signed v0.4.48 on Android 15 (OBS-VESNA-018/022/023; #103). |
| HOME-014 | Orientation/display-size behavior of workspace | discovered (Allow Home screen rotation toggle + overflow) |
| HOME-015 | Overview / preview of all pages | pending |
| HOME-106 | Try launcher from app icon without changing system Home | Vesna 0.4.46 verified with Nova as Home on Android 15; settings links not replayed (OBS-VESNA-020; AT-HOME-106). |

## DRAWER — App drawer

| ID | Feature | Discovery status |
|---|---|---|
| DRAWER-001 | Drawer open gesture(s) | observed (swipe up; free edition) |
| DRAWER-002 | Drawer close gesture(s) | partially observed (BACK verified; swipe-down untested) |
| DRAWER-003 | Drawer layout modes | Nova 9.0.1 and signed Vesna 0.4.51 have vertical list and horizontal pages. Both retain the horizontal page on search clear; Vesna resets vertical list scroll on clear (OBS-VESNA-027/035; #108). |
| DRAWER-004 | Sorting (alphabetical, custom, etc.) | discovered (alphabetical default) |
| DRAWER-005 | Drawer search | Both launchers found and launched a local app offline. Nova separates provider buttons from web suggestions and handed a query to Maps (OBS-VESNA-028/031; #113). |
| DRAWER-006 | Scrolling and fast-scroll behavior | Nova 9.0.1 and signed Vesna 0.4.49 jump by letter and suppress the rail during search; Vesna restores it after clear (OBS-VESNA-019/026; #109). |
| DRAWER-007 | Drawer grid configuration (columns/rows) | discovered (Drawer App Grid 6×5) |
| DRAWER-008 | Hide apps from alphabetical browsing | Nova 9.0.1 and signed Vesna 0.4.50 retain pins and exact-name search after hiding. Nova also matches a hidden app by prefix; Vesna requires its full name (OBS-VESNA-023/030; #119). |
| DRAWER-009 | App categories/tabs/folders in drawer | discovered (Tab bar placement: None; Drawer groups: Prime) |
| DRAWER-010 | App context menus in drawer | Nova 9.0.1: per-app dismissal in predictions, hiding in the alphabetical list, and a persistent action shortcut dragged from the drawer to Home. Vesna 0.4.48 offers ordinary pins, not the app action (OBS-VESNA-022/024; #112, #114, #119). |
| DRAWER-011 | App launching from drawer | pending |
| DRAWER-012 | Newly installed app handling | discovered (Add icons to home screen setting) |
| DRAWER-013 | Uninstalled app handling | pending |
| DRAWER-014 | Work profile apps | discovered (Work apps in their own tab setting, checked) |
| DRAWER-015 | Alphabetical index/scroller | discovered (Fast scrollbar) |
| DRAWER-016 | Empty and error states | pending |

## DOCK — Dock / favorites bar

| ID | Feature | Discovery status |
|---|---|---|
| DOCK-001 | Dock visibility and placement | Nova retains pins when disabled and supports gesture hide/show and auto-close on app launch. No corresponding control found in Vesna v0.4.51 (OBS-VESNA-036/037; #115). |
| DOCK-002 | Dock icon count / paging | Nova 9.0.1: independent paging, optional wrap, and a fifth pin hidden at four slots but restored when capacity increased again (OBS-VESNA-021/022/029; #115). |
| DOCK-003 | Dock background styles | discovered (Dock subpage: background setting) |
| DOCK-004 | Add/remove/reorder dock items | pending |
| DOCK-005 | Folders in dock | Nova forms a persistent folder on pin overlap. Vesna v0.4.52 swaps pins and the tested workspace-folder drop did not move it into the empty dock slot (OBS-VESNA-041; #115). |
| DOCK-006 | Dock across pages (persistent vs per-page) | Nova 9.0.1 keeps the selected dock page while the workspace changes pages. Vesna v0.4.53 keeps the same four dock pins and positions across workspace pages (OBS-VESNA-048; #115). |
| DOCK-007 | Dock in landscape | Nova 9.0.1 moves the dock and search bar to a side and pages by vertical swipe in both orientations. Vesna v0.4.53 kept a fixed bottom row on the tested swipes (OBS-VESNA-040/047; #115). |

## FOLDER — Folders

| ID | Feature | Discovery status |
|---|---|---|
| FOLDER-001 | Folder creation (drop icon on icon) | observed |
| FOLDER-002 | Folder naming and rename | observed |
| FOLDER-003 | Folder open/close behavior | observed (window mode; BACK closes) |
| FOLDER-004 | Folder icon appearance (preview grid) | observed (mini previews) |
| FOLDER-005 | Add/remove apps inside folder | pending |
| FOLDER-006 | Reorder inside folder | pending |
| FOLDER-007 | Folder deletion and last-item behavior | pending |
| FOLDER-008 | Folder layout (grid size inside folder) | discovered (Grid AUTO, Scroll direction Horizontal, Mode Window) |

## WIDGET — Widgets

| ID | Feature | Discovery status |
|---|---|---|
| WIDGET-001 | Widget picker / list | observed (full-screen grouped searchable picker) |
| WIDGET-002 | Widget placement | pending |
| WIDGET-003 | Widget resizing | discovered (Widget overlap setting; resize behavior pending) |
| WIDGET-004 | Widget configuration activity | pending |
| WIDGET-005 | Widget removal | pending |
| WIDGET-006 | Widget interaction (taps pass through) | pending |
| WIDGET-007 | Widget error states (provider missing) | pending |
| WIDGET-008 | Widget restore after restart | pending |

## GESTURE — Gestures and navigation

| ID | Feature | Discovery status |
|---|---|---|
| GESTURE-001 | Swipe up / down on home screen | Nova 8.9.9 and 9.0.1: the 1100 px/800 ms path opened the drawer in repeated checks (9.0.1: 3/3, after an earlier miss). Vesna 0.4.14 still opened the workspace menu on the slow path (OBS-VESNA-003, 006–007, 009; AT-GESTURE-112.5). |
| GESTURE-002 | Double tap | discovered (Prime-gated) |
| GESTURE-003 | Long press on empty space | observed (opens customize mode) |
| GESTURE-004 | Pinch in/out | discovered (Prime-gated rows) |
| GESTURE-005 | Two-finger gestures | discovered (swipe up/down, rotation CCW/CW rows) |
| GESTURE-006 | Assignable gesture actions catalog | discovered (bindings visible: Swipe up→App drawer, Swipe down→Expand notifications) |
| GESTURE-007 | Gesture conflicts with system navigation | pending |
| GESTURE-008 | Back button behavior in launcher surfaces | partially observed (drawer/settings) |

## APPPEAR — Appearance and personalization

| ID | Feature | Discovery status |
|---|---|---|
| APPPEAR-001 | Themes (light/dark/system) | pending |
| APPPEAR-002 | Icon size | pending |
| APPPEAR-003 | Icon shape / adaptive icon handling | pending |
| APPPEAR-004 | Icon packs | pending |
| APPPEAR-005 | Label font size / color / shadow | pending |
| APPPEAR-006 | Accent colors / dynamic colors | pending |
| APPPEAR-007 | Transparency of backgrounds (drawer/dock/search) | pending |
| APPPEAR-008 | Wallpaper-dependent behaviors | pending |
| APPPEAR-009 | Scroll/search bar personalization | pending |
| APPPEAR-010 | Animation speed preferences | pending |
| APPPEAR-011 | Grid density | pending |
| APPPEAR-012 | System theme (Material You) integration | pending |

## SETT — Settings infrastructure

| ID | Feature | Discovery status |
|---|---|---|
| SETT-001 | Settings entry points (long-press menu, app) | pending |
| SETT-002 | Settings hierarchy and navigation | pending |
| SETT-003 | Search within settings (if present) | pending |
| SETT-004 | Setting persistence and restart behavior | pending |
| SETT-005 | Import/export layout | pending |

## BACKUP — Backup and restore

| ID | Feature | Discovery status |
|---|---|---|
| BACKUP-001 | Backup creation | Nova exported a local file through the system picker; no matching route found in Vesna 0.4.50 (OBS-VESNA-033; #116). |
| BACKUP-002 | Backup content categories | pending |
| BACKUP-003 | Restore operation | Nova preserved the current layout on cancellation and malformed input; the valid backup replaced it after confirmation (OBS-VESNA-033). |
| BACKUP-004 | Missing app handling on restore | Nova restored a marked placeholder that survived restart; reinstall reactivated it in the same slot (OBS-VESNA-034; #116). |
| BACKUP-005 | Widget restore behavior | pending |
| BACKUP-006 | Restore errors/warnings | pending |

## SYS — System integration

| ID | Feature | Discovery status |
|---|---|---|
| SYS-001 | Default launcher selection / HOME role | pending |
| SYS-002 | App shortcuts (long-press launcher shortcuts) | pending |
| SYS-003 | Notification badges | pending |
| SYS-004 | Permission flows | pending |
| SYS-005 | Work profile handling | pending |
| SYS-006 | Screen rotation | pending |
| SYS-007 | Display size changes | pending |
| SYS-008 | Android version differences | pending |
| SYS-009 | Accessibility integration | pending |
| SYS-010 | Package install/remove events | pending |
| SYS-011 | Storage/permission requests | pending |

## NF — Nonfunctional (observed, no invented numbers)

| ID | Feature | Discovery status |
|---|---|---|
| NF-001 | Startup responsiveness | pending |
| NF-002 | Scroll smoothness | pending |
| NF-003 | Animation behavior | pending |
| NF-004 | Stability (crashes/ANRs observed) | pending |
| NF-005 | State restoration speed | pending |
