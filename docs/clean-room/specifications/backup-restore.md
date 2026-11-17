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

## BACKUP-101 — Backup sharing, restore, reset (inventory)

- Description: companion actions around backups.
- Observed actions: Share backup (create + share in one step, no local
  copy), Restore backup (roll back to a previous configuration), Quick
  start (re-run setup), Reset to default (remove customizations).
- Expected behavior (Vesna model): equivalent actions exist; destructive
  actions (Restore/Reset) must present an explicit confirmation and a fresh
  backup recommendation before executing.
- Evidence: OBS-BACKUP-001, OBS-BACKUP-003 (EVI-BACKUP-001)
- Confidence: Observed (existence); flows Not Tested (destructive on user
  device).

### Vesna requirements

- R-BACKUP-101.1: Provide share-in-one-step backup (no local copy).
- R-BACKUP-101.2: Restore accepts a previously exported backup and rolls
  the configuration back.
- R-BACKUP-101.3: Provide quick-start (re-run setup) and reset-to-default.
- R-BACKUP-101.4: Destructive operations require explicit confirmation and
  must offer creating a fresh backup first.
