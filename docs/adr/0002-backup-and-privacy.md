# ADR-BACKUP-1 — Layout export and restore; backup and privacy policy

**Status:** Part 1 accepted and implemented (2026-11-25 loop session;
revision merged as #213, implementation released in v0.4.88) — covers the
approved [#116](https://github.com/vladon/vesnalauncher/issues/116) file
export/restore scope; the Android device-to-device transfer section (part 2)
remains a draft, gated on review and transport verification. Supersedes the
2026-11-19 draft, which scoped device-to-device transfer only.

**Gates:** [ENG-106](../engineering/BACKLOG.md#eng-106-backuprestore-policy) ·
Scope decision: the [BACKLOG product-decisions
table](../engineering/BACKLOG.md#product-decisions-for-open-issues) (#116 row)

## Context

Vesna's manifest sets `android:allowBackup="true"` without transfer rules or
a backup agent. Vesna has no in-app export, restore, or backup control. Its
main manifest declares no Internet permission. App-owned storage that
Android's default backup domains can reach:

| Data | Storage | What it can reveal |
| --- | --- | --- |
| App package names, activity classes, profile serials, workspace and dock positions, folder membership | Room `workspace_items` in `vesna-launcher.db` | Which apps are pinned, their layout, and grouping by profile |
| Folder titles and positions | `folders` in the same Room database | Free text entered by the user and its location |
| `workspace_seeded` | `vesna_launcher` shared preferences | Whether the initial layout has already been seeded |

The privacy policy stays as decided: launcher state only; no analytics,
tracking, accounts, servers, or cloud services.
[Product principles](../../PRODUCT.md)

## Decision — part 1: user-controlled layout export and restore (approved scope)

The approved [#116](https://github.com/vladon/vesnalauncher/issues/116)
decision: an explicit export to a user-selected file and a restore from that
file. It is a user-initiated file operation, separate from Android-managed
transfer and from any launcher-managed cloud backup (which stays rejected).

**Snapshot.** The export file contains one versioned snapshot with the same
allowlist as the transfer section below: workspace page IDs and display
order, personal-profile app package/activity identifiers and placements,
global dock order, folders with their user-entered titles and membership,
and the initial-layout seeding flag. The snapshot format version, not the
Room version, selects the restore rules. Excluded always: work-profile pins,
profile serials, raw Room databases, other preferences, cached artwork,
wallpaper, credentials, logs, and device identifiers.

**Export.** The user picks the destination through the system file picker
(Storage Access Framework; no storage permission). Vesna generates the
snapshot from one consistent database read and writes the picked file
atomically (temp file + rename within the picked location). Vesna keeps no
mirror of the export beyond the write and does not register the file
anywhere.

**Restore.** The flow is preview → validate → confirm → apply:

1. **Preview before confirmation.** The user sees what the restore would
   apply — page count, pin count, folder count and titles — and which
   shortcuts would be unavailable (packages not installed on this device).
2. **Validation.** The file is checked against the snapshot version's
   documented rules before anything changes. Unsupported or corrupt files
   are rejected with the layout untouched.
3. **Explicit confirmation.** Restore replaces the current layout only
   after the user confirms the previewed operation.
4. **Atomic application.** The validated snapshot applies in a single
   database transaction; the previous layout is replaced wholesale. A
   transferred, deliberately empty layout stays empty.
5. **Failure containment.** Cancel, invalid input, validation failure, or a
   crash mid-apply leaves the current layout unchanged.

Restored pins for packages that are still absent stay pending; the normal
stale-item cleanup must not erase them during that period. Restore touches
the personal profile only; no snapshot record may bind a personal pin to a
work-profile app.

**Android backup surface.** The file feature adds nothing to Android's
backup surface: the export uses SAF paths the user picks, Vesna stores no
copy of it in its own data, and this ADR leaves `allowBackup` and the
transfer rules untouched until part 2 is approved.

## Decision — part 2: Android device-to-device transfer (draft, unchanged)

Not approved and not implemented; recorded here as the standing draft so the
two scopes stay separable. On supported Android-to-Android transports,
launcher layout transfer follows the device-to-device policy only:

- Allowlist a versioned transfer snapshot (the same fields as part 1),
  maintained in app-private `files/transfer/layout`, regenerated from one
  consistent read after each committed layout or seed-state change and
  replaced atomically; repair stale snapshots on startup, because Android
  setup may copy the file without launching Vesna.
- Exclude work-profile pins until a verified profile-mapping model exists.
- Platform enforcement (draft): API 31+ `dataExtractionRules` with an
  explicit `<device-transfer>` include for the snapshot and explicit
  `<cloud-backup>` exclusions (an empty section is not a deny rule); API
  28–30 versioned `fullBackupContent` with
  `requireFlags="deviceToDeviceTransfer"`; API 26–27 no transfer or cloud
  backup.
- Do not advertise a transport or API range until a positive transfer and a
  negative cloud backup/restore are verified for it.
- [Android Auto Backup](https://developer.android.com/identity/data/autobackup) ·
  [Android 12 backup behavior](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore)

## Verification (implementation review)

| Scenario | Check |
| --- | --- |
| Export/restore round trip | Export on one install, wipe or reinstall, restore from the picked file: page IDs, order, pins, dock, folder titles and memberships match; the seeding flag transfers. |
| Preview and confirmation | The preview lists counts and unavailable shortcuts; no layout change happens before the explicit confirmation step. |
| Corrupt and unsupported input | Truncated file, wrong version, wrong shape: rejected with a message; the current layout is unchanged. |
| Empty layout | A deliberately emptied layout restores as empty, not as a re-seed. |
| Pending apps | Restore before fixture apps install: their pins stay pending, stale-item cleanup keeps them, they resolve when the apps arrive. |
| Profile isolation | The export file contains no work-only pin and no profile serial; personal pins never bind to work apps. |
| Failure containment | Kill the process during apply: the pre-restore layout survives (single transaction). |

The record should identify the build, AVD/API, file location, and observed
outcome for each run. [Android backup testing
guidance](https://developer.android.com/identity/data/testingbackup)
