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
| HOME-100..103 | Customize mode, widget picker, icon popup, drag engagement | Completed | Completed (EVI-HOME-001/002) | Completed | Completed | Observed |
| HOME-104 | Page creation via edge drag | Completed | Completed (EVI-HOME-005) | Completed | Completed | Observed |
| HOME-105 | Nova Now edge surface | Completed | Partial (entry observed once; internals untested, EVI-BACKUP-002) | Completed | Completed (pointer-only) | Partially Observed |
| HOME-106 | Open workspace from app icon without replacing Home | Discovered | Vesna 0.4.46 icon opened workspace; drawer launched an app; system Home returned to Nova on Android 15 (OBS-VESNA-020) | R-HOME-106.1–3 | AT-HOME-106 core path verified; settings links not replayed | Observed on AVD |
| HOME-102 | Workspace shortcut context actions | Nova 9.0.1 on Android 15 | Opened app-scoped widgets and shortcut editor; pinned an app action, whose tap opened app setup (OBS-VESNA-021) | R-HOME-102 | AT-HOME-102; Vesna #114 open | Observed |
| HOME-002 | Workspace grid size | Nova 9.0.1 and Vesna v0.4.50 on Android 15 | Nova 4×4→2×2 reflow retained five shortcuts across two pages and restored original placement at 4×4. Vesna showed no grid control (OBS-VESNA-019/032). | Ticket #106 | Widget migration not tested | Observed |
| HOME-013, HOME-107 | Workspace editing lock | Nova 9.0.1, Vesna main and signed v0.4.48 on Android 15 | Vesna #103: signed APK blocked a drag, retained lock and app launch after restart, and restored drag after unlock (OBS-VESNA-023) | R-HOME-107.1–2 | Main replay OBS-VESNA-022; signed core replay OBS-VESNA-023 | Observed |
| Workspace auto-placement | Newly installed app shortcut preference | Nova 9.0.1 on Android 15 | ADB installs did not create a workspace shortcut with the preference off or on; store path untested (OBS-VESNA-019) | Ticket #107 | Positive Nova install path not verified | Partially Observed |
| DRAWER-003 | Drawer layout modes | Nova 9.0.1, Vesna main and signed v0.4.51 on Android 15 | Both retain the horizontal page on search clear, reset to page one on reopen, and persist the style. Only Vesna reset its vertical list to the top on clear (OBS-VESNA-027/035). | Ticket #108 | PR #126, signed layout core verified; list-position difference recorded | Observed |
| DRAWER-005 | External search and offline local search | Nova 9.0.1 and signed Vesna v0.4.50 on Android 15 | Both found and launched local Clock offline. Nova kept provider buttons but hid web suggestions offline and with the suggestion source disabled. Maps query handoff verified separately (OBS-VESNA-028/031). | Ticket #113 | Network transmission and offline provider launch not measured | Partially Observed |
| DRAWER-006 | Alphabetical fast scroll | Nova 9.0.1 and signed Vesna v0.4.49 on Android 15 | Both jump to distant letters and suppress the rail in search. Vesna reached A and Z, restored the rail on clear, and omitted it in the short list (OBS-VESNA-019/026). | Ticket #109 | PR #123 merged, tagged and independently verified in signed v0.4.49 | Observed |
| CARDS-100 | Condition-gated drawer cards | Completed | Nova 8.9.9: search and drawer (OBS-CARDS-117). Nova 9.0.1: inactive Day/time and unmatched Wi-Fi did not gate name search; an active time window did not surface the test card in the plain drawer. Weather transition and Bluetooth runtime not verified (OBS-VESNA-009/011). | Completed | AT-CARDS-100 | Observed |
| CARDS-101 | Custom card builder (Add card) | Discovered (builder UI inventoried — OBS-CARDS-116; build/save flow untested) | Not Started | Not Started | Not Started | Partially Observed |
| CARDS-102 | Overlay card affordances (deep link, gears, overflow, Fahrenheit render) | Completed (inventoried + units/stamp verified — OBS-CARDS-116/117) | Not Started | Not Started | Not Started | Observed |
| CARDS-103 | Calendar card variant | Completed (card renders with permission empty-state; adb grant insufficient — in-app "Connect to Calendar" is the gate; post-connect renders real events — OBS-CARDS-118) | Completed (connect flow) | Not Started | Not Started | Observed |
| CARDS-104 | Custom card builder: save gating, persistence and search | Nova 8.9.9: save, render and delete (OBS-CARDS-119) | Nova 9.0.1: name-only Save did not commit; action and condition persisted; drawer search matched full name, prefix and suffix (OBS-VESNA-009/013/014). | Completed | AT-CARDS-104 | Observed |
| CARDS-105 | Map locations built-in card (sixth card, below Screenshots): config = geocoded location editor (two-stage: text → resolve → display name + map preview; unresolvable text leaves OK disabled) + per-location schedules (time+day rows); search render INERT in every state incl. schedule active | Completed (add/commit/schedule/delete verified; render inert always — OBS-CARDS-119) | Not Started | Not Started | Not Started | Observed |
| DRAWER-100..103 | Open/close, search, contact offer | Completed | Nova drawer swipe obeyed the IME switch independently of app and shortcut predictions; the search affordance still focused search. Vesna v0.4.48 required an explicit tap (OBS-VESNA-025). Nova providers: OBS-VESNA-020. | Completed | AT-DRAWER-100..102; #112 | Observed/Partially Observed |
| DRAWER-008 | Per-app browsing visibility | Nova 9.0.1 and signed Vesna v0.4.50 on Android 15 | In both, a hidden app stayed out of browsing after restart and remained reachable from its pin and exact search. Nova exposed the app on prefix search; Vesna did not (OBS-VESNA-023/030). | Ticket #119 | PR #125 verified in signed v0.4.50; restart and unhide passed | Observed |
| DRAWER-010 | Drawer context actions | Nova 9.0.1 and Vesna v0.4.48 on Android 15 | Nova pinned an app action from the alphabetical drawer; it survived restart and reached the app. Vesna offered ordinary pinning only (OBS-VESNA-024). Prediction dismissal observed in OBS-VESNA-022. | #112, #114, #119 | Setup blocked verification of the compose destination | Observed |
| DRAWER-104 | Drawer tab bar (free: default Apps tab) | Completed | Completed (EVI-DRAWER-002) | Completed | Completed (tabs) | Observed |
| DRAWER-105 | Nova Now cards surface | Completed | Inventory + architecture statement | Folded into CARDS-100/HOME-105 | Not Started | Partially Observed |
| DRAWER-106 | Drawer groups (tabs + smart folders) | Nova 8.9.9: tabs and the per-folder enable/render cycle (OBS-DRAWER-112). | Nova 9.0.1: fresh master ON, folders individually OFF. All seven enabled folders survive process restart but master OFF→ON resets them (OBS-VESNA-010/012/014). | Completed | AT-DRAWER-106 | Observed |
| SETT-110 (covers SETT-003) | Settings search + deep-linking | Completed | Completed (OBS-SETT-110) | Completed | Completed | Observed |
| SETT-111 | Prime-gate interaction | Completed | Completed (OBS-SYS-110) | Completed | Completed | Observed |
| SYS-111..114 | Prime activation, badge pipeline, gating, third-party attempts | Completed | Completed (OBS-SYS-110..114) | Not Started (SYS-111 reference recorded in settings.md) | Not Started | Partially Observed (self-badge root cause found; third-party badge matrix inconclusive) |
| BACKUP-100..102 | Backup, share/quick-start/reset, restore | Completed | Nova preserved the layout on cancel/invalid input and retained a placeholder after valid restore with an app missing. Vesna v0.4.50 had no manual route (OBS-VESNA-033/034; #116, ENG-106). | Completed | Android-managed transfer and widget restore not tested | Observed/Partially Observed |
| APPPEAR-100 | Theme mode switch | Completed | Completed (EVI-APPPEAR-001) | Completed | Completed | Observed |
| HOME-001..015 | Home screen behavior | Most discovered (grid/labels/pages/lock/rotation settings) | Partial (OBS-HOME-001..007; page drag verified) | HOME-100..105 only | HOME-100..105 | Partially Observed |
| DRAWER-001..016 | App drawer behavior | Most discovered | Partial (open/close/search/ads/tabs verified; OBS-DRAWER-001..004, 110, 111) | DRAWER-100..105 only | DRAWER-100..103, 102b | Partially Observed |
| DOCK-001..007 | Dock | Completed (subpage inventoried) | Nova paging, wrap, capacity recovery, visibility, gesture hide/show and automatic app-launch close observed (OBS-VESNA-021/022/029/036/037). | DOCK-100 | Vesna #115 open; memory setting did not distinguish tested paths | Observed/Partially Observed |
| FOLDER-001..008 | Folders | Completed (core flow + settings inventory) | Partial (create/rename/window verified; drag-into/pagination untested) | FOLDER-100/101 | FOLDER-100..102 | Observed (core) |
| WIDGET-001..008 | Widgets | Completed (picker/placement/resize/remove verified) | Partial (undo restore verified; config-activity cancel path untested) | WIDGET-100..102 | WIDGET-100..102 | Observed (core) |
| GESTURE-001..006, GESTURE-112 | Gestures | Completed (catalog, live bindings and drawer-open routing) | Nova 8.9.9 and 9.0.1 retests (OBS-VESNA-003, 006–007); Vesna 0.4.16 still opens the workspace menu on the slow swipe (OBS-VESNA-014). | GESTURE-100, GESTURE-112 | Preset bindings; AT-GESTURE-112.5 covers slow-swipe/menu arbitration | Partially Observed |
| APPPEAR-001..012 | Appearance | Partial (headers/Style/Mode/theme-switch observed) | Partial (theme switch verified; Palette/per-surface colors/icon packs untested) | APPPEAR-100 only | APPPEAR-100 | Partially Observed |
| SETT-001/002 | Settings entry + hierarchy | Completed (16 entries + Style tab; search verified) | Completed (inventory; entry points partial) | SETT-110/111 only | SETT-110 | Partially Observed |
| SETT-004/005 | Setting persistence; import/export | In Progress (Backup & restore section) | Partial (backup create+restore verified = SETT-005 adjacent) | BACKUP-100..102 | BACKUP-100..102 | Partially Observed |
| APPPEAR-002/003/004/006/012 | Icon size/shape/packs/colors/theme | Discovered (inventory) | Not Started | Not Started | Not Started | Observed (inventory) |
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
