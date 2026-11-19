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
| CARDS-100 | Condition-gated drawer cards | Completed | Completed (UI/persistence/gating/render with granted permission + live data — render surface = Nova Now SEARCH results, EVI-CARDS-001 r7; conditions DECORATIVE: non-gating for search render AND never surface in plain drawer under any condition state — OBS-CARDS-117) | Completed | Completed (AT-CARDS-100) | Observed |
| CARDS-101 | Custom card builder (Add card) | Discovered (builder UI inventoried — OBS-CARDS-116; build/save flow untested) | Not Started | Not Started | Not Started | Partially Observed |
| CARDS-102 | Overlay card affordances (deep link, gears, overflow, Fahrenheit render) | Completed (inventoried + units/stamp verified — OBS-CARDS-116/117) | Not Started | Not Started | Not Started | Observed |
| CARDS-103 | Calendar card variant | Completed (card renders with permission empty-state; adb grant insufficient — in-app "Connect to Calendar" is the gate; post-connect renders real events — OBS-CARDS-118) | Completed (connect flow) | Not Started | Not Started | Observed |
| CARDS-104 | Custom card builder save flow | Completed (save gated on ≥1 action; custom section; defaults; delete verified — OBS-CARDS-119; search render untested) | Not Started | Not Started | Not Started | Observed |
| CARDS-105 | Map locations card (geocoded location editor) | Completed (add/commit/delete verified; two-stage geocode — OBS-CARDS-119) | Not Started | Not Started | Not Started | Observed |
| DRAWER-100..103 | Open/close, search, contact offer | Completed | Completed/partial (EVI-DRAWER-001) | Completed | Completed | Observed/Partially Observed |
| DRAWER-104 | Drawer tab bar (free: default Apps tab) | Completed | Completed (EVI-DRAWER-002) | Completed | Completed (tabs) | Observed |
| DRAWER-105 | Nova Now cards surface | Completed | Inventory + architecture statement | Folded into CARDS-100/HOME-105 | Not Started | Partially Observed |
| DRAWER-106 | Drawer groups (tabs + smart folders) | Completed (split verdict — OBS-DRAWER-112: TABS work end-to-end [create → Tab bar Top → bar renders → switch]; SMART FOLDERS render-inert in every state incl. restart + Folders first) | Not Started | Not Started | Not Started | Observed |
| SETT-110 (covers SETT-003) | Settings search + deep-linking | Completed | Completed (OBS-SETT-110) | Completed | Completed | Observed |
| SETT-111 | Prime-gate interaction | Completed | Completed (OBS-SYS-110) | Completed | Completed | Observed |
| SYS-111..114 | Prime activation, badge pipeline, gating, third-party attempts | Completed | Completed (OBS-SYS-110..114) | Not Started (SYS-111 reference recorded in settings.md) | Not Started | Partially Observed (self-badge root cause found; third-party badge matrix inconclusive) |
| BACKUP-100..102 | Backup, share/quick-start/reset, restore | Completed | Completed (EVI-BACKUP-001/002/003; reset semantics Partial) | Completed | Completed | Observed/Partially Observed |
| APPPEAR-100 | Theme mode switch | Completed | Completed (EVI-APPPEAR-001) | Completed | Completed | Observed |
| HOME-001..015 | Home screen behavior | Most discovered (grid/labels/pages/lock/rotation settings) | Partial (OBS-HOME-001..007; page drag verified) | HOME-100..105 only | HOME-100..105 | Partially Observed |
| DRAWER-001..016 | App drawer behavior | Most discovered | Partial (open/close/search/ads/tabs verified; OBS-DRAWER-001..004, 110, 111) | DRAWER-100..105 only | DRAWER-100..103, 102b | Partially Observed |
| DOCK-001..007 | Dock | Completed (subpage inventoried) | Partial (rendering observed; paging/drag untested, EVI-DOCK-001) | DOCK-100 | In Progress | Observed (inventory) |
| FOLDER-001..008 | Folders | Completed (core flow + settings inventory) | Partial (create/rename/window verified; drag-into/pagination untested) | FOLDER-100/101 | FOLDER-100..102 | Observed (core) |
| WIDGET-001..008 | Widgets | Completed (picker/placement/resize/remove verified) | Partial (undo restore verified; config-activity cancel path untested) | WIDGET-100..102 | WIDGET-100..102 | Observed (core) |
| GESTURE-001..006, GESTURE-112 | Gestures | Completed (catalog + Prime-era assignment dialogs + binding resolution) | Partial (catalog captured; binding ambiguity resolved — drawer-open is a separate config; binding-value live test pending) | GESTURE-100 | Completed (preset bindings) | Partially Observed |
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
