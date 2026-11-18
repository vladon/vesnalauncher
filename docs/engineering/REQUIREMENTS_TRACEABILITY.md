# Requirements Traceability

**Research source:** branch `vladon/vesna-research` at `ee15ed591c946cd3dc1fb623657c518e8a592424`
(pinned paths in [RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#pinned-research-paths);
approval status in [RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#approval-status--read-this-first)).
**Vesna baseline:** commit `888f557`, app 0.4.1 (see [PROJECT_STATUS.md](PROJECT_STATUS.md)).

> **What this matrix is.** Every row below is a **traceability pointer**: it
> records which Vesna code and tests correspond to a research requirement ID.
> It is **not** a claim of conformance with the research specification, and
> "Complete" in the research matrix is not repeated here. Approval status is
> tracked in [RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#approval-status--read-this-first);
> verification claims are limited to what the baseline audit actually ran
> ([PROJECT_STATUS.md](PROJECT_STATUS.md#quality-status-at-the-baseline)).

Status vocabulary:

- **Source-verified** — the implementation at the baseline commit implements the
  behavior as described; no automated/on-device test covers it.
- **Test-verified (exact scope named)** — covered by a named passing test suite
  (JVM) or a recorded on-device run; the covered behavior is named exactly.
- **Not implemented** — no corresponding Vesna code.
- **Blocked / decision needed** — work cannot proceed without a research-matrix
  fix or a product/privacy decision.
- "Outside the baseline audit" — the audit did not run it; no broader claim is
  made either way.

## Drawer

Pinned spec: [`specifications/app-drawer.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/app-drawer.md).

| Research ID | Meaning (as recorded in research/review) | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|---|
| `R-DRAWER-100.4` | Alphabetical drawer listing. The **owner-approved QA baseline covers only the order-preservation subset**: the drawer preserves a supplied order and does not re-order while filtering | Production sort lives in `LauncherAppsAppDiscovery.discover()` (`sortBy { label.lowercase(Locale.ROOT) }`); `DrawerView` filters via `AppSearch` and never re-orders | `DrawerSearchTest` preserves a pre-sorted fixture (3 recorded passes, API 35 AVD); discovery sort itself not exercised | **Test-verified (order-preservation subset only)** |
| `R-DRAWER-101.1` (local filtering subset) | Case-insensitive substring search of drawer labels | `AppSearch.filter`, `DrawerView` search field, empty state | Owner-approved QA baseline; `AppSearchTest` (5 JVM tests); `DrawerSearchTest` (mixed-case query, empty state, clear/restore) | **Test-verified (local filtering subset only)** |
| `DRAWER-102` | Back/IME key sequencing with open search | `DrawerView` search field handling | none | **Ambiguous in research** — sequencing not agreed; do not implement against a guess. Resolve in the research matrix first ([ENG-107 in BACKLOG.md](BACKLOG.md#eng-107-research-matrix-fix-ups-research-branch-not-trunk)) |
| `DRAWER-104` | (research matrix claims acceptance complete) | — | Named scenarios absent from research inventory | **Pointer only** — matrix status unsupported; no trunk action |
| `DRAWER-105` | Mistakenly reuses the ID `R-DRAWER-103.1`; its cards surface conflicts with the later `CARDS-100` | — | — | **Blocked** — ID reuse must be repaired in the research matrix; see the cards row |
| `R-DRAWER-103.1` | **Contact opt-in** | — | — | **Independent item** — kept separate from the `DRAWER-105`/`CARDS-100` conflict; no trunk work |

## Home screen and workspace pages

Pinned spec: [`specifications/home-screen.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/home-screen.md).

| Research ID | Meaning | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|---|
| `HOME-104` | **Workspace page management via drag** (not Android HOME registration) | none — single fixed 4×5 page | Named scenario absent from research inventory | **Not implemented**; matrix "complete" unsupported. Research candidate for [ENG-101 in BACKLOG.md](BACKLOG.md#eng-101-workspace-page-identities-and-schema-migration) |
| Workspace placement/ordering | — | `GridCanvas`, `WorkspaceResolver`, `WorkspaceRepository` | `GridCanvasTest` (11), `WorkspaceResolverTest` (6) | **Test-verified (grid/order model)** |

## HOME role and launching

Trunk QA items in this section are **Vesna platform baseline** work; none of
them carries an approved research ID.

| Item | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|
| HOME registration/selection, return-from-app | `HomeActivity` `CATEGORY_HOME` filter (`singleTask`, `clearTaskOnLaunch`, `excludeFromRecents`), `HomeRoleChecker`, `defaultHomeRequestIntent()` | none | **Outside the baseline audit**; first verification is [VESNA-QA-002 in BACKLOG.md](BACKLOG.md#vesna-qa-002-home-and-launch-return-verification-proposed) |
| HOME gesture (swipe-up drawer open) | `DrawerView` swipe-up / handle open | The baseline audit does not exercise the HOME gesture | **Outside the baseline audit**; research gesture follow-up (`ee15ed5` on research branch) records a separate drawer-open setting — binding values unvalidated in research |

## Dock

Pinned spec: [`specifications/dock.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/dock.md).

| Research ID | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|
| `DOCK-100` core ordering | `DockOrdering`, `DockView`, four slots | `DockOrderingTest` (9 JVM tests) | **Test-verified (ordering model)** |
| `DOCK-100` advanced paging, drag-out | partial | none | **Gap** — not implemented and not specified; see [BACKLOG.md](BACKLOG.md) candidates |

## Folders

Pinned spec: [`specifications/folders.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/folders.md).

| Research ID | Vesna anchors | Tests / evidence | Status |
|---|---|---|---|
| `FOLDER-100` core | `Folder` cell, `FolderDialog` open/rename/remove, dissolution below two members; **drag-into-folder**: drops onto a folder cell route through `LauncherController.addToFolder` (two `HomeActivity` paths) | **Exact model scope:** `WorkspaceResolverFolderTest` (7 JVM tests) covers the resolver projection only — member exclusion from top-level cells, folder cell with member count, invalid cell skipped, no override of an app in the target cell, in-folder ordering, stale members dropped, empty without folders. Create/rename/dissolution flows, the dialog UI, and the drag-into-folder UI flow are **not** covered | **Test-verified (resolver model only)**; drag-into-folder **source-verified, UI outside the baseline audit** |
| `FOLDER-100`/`FOLDER-101` pagination | none | none | **Gap** — pagination not implemented; scenarios missing in research too ([ENG-107](BACKLOG.md#eng-107-research-matrix-fix-ups-research-branch-not-trunk)) |

## Widgets

Pinned spec: [`specifications/widgets.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/widgets.md).

| Research ID | Meaning | Vesna anchors | Status |
|---|---|---|---|
| `WIDGET-100` | Placement; provider config-cancel behavior is a research gap | none | **Not implemented**; any widget work must include widget host cleanup design — [ADR-WIDGET-1 in BACKLOG.md](BACKLOG.md#adr-widget-1-widget-host-cleanup) |
| `WIDGET-102` | Removal/undo | none | **Not implemented** |
| `WIDGET-101` | Widget resize | none | **Not implemented** |

## Settings, backup, privacy

Pinned specs: [`specifications/settings.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/settings.md) · [`specifications/backup-restore.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/backup-restore.md).

| Research ID | Meaning | Vesna anchors | Status |
|---|---|---|---|
| `SETT-110` | **Settings search/deep-linking** (not reset) | none | **Not implemented**; no dedicated research scenario either ([ENG-107](BACKLOG.md#eng-107-research-matrix-fix-ups-research-branch-not-trunk)) |
| `BACKUP-101` | Reset (coverage partial in research) | none — trunk has no reset feature | **Gap on both sides**; reset work pairs with [ENG-106 in BACKLOG.md](BACKLOG.md#eng-106-backuprestore-policy) |
| `BACKUP-102` | Backup | `android:allowBackup="true"` in the manifest (platform-managed); no explicit backup/restore feature | **Not implemented (feature)**; platform backup enabled by manifest default. No dedicated research scenario. Scope decision pending — [ADR-BACKUP-1 in BACKLOG.md](BACKLOG.md#adr-backup-1-backup-and-privacy-policy) |
| `CARDS-100` | **Condition-triggered content** (cards). Location/manual-fallback privacy question open; notification indicators are a separate item | none | **Blocked** — product + privacy decision required before any planning |
| `DRAWER-105`/`R-DRAWER-103.1` | See drawer section | — | ID reuse to repair; contact opt-in independent |

## Data and platform behaviors without research IDs

| Vesna behavior | Anchors | Tests / evidence | Status |
|---|---|---|---|
| Room schema evolution 1→2→3 | `LauncherDatabase` (`MIGRATION_1_2`, `MIGRATION_2_3`), schema version 3 | none — the baseline audit does not exercise migrations | **Outside the baseline audit**; planned P0 ([VESNA-QA-004](BACKLOG.md#vesna-qa-004-migration-and-restart-verification-proposed)) |
| Package install/update/uninstall across profiles | `PackageChangeMonitor`, `userSerial` tagging, stale-item pruning | none — outside the baseline audit | **Outside the baseline audit**; planned P0 ([VESNA-QA-003](BACKLOG.md#vesna-qa-003-package-lifecycle-verification-proposed)) |
| Persistence across process death / reboot | Room persistence (intended behavior) | none — the baseline audit does not exercise reboot | **Outside the baseline audit**; planned P0 |
| Managed-profile behavior | `LauncherApps` discovery/launch, profile badges | none — outside the baseline audit | **Outside the baseline audit** |
| Drag-and-drop across containers | `LauncherDragLayer`, `WorkspaceView`, `DockView`, `DrawerView` | none recorded in the baseline audit | **Source-verified only** |

## Coverage summary

| Verification level | Research IDs / behaviors |
|---|---|
| Test-verified (exact scope) | `R-DRAWER-100.4` order-preservation subset, `R-DRAWER-101.1` local-filtering subset, dock ordering model, workspace grid/order model, folder resolver model |
| Source-verified only | HOME role, discovery across profiles, drag-and-drop, package events, seeding, migrations, settings/about |
| Outside the baseline audit | HOME gesture, migrations, reboot persistence, managed profiles, package removal/unavailability, app-to-HOME return, folder/dock UI flows |
| Not implemented | Widgets, cards, settings search/deep-linking, settings reset, explicit backup/restore, icon packs, themes, fuzzy search, workspace page management |
| Blocked / decision needed | `DRAWER-105` ID reuse, `CARDS-100` (condition-triggered content; location/manual-fallback privacy), `DRAWER-102` ambiguity, `BACKUP-102` scenario |

New engineering work must add or update a row here in the same change that adds
the code or test, so the matrix never silently rots. Work items live in
[BACKLOG.md](BACKLOG.md); sequencing is [ROADMAP.md](ROADMAP.md).
