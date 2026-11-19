# ADR-BACKUP-1 — Backup and privacy policy

**Status:** Proposed, pending owner decision

**Date:** 2026-11-19

**Gates:** [ENG-106](../engineering/BACKLOG.md#eng-106-backuprestore-policy)

## Current behavior

Vesna 0.4.1 supports API 26 and targets API 37. Its application manifest sets
`android:allowBackup="true"` and supplies no backup rules or backup agent. Vesna
has no in-app backup control or export workflow. Its main manifest declares no
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

## Platform controls

| Device version | Available controls |
| --- | --- |
| API 26–30 | `fullBackupContent` scopes files. Android 9 and later support conditional include flags such as `clientSideEncryption` and `deviceToDeviceTransfer`. API 26–27 need compatible resources when those flags are used. |
| API 31+ | With `dataExtractionRules` configured, use separate cloud and device-transfer file rules. Android ignores the older rule file on these devices. Without the modern file, legacy include/exclude rules can scope cloud backup, while Android 12's device-to-device behavior follows platform and OEM rules. |
| OEM device transfers | Android documents that some devices can still transfer app data with `allowBackup="false"`. Verify the device-to-device policy on the supported transports and devices. |

A modern cloud section can require encryption capability. A legacy policy
needs a separate choice for API 26–27 if it requires client-side encryption.
Rules select files or domains. Excluding folder titles by themselves therefore
requires a separately scoped representation or an approved serialization
design. A configuration-only policy can exclude the entire Room database.
[Android Auto Backup](https://developer.android.com/identity/data/autobackup) ·
[Android 12 backup behavior](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore)

This decision covers Android backup and transfer. Android's cross-platform
rules require identifying a matching app on the destination platform. Vesna's
current design scope is Android. [Cross-platform transfer configuration](https://developer.android.com/identity/data/autobackup#configure-cross-platform-transfers)

## Restore risks

- A restored layout can refer to apps that the package manager has yet to
  restore. Vesna prunes pins for packages it cannot find during refresh, while
  retaining disabled apps that are still installed. If a restored pin is pruned,
  a later package-added event refreshes discovery without restoring the pin.
  Verify the effect of restore ordering.
  [Launcher refresh](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/LauncherController.kt)
- Profile serials identify users within a device. On another device the same
  number can identify a different profile, or have no match. Define how
  imported placements are matched before making any portability claim. Vesna
  currently uses serial 0 as its personal-profile convention.
  [App key](../../app/src/main/java/dev/vladon/vesna/launcher/apps/AppKey.kt) ·
  [Discovery and launch](../../app/src/main/java/dev/vladon/vesna/launcher/platform/AppDiscovery.kt)
- Preserve the seeding flag when restoring a deliberately emptied layout.
  Check the empty and missing-preference cases separately. Room migration,
  SQLite consistency, and recovery when only part of the state is restored
  need testing for the selected transport.
  [Persistence](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/WorkspaceRepository.kt)

## Options for the owner

| Option | Behavior | Tradeoff |
| --- | --- | --- |
| A. Keep framework defaults | Retain the current manifest declaration and document the platform-managed channels and eligible state. | No additional configuration. Scope follows Android defaults. |
| B. Set explicit file rules | Choose cloud eligibility and device-transfer eligibility separately. Allow only the approved paths, with compatible legacy rules. Device-to-device-only is an option where supported and verified. | Requires version-specific rules and transport testing. Privacy scope within a database requires a data-model decision. |
| C. Minimize platform backup | Disable cloud participation and verify scoped device-transfer behavior against the OEM caveat. Consider a user-initiated export as a separate feature. | Automatic restore may be unavailable. An export requires a file format, import validation, destination controls, and a decision about encryption. |

When any platform backup is desired, explicit file rules give the owner a
reviewable scope. The owner decides which channels to allow, which data may
leave the originating device, and how restore handles missing apps and
profiles. An in-app export is an independent product decision.

## Verification after approval

| Scenario | Check |
| --- | --- |
| File scope and encryption | Test included and excluded state with synthetic data. Verify API 26–27 resources and the selected legacy and modern rule behavior, with and without eligible encryption settings. |
| Restore and schema migration | Restore a representative older Room database. Verify positions, folder integrity, app identities, and a consistent SQLite open after migration. |
| Empty workspace and partial state | Verify a restored seeded empty layout stays empty across startup. Exercise missing preferences and missing Room state as distinct cases. |
| App and profile reconciliation | Restore while fixture apps are absent, install them later, and verify the approved policy. Test missing and colliding profile serials. |
| Transport coverage | Use a local test transport for repeatable file and restore tests. Record cloud and OEM device-transfer evidence separately on approved environments supporting those transports. |
| Optional export | Verify explicit destination choice, versioned import validation, the selected encryption policy, and recovery after an interrupted import. |

The record should identify the build, device/API, selected transport, and
observed outcome for each run. [Android backup testing guidance](https://developer.android.com/identity/data/testingbackup)
