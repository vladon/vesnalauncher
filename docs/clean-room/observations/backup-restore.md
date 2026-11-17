# Observations — Backup and Restore (ENV-1, Nova 8.9.9 free)

Evidence under `EVI-BACKUP-001`.

## OBS-BACKUP-001 — Backup & restore page inventory

- Date / Env: 2026-11-17 / ENV-1
- Navigation: Settings ▸ Backup & restore
- Observed actions on the page:
  1. Backup — "Saves your current configuration"
  2. Share backup — "Create and share a backup in one step. The backup will
     not be saved locally." (with a share affordance)
  3. Restore backup — "Roll back to a previous configuration"
  4. Quick start — "Re-launches the initial setup process"
  5. Reset to default — "Remove customizations and start from scratch"
- Evidence: EVI-BACKUP-001 (backup-page)
- Confidence: Observed (inventory)

## OBS-BACKUP-002 — Backup creation flow (verified end to end)

- Action: tapped Backup.
- Observed:
  1. The system document picker (SAF) opened at the Downloads folder.
  2. A default filename was prefilled with a timestamp pattern:
     `2026-11-17_15-48.novabackup…` (extension begins "novabackup").
  3. Tapping SAVE wrote the file; the picker closed.
  4. File verified at `/sdcard/Download/2026-11-17_15-48.novabackup` via
     shell (creation succeeded).
- Note: backup CONTENTS were not inspected (proprietary format; out of
  scope per clean-room rules). The user-visible contract is: one file,
  user-chosen location, timestamped name.
- Evidence: EVI-BACKUP-001 (backup-flow, backup-saved; shell listing)
- Confidence: Observed

## OBS-BACKUP-004 — Restore flow verified end to end

- Date / Env: 2026-11-17 / ENV-1
- Setup: state was deliberately changed after the 15:48 backup (clock widget
  placed on page 1; Play Store dragged to a NEW page 2 via edge drag).
- Action: Settings ▸ Backup & restore ▸ Restore backup ▸ pick
  `2026-11-17_15-48.novabackup` in the system picker.
- Observed:
  1. The system document picker opened (Downloads).
  2. Selecting the file showed an explicit confirmation dialog:
     "Overwrite current layout?" / "This will replace your current layout
     with the one from the backup file" with Cancel/OK.
  3. After OK, the workspace exactly matched the backup state: the widget
     added after the backup was GONE, the post-backup page 2 was REMOVED
     (single-page workspace again), and Play Store returned to its backup
     position on page 1. The Media folder (present in the backup) remained.
- Conclusion: restore reproduces the backup state completely, including
  removing items and pages created afterwards.
- Evidence: EVI-BACKUP-002 (restore-picker, restore-confirm,
  restored-workspace, page2-gone-check)
- Confidence: Observed

## OBS-BACKUP-005 — Nova Now discovery (during post-restore check)

- Action: horizontal swipe starting near the left edge of the workspace
  (single-page workspace at the time).
- Observed: a full-screen "Nova Now" overlay opened (the surface referenced
  in Style and Search settings): top search bar, a "Nova AI" section
  ("Smart. Personal. Yours to control." / smart-insights bullet / "You
  control your data. Delete anytime.") with Enable Nova AI / Disable Nova AI
  controls, a settings chip, and a "Welcome to Nova Now!" bottom sheet
  introducing Cards onboarding.
- Attribution note: this matches the "Feed page" setting ("feed pages at the
  edge of your home screen"); the exact trigger (edge zone vs gesture) is to
  be confirmed in a controlled pass.
- Evidence: EVI-BACKUP-002 (page2-gone-check capture)
- Confidence: Observed (existence/entry); internals Not Tested

## OBS-BACKUP-110 — Quick start and Reset to default flows (free edition)

- Date / Env: 2026-11-17 / ENV-1. A fresh backup
  (`2026-11-17_23-11.novabackup`) was created first as a guard rail.
- Quick start: tapping it opens the subscription page (PAYWALLED in free
  edition) — the setup re-run is a paid feature.
- Reset to default: tapping it launches the DARK-THEMED setup wizard
  directly (no separate confirmation dialog — the wizard itself is the
  flow; progress-bar steps: Search ▸ App drawer ▸ Icon style ▸ All done).
  The wizard pre-fills CURRENT values (observed: Mode Immersive, Shortcut
  Search ON, Contact Search OFF, search placements Dock (Below icons)/Top,
  icon size 125%). Completing it ("All done ▸ Go home") returned to the
  workspace with the LAYOUT INTACT (Media folder, Play Store, dock,
  shortcut all survived).
- Interpretation (honest limits): in this pass every touched setting was
  already at its default, so which values the reset actually changed is not
  measurable; the layout was NOT wiped by the reset (differs from the
  preconception that reset restores the out-of-box layout). Reset semantics
  remain partially undetermined.
- Wizard bonus: the Search page's "Search bar placement" dropdown exposed
  the full option list: None / Widget / Dock (Above icons) / Dock (Below
  icons) / Persistent.
- Evidence: EVI-BACKUP-003 (backup-page2/3, quickstart-flow, reset-flow,
  reset-wizard1..3, after-reset); fresh backup verified via shell ls
- Confidence: Observed (flows as far as run); reset semantics Partially
  Observed
