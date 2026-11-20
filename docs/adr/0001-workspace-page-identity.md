# ADR-PAGE-1: Workspace page identity and its migration

**Status:** Accepted for persistence and migration (Room version 4).
Page-management UI is not part of this decision.
**Date:** 2026-11-19
**Gates:** [ENG-101](../engineering/BACKLOG.md#eng-101-workspace-page-identities-and-schema-migration)
(page identities + schema migration) and, transitively, ENG-102 (widgets occupy
grid cells). The ADR candidate is tracked as
[ADR-PAGE-1 in BACKLOG.md](../engineering/BACKLOG.md#adr-page-1-workspace-page-identity-and-its-migration).

This decision establishes stable page identity and an additive migration away
from the published single-page Room v3 schema. Vesna still renders the default
4×5 page. `HOME-104` (page management via drag) remains an unapproved research
candidate and is not consumed by this change.

## Context (shipped state, source-verified)

Published Vesna 0.4.2:

- [Room v3](../../app/src/main/java/dev/vladon/vesna/launcher/data/LauncherDatabase.kt)
  (`version = 3`, `exportSchema = false`), database `vesna-launcher.db`.
  One fixed 4×5 page — a documented
  [known limitation](../architecture/OVERVIEW.md#known-limitations-041).
- [`workspace_items`](../../app/src/main/java/dev/vladon/vesna/launcher/data/WorkspaceItemEntity.kt):
  `(packageName, className, container, cellIndex, userSerial, folderId)`.
  `cellIndex` is overloaded: a grid cell (0…19) for `WORKSPACE` rows, a dock
  slot order for `DOCK` rows
  ([`GridCanvas`](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/GridCanvas.kt)).
- [`folders`](../../app/src/main/java/dev/vladon/vesna/launcher/data/FolderEntity.kt):
  `(title, cellIndex)` — a folder *is* a grid occupant. Folder membership is
  `folderId` on the item; the item's `cellIndex` is then its position inside
  the folder.
- Occupancy today is **global over the one page**: top-level workspace items
  *and* folder cells together
  ([`WorkspaceRepository`](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/WorkspaceRepository.kt);
  read-time tolerance in
  [`WorkspaceResolver`](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/WorkspaceResolver.kt)
  ignores invalid and duplicate cells).
- All mutations serialize on `WorkspaceRepository.mutationMutex`; multi-row
  updates run in `db.withTransaction`.
- Seeding: the `workspace_seeded` SharedPreferences flag, deliberately **not**
  derived from table emptiness (a user-emptied workspace must not re-seed; an
  upgrading layout is adopted untouched via `markSeeded`).
- Migrations `1→2` and `2→3` are additive `ALTER`s. Historical-schema migration
  verification exists as the **QA-004A** harness
  ([`RoomMigrationQaTest`](../../app/src/androidTest/java/dev/vladon/vesna/launcher/data/RoomMigrationQaTest.kt),
  added by commit `14ec712` — one file, +271 lines, no production-code change):
  authentic v1/v2 schemas sourced from tags `v0.1.0`/`v0.2.0` are seeded into
  isolated disposable database files and migrated through the unmodified
  production `LauncherDatabase.build`; Room's own post-migration schema and
  identity-hash validation run on open. Its KDoc explicitly leaves
  process-death and reboot durability to a separate recorded run.

## Decision drivers

1. Page **identities** must be stable: reordering, inserting, or removing
   pages must never change how an item refers to its page.
2. Display **order** must be mutable without rewriting workspace items.
3. Migration must be additive and forward-only, matching the
   `MIGRATION_1_2`/`MIGRATION_2_3` discipline, with **no destructive
   fallback** — none exists today and none is introduced.
4. Collision semantics must generalize today's folder-inclusive occupancy to
   page scope without changing dock behavior.
5. The result must be verifiable with the QA-004A pattern.

## Considered options

| Option | Verdict | Why |
|---|---|---|
| **A. Index arithmetic** — encode the page into `cellIndex` (e.g. `page · CAPACITY + cell`) | Rejected | Couples a UI geometry constant into persistence; collides with the dock's slot-order meaning of `cellIndex`; every page reorder/delete rewrites all rows; the "identity" degenerates into a position. |
| **B. Page-number column on each item** — `pageIdx`, ordered by number | Rejected | A number is a position, not an identity: inserting or reordering pages renumbers every row on every page (write amplification, and transient duplicates would fight any uniqueness constraint). References break by design. |
| **C. Visual order stored in each item** — dense per-item sequence numbers | Rejected | Same renumbering problem, worse (per item). Dock slot renumbering is affordable at ≤4 entries with no other positional data; workspace items already carry stable cells that must not churn. Conflates identity with display order. |
| **D. Stable opaque page IDs + page-level ordered metadata** — a `pages` table with opaque row IDs (same convention as `folders`) plus a `rank` column | **Preferred proposal** | Identities never change; reordering pages updates *k* rank values, zero item rewrites; ordering metadata lives on the few pages, not on the many items; the table generalizes cleanly to folders and later widgets. |

## Accepted model

- New `workspace_pages` table: opaque autogen `id` (Long, mirroring the `folders`
  convention) and `rank` for display order. **No `UNIQUE` on `rank`**: reorder
  transactions may transiently hold duplicate ranks; the total order is
  `(rank, id)` — deterministic without a constraint.
- The page lives on the **locating record**:
  - top-level `WORKSPACE` items carry a page reference;
  - `folders` carry a page reference;
  - **folder members carry no page of their own** — their page derives from
    their folder (invariant: a row never carries both claims);
  - **dock rows are page-independent** — the dock remains global, `cellIndex`
    stays its slot order. This is an invariant of the model, unchanged by
    pages.
- `cellIndex` remains page-local, `0 … CAPACITY-1`.
- **Page-scoped occupancy (write-path invariant):** per page *P*, the occupied
  cells are {top-level items on *P*} ∪ {folders on *P*}; at most one locating
  record per cell per page. This is today's rule (folders already count as
  occupants) evaluated per page instead of globally. Occupancy checks stay
  inside the existing `mutationMutex`; the model adds no new synchronization.
  This invariant binds **normal writes** once the model is implemented — it is
  enforced on the write path, before persisting.
- Legacy invalid and duplicate cells remain stored unchanged. The resolver's
  existing read-time tolerance still applies.

## Invariants along the existing transaction paths

- **Move** (`moveWithinWorkspace`): unchanged semantics, occupancy check
  page-scoped. A cross-page relocation is expressible without new machinery
  (update the page reference and cell, validated against the target page's
  occupancy); whether the UI offers it is product semantics, not decided here.
- **Add** (`addToWorkspace`, `insertWorkspaceAt`): the first-free-cell search
  generalizes to a page-scan, but **the scan order across pages is an open
  product decision** (e.g. first page by `(rank, id)` vs. active page first);
  likewise *creating a page when all pages are full* is an engine capability
  this model supports, while whether adds auto-create pages or refuse with
  `WORKSPACE_FULL` stays product semantics — open decisions listed below, not
  fixed by this proposal.
- **Drop** (`createFolderFromDrop`): the new folder row takes the **target
  item's page**; both entries become members. Consequence: a folder never
  straddles pages; all members share one page through the folder.
- **Dissolve** (`removeFromFolder`, `removeFolder`, `dissolveIfSparse`):
  members return to free cells **on the folder's page**, and `removeFolder`'s
  fit check is page-scoped. What happens **when the folder's page cannot fit
  all returning members** — refuse (today's `false`), distribute across other
  pages, or something else — is an open product decision. Pages are never
  deleted while locating records reference them; **whether and when an emptied
  non-default page is retired is likewise open** (this proposal fixes only the
  floor: the default page always exists).
- **Dock** (`reorderDock`, `insertIntoDock`, `moveWorkspaceToDock`): current
  slot arithmetic unchanged; no page dimension enters the dock.

## Migration from the shipped v3 schema

One Room migration in the established additive style, executed inside Room's
migration transaction:

1. Create `workspace_pages` and insert default page ID `1`, rank `0`.
2. Add nullable indexed `pageId` foreign keys to `workspace_items` and
   `folders`. Page references restrict deletion of a referenced page.
3. Assign all top-level non-dock items and all folders to the default page.
   IDs, app keys, profile serials, cell positions, and folder links are
   unchanged. Dock rows and folder members receive no page of their own.
4. **Empty tables:** migrate to the schema containing just the default page;
   the seeded flag — not migration — governs re-seeding.

**Malformed legacy data:** Unknown top-level container names keep their
stored value and follow the existing workspace fallback, on the default page.
Dangling folder members retain their existing folder IDs and remain hidden.
Out-of-range or colliding positions are preserved; migration never silently
reorders or drops them. The migration harness exercises these cases.

If a migration ever fails, Room aborts the transaction and the v3 file remains
intact (atomicity); there is no runtime data-wiping catch. The mitigation for
failure is the verification harness below, not a fallback.

## Seeding flag and empty layout

`workspace_seeded` semantics are unchanged: a fresh install seeds one default
page plus dock
([`SeedPlanner`](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/SeedPlanner.kt));
a pre-flag upgrading layout is adopted via `markSeeded` untouched; a
user-emptied workspace never re-seeds (the flag is deliberately not derived
from table emptiness). App adds never create pages. The default page persists
even when it holds no locating records.

## Durability, restart, and rollback limits

- Page state lives in Room. `workspace_seeded` remains in shared preferences
  and is not changed by the migration.
- **Forward-only.** No in-app down-migration is provided. After the
  migration, an older app build cannot open the newer database file (Room's
  version check fails). Rollback options beyond uninstall/clear-data: a
  **pre-upgrade backup of the database file** can be restored under an older
  build, but only if that build accepts the file — this is an operational
  escape hatch, not a guaranteed path, and it presupposes a backup taken
  before the upgrade. Platform auto-backup (`allowBackup="true"`, no feature)
  could carry a newer-schema file to an older install — an accepted limit;
  backup scope is
  [ADR-BACKUP-1](../engineering/BACKLOG.md#adr-backup-1-backup-and-privacy-policy)'s
  decision, not made here.

## Schema export and migration verification (QA-004A pattern)

- Room schemas are exported under `app/schemas` starting at version 4.
- The [Room migration harness](../../app/src/androidTest/java/dev/vladon/vesna/launcher/data/RoomMigrationQaTest.kt)
  opens isolated historical databases through the production builder. It
  checks migrations from versions 1, 2 and 3, including malformed rows and
  an empty v3 layout, and verifies the migrated database on reopen.
- Repository tests cover page-scoped cell occupancy, moving and unpinning
  duplicate app keys on different pages, rank changes without rewriting pins,
  and dissolving folders onto their own page.
- On the dedicated API 35 `Vesna_Pages_V4_QA` emulator, the debug candidate
  opened a copy of the signed 0.4.2 database and assigned the default page
  without changing any of its 18 placement rows, folder data, or seeded
  preference. The layout survived a guest reboot. A seeded empty v3 fixture
  stayed empty after migration. The same build passed all 23 instrumentation
  tests. The home hierarchy was checked in light mode and again in dark mode
  at 1.3× font scale; this was not a keyboard or TalkBack audit.

## Decisions left for page management

Page creation, deletion, display-order gestures, page-count limits, and
cross-page drags are not exposed in this change. Adds target the current
page and return the existing full-page error; removing a folder returns
members only to its own page and refuses if they do not fit. There is no
automatic page creation or retirement.

Before consuming `HOME-104`, record the page-management behavior on the
[research branch](../engineering/BACKLOG.md#eng-107-research-matrix-fix-ups-research-branch-not-trunk)
and get product approval for the interactions.

Out of scope for this ADR: UI features of any kind, widgets
([ADR-WIDGET-1](../engineering/BACKLOG.md#adr-widget-1-widget-host-cleanup)),
backup scope (ADR-BACKUP-1). No monetization or privacy change is considered
or implied by this document.

## References

- Code: [`LauncherDatabase`](../../app/src/main/java/dev/vladon/vesna/launcher/data/LauncherDatabase.kt)
  · [`WorkspaceItemEntity`](../../app/src/main/java/dev/vladon/vesna/launcher/data/WorkspaceItemEntity.kt)
  · [`FolderEntity`](../../app/src/main/java/dev/vladon/vesna/launcher/data/FolderEntity.kt)
  · [`WorkspaceRepository`](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/WorkspaceRepository.kt)
  · [`WorkspaceResolver`](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/WorkspaceResolver.kt)
  · [`GridCanvas`](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/GridCanvas.kt)
  · [`LauncherController`](../../app/src/main/java/dev/vladon/vesna/launcher/workspace/LauncherController.kt)
- Planning: [ENG-101](../engineering/BACKLOG.md#eng-101-workspace-page-identities-and-schema-migration)
  · [VESNA-QA-004](../engineering/BACKLOG.md#vesna-qa-004-migration-and-restart-verification-done)
  · [traceability: home screen and workspace pages](../engineering/REQUIREMENTS_TRACEABILITY.md#home-screen-and-workspace-pages)
