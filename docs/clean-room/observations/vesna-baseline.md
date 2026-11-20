# Observations — Vesna Launcher 0.4.3 baseline (the comparison target)

Installed on the research Pixel (55021JEBF03248) from the GitHub release
`v0.4.3` (2026-11-20). Purpose: the feature-state baseline against the Nova
findings and the Vesna specifications. Vesna runs as a foreground activity;
Nova remains the system HOME (comparison-only install).

## OBS-VESNA-001 — v0.4.3 feature baseline

- Date / Env: 2026-11-20 17:33 / ENV-1 (the Pixel, Vesna 0.4.3 over Nova)
- UI surface (single-container design): a workspace/app-list page with the
  search pill (grid icon + more button) and a 4-icon dock; a popup menu
  (Vesna settings / Set wallpaper / Default launcher / Add home page); a
  ONE-PAGE settings screen: Default launcher button, Theme
  (System/Light/Dark), Show app names toggle, MIT license + GitHub link.
- Feature state vs the researched areas:
  - Card system (conditions, schedules, provider cards, builder): NOT YET
    PRESENT.
  - Drawer groups / smart folders: NOT YET PRESENT.
  - Settings search field: ABSENT (R-SETT-112.1 satisfied trivially — the
    state trap cannot occur).
  - Widget placement: not yet observed in this pass.
- Settings hygiene: cold open = the single root page, no search field, no
  focus, no keyboard — the SETT-112.1 target is met by design at this
  scope.
- The spec requirements (CARDS-100..105, DRAWER-106) are the design target
  for future releases; 0.4.3 predates them.
- Evidence: EVI-CARDS-001 (vesna-first-launch, vesna-explore-1,
  vesna-settings, vesna-settings-btm)
- Confidence: Observed

## Comparison table (Nova 8.9.9 findings vs Vesna 0.4.3 vs the spec)

| Researched area | Nova 8.9.9 | Vesna 0.4.3 | Spec requirement |
|---|---|---|---|
| Card conditions render gate | decorative (all states render; expiry silent) | n/a — no cards | enforced engine + loud expiry (NT-CARDS-01/02) |
| Met-later surface promise | never surfaced | n/a | one-refresh enqueue (NT-CARDS-03) |
| Manual-location fallback | promised, nonexistent | n/a — no cards | copy-gated surfaces (NT-CARDS-04) |
| Scheduled location go-live | never rendered at go-live | n/a | go-live surfacing (NT-CARDS-03/105) |
| Calendar connect gate | runtime grant ignored | n/a — no calendar card | one-step enable (NT-CARDS-06) |
| Smart folders | dark-by-default (per-item Enable) | n/a — no groups | master-toggle sufficiency + first-run choice (NT-DRAWER-01/02) |
| Empty generated folders | bare unlabeled icons | n/a | not rendered (NT-DRAWER-03) |
| Builder save gating | grey Save, silent persistence | n/a — no builder | live reasons + total semantics (NT-CARDS-10/11) |
| Hidden defaults | Activate ON + pre-attached dead condition | n/a | no hidden defaults (NT-CARDS-12) |
| Settings state hygiene | stale focused search survives force-stop | clean single page ✓ | R-SETT-112.1..4 (already met at this scope) |
