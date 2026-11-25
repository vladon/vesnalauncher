# Research Handoff

**Research source:** branch `vladon/vesna-research`, pinned commit
`ee15ed591c946cd3dc1fb623657c518e8a592424`, maintained in a separate worktree
in this repository (`origin`: [vladon/vesnalauncher](https://github.com/vladon/vesnalauncher)).
**Handoff reviewed by:** the project coordinator (this summary was reviewed; no
document changes were made on trunk).
**Date:** 2026-11-18.

This document records what the research track concluded, what its status is, and
which parts are approved for use in trunk engineering. It intentionally does
**not** copy observations or evidence into trunk: research artifacts stay in the
research worktree at the pinned commit, and trunk references them by pinned path
and ID (see [Pinned research paths](#pinned-research-paths)).

## Inventory at the pinned commit

- 10 specification files under `docs/clean-room/specifications/`.
- 25 requirement/spec IDs (`R-*` and area IDs such as `HOME-*`, `DRAWER-*`,
  `DOCK-*`, `FOLDER-*`, `WIDGET-*`, `SETT-*`, `BACKUP-*`, `CARDS-*`).
- 22 scenarios (`docs/clean-room/acceptance-tests/scenarios.md`).

## Pinned research paths

Links are permalinks to the pinned commit. Paths, not content — observations
and raw evidence are never copied into trunk.

| Area | Spec (pinned) |
|---|---|
| App drawer | [`docs/clean-room/specifications/app-drawer.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/app-drawer.md) |
| Appearance | [`docs/clean-room/specifications/appearance.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/appearance.md) |
| Backup/restore | [`docs/clean-room/specifications/backup-restore.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/backup-restore.md) |
| Cards | [`docs/clean-room/specifications/cards.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/cards.md) |
| Dock | [`docs/clean-room/specifications/dock.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/dock.md) |
| Folders | [`docs/clean-room/specifications/folders.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/folders.md) |
| Gestures | [`docs/clean-room/specifications/gestures.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/gestures.md) |
| Home screen | [`docs/clean-room/specifications/home-screen.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/home-screen.md) |
| Settings | [`docs/clean-room/specifications/settings.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/settings.md) |
| Widgets | [`docs/clean-room/specifications/widgets.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/specifications/widgets.md) |

Scenario inventory:
[`docs/clean-room/acceptance-tests/scenarios.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/acceptance-tests/scenarios.md).
Coverage matrix:
[`docs/clean-room/COVERAGE_MATRIX.md`](https://github.com/vladon/vesnalauncher/blob/ee15ed591c946cd3dc1fb623657c518e8a592424/docs/clean-room/COVERAGE_MATRIX.md).

## Approval status — read this first

- **No reviewed approval record exists** for the research body as a whole.
  Familiarity with observations or authored spec labels is **not** formal
  sign-off. The full research requirements remain **gated**.
- The only owner-approved slice is a **Vesna QA baseline** covering:
  - the **order-preservation subset** of `R-DRAWER-100.4` (the full ID refers to
    alphabetical drawer listing; the approval covers only that the tested drawer
    preserves a supplied order), and
  - the **local filtering subset** of `R-DRAWER-101.1` (case-insensitive
    substring search).
  - Supporting observations/evidence: `OBS-DRAWER-001`, `OBS-DRAWER-002`,
    `EVI-DRAWER-001`.
- These approved subsets are what trunk engineering may treat as an acceptance
  baseline today. Everything else in the research track is input for planning,
  not an approved contract. Trunk-side verification status per ID is maintained
  in [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).

## Known weaknesses in the research matrix

The coordinator review found the following; carry these caveats into any plan
that consumes the research matrix:

| Area | Finding |
|---|---|
| `HOME-104`, `DRAWER-104` | Matrix marks acceptance **complete**, but the named scenarios are absent from the inventory. `HOME-104` is **workspace page management via drag** — not Android HOME registration. Treat the complete status as unsupported either way. |
| `DRAWER-102` | Back/IME key sequencing is ambiguous; no single agreed behavior is recorded. |
| `DOCK-100` | Advanced paging and drag-out behaviors are gaps. |
| `FOLDER-100`, `FOLDER-101` | Pagination is a gap. Drag-into-folder is implemented in trunk (source-verified, UI unverified) but has no dedicated research scenario. |
| `WIDGET-100` | Placement; the provider config-cancel behavior is a gap. |
| `WIDGET-102` | Removal/undo. |
| `SETT-110` (settings search/deep-linking), `BACKUP-102` | No dedicated scenarios for either. Reset coverage is partial and belongs under `BACKUP-101`. |
| `DRAWER-105` / `R-DRAWER-103.1` | `DRAWER-105` mistakenly reuses the ID `R-DRAWER-103.1` (which is **contact opt-in**). The duplication must be repaired in the research matrix; contact opt-in remains an independent item, separate from the cards conflict below. |
| `CARDS-100` | Condition-triggered content. The location/manual-fallback privacy question is open; notification indicators are a separate item. Requires a product/privacy decision before any work. |
| Gestures | Follow-up commit `ee15ed5` documents a separate drawer-open setting and gesture assignment configuration; live binding-value validation is still pending. |
| Badge compatibility | **Concluded 2026-11-25** (ROADMAP P3.3 deterministic checklist, [VESNA-QA-BADGE-COMPAT](verification/VESNA-QA-BADGE-COMPAT.md); research OBS-SYS-116): a deterministic badgeable notification probe (importance HIGH, no group) now exists; Vesna v0.4.84 renders no notification badge (structural: no NotificationListenerService, #200) with work badges intact; Nova’s listener is grantable headlessly and its Work-tab popup shows no notification content even with a live record (Nova-side rendering/popup content remains unconfirmed on Nova’s own surfaces). |
| Performance | No measured data anywhere in the research track. |

**Inventory discipline:** ID references on trunk (in code comments, tests, and
[REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md)) are traceability
pointers. They record correspondence with research items; they are **not** a
claim of full conformance with the research specifications.

## Policies that apply to everything below

- **Independent development policy:** [../clean-room/PROCESS.md](../clean-room/PROCESS.md).
  Authorized public APIs only; original UI, assets, and text; no proprietary
  internals, no decompiled artifacts, no restricted raw evidence. Third-party
  launchers are functional/UX references only.
- **No monetization:** the product has no ads, paid features, or commercial
  tracking of any kind. Research items must not introduce monetization, premium
  tiers, or commercial analytics.
- **Evidence stays on the research branch:** cite pinned paths and IDs (commit
  `ee15ed5…` on `vladon/vesna-research`); never paste observations, raw evidence,
  or Nova-related artifacts into trunk documentation or code.

## What trunk engineering may do with this

1. Use the approved `R-DRAWER-100.4` order-preservation subset and the approved
   `R-DRAWER-101.1` local-filtering subset as the current acceptance baseline
   (already covered by `AppSearchTest` and `DrawerSearchTest`; see
   [PROJECT_STATUS.md](PROJECT_STATUS.md)).
2. Treat the remaining IDs as planning input with the caveats above; convert any
   item into engineering work only through [BACKLOG.md](BACKLOG.md), and resolve
   the matrix weaknesses (missing scenarios, duplication, privacy decision)
   before marking anything accepted.
3. When a research item is picked up, fix the research matrix on the research
   branch first (add the missing scenario, resolve the duplicate), then link the
   trunk work item to the pinned path/ID.
