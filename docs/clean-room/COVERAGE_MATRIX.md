# Vesna Launcher — Coverage Matrix

Statuses: `Not Started`, `In Progress`, `Blocked`, `Completed`.
A row is `Completed` only when observation, specification, and acceptance
criteria all have recorded evidence. Confidence: `Observed`, `Partially
Observed`, `Inferred`, `Not Tested`.

Discovery = feature found and inventoried. Observation = behavior recorded
with evidence ID. Specification = implementation-independent spec written.
Acceptance = Given/When/Then criteria written.

Last updated: 2026-11-17 (session 1, Nova 8.9.9 free on ENV-1).

| Feature ID | Area | Discovery | Observation | Specification | Acceptance Tests | Confidence |
|---|---|---|---|---|---|---|
| HOME-100..103 | Customize mode, widget picker, icon popup, drag engagement | Completed | Completed (EVI-HOME-001/002) | Completed | Completed | Observed |
| APPPEAR-100 | Theme mode switch | Completed | Completed (EVI-APPPEAR-001) | Completed | Completed | Observed |
| BACKUP-100..102 | Backup, share/reset inventory, restore | Completed | Completed (create+restore verified; EVI-BACKUP-001/002) | Completed | Completed | Observed |
| HOME-104/105 | Page management drag, Nova Now edge surface | Completed | Completed/Partial (EVI-HOME-005, EVI-BACKUP-002) | Completed | Completed | Observed / Partially Observed |
| WIDGET-100..102 | Placement, resize, popup/removal+undo | Completed | Completed (undo verified, EVI-WIDGET-004) | Completed | Completed | Observed |
| GESTURE-100 | Preset bindings + catalog | Completed | Completed (EVI-GESTURE-001) | Completed | Completed | Partially Observed (multi-touch untested) |
| DOCK-100 | Dock structure/config | Completed | Completed (EVI-DOCK-001) | Completed | In Progress | Observed (inventory) |
| DRAWER-100..103 | Open/close, search, contact offer | Completed | Completed/partial (EVI-DRAWER-001) | Completed | Completed | Observed/Partially Observed |
| FOLDER-100..101 | Folder create, rename, window | Completed | Completed (EVI-FOLDER-001) | Completed | Completed | Observed |
| SETT-001 | Settings entry points | Completed | In Progress (workspace shortcut observed) | Not Started | Not Started | Partially Observed |
| SETT-002 | Settings hierarchy | Completed | Completed (full inventory, 16 entries + Style tab) | Not Started | Not Started | Observed |
| SETT-003 | Settings search | In Progress (field exists) | Not Started | Not Started | Not Started | Not Tested |
| SETT-004 | Setting persistence/restart | Not Started | Not Started | Not Started | Not Started | Not Tested |
| SETT-005 | Import/export layout | In Progress (Backup & restore section found) | Not Started | Not Started | Not Started | Not Tested |
| APPPEAR-002 | Icon size | Completed (discovered; 125% default, slider+steppers) | Not Started | Not Started | Not Started | Observed (inventory) |
| APPPEAR-003 | Icon shape | Completed (Round/Squircle/Rounded square/Flower/More/Custom) | Not Started | Not Started | Not Started | Observed (inventory) |
| APPPEAR-004 | Icon packs | In Progress (Icon theme row found, empty without packs) | Not Started | Not Started | Not Started | Not Tested |
| APPPEAR-012 | System theme integration | In Progress (Style: Mode=Follow system; Palette=System) | Not Started | Not Started | Not Started | Observed (inventory) |
| APPPEAR-006 | Accent/dynamic colors | In Progress (Palette + per-surface color controls found) | Not Started | Not Started | Not Started | Observed (inventory) |
| GESTURE-001..006 | Gestures | Completed (full catalog inventoried) | Partially (all assignment Prime-gated in free; bindings shown) | Not Started | Not Started | Partially Observed |
| DRAWER-009 | Drawer tabs/groups | In Progress (Tab bar placement=None; Drawer groups=Prime) | Not Started | Not Started | Not Started | Observed (inventory) |
| DRAWER-014 | Work profile apps | In Progress ("Work apps in their own tab" setting found) | Not Started | Not Started | Not Started | Not Tested |
| DRAWER-015 | Fast scroll | In Progress (Fast scrollbar toggle found, on) | Not Started | Not Started | Not Started | Not Tested |
| FOLDER-008 | Folder layout | In Progress (Folders section inventoried: Mode/Window, Grid AUTO, Scroll direction Horizontal) | Not Started | Not Started | Not Started | Observed (inventory) |
| WIDGET-003 | Widget resizing | In Progress (Widget overlap + corner radius settings found) | Not Started | Not Started | Not Started | Not Tested |
| SYS-003 | Notification badges | In Progress (section found; Prime-gated) | Not Started | Not Started | Not Started | Observed (inventory) |
| SYS-001 | Default launcher selection | Completed (system HOME-role dialog observed during setup) | In Progress | Not Started | Not Started | Observed |
| SYS-006 | Screen rotation | In Progress ("Allow Home screen rotation" off + overflow) | Not Started | Not Started | Not Started | Not Tested |
| HOME-001..015 | Home screen | Not Started (Dock subpage known to exist) | Not Started | Not Started | Not Started | Not Tested |
| DRAWER-001..016 (rest) | App drawer behavior | Not Started | Not Started | Not Started | Not Started | Not Tested |
| DOCK-001..007 | Dock | In Progress (Dock subpage exists under Home screen) | Not Started | Not Started | Not Started | Not Tested |
| FOLDER-001..007 (rest) | Folders behavior | Not Started | Not Started | Not Started | Not Started | Not Tested |
| WIDGET-001..008 (rest) | Widgets | Not Started | Not Started | Not Started | Not Started | Not Tested |
| BACKUP-001..006 | Backup/restore | In Progress (Backup & restore section found) | Not Started | Not Started | Not Started | Not Tested |
| SYS-002/004/005/007..011 | System integration | Not Started | Not Started | Not Started | Not Started | Not Tested |
| NF-001..005 | Nonfunctional | Not Started | Not Started | Not Started | Not Started | Not Tested |

## Coverage notes

- This matrix tracks **discovered-feature** coverage only. Nova's total
  feature surface is unknown until navigation is complete; no "100%
  coverage" claim may be made before an independent review.
- Discovery states above refer to the settings hierarchy inventory (session 1).
  Behavioral observation has not started except where noted.
- Free-edition gating observed: Notification badges, Hide apps, Drawer groups,
  Folders first, and all customizable gestures are Prime-locked; Nova AI is
  Plus-locked. Vesna is free — these are recorded as *reference behaviors
  unavailable for direct testing* in the current environment.
