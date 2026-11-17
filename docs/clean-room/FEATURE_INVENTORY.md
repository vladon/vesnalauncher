# Vesna Launcher — Feature Inventory (Clean-Room)

Hierarchical inventory of feature areas under investigation. This file lists
**feature areas to investigate**; it does not claim Nova has a feature until an
observation file records it with evidence. Status lives in
[COVERAGE_MATRIX.md](COVERAGE_MATRIX.md).

ID ranges are pre-allocated per area. IDs are stable: once assigned to a
behavior, an ID is never reassigned to a different behavior.

## HOME — Home screen / workspace

| ID | Feature | Discovery status |
|---|---|---|
| HOME-001 | Home screen page management (add/remove/reorder pages) | pending |
| HOME-002 | Grid sizing (rows/columns) | pending |
| HOME-003 | Icon placement: add app/shortcut to workspace | pending |
| HOME-004 | Drag-and-drop rearrangement on workspace | pending |
| HOME-005 | Drag side-by-side push/placement behavior on occupied cells | pending |
| HOME-006 | Long-press context menu on workspace items | pending |
| HOME-007 | Wallpaper interactions (pick, scroll behavior) | pending |
| HOME-008 | Icon labels (show/hide/edit) | pending |
| HOME-009 | Page transition effects and scrolling behavior | pending |
| HOME-010 | Workspace search bar / persistent search | pending |
| HOME-011 | Empty cell behavior and drop targeting | pending |
| HOME-012 | Widget placement on workspace (see WIDGET area) | pending |
| HOME-013 | Lock workspace (prevent changes) | pending |
| HOME-014 | Orientation/display-size behavior of workspace | pending |
| HOME-015 | Overview / preview of all pages | pending |

## DRAWER — App drawer

| ID | Feature | Discovery status |
|---|---|---|
| DRAWER-001 | Drawer open gesture(s) | pending |
| DRAWER-002 | Drawer close gesture(s) | pending |
| DRAWER-003 | Drawer layout: list vs grid modes | pending |
| DRAWER-004 | Sorting (alphabetical, custom, etc.) | pending |
| DRAWER-005 | Drawer search | pending |
| DRAWER-006 | Scrolling and fast-scroll behavior | pending |
| DRAWER-007 | Drawer grid configuration (columns/rows) | pending |
| DRAWER-008 | Hidden apps | pending |
| DRAWER-009 | App categories/tabs/folders in drawer | pending |
| DRAWER-010 | App context menus in drawer | pending |
| DRAWER-011 | App launching from drawer | pending |
| DRAWER-012 | Newly installed app handling | pending |
| DRAWER-013 | Uninstalled app handling | pending |
| DRAWER-014 | Work profile apps | pending |
| DRAWER-015 | Alphabetical index/scroller | pending |
| DRAWER-016 | Empty and error states | pending |

## DOCK — Dock / favorites bar

| ID | Feature | Discovery status |
|---|---|---|
| DOCK-001 | Dock visibility and placement | pending |
| DOCK-002 | Dock icon count / paging | pending |
| DOCK-003 | Dock background styles | pending |
| DOCK-004 | Add/remove/reorder dock items | pending |
| DOCK-005 | Folders in dock | pending |
| DOCK-006 | Dock across pages (persistent vs per-page) | pending |
| DOCK-007 | Dock in landscape | pending |

## FOLDER — Folders

| ID | Feature | Discovery status |
|---|---|---|
| FOLDER-001 | Folder creation (drop icon on icon) | pending |
| FOLDER-002 | Folder naming and rename | pending |
| FOLDER-003 | Folder open/close behavior | pending |
| FOLDER-004 | Folder icon appearance (preview grid) | pending |
| FOLDER-005 | Add/remove apps inside folder | pending |
| FOLDER-006 | Reorder inside folder | pending |
| FOLDER-007 | Folder deletion and last-item behavior | pending |
| FOLDER-008 | Folder layout (grid size inside folder) | pending |

## WIDGET — Widgets

| ID | Feature | Discovery status |
|---|---|---|
| WIDGET-001 | Widget picker / list | pending |
| WIDGET-002 | Widget placement | pending |
| WIDGET-003 | Widget resizing | pending |
| WIDGET-004 | Widget configuration activity | pending |
| WIDGET-005 | Widget removal | pending |
| WIDGET-006 | Widget interaction (taps pass through) | pending |
| WIDGET-007 | Widget error states (provider missing) | pending |
| WIDGET-008 | Widget restore after restart | pending |

## GESTURE — Gestures and navigation

| ID | Feature | Discovery status |
|---|---|---|
| GESTURE-001 | Swipe up / down on home screen | pending |
| GESTURE-002 | Double tap | pending |
| GESTURE-003 | Long press on empty space | pending |
| GESTURE-004 | Pinch in/out | pending |
| GESTURE-005 | Two-finger gestures | pending |
| GESTURE-006 | Assignable gesture actions catalog | pending |
| GESTURE-007 | Gesture conflicts with system navigation | pending |
| GESTURE-008 | Back button behavior in launcher surfaces | pending |

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
| BACKUP-001 | Backup creation | pending |
| BACKUP-002 | Backup content categories | pending |
| BACKUP-003 | Restore operation | pending |
| BACKUP-004 | Missing app handling on restore | pending |
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
