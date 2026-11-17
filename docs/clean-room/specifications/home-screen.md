# Specifications — Home Screen

Approved implementation-independent behavior for Vesna Launcher, derived from
observations in `observations/home-screen.md`. Each feature lists evidence and
confidence; Vesna requirements are numbered and testable.

## HOME-100 — Workspace customization mode

- Description: long-pressing an empty area of the workspace enters a
  customization state that surfaces workspace-level actions.
- Preconditions: launcher showing the workspace; user touches an empty cell
  or wallpaper area; hold exceeds the long-press threshold.
- Trigger: long-press on empty workspace area.
- Expected behavior:
  1. The launcher presents workspace customization affordances without
     leaving the home screen: a widgets entry point, a wallpapers entry
     point, and a launcher-settings entry point.
  2. The dock and other persistent chrome are visually de-emphasized or
     hidden while customizing.
  3. An explicit affordance (home control) exits customization and returns
     to the normal workspace.
  4. The system BACK control collapses any open panel first; only an
     explicit exit control (or its equivalent) leaves customization mode.
- State transitions: normal → customizing → (panel open/closed) → normal.
- Configuration parameters: none observed for entry; panel contents follow
  installed widget providers.
- Boundary conditions: long-press on an icon does NOT trigger customization
  mode (it triggers the item popup). Long-press during an active drag must
  not re-enter customization.
- Interactions: widget placement originates from the widgets panel
  (WIDGET-100); wallpaper behavior interacts with APPPEAR-008.
- Persistence: none (transient).
- Android integration: widget list is provided by the platform widget
  provider query (public API); wallpaper picking uses the system wallpaper
  UI.
- Evidence: OBS-HOME-001, OBS-HOME-002 (EVI-HOME-001)
- Confidence: Observed

### Vesna requirements

- R-HOME-100.1: Long-press (≥ system long-press timeout) on an empty
  workspace area opens workspace customization.
- R-HOME-100.2: Customization exposes: Widgets, Wallpapers, Settings.
- R-HOME-100.3: BACK first closes an open customization panel; customization
  exits only via its explicit exit control.
- R-HOME-100.4: Dock/chrome are hidden or de-emphasized while customizing.

## HOME-101 — Widget picker (grouped, searchable)

- Description: the widgets entry point presents all installed widgets grouped
  by provider app, searchable, expandable per app, with size labels.
- Expected behavior: grouped list with per-app widget/shortcut counts; a
  search field filters by app/widget name; expanding an app reveals previews
  with grid sizes; items can be dragged/placed onto the workspace (placement
  behavior specified under WIDGET-100).
- Boundary conditions: apps without widgets do not appear; an app with only
  launcher-provided shortcuts appears under its group.
- Evidence: OBS-HOME-002 (EVI-HOME-001)
- Confidence: Observed (list/search); placement Not Tested yet.

### Vesna requirements

- R-HOME-101.1: Widget picker lists widgets grouped by app with counts.
- R-HOME-101.2: Picker offers text search over providers/widgets.
- R-HOME-101.3: Each widget entry shows its placement size preview.

## HOME-102 — Item popup menu on workspace icons

- Description: long-pressing an app icon opens a contextual popup anchored to
  the icon.
- Expected behavior:
  1. Header action row with close, per-app widgets, app info, and edit
     affordances.
  2. App-provided launcher shortcuts listed; each is launchable by tap.
  3. Uninstall/Store entries follow per-surface visibility configuration
     (see SETT-007b: default Home configuration hides them).
  4. Tapping outside the popup dismisses it.
- Boundary conditions: apps with no shortcuts show no shortcut rows.
- Interactions: shortcut rows are draggable to place shortcuts on the
  workspace (observed drag affordance; placement behavior Not Tested yet).
- Evidence: OBS-HOME-003 (EVI-HOME-002)
- Confidence: Observed

### Vesna requirements

- R-HOME-102.1: Long-press on a workspace app icon opens a popup anchored to
  the icon.
- R-HOME-102.2: Popup shows launcher shortcuts (tap to launch).
- R-HOME-102.3: Popup offers per-app widgets, app info, and edit entry
  points, plus close.
- R-HOME-102.4: Outside tap dismisses the popup.
- R-HOME-102.5: Popup item visibility per surface (home vs drawer) is
  user-configurable.

## HOME-103 — Drag engagement requires stationary hold

- Description: dragging a workspace item requires touching the item and
  holding stationary until drag mode engages; movement during the hold window
  cancels drag intent (the gesture is treated as scrolling/no-op).
- Expected behavior: press-and-hold (≈ long-press timeout) engages drag; then
  movement carries the item; release drops it per drop-target rules.
- Boundary conditions: immediate-move gestures do not engage drag; drag from
  a popup shortcut row uses its drag affordance.
- Evidence: OBS-HOME-004 (EVI-FOLDER-001)
- Confidence: Observed

### Vesna requirements

- R-HOME-103.1: Workspace drag starts only after a stationary hold exceeding
  the long-press timeout on an item.
- R-HOME-103.2: Movement before the hold threshold must not initiate a drag.
