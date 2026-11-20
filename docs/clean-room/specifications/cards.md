# Specifications — Cards (Nova Now drawer cards)

Derived from `observations/round2.md` (OBS-CARDS-110, OBS-BACKUP-005) and the
Cards settings inventory (OBS-SETT-006).

## CARDS-100 — Condition-triggered content cards in the drawer

- Description: Nova 8.9 provides composable content "Cards" (media player,
  weather, games, …) whose visibility in the app drawer is controlled by
  user-defined condition sets ("Activate in Nova Now").
- Preconditions: none for inventory; conditions may require runtime
  permissions (Location for Wi-Fi condition, Bluetooth for Bluetooth
  condition).
- Configuration (per card, "Card details" page with Save + overflow):
  1. Condition adders: Day/time; Wi-Fi network; When weather changes;
     Bluetooth connection.
  2. Semantics (stated on the page): a card shows in the app drawer when
     ALL configured conditions are met.
  3. Card list page ("Your cards") with per-card configure gear and a (+)
     add-card FAB; default set includes Media controls, AccuWeather,
     Games.
- Boundary conditions: behavior of empty condition sets, multiple
  simultaneous cards, and card ordering Not Tested.
- Interactions: cards surface inside the drawer (DRAWER-100 area) and the
  Nova Now overlay (HOME-105); Style tab exposes per-surface colors for
  "Nova Now".
- Persistence: card configuration persists across restart (assumed from
  settings architecture; not directly verified).
- Evidence: OBS-CARDS-110..113 (EVI-CARDS-001), OBS-CARDS-115 (r7 captures)
- Confidence: Observed (config, condition UI/persistence, permission
  gating model, render with granted permission + live data)

### Vesna requirements

- R-CARDS-100.1: (Independent Vesna decision — reference recorded) Vesna
  may implement optional, condition-gated content cards surfaced in the
  drawer; Nova's model is recorded as reference: composable conditions
  (time, network, weather-change, bluetooth) combined with AND semantics,
  permission-gated where needed.
- R-CARDS-100.2: A card manager lists existing cards with per-card
  configuration and an add-card affordance.
- R-CARDS-100.3 (verified gating model): a card renders only when its
  condition is met AND its data permission is granted AND the render
  surface is the Nova Now SEARCH results (typed query) — an active
  condition with granted permissions does NOT render the card in the plain
  drawer browse view (all variants observed 2026-11-18/19).
- R-CARDS-100.4: Cards advertise their required permissions up front (Nova
  lists them in the onboarding sheet with per-card toggles) and offer a
  manual-data alternative (e.g. manual location) INSTEAD of a mandatory
  runtime permission. Nova counter-example recorded: its sheet advertises a
  manual-location fallback that does not exist anywhere in its settings
  (OBS-CARDS-113) — Vesna's advertised fallbacks must be reachable
  (verifiable via settings search).
- R-CARDS-100.5: Once the data permission is granted, the onboarding
  toggles persist (no revert) and the condition-met card renders with live
  data (verified: real district-level weather "Khamovniki, Moscow").

## The verified render model (normative)

Research-verified on Nova 8.9.9 Prime across every observable state
(OBS-CARDS-116..119; the complete matrix). Vesna's card system MUST satisfy
this matrix — it is both the compatibility reference and the floor for
Vesna's own, stricter behavior.

| Card state | Search surface | Plain drawer |
|---|---|---|
| Condition met + permission granted | renders (name-matching query) | never renders |
| Condition expired (window past) | renders (same as met) | never renders |
| Condition never-met | renders (same as met) | never renders |
| Condition met-later transition | renders | never renders |
| Scheduled location active at set time | never renders (Nova defect) | never renders |
| No location / no data | empty state + connect affordance | never renders |

- Nova defects recorded for avoidance: conditions are decorative in every
  tested state; the scheduled-location card never renders even at its exact
  activation time; the Calendar card ignores a granted runtime permission
  until an in-app connect flow runs.

### CARDS-101 — Provider card connect gate

- Vesna requirements:
  - R-CARDS-101.1: Granting the card's runtime permission MUST enable the
    card immediately — there is no second, hidden in-app connect step
    (Nova counter-example: Calendar card ignores a granted
    READ/WRITE_CALENDAR until its own "Connect" flow runs;
    OBS-CARDS-118).
  - R-CARDS-101.2: The card's empty state states the exact missing
    prerequisite and offers the action that satisfies it in one step.
- Evidence: OBS-CARDS-118 (EVI-CARDS-001 overlay-cal-*).

### CARDS-102 — Provider card search render

- Vesna requirements:
  - R-CARDS-102.1: A provider card with data renders for content-matching
    queries (a locations card for place queries; weather for place/weather
    queries) — Nova counter-example: its Map locations card never renders
    for its own name, its saved location, or partials, with or without an
    active schedule (OBS-CARDS-119 schedule matrix).
  - R-CARDS-102.2: The card render refreshes with current data at each
    surface entry; its visible timestamp must never contradict the data
    shown.
- Evidence: OBS-CARDS-119 (render-search-*, moscow-* captures).

### CARDS-103 — Custom cards (user-authored)

- Vesna requirements:
  - R-CARDS-103.1: Custom cards render in the search surface for
    name-matching queries, presented with a distinct marker and their
    pinned action chips (verified pattern).
  - R-CARDS-103.2: Queries matching only an action's target app surface
    the target app, not the card (matching is name-based — verified).
  - R-CARDS-103.3: Custom cards persist across restarts and are deletable
    from their own details overflow without a confirmation dialog.
- Evidence: OBS-CARDS-119 (VesnaRender create/render/delete cycle,
  re-verified 2026-11-20).

### CARDS-104 — Builder UX

- Vesna requirements:
  - R-CARDS-104.1: A disabled primary action carries its live reason
    inline ("Add at least one action to save"); the reason list updates as
    requirements are met.
  - R-CARDS-104.2: Save semantics are total — commit or provably discard;
    the discard path must never silently create (Nova counter-example: a
    name-only card persisted through its discard-dialog path;
    OBS-CARDS-119).
  - R-CARDS-104.3: New cards start with NO hidden defaults (Nova ships
    Activate-ON plus a pre-attached never-satisfiable Wi-Fi condition;
    OBS-CARDS-119).
- Evidence: OBS-CARDS-119 (builder-open, action-added, rebuild2 captures).

### CARDS-105 — Scheduled locations

- Vesna requirements:
  - R-CARDS-105.1: Schedules are time+day rows; creating one snapshots a
    chosen time (verified pattern) and the location activates at that
    time on those days.
  - R-CARDS-105.2: An active schedule MUST surface the card on the
    surfaces the schedule copy promises, at the activation moment — Nova
    counter-example: no render at go-live on any surface (OBS-CARDS-119
    schedule matrix).
  - R-CARDS-105.3: Deletion: row-level "Delete location" with an undo
    snackbar (verified pattern).
- Evidence: OBS-CARDS-119 (sched-editor, chip-thu-selected,
  moscow-committed, moscow-deleted captures).
