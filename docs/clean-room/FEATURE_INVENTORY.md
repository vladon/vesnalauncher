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
| HOME-001 | Home screen page management (add/remove/reorder pages) | Signed Vesna v0.4.64 immediately prunes secondary pages after last-pin removal and preserves manually added blank pages. Nova prunes by next cold launch and offers Undo. Both keep the sole primary page after its last pin is removed (OBS-VESNA-091/096/098; #153). |
| HOME-002 | Grid sizing (rows/columns) | Matched five-item geometry: Nova 9.0.1 overflowed a shortcut and restored its original layout on re-expansion; signed Vesna v0.4.53 overflowed the folder and kept both pages without losing members (OBS-VESNA-043/045; #106). |
| HOME-003 | Icon placement: add app/shortcut to workspace | pending |
| HOME-004 | Drag-and-drop rearrangement on workspace | Both place regular pins, folder members and closed folders in exact cells on existing or new neighboring pages (085/090/099/105/106/114; #139/#151/#160/#163/#167). After a cancelled cross-page drag Nova stays on the previewed page (106). |
| HOME-005 | Drag side-by-side push/placement behavior on occupied cells | App onto app creates a folder on the same page in both, and an app dropped onto a folder joins it. Across pages Nova creates the folder or adds the member; signed Vesna v0.4.68 and v0.4.69 refuse both, and v0.4.72 does the same as Nova but leaves the folder closed (110/111/115/124; #168). A closed folder dropped onto an app moves that app to a free cell in Nova and is refused in Vesna, as #139/#163 specified (106/110; #169). Full-page cases not compared. |
| HOME-006 | Long-press context menu on workspace items | Nova 9.0.1 and signed Vesna v0.4.54 open and pin the native Clock timer action. Vesna can rename it without losing its launch target; layout lock retains Open but blocks edits and pinning. Nova additionally exposes widgets, artwork and per-pin swipe editing (OBS-VESNA-021/046/049/051; #114). |
| HOME-007 | Wallpaper interactions (pick, scroll behavior) | Nova scrolls the wallpaper between Home pages by default, 593 px per page on this screen, with an On, Off and Reverse choice. Signed Vesna v0.4.71 keeps it fixed and has no setting (OBS-VESNA-122; #181). |
| HOME-008 | Icon labels (show/hide/edit) | Nova 9.0.1 and signed Vesna v0.4.54 can rename a Home pin without changing its drawer app. Vesna discards a draft on Back and offers immediate Reset; Nova kept the edit on the equivalent Back path (OBS-VESNA-042/053; #114). |
| HOME-009 | Page transition effects and scrolling behavior | Nova loops pages only with Infinite scroll on and offers None, CIRCLE, DOT and LINE page indicators (default LINE). Signed Vesna v0.4.70 never loops and always shows a Home page counter with Previous and Next buttons (OBS-VESNA-119; #174). Nova's page change slides, follows the finger and offers nine transition effects; signed Vesna v0.4.71 switches in a single frame without a setting (OBS-VESNA-122; #180). |
| HOME-010 | Workspace search bar / persistent search | discovered (Search bar placement: Dock Below icons) |
| HOME-011 | Empty cell behavior and drop targeting | pending |
| HOME-012 | Widget placement on workspace (see WIDGET area) | pending |
| HOME-013 | Lock workspace editing without blocking app launch | Verified in Nova 9.0.1, Vesna main and signed v0.4.48 on Android 15 (OBS-VESNA-018/022/023; #103). Signed v0.4.70 also blocks the whole-folder page append. Nova's locked sheet offers a padlock to unlock and an automatic relock switch; the Vesna message has no action (OBS-VESNA-120; #177). |
| HOME-014 | Orientation/display-size behavior of workspace | discovered (Allow Home screen rotation toggle + overflow) |
| HOME-015 | Overview / preview of all pages | Nova 9.0.1: pinch-in, bound to Desktop previews, opened the customize screen in seven trials, and no page overview. In edit mode a swipe past the last page shows a + that adds a blank page, and the house button sets the Home page. Signed Vesna v0.4.72: Go to page…, Reorder this page… one step at a time, Add home page and Remove empty page; page 1 stays the main page (OBS-VESNA-125; #184). |
| HOME-106 | Try launcher from app icon without changing system Home | Vesna 0.4.46 verified with Nova as Home on Android 15; settings links not replayed (OBS-VESNA-020; AT-HOME-106). |

## DRAWER — App drawer

| ID | Feature | Discovery status |
|---|---|---|
| DRAWER-001 | Drawer open gesture(s) | observed (swipe up; free edition). Signed Vesna v0.4.71 adds an off-by-default Show keyboard on open: with it on, a swipe and the drawer button focus search and show the keyboard, like Nova's Show Keyboard; with it off, neither does (OBS-VESNA-121; #112). Nova also opens the drawer with an outward swipe on the first Home page; signed Vesna v0.4.71 does nothing there (OBS-VESNA-123; #179). |
| DRAWER-002 | Drawer close gesture(s) | Both dismiss on the tested fast top swipe or via two Back presses from an active query in list mode. Signed Vesna v0.4.56 and Nova match on auto-close off/on in the list, vertical grid and first horizontal page (OBS-VESNA-063/068–070; #137). |
| DRAWER-003 | Drawer layout modes | Nova 9.0.1 and signed Vesna 0.4.51 have vertical list and horizontal pages. Both retain the horizontal page on search clear; Vesna resets vertical list scroll on clear (OBS-VESNA-027/035; #108). |
| DRAWER-004 | Sorting (alphabetical, custom, etc.) | discovered (alphabetical default) |
| DRAWER-005 | Drawer search | Both launch local apps offline. Nova separates provider buttons from web suggestions and passes the query to Maps even offline; Vesna v0.4.54 opens the installed Maps app by name only on the tested paths (OBS-VESNA-028/031/056; #113). |
| DRAWER-006 | Scrolling and fast-scroll behavior | Both jump by letter (#109). Signed v0.4.62 restores the plain vertical-list position after Android Home with auto-close on or off, matching Nova on that path (#141/#147, OBS-VESNA-077/082/089). |
| DRAWER-007 | Drawer grid configuration (columns/rows) | discovered (Drawer App Grid 6×5) |
| DRAWER-008 | Hide apps from alphabetical browsing | Nova 9.0.1 and signed Vesna 0.4.50 retain pins and exact-name search after hiding. Nova also matches a hidden app by prefix; Vesna requires its full name (OBS-VESNA-023/030; #119). |
| DRAWER-009 | App categories/tabs/folders in drawer | discovered (Tab bar placement: None; Drawer groups: Prime) |
| DRAWER-010 | App context menus in drawer | Nova 9.0.1 offers per-app dismissal in predictions, alphabetical hiding, and native app actions in drawer search. Vesna v0.4.54 lists the tested Clock action on Home but not in either drawer menu (OBS-VESNA-022/024/052; #112, #114, #119). |
| DRAWER-011 | App launching from drawer | pending |
| DRAWER-012 | Newly installed app handling | discovered (Add icons to home screen setting) |
| DRAWER-013 | Uninstalled app handling | Both remove an uninstalled app's pins from Home, the dock and folders at once, keep the other pins and members, and drop it from the drawer; the same after restart. Reinstalling the app does not bring the pins back in either (signed Vesna v0.4.68/69; OBS-VESNA-113/115). |
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
| FOLDER-005 | Add/remove apps inside folder | Signed Vesna v0.4.60 supports a single batch add and discards Cancel/Back drafts, alongside ordinary drag and layout lock (OBS-VESNA-061/081; #134). Signed v0.4.55 supports exact-cell drag-out and dissolution (OBS-VESNA-057/065; #130). |
| FOLDER-006 | Reorder and sort inside folder | Both insert at the chosen rank, including offscreen compact placement (signed Vesna v0.4.57, 072/073; #131). Vesna matches Nova on custom-label sort, stable exact ties, English case/accent comparisons, and two-member reordering and sort in `ar-EG` (074/075/078/083/084; #131/#135). |
| FOLDER-007 | Folder deletion and last-item behavior | Both dissolve to the last member and can unpin the whole folder from a packed 2×2 page with exact Undo (signed Vesna v0.4.66). Vesna's separate Ungroup still needs space and retains the members as Home pins (OBS-VESNA-057/059/076/102; #132). |
| FOLDER-008 | Folder layout (grid size inside folder) | Nova pages along the selected axis (rightward swipe advances in RTL). Signed Vesna scrolls vertically with stationary actions. All 12 members reachable in both at about 198×339 dp and 200% text, including RTL on v0.4.62 (OBS-VESNA-060/092; #133). |

## WIDGET — Widgets

| ID | Feature | Discovery status |
|---|---|---|
| WIDGET-001 | Widget picker / list | Nova offers grouped search, named previews and dimensions, and an explicit no-results state; no equivalent entry found in the tested Vesna v0.4.60/61 paths (OBS-VESNA-086; #149). |
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
| GESTURE-001 | Swipe up / down on home screen | Nova 8.9.9 and 9.0.1 open the drawer on the 1100 px path at 500–925 ms as well as in fast swipes. Signed Vesna v0.4.68/69 opened the workspace menu instead; v0.4.70 opens the drawer on slow and fast long swipes, opens neither surface on a slow 400 px swipe, and keeps the stationary-hold menu and the tile long-press (OBS-VESNA-003, 006–007, 009, 112, 115, 118; AT-GESTURE-112.5; #170). |
| GESTURE-002 | Double tap | A double tap on an empty Home cell does nothing in Nova 9.0.1, where it is bound to None, or in signed Vesna v0.4.72 (OBS-VESNA-125). |
| GESTURE-003 | Long press on empty space | observed (opens customize mode) |
| GESTURE-004 | Pinch in/out | Nova 9.0.1: fingers apart, bound to Recent apps, asked for the Accessibility permission; fingers together, bound to Desktop previews, opened the customize screen, not a page overview. Signed Vesna v0.4.72 ignores both (OBS-VESNA-125; #183). |
| GESTURE-005 | Two-finger gestures | Nova 9.0.1: a two-finger swipe up opened Nova Settings, and a two-finger tap did nothing. Signed Vesna v0.4.72 ignores two-finger input (OBS-VESNA-125; #183). |
| GESTURE-006 | Assignable gesture actions catalog | discovered (bindings visible: Swipe up→App drawer, Swipe down→Expand notifications). All default bindings of Nova 9.0.1 are read in OBS-VESNA-125; signed Vesna v0.4.72 has no gesture settings (#183). |
| GESTURE-007 | Gesture conflicts with system navigation | pending |
| GESTURE-008 | Back button behavior in launcher surfaces | partially observed (drawer/settings) |

## APPPEAR — Appearance and personalization

| ID | Feature | Discovery status |
|---|---|---|
| APPPEAR-001 | Themes (light/dark/system) | pending |
| APPPEAR-002 | Icon size | Nova changes actual desktop/drawer sizes together or independently; no equivalent size controls were found in signed Vesna v0.4.59 (OBS-VESNA-080; #146). Signed v0.4.73 adds Home icon size and Drawer icon size, with Match Home icon size on by default: artwork of 126 px at 100%, 164 px at 130% and 101 px at 80%, with Home cells unchanged (OBS-VESNA-126). |
| APPPEAR-003 | Icon shape / adaptive icon handling | Nova changes adaptive masks on Home, dock and the unfiltered app list. The tested Vesna v0.4.59 settings offer no equivalent selector (OBS-VESNA-079; #144). |
| APPPEAR-004 | Icon packs | Nova applies installed Arcticons Black to Home, dock and drawer and retains the choice after process restart. Uncovered apps keep their own artwork, and Nova returns to system artwork after the pack is removed and the launcher restarts (OBS-VESNA-095/108). No pack selector found in signed Vesna v0.4.63 with the same pack installed (#157). |
| APPPEAR-005 | Label font size / color / shadow | Nova changes label size (six steps), font, shadow and single-line behaviour from Home screen → Label, and text tone from Style → Home screen → Text (OBS-VESNA-116; #173, #155). Signed Vesna v0.4.69 offers only the show/hide switches; hiding Home labels keeps drawer names (OBS-VESNA-088/093; #152). |
| APPPEAR-006 | Accent colors / dynamic colors | Nova changes its global palette independently of theme mode and can override just the drawer background. No equivalent controls found in tested signed Vesna v0.4.63–69 (OBS-VESNA-094/097/116; #155/#158). Wallpaper colors untested. |
| APPPEAR-007 | Transparency of backgrounds (drawer/dock/search) | No transparency control found in Nova 9.0.1's app drawer color picker or dock background card; Vesna v0.4.70 settings search finds none either. Not a confirmed difference. Nova shapes the dock background with a per-corner radius and style (OBS-VESNA-117). |
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
| BACKUP-001 | Backup creation | Nova exports a local file through the system picker; no matching Settings route found in signed Vesna v0.4.54 (OBS-VESNA-033/055; #116). |
| BACKUP-002 | Backup content categories | pending |
| BACKUP-003 | Restore operation | Nova preserved the current layout on cancellation and malformed input. Valid restore after confirmation brought back a renamed native Clock action in its saved cell and it still launched Timer (OBS-VESNA-033/055). |
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
