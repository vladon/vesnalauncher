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

## SETT-112 — Settings state and navigation hygiene (verified)

- Research-verified on Nova 8.9.9 Prime across the 2026-11-19/20 sessions
  (the recurring navigation failures): Nova's settings app persists the
  page, scroll position, search query text, AND field focus across
  navigation AND force-stops — reopening landed on a stale search page
  holding garbage, focused, with the keyboard up, repeatedly swallowing
  navigation input. A deterministic deep link to the settings ACTIVITY
  (not the named page) was the working escape.

### Vesna requirements

- R-SETT-112.1: Opening settings lands on the ROOT page (or a defined
  default) with NO search text, NO field focus, and NO keyboard — saved
  page/scroll/search state restores only through explicit navigation, never
  on cold open (Nova counter-example recorded: a stale focused search field
  survived force-stops and swallowed an entire session's navigation).
- R-SETT-112.2: Every named settings page has a deterministic deep link
  that lands on that page from any prior state.
- R-SETT-112.3: With the search field focused, BACK dismisses the keyboard
  first and never leaks keystrokes into other surfaces.
- R-SETT-112.4: Unsaved-changes dialogs disable nothing silently: both
  options are always actionable, and the dialog states what would be lost
  (Nova's Discard/Save pattern, verified working — preserve it).
