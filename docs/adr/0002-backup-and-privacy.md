# ADR-BACKUP-1 — Backup and privacy policy

**Status:** Draft for review — device-to-device only; not implemented.

**Date:** 2026-11-19

**Gates:** [ENG-106](../engineering/BACKLOG.md#eng-106-backuprestore-policy)

## Current behavior

Published Vesna 0.4.2 supports API 26 and targets API 37. Its manifest sets
`android:allowBackup="true"` without transfer rules or a backup agent.
Vesna has no in-app backup control or export. Its main manifest declares no
Internet permission. Android can back up eligible files through the device's
configured transport. Actual participation depends on Android settings and the
available transport. [App manifest](../../app/src/main/AndroidManifest.xml) ·
[Build configuration](../../app/build.gradle.kts) ·
[Android Auto Backup](https://developer.android.com/identity/data/autobackup)

The table lists app-owned storage that falls within Android's default eligible
domains. Transport verification remains pending.

| Data | Storage | What it can reveal |
| --- | --- | --- |
| App package names, activity classes, profile serials, workspace and dock positions, folder membership | Room v3 `workspace_items` in `vesna-launcher.db` | Which apps are pinned, their layout, and grouping by profile |
| Folder titles and positions | `folders` in the same Room database | Free text entered by the user and its location |
| `workspace_seeded` | `vesna_launcher` shared preferences | Whether the initial layout has already been seeded |

[Workspace rows](../../app/src/main/java/dev/vladon/vesna/launcher/data/WorkspaceItemEntity.kt) ·
[Folder rows](../../app/src/main/java/dev/vladon/vesna/launcher/data/FolderEntity.kt) ·
[Persistence](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/WorkspaceRepository.kt)

The product policy limits this decision to launcher state and excludes
analytics and tracking. [Product principles](../../PRODUCT.md)

## Proposed policy

- Allow launcher layout transfer only as part of Android device-to-device
  setup on supported Android-to-Android transports. No cloud backup or restore,
  cross-platform transfer, Vesna account, server, analytics, or in-app export.
- Allowlist a versioned transfer snapshot: workspace page IDs and display
  order, personal-profile app package/activity identifiers and placements,
  global dock order, folders (including their user-entered titles), membership,
  and the initial-layout seeding flag. Exclude work-profile pins until there is
  a verified profile-mapping model. Never transfer profile serials, raw Room
  databases, other preferences, cached artwork, wallpaper, credentials, logs,
  or device identifiers.
- Vesna maintains the snapshot in app-private `files/transfer/layout`, the
  only allowlisted path. After each committed layout or seed-state change,
  generate it from one consistent database read and replace it atomically.
  Android setup may copy the file without launching Vesna, so repair stale
  snapshots on startup. If setup interrupts a write, it may copy the last
  complete snapshot, never a partial update. The snapshot format version,
  not the Room version, selects the restore rules.
- Validate and apply the snapshot transactionally before seeding. Restore the
  personal-profile pins against the destination's personal profile only.
  Keep restored pins pending while destination apps are still installing;
  normal stale-item cleanup must not erase them during setup. Skip invalid
  records only according to the snapshot version's documented rules; reject
  corrupt or unsupported snapshots without replacing the destination layout.
  A transferred, deliberately empty layout must stay empty.

## Platform enforcement

| Android version | Rule |
| --- | --- |
| API 31+ | `dataExtractionRules` with an explicit `<device-transfer>` include for the snapshot only. Explicitly exclude all applicable data domains in `<cloud-backup>`; an empty section is not a deny rule. Do not opt into cross-platform transfer. |
| API 28–30 | Versioned `fullBackupContent` includes the snapshot only with `requireFlags="deviceToDeviceTransfer"`; verify negative coverage when the transport does not report that capability. |
| API 26–27 | No transfer or cloud backup. Use compatible legacy resources and an explicit deny policy; do not rely on the API 28 transfer flag on these versions. |

Android and OEM setup transports differ. Do not advertise a transport or API
range until a positive transfer and a negative cloud backup/restore have been
verified for it. Do not rely on `allowBackup="false"` alone for OEM transfer
behavior.
[Android Auto Backup](https://developer.android.com/identity/data/autobackup) ·
[Android 12 backup behavior](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore)

## Verification after approval

| Scenario | Check |
| --- | --- |
| Transport allowlist | Verify no data on cloud backup or restore; verify only the snapshot moves on approved Android device transfers. Check API 26–27 transfers nothing and API 28–30 requires the transfer capability. |
| Restore and migration | Transfer older and current snapshots. Verify page IDs, order, pins, folder names and memberships, atomic application, and unchanged destination data on corrupt or unsupported input. |
| Empty layout and delayed apps | Confirm a deliberately emptied layout stays empty. Transfer before fixture apps arrive; confirm their restored pins survive and appear when the apps are installed. |
| Profile isolation | Use personal and work copies of the same app and colliding serials across devices. Verify the transfer contains no work-only pin or raw profile serial and never binds a personal pin to a work app. |

The record should identify the build, device/API, selected transport, and
observed outcome for each run. [Android backup testing guidance](https://developer.android.com/identity/data/testingbackup)
