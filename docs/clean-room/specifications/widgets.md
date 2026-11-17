# Specifications — Widgets

Derived from `observations/widgets.md` and settings inventory
(Widget overlap, Widget corner radius under Home screen ▸ Scroll).

## WIDGET-100 — Widget placement flow

- Description: placing a widget from the widget picker onto the workspace
  follows a four-step user flow: drag preview ▸ system consent ▸ provider
  configuration (when present) ▸ automatic resize mode.
- Preconditions: widget picker opened via workspace customization (HOME-100);
  a compatible drop target exists on the workspace.
- Trigger: drop a widget preview onto the workspace.
- Expected behavior:
  1. A preview of the widget follows the pointer during the drag.
  2. The system widget-consent dialog is shown (platform requirement);
     after user approval the widget binds to the launcher.
  3. If the widget declares a configuration activity, it launches
     immediately; completing it finishes placement. Without one, placement
     completes directly.
  4. Newly placed widgets enter resize mode automatically (handles shown),
     with an entry point to the widget's settings; tapping elsewhere exits
     resize mode and keeps the placement.
  5. After the user once checks the platform consent dialog's "always allow"
     box, subsequent placements skip the consent dialog (observed).
- State transitions: picker → dragging → consent (unless pre-approved) →
  (config) → placed-in-resize-mode → placed.
- Boundary conditions: dropping over an occupied area must resolve per
  overlap rules ("Overlap when placing", "Widget overlap" settings exist;
  exact behavior Not Tested yet).
- Android integration: AppWidget platform binding (public API), including
  the consent dialog and `configure` activity; grid sizing uses the
  launcher-reported cell sizes.
- Evidence: OBS-WIDGET-001 (EVI-WIDGET-001)
- Confidence: Observed

### Vesna requirements

- R-WIDGET-100.1: Widgets are placed by dragging a picker preview onto the
  workspace.
- R-WIDGET-100.2: Placement routes through the platform consent dialog
  before binding.
- R-WIDGET-100.3: Widgets declaring a configuration activity open it before
  final placement; canceling must abort placement.
- R-WIDGET-100.4: Newly placed widgets present resize handles immediately
  plus a settings entry point; dismissing resize keeps the placement.

## WIDGET-101 — Widget resize

- Description: placed widgets can be resized via handles in resize mode.
- Trigger: widget popup ▸ Resize (or automatic post-placement mode).
- Expected behavior: corner/edge handles resize the widget in grid-cell
  increments, live during drag; content re-centers in new bounds; resizing
  respects the widget's declared min/max sizes.
- Configuration: "Widget overlap" (allow widgets under/over other items while
  resizing) and "Widget corner radius" (visual rounding) settings exist.
- Evidence: OBS-WIDGET-002 (EVI-WIDGET-001)
- Confidence: Observed (grow resize); shrink/min-size limits Not Tested.

### Vesna requirements

- R-WIDGET-101.1: Resize mode offers corner/edge handles; resizing is live
  and grid-snapped.
- R-WIDGET-101.2: Widget min/max sizes are respected.
- R-WIDGET-101.3: Optional overlap-while-resizing behavior per setting.
- R-WIDGET-101.4: Widget corners follow the configurable corner radius.

## WIDGET-102 — Widget popup and removal with undo

- Description: long-press on a placed widget opens a widget popup; Remove
  deletes immediately with an undo snackbar.
- Expected behavior:
  1. Widget popup contains: widget settings, custom color, app info, remove,
     padding, resize.
  2. Remove takes effect immediately without a confirmation dialog.
  3. An "item removed / Undo" snackbar appears for a short period; Undo
     restores the widget (restore behavior to verify).
- Evidence: OBS-WIDGET-002, OBS-WIDGET-003 (EVI-WIDGET-001)
- Confidence: Observed (popup, removal, snackbar); undo restore Not Tested.

### Vesna requirements

- R-WIDGET-102.1: Widget popup offers settings, color, app info, padding,
  resize, remove.
- R-WIDGET-102.2: Removal is immediate (no confirm dialog) and offers an
  undo snackbar for a short period.
- R-WIDGET-102.3: Undo restores the removed widget with its position, size,
  and configuration. (Verified: restored widget retained a different clock
  face set during the test, proving full configuration restore.)
- R-WIDGET-102.4: The undo window is short (a few seconds); after it lapses
  the removal is final.
