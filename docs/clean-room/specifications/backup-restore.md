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

## BACKUP-101 — Share backup, quick start, reset (verified inventory + flows)

- Description: companion actions around backups.
- Observed: Share backup (create + share in one step, no local copy);
  Restore backup (BACKUP-102, verified); **Quick start is PAYWALLED** in the
  free edition — tapping it opens the Manage Subscription page;
  **Reset to default** launches the dark-themed setup wizard directly (no
  separate confirm dialog); completing the wizard PRESERVED the workspace
  layout in our pass (all touched settings were already default, so which
  values reset actually changes is undetermined — see OBS-BACKUP-110).
- Evidence: OBS-BACKUP-110, OBS-SYS-110 (EVI-BACKUP-003, EVI-SYS-002)
- Confidence: Observed (existence + gating); reset semantics Partially
  Observed.

### Vesna requirements

- R-BACKUP-101.1: Provide share-in-one-step backup (no local copy).
- R-BACKUP-101.2: Provide quick-start (re-run setup) and reset-to-default
  as FREE features (Vesna independent decision — Nova gates Quick start
  behind its paywall; recorded as reference).
- R-BACKUP-101.3: Reset offers an explicit confirmation and states its
  scope (settings vs layout) before executing; layout-wiping resets must
  offer a fresh backup first.
- R-BACKUP-101.4: Destructive operations must offer creating a fresh
  backup first.

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
- Boundary conditions: cancellation and one plain-text file with the
  backup suffix left the current layout unchanged (OBS-VESNA-033).
  Other invalid inputs and transient error wording were not verified.
  A valid restore with one app missing retained a marked placeholder at
  its saved position. Reinstalling that app cleared the mark without
  repinning (OBS-VESNA-034); no store-driven download was verified.
- Evidence: OBS-BACKUP-004 (EVI-BACKUP-002), OBS-VESNA-033/034 (EVI-VESNA-003).
- Confidence: Observed

### Vesna requirements

- R-BACKUP-102.1: Restore routes through the system file picker.
- R-BACKUP-102.2: An explicit overwrite confirmation precedes any change.
- R-BACKUP-102.3: Restore reproduces the backed-up state completely,
  including deleting items and pages created after the backup.
- R-BACKUP-102.4: Cancelling restore or rejecting an invalid file leaves
  the current layout unchanged.

## Note on earlier inventory entries

OBS-BACKUP-003 (restore/reset inventoried-only) was superseded when the
restore flow was executed; Quick start and Reset to default remain
inventory-only (destructive). See OBS-BACKUP-004.
