# Observations — Widgets (ENV-1, Nova 8.9.9 free)

Evidence under `EVI-WIDGET-001`. Widget placed during test: system Clock
"Analog" (declared 2×2), resized to ~2×3, then removed.

## OBS-WIDGET-001 — Widget placement flow

- Date / Env: 2026-11-17 / ENV-1
- Action: long-press empty workspace ▸ drag widgets sheet up to full-screen
  picker ▸ expand Clock group ▸ drag the "Analog 2 × 2" preview onto an empty
  workspace cell (`input draganddrop`, 1.5 s).
- Observed, in order:
  1. While dragging from the picker, a floating widget preview follows the
     pointer over the workspace.
  2. On drop, the **system widget-creation consent dialog** appears
     ("Create widget and allow access?" with an application/widget
     attribution and an optional "Always allow …" checkbox; Cancel/Create).
     This is the platform's widget-binding consent (Android 11+), not
     launcher UI.
  3. After Create, the widget provider's **configuration activity** opened
     ("Select a Clock Face": Scallop/Numeral/Ring…). Choosing a face
     completed placement.
  4. Immediately after placement the widget enters **resize mode
     automatically**: dashed selection frame with corner/edge handles and an
     edit (pencil) affordance, plus a toast "Tap to change widget settings".
  5. Tapping elsewhere exits resize mode; the widget remains placed.
- Evidence: EVI-WIDGET-001 (picker-full, clock-group, widget-drop,
  widget-placed, widget-final, after-dismiss)
- Confidence: Observed

## OBS-WIDGET-002 — Widget resize

- Action: long-press the placed widget ▸ popup ▸ Resize; drag the bottom
  handle down one row.
- Observed: resize frame expands (~2×2 → ~2×3); the widget content
  re-centers within the new bounds; resizing happens live during the drag.
- Widget popup menu (long-press): Widget settings, Custom color, App info,
  Remove, Padding, Resize.
- Evidence: EVI-WIDGET-001 (widget-popup, resize-mode, resized)
- Confidence: Observed

## OBS-WIDGET-003 — Widget removal with undo

- Action: widget popup ▸ Remove.
- Observed: the widget is removed immediately (no confirmation dialog) and a
  snackbar appears at the bottom of the workspace: "Item removed" with an
  "Undo" button (consistent with the "Show undo bar" setting, on by default).
  Undo was NOT exercised in this pass (to verify: restores widget, position,
  and size).
- Evidence: EVI-WIDGET-001 (popup-again, removed)
- Confidence: Observed (removal); undo restore Not Tested

## OBS-WIDGET-004 — Wallpaper appearance anomaly during session

- Note: workspace captures from the first half of the session show a
  different wallpaper than captures after the widget-picker interactions
  (Material-style vs abstract petals). Cause not yet determined (candidates:
  wallpaper scrolling/zoom effect during drags, a wallpaper change by another
  app, or Android 17 wallpaper behavior). No launcher action was taken that
  intentionally changes the wallpaper.
- Evidence: compare EVI-HOME-003 workspace captures with EVI-WIDGET-001
  after-dismiss
- Confidence: Unexplained anomaly — open question, needs a controlled test.
