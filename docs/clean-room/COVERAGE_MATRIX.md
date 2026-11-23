# Vesna Launcher — Coverage Matrix

Statuses: `Not Started`, `In Progress`, `Blocked`, `Completed`.
A row is `Completed` only when observation, specification, and acceptance
criteria all have recorded evidence. Confidence: `Observed`, `Partially
Observed`, `Inferred`, `Not Tested`.

Discovery = feature found and inventoried. Observation = behavior recorded
with evidence ID. Specification = implementation-independent spec written.
Acceptance = Given/When/Then criteria written.

Last updated: 2026-11-18 (rounds 1–5; ENV-1 Nova 8.9.9 — free era rounds
1–2, Nova Prime era rounds 3–5; ENV-2 Android 16 AVD comparison in round 3).

| Feature ID | Area | Discovery | Observation | Specification | Acceptance Tests | Confidence |
|---|---|---|---|---|---|---|
| HOME-100..103 | Customize mode, widget picker, icon popup, drag engagement | Completed | Nova widget catalog filters to the provider, shows sizes and returns from an empty result; no equivalent entry found in signed Vesna v0.4.60/61 (086; #149). | Completed | AT-HOME-100..103 | Partially Observed |
| HOME-104 | Page creation and navigation via edge drag | Completed | Signed Vesna v0.4.65 previews and creates the next page at the outer edge in LTR/RTL; cancel or invalid release does not create it (087/099). Its eight-page cap is preserved; Nova accepted a ninth (101). Existing-neighbor moves stay exact (085/090/099), and v0.4.68 moves a closed folder to an existing page like Nova (106). A shortcut dropped onto an app on another page makes a folder in Nova; Vesna refuses (110; #168). Vesna prunes secondary pages emptied by unpinning; both keep the sole empty page (091/096/098). Nova ran its default 4×4 grid in 098–101; see 107. | R-HOME-104.1–4 | #139, #151, #153 and #163 independently verified | Observed |
| HOME-105 | Nova Now edge surface | Completed | Partial (entry observed once; internals untested, EVI-BACKUP-002) | Completed | Completed (pointer-only) | Partially Observed |
| HOME-106 | Open workspace from app icon without replacing Home | Discovered | Vesna 0.4.46 icon opened workspace; drawer launched an app; system Home returned to Nova on Android 15 (OBS-VESNA-020) | R-HOME-106.1–3 | AT-HOME-106 core path verified; settings links not replayed | Observed on AVD |
| HOME-102 | Workspace shortcut context actions | Nova 9.0.1 and signed Vesna v0.4.54 on Android 15 | Both open and pin Clock actions. Vesna discovery is Home-role gated, but existing pins still launch after role loss (OBS-VESNA-051/052). Home label edits leave drawer names unchanged in both; Back keeps the edit in Nova but discards it in Vesna (OBS-VESNA-042/053). | R-HOME-102 | #114 / PR #129 shipped; Vesna widgets not replayed | Observed |
| HOME-002 | Workspace grid size | Nova 9.0.1 and signed Vesna v0.4.53/v0.4.54 on Android 15 | Vesna preserves pins, folders and dock on resize. Matched 4×4→2×2→4×4: Nova overflowed a shortcut and restored original cells; Vesna overflowed the folder and kept both pages (OBS-VESNA-043/044/045). Signed v0.4.54 accepts 32 items at 2×2 across eight pages and rejects 33 without changing the layout (054). | #106 / PR #128 shipped in v0.4.53 | Widget migration and Nova capacity limit not tested | Observed |
| HOME-013, HOME-107 | Workspace editing lock | Nova 9.0.1, Vesna main and signed v0.4.48 on Android 15 | Vesna #103: signed APK blocked a drag, retained lock and app launch after restart, and restored drag after unlock (OBS-VESNA-023) | R-HOME-107.1–2 | Main replay OBS-VESNA-022; signed core replay OBS-VESNA-023 | Observed |
| Workspace auto-placement | Newly installed app shortcut preference | Nova 9.0.1 and Vesna v0.4.51/v0.4.52 on Android 15 | Nova ADB install did not pin with either setting (OBS-VESNA-019). Vesna v0.4.52 opt-in pins once on the selected page; replacement and restart preserve the pin; locked/stopped installs do not pin; a full selected page does not spill to an empty page (OBS-VESNA-038/039). | #107 / PR #127 shipped in v0.4.52 | Nova store path untested | Observed for Vesna; Nova positive path unverified |
| DRAWER-002 | Drawer dismissal and app return | Nova 9.0.1 and signed Vesna v0.4.54–56 on Android 15 | Both dismiss on the tested fast top swipe (OBS-VESNA-063) and via two Back presses from an active query (068). Signed v0.4.56 passed auto-close off/on for list, vertical grid, first horizontal page, and the enabled search Enter path (069–070). | AT-DRAWER-102b; #137 | PR #138 shipped and independently verified; later drawer pages and failed launches not replayed | Observed |
| DRAWER-003 | Drawer layout modes | Nova 9.0.1, Vesna main and signed v0.4.51 on Android 15 | Both retain the horizontal page on search clear, reset to page one on reopen, and persist the style. Only Vesna reset its vertical list to the top on clear (OBS-VESNA-027/035). | Ticket #108 | PR #126, signed layout core verified; list-position difference recorded | Observed |
| DRAWER-005 | External search and offline local search | Nova 9.0.1 and signed Vesna v0.4.50/v0.4.54 on Android 15 | Both launch local apps offline. Nova keeps provider buttons without web suggestions offline, and hands an offline place query to Maps, which displays an offline notice. Vesna v0.4.54 shows local no-match for the place query and opens Maps by app name (OBS-VESNA-028/031/056). | #113 | Network transmission, opt-out traffic and other offline providers not measured | Partially Observed |
| DRAWER-006 | Fast scroll and optional position retention | Nova 9.0.1 and signed Vesna through v0.4.62 on Android 15 | Both jump by letter (019/026). Signed v0.4.62 restores the exact vertical-list offset after Clock → Android Home with auto-close on or off; Back and a filtered launch start fresh with auto-close on (089). | #109, #141, #147 | The offset is discarded on process restart; settings persist. Other layouts and screen-off not covered. | Observed |
| CARDS-100 | Condition-gated drawer cards | Completed | Nova 8.9.9: search and drawer (OBS-CARDS-117). Nova 9.0.1: inactive Day/time and unmatched Wi-Fi did not gate name search; an active time window did not surface the test card in the plain drawer. Weather transition and Bluetooth runtime not verified (OBS-VESNA-009/011). | Completed | AT-CARDS-100 | Observed |
| CARDS-101 | Custom card builder (Add card) | Discovered (builder UI inventoried — OBS-CARDS-116; build/save flow untested) | Not Started | Not Started | Not Started | Partially Observed |
| CARDS-102 | Overlay card affordances (deep link, gears, overflow, Fahrenheit render) | Completed (inventoried + units/stamp verified — OBS-CARDS-116/117) | Not Started | Not Started | Not Started | Observed |
| CARDS-103 | Calendar card variant | Completed (card renders with permission empty-state; adb grant insufficient — in-app "Connect to Calendar" is the gate; post-connect renders real events — OBS-CARDS-118) | Completed (connect flow) | Not Started | Not Started | Observed |
| CARDS-104 | Custom card builder: save gating, persistence and search | Nova 8.9.9: save, render and delete (OBS-CARDS-119) | Nova 9.0.1: name-only Save did not commit; action and condition persisted; drawer search matched full name, prefix and suffix (OBS-VESNA-009/013/014). | Completed | AT-CARDS-104 | Observed |
| CARDS-105 | Map locations built-in card (sixth card, below Screenshots): config = geocoded location editor (two-stage: text → resolve → display name + map preview; unresolvable text leaves OK disabled) + per-location schedules (time+day rows); search render INERT in every state incl. schedule active | Completed (add/commit/schedule/delete verified; render inert always — OBS-CARDS-119) | Not Started | Not Started | Not Started | Observed |
| DRAWER-100..103 | Open/close, search, contact offer | Completed | Nova IME and prediction toggles are independent (OBS-VESNA-025). Fresh app entered Nova's suggestion row after six Home launches and a restart; Vesna v0.4.53 still showed its alphabetical grid and exact search (OBS-VESNA-050). | Completed | AT-DRAWER-100..102; #112; ranking formula/refresh trigger not established | Observed/Partially Observed |
| DRAWER-008 | Per-app browsing visibility | Nova 9.0.1 and signed Vesna v0.4.50 on Android 15 | In both, a hidden app stayed out of browsing after restart and remained reachable from its pin and exact search. Nova exposed the app on prefix search; Vesna did not (OBS-VESNA-023/030). | Ticket #119 | PR #125 verified in signed v0.4.50; restart and unhide passed | Observed |
| DRAWER-010 | Drawer context actions | Nova 9.0.1 and Vesna v0.4.54 on Android 15 | Nova opens Clock's native action from drawer search. Vesna exposes it on Home, but not in filtered or unfiltered drawer menus, even as default Home (OBS-VESNA-052). Earlier Nova action pinning and prediction dismissal: OBS-VESNA-022/024. | #112, #114, #119 | Gmail compose destination still unverified | Observed |
| DRAWER-104 | Drawer tab bar (free: default Apps tab) | Completed | Completed (EVI-DRAWER-002) | Completed | Completed (tabs) | Observed |
| DRAWER-105 | Nova Now cards surface | Completed | Inventory + architecture statement | Folded into CARDS-100/HOME-105 | Not Started | Partially Observed |
| DRAWER-106 | Drawer groups (tabs + smart folders) | Nova 8.9.9: tabs and the per-folder enable/render cycle (OBS-DRAWER-112). | Nova 9.0.1: fresh master ON, folders individually OFF. All seven enabled folders survive process restart but master OFF→ON resets them (OBS-VESNA-010/012/014). | Completed | AT-DRAWER-106 | Observed |
| SETT-110 (covers SETT-003) | Settings search + deep-linking | Completed | Completed (OBS-SETT-110) | Completed | Completed | Observed |
| SETT-111 | Prime-gate interaction | Completed | Completed (OBS-SYS-110) | Completed | Completed | Observed |
| SYS-111..114 | Prime activation, badge pipeline, gating, third-party attempts | Completed | Completed (OBS-SYS-110..114) | Not Started (SYS-111 reference recorded in settings.md) | Not Started | Partially Observed (self-badge root cause found; third-party badge matrix inconclusive) |
| BACKUP-100..102 | Backup, share/quick-start/reset, restore | Completed | Nova preserves layout on cancel/invalid input, retains a placeholder for an absent app, and restores a renamed native action that remains launchable after restart (OBS-VESNA-033/034/055). Vesna v0.4.54 Settings search found no manual route. | Completed | #116, ENG-106; Android-managed transfer and widget restore not tested | Observed/Partially Observed |
| APPPEAR-100 | Theme mode switch | Completed | Completed (EVI-APPPEAR-001) | Completed | Completed | Observed |
| HOME-001..015 | Home screen behavior | Most discovered (grid/labels/pages/lock/rotation settings) | Partial (OBS-HOME-001..007; page drag verified) | HOME-100..105 only | HOME-100..105 | Partially Observed |
| DRAWER-001..016 | App drawer behavior | Most discovered | Open/close/search/ads/tabs observed. Signed v0.4.62 restores the vertical-list offset after Android Home with both options on, matching the plain Nova list (082/089). | DRAWER-100..105 only | DRAWER-100..103, 102b; #141 and #147 independently verified | Partially Observed |
| DOCK-001..007 | Dock | Completed (subpage inventoried) | Nova paging, wrap, capacity recovery, visibility and auto-close observed (OBS-VESNA-021/022/029/036/037). Landscape: Nova pages vertically, Vesna keeps a bottom row (040/047). Nova creates a dock folder on overlap, Vesna swaps pins (041). Both docks stay fixed across workspace pages; Nova preserves the selected dock page (048). | DOCK-100 | Vesna #115 open; memory semantics not fully verified | Observed/Partially Observed |
| FOLDER-001..008 | Folders | Completed (core flow + settings inventory) | Signed Vesna matches Nova on rank insertion, sorting, batch add and drag-out to existing pages (072–075/078/081/083/084/085/090), member moves to a new page (100/105), packed whole-folder unpin with exact Undo (076/102/103) and whole-folder moves to an existing page (104/106). Cross-page drops of a shortcut onto an app create a folder in Nova but are refused by Vesna (110; #168). Dragged past the last page, a whole folder creates a page in Nova but not in Vesna v0.4.68 (109); Nova also pushes an occupant where Vesna refuses (106/110); overflow navigation differs (060/092). | FOLDER-100/101 | #130, #131, #132, #134, #135, #139, #160 and #163 verified; #133/#167/#168/#169 separate | Observed/Partially Observed |
| WIDGET-001..008 | Widgets | Nova picker, placement, resize and remove observed | Nova search, empty state and restore after clearing verified. No picker entry found on Vesna v0.4.60/61 (086). Placement/configuration cancellation not compared in this pass. | WIDGET-100..102 | #149 covers catalog browsing; configuration-activity cancel untested | Observed/Partially Observed |
| GESTURE-001..006, GESTURE-112 | Gestures | Completed (catalog, live bindings and drawer-open routing) | Nova 8.9.9 and 9.0.1 retests (OBS-VESNA-003, 006–007); Vesna 0.4.16 still opens the workspace menu on the slow swipe (OBS-VESNA-014). | GESTURE-100, GESTURE-112 | Preset bindings; AT-GESTURE-112.5 covers slow-swipe/menu arbitration | Partially Observed |
| APPPEAR-001..012 | Appearance | Partial (headers/Style/Mode/theme-switch observed) | Signed Vesna v0.4.63 independently hides Home/dock labels while retaining list/folder names, matching Nova's Home/list independence (088/093; #152). Shape, size, palette, pack and drawer-background gaps tracked separately (079/080/094/095/097). | APPPEAR-100 only | APPPEAR-100; #144/#146/#155/#157/#158 | Partially Observed |
| SETT-001/002 | Settings entry + hierarchy | Completed (16 entries + Style tab; search verified) | Completed (inventory; entry points partial) | SETT-110/111 only | SETT-110 | Partially Observed |
| SETT-004/005 | Setting persistence; import/export | In Progress (Backup & restore section) | Partial (backup create+restore verified = SETT-005 adjacent) | BACKUP-100..102 | BACKUP-100..102 | Partially Observed |
| APPPEAR-004/006/012 | Icon packs, colors and theme detail | Discovered (inventory) | Nova recolors Settings or the drawer independently, applies installed Arcticons across three surfaces, keeps system artwork for uncovered apps and falls back after the pack is removed (094/095/097/108). No equivalent controls found in tested signed Vesna v0.4.63/64 (094/095/097). Wallpaper colors and other surface overrides untested. | Not Started | #155 custom palette; #157 installed pack; #158 drawer background | Partially Observed |
| DRAWER-009/014/015 | Drawer groups/work apps/fast-scroll | Discovered (Prime era: tabs verified) | Partial | DRAWER-104 | Completed (tabs) | Partially Observed |
| SYS-001..011 (rest) | Default launcher, shortcuts, badges config, rotation, accessibility | Partial (SYS-001 observed; badges config inventoried) | Partial (OBS-SYS-111..114) | Not Started | Not Started | Partially Observed |
| BACKUP-001..006 | Backup/restore detail | Completed (section) | Partial (see BACKUP-100..102) | Covered by BACKUP-100..102 | Completed | Observed/Partially |
| NF-001..005 | Nonfunctional | Not Started (anecdotes only: startup, scroll, stability unmeasured) | Not Started | Not Started | Not Started | Not Tested |

## Coverage notes

- This matrix tracks **discovered-feature** coverage only. Nova's total
  feature surface is unknown until navigation is complete; no "100%
  coverage" claim may be made before an independent review.
- **Edition timeline**: rounds 1–2 on the free edition (gating observed:
  badges/gestures/hide-apps/drawer-groups Prime-locked, Nova AI Plus-locked,
  drawer ads); Nova Prime (lifetime, Pro Key APK) activated 2026-11-18 —
  all gated features became researchable from round 3 onward; drawer ads
  disappeared with Prime.
- Historical free-era observations remain valid as free-tier records; the
  stale "Blocked" badge-rendering entry was superseded by OBS-SYS-112/113
  (root cause: test button posts a GROUP_SUMMARY — unbadged by design) and
  OBS-SYS-114 (third-party badge matrix inconclusive).

- **Product decisions (2026-11-23)**: the maintainer recorded scope
  for the open research issues in
  [PR #165](https://github.com/vladon/vesnalauncher/pull/165).
  Implementation is paused; later verification follows the decided
  scope rather than the original acceptance text.
  - #144: two shapes only, the current default and a rounded
    square, for supported adaptive icons on Home, in the dock and
    in the drawer.
  - #146: separate Home and drawer sizes with **Follow Home** as
    the drawer default; artwork only, without cell or placement
    changes.
  - #155: curated palettes only, each with contrast-checked Light
    and Dark variants, independent of theme mode.
  - #157: after #144 and #146; one format verified with Arcticons;
    system artwork for unmatched icons or a removed pack; no shape
    mask on pack artwork.
  - #158: deferred until #155; a separate drawer setting with
    **Follow theme**.
  - #149: no browse-only picker; revisit with a working
    add-widget flow.
  - #133: vertical scrolling stays; horizontal paging is deferred
    until vertical navigation is shown to hinder access.
  - #112: split; only an off-by-default option that focuses
    search and opens the keyboard when the drawer opens;
    launch history and frequency suggestions deferred.
  - #113: split; only optional off-by-default actions that pass
    a query to the chosen app on an explicit tap; no network
    suggestions while typing and no change to offline search.
  - #115: in stages, visibility and icon size first, then
    capacity and paging; overflow carries to later pages in
    order, and a change is rejected if the dock cannot hold
    every pin.
  - #116: explicit export to a user-selected file and restore
    from it, with a preview and explicit confirmation before
    an atomic replacement; no launcher-managed cloud backup.
