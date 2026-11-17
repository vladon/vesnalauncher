# Specifications — Backup and Restore

Derived from `observations/backup-restore.md`.

## BACKUP-100 — Backup creation

- Description: the user can export the current launcher configuration to a
  single file at a location chosen through the system file picker.
- Trigger: Settings ▸ Backup & restore ▸ Backup.
- Expected behavior:
  1. The system document picker opens with a default timestamped filename
     (observed pattern: `<date>_<time>.novabackup…`).
  2. Confirming writes one backup file to the chosen location.
  3. The backup captures the user-visible configuration (layout and
     settings) sufficient to restore the launcher state later. (Format is
     proprietary; Vesna defines its own format independently.)
- Boundary conditions: storage-full and picker-cancel paths to verify.
- Evidence: OBS-BACKUP-002 (EVI-BACKUP-001)
- Confidence: Observed

### Vesna requirements

- R-BACKUP-100.1: Backup exports configuration to a single file chosen via
  the system file picker.
- R-BACKUP-100.2: Default filename embeds the creation timestamp.
- R-BACKUP-100.3: Canceling the picker must not modify any state.

## BACKUP-101 — Share backup, quick start, reset (inventory)

- Description: companion actions around backups.
- Observed actions: Share backup (create + share in one step, no local
  copy), Restore backup (see BACKUP-102), Quick start (re-run setup), Reset
  to default (remove customizations).
- Expected behavior (Vesna model): equivalent actions exist; destructive
  actions must present an explicit confirmation and a fresh backup
  recommendation before executing.
- Evidence: OBS-BACKUP-001 (EVI-BACKUP-001)
- Confidence: Observed (existence); Quick start / Reset flows Not Tested
  (destructive).

### Vesna requirements

- R-BACKUP-101.1: Provide share-in-one-step backup (no local copy).
- R-BACKUP-101.2: Provide quick-start (re-run setup) and reset-to-default.
- R-BACKUP-101.3: Destructive operations require explicit confirmation and
  must offer creating a fresh backup first.

## BACKUP-102 — Restore operation (verified)

- Description: importing a backup file replaces the current configuration
  with the backup state, behind an explicit confirmation.
- Trigger: Settings ▸ Backup & restore ▸ Restore backup ▸ choose file.
- Expected behavior (observed):
  1. The system document picker opens for file selection.
  2. An explicit overwrite confirmation is shown ("Overwrite current
     layout?" — Cancel/OK) before any change.
  3. After confirmation the launcher reproduces the backup state exactly:
     items added after the backup are removed, pages created after the
     backup are removed, and previously backed-up items return to their
     backed-up positions (verified: widget removed, page 2 removed, icon
     repositioned, folder preserved).
- Boundary conditions: malformed/foreign files picked (error handling) —
  Not Tested yet.
- Evidence: OBS-BACKUP-004 (EVI-BACKUP-002)
- Confidence: Observed

### Vesna requirements

- R-BACKUP-102.1: Restore routes through the system file picker.
- R-BACKUP-102.2: An explicit overwrite confirmation precedes any change.
- R-BACKUP-102.3: Restore reproduces the backed-up state completely,
  including deleting items and pages created after the backup.

## Note on earlier inventory entries

OBS-BACKUP-003 (restore/reset inventoried-only) was superseded when the
restore flow was executed; Quick start and Reset to default remain
inventory-only (destructive). See OBS-BACKUP-004.
