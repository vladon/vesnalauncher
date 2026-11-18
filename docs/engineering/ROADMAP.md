# Engineering Roadmap

Baseline: `888f557` (0.4.1). Current verification state and known gaps are
documented in [PROJECT_STATUS.md](PROJECT_STATUS.md) and
[REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md). The authoritative
item list with full descriptions is [BACKLOG.md](BACKLOG.md); this file orders
it by dependency. Item IDs below link into the backlog.

Guiding constraints from the baseline:

- Research requirements beyond the owner-approved QA baseline (order-preservation
  subset of `R-DRAWER-100.4` + local-filtering subset of `R-DRAWER-101.1`) are
  **gated**; roadmap phases consume them only after the research matrix is
  repaired ([RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#known-weaknesses-in-the-research-matrix)).
- No performance work is scheduled yet: no measurements exist, so there is no
  baseline to optimize against (documented in [PROJECT_STATUS.md](PROJECT_STATUS.md#quality-status-at-the-baseline)).
- ADR candidates (page identity/schema migration, widget host cleanup,
  backup/privacy) precede the features that depend on them; see
  [BACKLOG.md](BACKLOG.md#adr-candidates).
- Nothing here implies a product or privacy decision has been made; where one is
  required, it is stated as an open gate.

## P0 — close the riskiest verification gaps (no new features)

Everything in P0 verifies what already ships, on the **existing dedicated API 35
AVD** (no new device work required to start).

1. **[VESNA-QA-002](BACKLOG.md#vesna-qa-002-home-and-launch-return-verification-done) — HOME registration/selection and return-from-app.**
   Status: **done** — recorded run 2026-11-18 on the dedicated API 35 AVD
   ([verification/VESNA-QA-002.md](verification/VESNA-QA-002.md)). A Vesna
   platform baseline item; carries **no approved research ID**. Scope was:
   verify Vesna registers as HOME, can be selected as default, and the user
   returns to the workspace from a launched app; restore the previous HOME
   holder during cleanup; removing the stock launcher is not required. The
   product baseline acceptance (what "HOME works" means for 0.4.1) was
   identified first and is recorded in the verification doc, since no research
   scenario covers this today.
2. **Package removal and temporary unavailability** — uninstall and update/disable
   paths: stale items leave workspace, dock, drawer, and folders
   ([BACKLOG.md](BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial)).
   Status: **in-progress** — drawer side accepted for review as VESNA-QA-003A
   (recorded 2026-11-19,
   [verification/VESNA-QA-003.md](verification/VESNA-QA-003.md)). The
   workspace/dock/folder and managed-profile branches remain open: the
   blocking defect (drawer drag-pin no-op) is fixed in **PR #15**
   (independently verified on API 35 — seeded baseline, pinned cell 14 and
   dock 3 persist); placed-item QA runs after integration on the fixed
   revision.
3. **Migrations and restart** — Room 1→2→3 migration on representative databases,
   layout intact after process death and device reboot
   ([BACKLOG.md](BACKLOG.md#vesna-qa-004-migration-and-restart-verification-proposed)).

**Exit criteria for P0:** all three QA items have recorded runs (device, build,
result) linked from [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).

## P1 — structural groundwork and the strongest product candidates

Depends on: P0 green (stable verification base), and research-matrix fix-ups for
any gated ID consumed below.

1. **[ENG-101](BACKLOG.md#eng-101-workspace-page-identities-and-schema-migration): workspace page identities + schema migration.**
   The single fixed 4×5 page is the platform for page management via drag
   (research candidate `HOME-104`) and for widgets. The ADR fixes the page
   identity model and the Room schema version — the version number is **not**
   pre-committed before the ADR.
2. **[ENG-102](BACKLOG.md#eng-102-widget-host--lifecycle-only): widget host — lifecycle only.**
   Requires the **widget host cleanup ADR**; widgets occupy grid cells, so it
   builds on ENG-101's identity model. Config-cancel behavior stays out until
   the research `WIDGET-100` gap is resolved.
3. **[ENG-103](BACKLOG.md#eng-103-folder-pagination-and-drag-into-verification): folder pagination and drag-into verification.**
   Pagination is the implementation work; drag-into-folder already exists in
   source (drops onto folder cells route through `addToFolder`) and needs
   on-device verification only. Research-gated: `FOLDER-100`/`FOLDER-101`
   scenarios are missing. No hard dependency on page identities is assumed; if
   the ENG-101 ADR work shows one, it gets recorded in the item with evidence.
4. **[ENG-104](BACKLOG.md#eng-104-settings-search-and-structured-preferences): settings search/deep-linking and structured preferences.**
   Pairs the missing `SETT-110` scenario (research) with a trunk settings
   structure to hang it on. Settings reset is tracked under
   [ENG-106](BACKLOG.md#eng-106-backuprestore-policy) (research `BACKUP-101`).
5. **Product-candidate clusters**, each gated on research scenario authoring and
   explicit approval — listed as candidates, not commitments
   ([CAND-201..207 in BACKLOG.md](BACKLOG.md#candidate-items-unapproved)):
   appearance/theming, essential gestures, icon packs, extended dock/grid/folder
   capacity.

## P2 — product decisions released from the gate

These are decision-blocked, not code-blocked. The decisions are open; this
roadmap only records what unblocks them.

1. **[ENG-105](BACKLOG.md#eng-105-cards-surface): cards surface (`CARDS-100`,
   condition-triggered content).** Blocked on the product/privacy decision —
   location/manual-fallback handling is the open privacy question; notification
   indicators are a separate item; the `DRAWER-105`/`R-DRAWER-103.1` ID reuse
   must be repaired first.
2. **[ENG-106](BACKLOG.md#eng-106-backuprestore-policy): backup/restore policy.**
   The backup/privacy ADR decides whether platform backup stays enabled and
   defines its scope; an explicit backup/restore feature is one option, not a
   given. Reset (research `BACKUP-101`, partial coverage) lands here too.
3. **Gesture settings** (drawer-open configuration): only after live binding-value
   validation lands on the research branch.

## P3 — quality and polish (unblocked but unscheduled)

1. **Performance baseline** — cold start, drawer scroll, drag latency measured
   on the reference AVD; first numbers turn "fast" from an aspiration into a
   regression guard.
2. **API 36.1 verification** — close the unverified-platform gap from
   [PROJECT_STATUS.md](PROJECT_STATUS.md#instrumentation-verification-record).
   Retained here as P3 coverage, not as a P0 gate.
3. **Badge compatibility** — re-run the inconclusive research probe as a
   deterministic checklist once a work-profile AVD is part of the QA inventory.
4. **Accessibility, foldables, work-profile UX** — larger-surface candidates
   ([CAND-205..207](BACKLOG.md#candidate-items-unapproved)); unscheduled and
   research-gated like everything else without approval.

## Dependency sketch

```mermaid
flowchart TD
    QA2["VESNA-QA-002<br/>HOME + return (P0)"] --> QA3["VESNA-QA-003<br/>package lifecycle (P0)"]
    QA2 --> AVD["existing dedicated<br/>API 35 AVD inventory"]
    QA4["VESNA-QA-004<br/>migrations + restart (P0)"] --> exit["P0 exit:<br/>recorded runs + traceability"]
    QA3 --> exit
    AVD --> exit
    exit --> pageADR["ADR-PAGE-1:<br/>page identity + schema version"]
    pageADR --> pages["ENG-101: page IDs + migration"]
    pages --> widgets["ENG-102: widget host lifecycle"]
    widgetADR["ADR-WIDGET-1:<br/>widget host cleanup"] --> widgets
    folders["ENG-103: folder pagination/drag<br/>(research-gated)"]
    sett["ENG-104: settings search/deep-linking<br/>(research-gated SETT-110)"]
    researchfix["ENG-107: research-matrix<br/>fix-ups (research branch)"]
    researchfix -.->|scenarios for| folders
    researchfix -.->|scenario for| sett
    decision["product + privacy decision<br/>(open)"] --> cards["ENG-105: cards"]
    decision --> backupADR["ADR-BACKUP-1:<br/>backup scope + privacy"]
    backupADR --> backup["ENG-106: backup/reset policy"]
    gestureval["research: gesture binding<br/>validation (pending)"] --> gestures["P2: gesture settings"]
    exit --> perf["P3: performance baseline"]
    perf --> polish["P3: API 36.1, badges"]
```

Sequencing rules worth restating: verification (P0) precedes features (P1);
ADR candidates precede the feature that depends on them; research-matrix repairs
happen on the research branch **before** trunk consumes a gated ID; and every
shipped change updates [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md)
in the same commit.
