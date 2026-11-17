# Vesna Launcher — Clean-Room Research Plan

This document defines how the Vesna Launcher specification team observes Nova
Launcher as a black box and turns observations into implementation-independent
specifications. It complements [PROCESS.md](PROCESS.md) (independent development
policy).

## 1. Goal

Produce a comprehensive, verifiable, implementation-independent behavioral
specification of a modern Android launcher, based on authorized black-box
observation of Nova Launcher, sufficient to independently design and implement
Vesna Launcher.

## 2. Roles

Research is performed by the specification team. Where multiple agents are
available they may take specialized roles (coordinator, per-area researchers,
QA reviewer). This session is performed by one agent sequentially unless
delegation is explicitly used; work attributed to a role is work performed by
that agent under that role's checklist.

## 3. Legal and ethical boundaries (summary)

Full rules: [PROCESS.md](PROCESS.md). Operational rules for this plan:

- MAY: install and use Nova Launcher; interact through its user-facing UI;
  change settings; record observable behavior; consult official Android
  documentation.
- MUST NOT: decompile, disassemble, inspect DEX/native code/resources of Nova
  Launcher; use runtime instrumentation to read Nova's private state; copy
  Nova assets, strings verbatim, or implementation structures into the
  repository.
- Nova screenshots and other raw captures stay in a restricted local evidence
  store OUTSIDE the repository. Documents reference them by evidence ID only.
- Specifications describe *what the user sees*, never *how Nova implements it*.

## 4. Methodology

For every investigated feature:

1. **Initial state** — record the launcher state before the test (relevant
   settings at their current values).
2. **Action** — perform one controlled user action (tap, swipe, long-press,
   settings change) using real coordinates or accessibility nodes.
3. **Observation** — capture the result (screenshot and/or accessibility
   tree) and record what changed.
4. **Variations** — repeat with alternative values (other options, extremes,
   repeated actions).
5. **Boundaries and errors** — test invalid/unusual inputs and error paths
   where reachable through the UI.
6. **Interactions** — test combinations with dependent features.
7. **Persistence** — verify after launcher restart and, where relevant, after
   device reboot.
8. **Evidence** — assign an evidence ID; store captures under the evidence ID.
9. **Confidence** — mark Observed / Partially Observed / Inferred / Not Tested.

Do not infer functionality from labels alone. Run the control and look.

## 5. Evidence conventions

- Evidence IDs: `EVI-<AREA>-<NNN>` (e.g. `EVI-ENV-001`, `EVI-HOME-001`).
- Captures: PNG screenshots and uiautomator XML dumps, stored in the local
  restricted store, one sub-directory per evidence ID.
- Observation files under `observations/` cite evidence IDs; they record
  navigation path, initial state, action, result, and open questions.
- No image, video, or verbatim Nova UI text is committed to the repository.

## 6. Documentation flow

```
observation (raw, per area) -> specification (approved behavior) -> acceptance test (Given/When/Then)
        |                              |                              |
   observations/*.md             specifications/*.md          acceptance-tests/scenarios.md
        └──────────────> COVERAGE_MATRIX.md tracks all three per feature ID <──────────────┘
```

- Observations are raw and may contain uncertainty notes.
- Specifications contain only reviewed, implementation-independent behavior.
- A feature is *Completed* in the coverage matrix only when observation,
  specification, and acceptance criteria all have evidence.

## 7. Feature ID scheme

`<AREA>-<NNN>` with stable numbering; IDs are never reused for a different
behavior. Areas: HOME, DRAWER, DOCK, FOLDER, WIDGET, GESTURE, APPPEAR
(appearance), SETT (settings infrastructure), BACKUP, SYS (system
integration), NF (nonfunctional).

## 8. Phase plan

| Phase | Scope | Exit criterion |
|---|---|---|
| 1 | Environment, policies, taxonomy, templates | Environment recorded; inventory + matrix exist |
| 2 | Home screen, app drawer | Specs with observed evidence |
| 3 | Dock, folders, widgets | Specs with observed evidence |
| 4 | Gestures, appearance, settings | Specs with observed evidence |
| 5 | Persistence, backup/restore, system integration | Specs with observed evidence |
| 6 | Gap analysis, re-testing, completeness report | Coverage reviewed; open questions triaged |

## 9. Session protocol

- Work in small, reviewable documentation commits on the trunk
  (Conventional Commits); never push or merge without authorization.
- Never modify Android application source (`app/`) from this session.
- Keep the coverage matrix current so another session can resume from the
  last verified observation.
- Report blockers precisely (e.g. licensing dialogs, unavailable features).
- **Tap coordinates must come only from the most recent capture of the
  CURRENT screen.** Reusing coordinates from an earlier layout risks
  silently flipping a setting the observations must not corrupt.
- **Capture helper returns the exact output path** (`P=$(cap.sh …)`); never
  hand-assemble capture filenames/timestamps.
- **Device hygiene:** any host-side bridging (e.g. the Play-download proxy)
  is temporary; see the session-close checklist in TEST_ENVIRONMENTS.md.
