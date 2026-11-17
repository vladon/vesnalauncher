# Specifications — Settings Infrastructure

Derived from `observations/settings.md` (OBS-SETT-001..011) and
`observations/round2.md` (OBS-SETT-110, OBS-SYS-110).

## SETT-110 — Settings search with section deep-linking

- Description: the main settings page has a pinned search field; queries
  match setting titles AND subtitles and render as breadcrumb cards that
  deep-link into the section.
- Trigger: typing in the settings search field.
- Expected behavior (observed with query "dock"):
  1. Results appear as cards: "Home screen ▸ Dock — Icon layout, background,
     pages" and "Home screen ▸ Page indicator — Displays above the dock…"
     (i.e. title + subtitle text both participate in matching).
  2. Tapping a result opens the target section pre-scrolled to the matching
     row, visually highlighted.
- Boundary conditions: multi-word queries, prime-locked results in search —
  Not Tested.
- Evidence: OBS-SETT-110 (EVI-SETT-003)
- Confidence: Observed

### Vesna requirements

- R-SETT-110.1: Settings offer a search field pinned to the settings page.
- R-SETT-110.2: Search matches setting titles and descriptive subtitles,
  case-insensitively.
- R-SETT-110.3: Results show the owning section as a breadcrumb and
  deep-link to the highlighted setting.

## SETT-111 — Prime gating interaction (reference behavior)

- Description (Nova reference, recorded for parity decisions): tapping a
  Prime-locked settings row opens the full "Manage Subscription" page (with
  plan cards and trial buttons), not a modal dialog. The same page backs
  Nova AI (Plus) and Quick start.
- Evidence: OBS-SYS-110 (EVI-SYS-002), OBS-SETT-009 (EVI-SETT-009),
  OBS-BACKUP-110 (EVI-BACKUP-003)
- Confidence: Observed

### Vesna requirements

- R-SETT-111.1: Vesna has NO paid tier; every settings row is functional
  (independent product decision — Nova's gating recorded as reference
  only).
