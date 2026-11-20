# Research Summary — for the Vesna team

One page. Everything else this references lives in this repository:
`observations/` (raw), `design-lessons.md` (distilled), `specifications/`
(normative design), `implementation-tickets.md` (buildable tickets),
`acceptance-tests/scenarios.md` (proofs), `COVERAGE_MATRIX.md` (traceability).

## What was researched

Nova Launcher 8.9.9 (free, then Prime) on a Pixel 9a, black-box, over two
days: the card system (conditions, provider cards, custom cards, schedules),
drawer groups (smart folders, tabs), the builder, gestures, widgets, dock,
and the settings surface. ~140 evidence captures; every finding backed by a
screenshot + an accessibility dump; every state transition exercised, not
assumed.

## The render model (fully characterized)

Cards render ONLY in the search surface (typed query + Activate + granted
permission). The plain drawer never shows them. Conditions do not gate
anything (see the gaps below). Custom cards match by NAME only. The Calendar
and Map-locations cards never render at all. The AccuWeather card renders
live data and refreshes across sessions; its timestamp label lags within a
session.

## The four quality gaps (all Observed, evidence-backed)

1. **Conditions are decorative.** Expired, never-satisfied, and
   later-satisfied conditions all behave identically at render time; expiry
   never removes a condition; the promised drawer surface never shows a
   card. Nova's condition copy promises behavior that does not exist.
2. **The manual-location promise is unfulfilled.** "Set a manual location in
   Nova Settings" — four search queries and every surface checked: it does
   not exist. The only location editor belongs to a different card
   (Map locations) and was nearly undiscoverable.
3. **Smart folders ship dark-by-default.** The master toggle plus populated
   groups still hid all seven folders behind per-item ⋮ ▸ Enable menus.
   Enabling renders immediately and persists across restarts — a
   discoverability failure, not dead code.
4. **Builder save gating without feedback.** Save is grey with no reason for
   a name-only card, and that name-only card silently persisted through the
   discard path anyway. Save semantics are neither explained nor total.

## What works in Nova (preserve these patterns in Vesna)

- Custom-card name-matching search render (star marker + action chips + gear)
- The two-stage geocoded location editor (text → resolve → display name +
  map preview; unresolvable text honestly leaves OK disabled)
- Per-folder Enable state persisting across restarts
- Widget consent pre-approval honored on later placements
- Widget undo restoring position, size, AND configuration

## Deliverables in this repo

1. `design-lessons.md` — the four gaps + cross-cutting lessons + per-gap
   fix guidance (implementation directives).
2. `specifications/cards.md` — the normative render model table +
   CARDS-101..105 requirements (connect gate, provider search render,
   custom cards, builder UX, scheduled locations).
3. `specifications/app-drawer.md` — DRAWER-106 groups requirements (master
   toggle sufficiency, first-run choice, empty-folder rule, persistence,
   single-editor surface) + the grid label-order rule.
4. `specifications/dock.md`, `specifications/widgets.md` — expanded with
   persistence, gesture-ownership, dependent-option-explanation, consent
   pre-approval, and resize-affordance requirements.
5. `implementation-tickets.md` — 19 tickets (NT-CARDS-01..12, NT-DRAWER-01..03,
   NT-DOCK-01..03), each with its Nova counter-evidence and the acceptance
   scenario that proves the fix.
6. `acceptance-tests/scenarios.md` — the runnable Given/When/Then proofs.

## Still open

- Device-side: none. The observable behavior matrix is complete.
- Code-side: WHY Nova's render paths for these features are dead —
  unanswerable black-box; Vesna controls its own implementation.
- The tickets in `implementation-tickets.md` are ready to move into the
  implementation backlog.

## How to re-run or extend

`RESEARCH_PLAN.md` (method) + `TEST_ENVIRONMENTS.md` (devices) +
`observations/_TEMPLATE.md` (entry format). The session recipe for the
device work: the managed skills `vesna-nova-blackbox-research` and
`vesna-nova-cards-folders-marathon`.
