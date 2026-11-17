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

## OBS-BACKUP-003 — Restore / Quick start / Reset (inventoried, not executed)

- Restore backup, Quick start, and Reset to default were identified but NOT
  executed on this device (restore would replace the current configuration;
  reset would wipe customizations — both destructive on the user's personal
  device). Their flows remain to be tested deliberately, ideally after
  re-creating a fresh backup, in a later session.
- Evidence: EVI-BACKUP-001 (backup-page)
- Confidence: Not Tested (flows); Observed (existence/labels)
