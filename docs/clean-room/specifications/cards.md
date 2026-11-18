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
